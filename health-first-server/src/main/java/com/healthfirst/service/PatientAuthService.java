package com.healthfirst.service;

import com.healthfirst.dto.PatientLoginRequest;
import com.healthfirst.dto.PatientLoginResponse;
import com.healthfirst.entity.Patient;
import com.healthfirst.entity.PatientRefreshToken;
import com.healthfirst.repository.PatientRepository;
import com.healthfirst.repository.PatientRefreshTokenRepository;
import com.healthfirst.util.JwtUtils;
import com.healthfirst.util.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for patient authentication with HIPAA compliance and enhanced security
 * Handles patient login, session management, and security monitoring
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PatientAuthService {

    private final PatientRepository patientRepository;
    private final PatientRefreshTokenRepository patientRefreshTokenRepository;
    private final PasswordUtils passwordUtils;
    private final JwtUtils jwtUtils;
    private final AuditService auditService;
    private final AgeVerificationService ageVerificationService;

    // Configuration values
    @Value("${app.auth.patient.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.auth.patient.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Value("${app.auth.patient.max-concurrent-sessions:3}")
    private int maxConcurrentSessions;

    @Value("${app.auth.patient.suspicious-activity-threshold:5}")
    private int suspiciousActivityThreshold;

    @Value("${app.auth.patient.session-timeout-hours:24}")
    private int sessionTimeoutHours;

    /**
     * Authenticate patient and create session
     */
    public PatientLoginResponse login(PatientLoginRequest request, String clientIp, String userAgent) {
        try {
            log.debug("Patient login attempt with identifier: {} from IP: {}", 
                    request.getMaskedIdentifier(), clientIp);

            // Validate request
            if (!request.isValidLoginRequest()) {
                auditService.logPatientLogin(null, request.getMaskedIdentifier(), clientIp, false);
                return PatientLoginResponse.invalidCredentials();
            }

            // Find patient by identifier
            Patient patient = findPatientByIdentifier(request.getNormalizedIdentifier());
            if (patient == null) {
                auditService.logPatientLogin(null, request.getMaskedIdentifier(), clientIp, false);
                return PatientLoginResponse.invalidCredentials();
            }

            // Check account status
            PatientLoginResponse statusCheck = checkAccountStatus(patient, clientIp);
            if (statusCheck != null) {
                return statusCheck;
            }

            // Verify password
            if (!passwordUtils.verifyPassword(request.getPassword(), patient.getPasswordHash())) {
                handleFailedLogin(patient, clientIp, "Invalid password");
                return PatientLoginResponse.invalidCredentials();
            }

            // Check for suspicious activity
            if (isSuspiciousLoginActivity(patient, clientIp, request)) {
                auditService.logSuspiciousActivity(patient.getEmail(), clientIp, 
                    "Suspicious login pattern detected", "Patient login from unusual location/device");
                return PatientLoginResponse.suspiciousActivity();
            }

            // Generate tokens
            String accessToken = jwtUtils.generatePatientAccessToken(
                patient.getId(),
                patient.getEmail(),
                ageVerificationService.getAgeCategory(patient.getDateOfBirth()).name(),
                patient.getEmailVerified() ? "VERIFIED" : "PENDING",
                request.isRememberMe()
            );

            String refreshToken = jwtUtils.generatePatientRefreshToken(
                patient.getId(),
                patient.getEmail()
            );

            // Create and save refresh token entity
            PatientRefreshToken refreshTokenEntity = createRefreshTokenEntity(
                patient, refreshToken, request, clientIp, userAgent
            );

            // Check concurrent sessions limit
            long activeSessionsCount = patientRefreshTokenRepository.countActiveTokensByPatientId(
                patient.getId(), LocalDateTime.now());
            
            if (activeSessionsCount >= maxConcurrentSessions) {
                // Revoke oldest session
                List<PatientRefreshToken> activeSessions = patientRefreshTokenRepository
                    .findActiveTokensByPatientId(patient.getId(), LocalDateTime.now());
                if (!activeSessions.isEmpty()) {
                    PatientRefreshToken oldestSession = activeSessions.get(activeSessions.size() - 1);
                    patientRefreshTokenRepository.revokeTokenByHash(
                        oldestSession.getTokenHash(), LocalDateTime.now(), "Concurrent session limit exceeded");
                }
            }

            // Save refresh token
            patientRefreshTokenRepository.save(refreshTokenEntity);

            // Update patient login information
            updateSuccessfulLogin(patient);

            // Check if this is a new device
            boolean isNewDevice = !patientRefreshTokenRepository.isKnownDevice(
                patient.getId(), refreshTokenEntity.getDeviceFingerprint());

            // Log successful login
            auditService.logPatientLogin(patient.getId(), patient.getEmail(), clientIp, true);

            // Get token expiration
            long expiresIn = jwtUtils.getTokenExpirationInSeconds(request.isRememberMe());

            // Create success response
            return PatientLoginResponse.success(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getEmail(),
                patient.getPhoneNumber(),
                patient.getDateOfBirth(),
                patient.getGender().getValue(),
                patient.getEmailVerified(),
                patient.getPhoneVerified(),
                patient.getIsActive(),
                patient.meetsMinimumAge(),
                ageVerificationService.getAgeCategory(patient.getDateOfBirth()).name(),
                patient.getLastLogin(),
                patient.getLoginCount(),
                accessToken,
                refreshToken,
                expiresIn,
                refreshTokenEntity.getId(),
                refreshTokenEntity.getDeviceFingerprint(),
                clientIp,
                refreshTokenEntity.getFormattedDeviceInfo(),
                isNewDevice
            );

        } catch (Exception e) {
            log.error("Patient login error for identifier: {}", request.getMaskedIdentifier(), e);
            auditService.logPatientLogin(null, request.getMaskedIdentifier(), clientIp, false);
            return PatientLoginResponse.internalError();
        }
    }

    /**
     * Refresh patient access token
     */
    public PatientLoginResponse refreshToken(String refreshTokenValue, String clientIp) {
        try {
            String tokenHash = jwtUtils.createTokenHash(refreshTokenValue);
            
            Optional<PatientRefreshToken> tokenOpt = patientRefreshTokenRepository
                .findActiveTokenByHash(tokenHash, LocalDateTime.now());
            
            if (tokenOpt.isEmpty()) {
                auditService.logTokenRefresh(null, clientIp, false, "Invalid or expired refresh token");
                return PatientLoginResponse.tokenRefreshError();
            }

            PatientRefreshToken refreshTokenEntity = tokenOpt.get();
            
            // Update token activity
            refreshTokenEntity.updateActivity();
            patientRefreshTokenRepository.save(refreshTokenEntity);

            // Find patient
            Optional<Patient> patientOpt = patientRepository.findById(refreshTokenEntity.getPatientId());
            if (patientOpt.isEmpty()) {
                auditService.logTokenRefresh(refreshTokenEntity.getPatientId(), clientIp, false, "Patient not found");
                return PatientLoginResponse.tokenRefreshError();
            }

            Patient patient = patientOpt.get();

            // Check account status
            if (!patient.canLogin()) {
                auditService.logTokenRefresh(patient.getId(), clientIp, false, "Account cannot login");
                return PatientLoginResponse.accountInactive();
            }

            // Generate new access token
            String newAccessToken = jwtUtils.generatePatientAccessToken(
                patient.getId(),
                patient.getEmail(),
                ageVerificationService.getAgeCategory(patient.getDateOfBirth()).name(),
                patient.getEmailVerified() ? "VERIFIED" : "PENDING",
                false // Default expiration for refresh
            );

            // Get token expiration
            long expiresIn = jwtUtils.getTokenExpirationInSeconds(false);

            auditService.logTokenRefresh(patient.getId(), clientIp, true, "Token refreshed successfully");

            return PatientLoginResponse.refreshSuccess(newAccessToken, expiresIn);

        } catch (Exception e) {
            log.error("Patient token refresh error from IP: {}", clientIp, e);
            auditService.logTokenRefresh(null, clientIp, false, "Internal error during token refresh");
            return PatientLoginResponse.tokenRefreshError();
        }
    }

    /**
     * Logout patient (revoke refresh token)
     */
    public boolean logout(String refreshTokenValue, String clientIp) {
        try {
            String tokenHash = jwtUtils.createTokenHash(refreshTokenValue);
            
            int revokedCount = patientRefreshTokenRepository.revokeTokenByHash(
                tokenHash, LocalDateTime.now(), "User logout");
            
            if (revokedCount > 0) {
                auditService.logLogout(null, clientIp, true, "Patient single logout");
                log.info("Patient successfully logged out from IP: {}", clientIp);
                return true;
            } else {
                auditService.logLogout(null, clientIp, false, "Patient logout - token not found");
                return false;
            }

        } catch (Exception e) {
            log.error("Patient logout error from IP: {}", clientIp, e);
            auditService.logLogout(null, clientIp, false, "Patient logout error");
            return false;
        }
    }

    /**
     * Logout from all devices
     */
    public boolean logoutAll(UUID patientId, String clientIp) {
        try {
            int revokedCount = patientRefreshTokenRepository.revokeAllTokensByPatientId(
                patientId, LocalDateTime.now(), "User logout all");
            
            auditService.logLogout(patientId, clientIp, true, 
                String.format("Patient logout all - %d sessions revoked", revokedCount));
            
            log.info("Patient {} logged out from all devices. Sessions revoked: {}", patientId, revokedCount);
            return true;

        } catch (Exception e) {
            log.error("Patient logout all error for patient: {}", patientId, e);
            auditService.logLogout(patientId, clientIp, false, "Patient logout all error");
            return false;
        }
    }

    /**
     * Get active sessions for patient
     */
    public List<PatientRefreshToken> getActiveSessions(UUID patientId) {
        return patientRefreshTokenRepository.findActiveTokensByPatientId(patientId, LocalDateTime.now());
    }

    /**
     * Revoke specific session
     */
    public boolean revokeSession(UUID sessionId, UUID patientId, String clientIp) {
        try {
            Optional<PatientRefreshToken> tokenOpt = patientRefreshTokenRepository.findById(sessionId);
            
            if (tokenOpt.isEmpty() || !tokenOpt.get().getPatientId().equals(patientId)) {
                auditService.logSessionRevocation(patientId, sessionId, clientIp, false);
                return false;
            }

            PatientRefreshToken token = tokenOpt.get();
            token.revoke("Session revoked by user");
            patientRefreshTokenRepository.save(token);

            auditService.logSessionRevocation(patientId, sessionId, clientIp, true);
            return true;

        } catch (Exception e) {
            log.error("Error revoking session {} for patient {}", sessionId, patientId, e);
            auditService.logSessionRevocation(patientId, sessionId, clientIp, false);
            return false;
        }
    }

    // Private helper methods

    private Patient findPatientByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return patientRepository.findByEmail(identifier).orElse(null);
        } else {
            return patientRepository.findByPhoneNumber(identifier).orElse(null);
        }
    }

    private PatientLoginResponse checkAccountStatus(Patient patient, String clientIp) {
        // Check if account is active
        if (!patient.getIsActive()) {
            auditService.logPatientLogin(patient.getId(), patient.getEmail(), clientIp, false);
            return PatientLoginResponse.accountInactive();
        }

        // Check if email is verified
        if (!patient.getEmailVerified()) {
            auditService.logPatientLogin(patient.getId(), patient.getEmail(), clientIp, false);
            return PatientLoginResponse.accountNotVerified();
        }

        // Check if account is locked
        if (patient.isAccountLocked()) {
            long lockDurationMinutes = java.time.Duration.between(
                LocalDateTime.now(), patient.getLockedUntil()).toMinutes();
            auditService.logPatientLogin(patient.getId(), patient.getEmail(), clientIp, false);
            return PatientLoginResponse.accountLocked(lockDurationMinutes);
        }

        return null; // Account status is okay
    }

    private void handleFailedLogin(Patient patient, String clientIp, String reason) {
        patient.setFailedLoginAttempts(
            (patient.getFailedLoginAttempts() == null ? 0 : patient.getFailedLoginAttempts()) + 1);
        patient.setLastFailedAttempt(LocalDateTime.now());

        if (patient.getFailedLoginAttempts() >= maxFailedAttempts) {
            patient.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
            auditService.logAccountLockout(patient.getId(), patient.getEmail(), clientIp, 
                patient.getFailedLoginAttempts());
        }

        patientRepository.save(patient);
        auditService.logPatientLogin(patient.getId(), patient.getEmail(), clientIp, false);
    }

    private boolean isSuspiciousLoginActivity(Patient patient, String clientIp, PatientLoginRequest request) {
        // Check for unusual IP patterns
        long recentIpCount = patientRefreshTokenRepository.countDistinctIpAddresses(
            patient.getId(), LocalDateTime.now().minusDays(7));
        
        if (recentIpCount > suspiciousActivityThreshold) {
            return true;
        }

        // Check for unusual device patterns
        if (request.hasDeviceInfo()) {
            long recentDeviceCount = patientRefreshTokenRepository.countDistinctDevices(
                patient.getId(), LocalDateTime.now().minusDays(7));
            
            if (recentDeviceCount > suspiciousActivityThreshold) {
                return true;
            }
        }

        return false;
    }

    private PatientRefreshToken createRefreshTokenEntity(Patient patient, String refreshToken,
            PatientLoginRequest request, String clientIp, String userAgent) {
        
        String tokenHash = jwtUtils.createTokenHash(refreshToken);
        LocalDateTime expiresAt = jwtUtils.getRefreshTokenExpiration(request.isRememberMe());
        
        String deviceFingerprint = request.hasDeviceInfo() ? 
            request.getDeviceFingerprint() : "unknown-device";
        
        PatientLoginRequest.DeviceInfo deviceInfo = request.getDeviceInfo();
        
        return PatientRefreshToken.create(
            patient.getId(),
            tokenHash,
            expiresAt,
            deviceInfo != null ? deviceInfo.getDeviceType() : null,
            deviceInfo != null ? deviceInfo.getDeviceName() : null,
            deviceFingerprint,
            deviceInfo != null ? deviceInfo.getAppVersion() : null,
            deviceInfo != null ? deviceInfo.getOperatingSystem() : null,
            deviceInfo != null ? deviceInfo.getBrowser() : null,
            clientIp,
            userAgent,
            request.getIdentifierType()
        );
    }

    private void updateSuccessfulLogin(Patient patient) {
        patient.setLastLogin(LocalDateTime.now());
        patient.setFailedLoginAttempts(0);
        patient.setLockedUntil(null);
        patient.setLoginCount((patient.getLoginCount() == null ? 0 : patient.getLoginCount()) + 1);
        patientRepository.save(patient);
    }
} 