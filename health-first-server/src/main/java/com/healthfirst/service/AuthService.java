package com.healthfirst.service;

import com.healthfirst.dto.LoginRequest;
import com.healthfirst.dto.LoginResponse;
import com.healthfirst.entity.Provider;
import com.healthfirst.entity.RefreshToken;
import com.healthfirst.exception.ValidationException;
import com.healthfirst.repository.ProviderRepository;
import com.healthfirst.repository.RefreshTokenRepository;
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
 * Service for provider authentication with enhanced security features
 * Handles login, logout, token refresh, and session management
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final ProviderRepository providerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordUtils passwordUtils;
    private final JwtUtils jwtUtils;
    private final AuditService auditService;

    @Value("${app.auth.max-failed-attempts:3}")
    private int maxFailedAttempts;

    @Value("${app.auth.lockout-duration-minutes:60}")
    private int lockoutDurationMinutes;

    @Value("${app.auth.max-concurrent-sessions:3}")
    private int maxConcurrentSessions;

    @Value("${app.auth.suspicious-activity-threshold:5}")
    private int suspiciousActivityThreshold;

    /**
     * Authenticate provider and generate JWT tokens
     */
    public LoginResponse login(LoginRequest request, String clientIp, String userAgent) {
        try {
            // Validate input
            validateLoginRequest(request);

            // Find provider by email or phone
            Provider provider = findProviderByIdentifier(request.getIdentifier());
            if (provider == null) {
                auditService.logFailedLogin(request.getIdentifier(), clientIp, "PROVIDER_NOT_FOUND");
                return LoginResponse.invalidCredentials();
            }

            // Check account status and security constraints
            LoginValidationResult validationResult = validateProviderForLogin(provider, clientIp);
            if (!validationResult.isValid()) {
                return validationResult.getErrorResponse();
            }

            // Verify password
            if (!passwordUtils.verifyPassword(request.getPassword(), provider.getPasswordHash())) {
                handleFailedLogin(provider, clientIp, "INVALID_PASSWORD");
                return LoginResponse.invalidCredentials();
            }

            // Check for suspicious concurrent sessions
            if (detectSuspiciousActivity(provider, clientIp)) {
                log.warn("Suspicious login activity detected for provider: {} from IP: {}", 
                        provider.getEmail(), clientIp);
                // Could trigger additional verification here
            }

            // Successful login - generate tokens and update provider
            return handleSuccessfulLogin(provider, request, clientIp, userAgent);

        } catch (ValidationException e) {
            auditService.logFailedLogin(request.getIdentifier(), clientIp, "VALIDATION_ERROR");
            throw e;
        } catch (Exception e) {
            log.error("Login failed for identifier: {} from IP: {}", request.getIdentifier(), clientIp, e);
            auditService.logFailedLogin(request.getIdentifier(), clientIp, "INTERNAL_ERROR");
            return LoginResponse.internalError();
        }
    }

    /**
     * Refresh JWT access token using refresh token
     */
    public LoginResponse refreshToken(String refreshTokenValue, String clientIp) {
        try {
            // Validate refresh token format
            if (!jwtUtils.validateToken(refreshTokenValue) || !jwtUtils.isRefreshToken(refreshTokenValue)) {
                return LoginResponse.error("Invalid refresh token", "INVALID_REFRESH_TOKEN");
            }

            // Find token in database
            String tokenHash = jwtUtils.createTokenHash(refreshTokenValue);
            Optional<RefreshToken> tokenOpt = refreshTokenRepository.findActiveTokenByHash(tokenHash, LocalDateTime.now());
            
            if (tokenOpt.isEmpty()) {
                auditService.logTokenRefresh(null, clientIp, false, "TOKEN_NOT_FOUND");
                return LoginResponse.error("Refresh token not found or expired", "INVALID_REFRESH_TOKEN");
            }

            RefreshToken refreshToken = tokenOpt.get();
            
            // Get provider
            Optional<Provider> providerOpt = providerRepository.findById(refreshToken.getProviderId());
            if (providerOpt.isEmpty()) {
                auditService.logTokenRefresh(refreshToken.getProviderId(), clientIp, false, "PROVIDER_NOT_FOUND");
                return LoginResponse.error("Provider not found", "PROVIDER_NOT_FOUND");
            }

            Provider provider = providerOpt.get();

            // Validate provider status
            if (!provider.getEmailVerified() || !isProviderActive(provider)) {
                refreshTokenRepository.revokeTokenByHash(tokenHash, LocalDateTime.now());
                auditService.logTokenRefresh(provider.getId(), clientIp, false, "PROVIDER_INACTIVE");
                return LoginResponse.error("Account is inactive", "ACCOUNT_INACTIVE");
            }

            // Generate new access token
            String newAccessToken = jwtUtils.generateAccessToken(
                provider.getId(), 
                provider.getEmail(), 
                provider.getSpecialization(),
                provider.getVerificationStatus().toString().toLowerCase(),
                false // refresh doesn't extend remember me
            );

            // Update token last used timestamp
            refreshTokenRepository.updateLastUsedByHash(tokenHash, LocalDateTime.now());

            // Create response
            long expiresIn = jwtUtils.getTokenExpirationInSeconds(false);
            LoginResponse response = LoginResponse.success(
                newAccessToken, refreshTokenValue, expiresIn,
                provider.getId(), provider.getFirstName(), provider.getLastName(),
                provider.getEmail(), provider.getPhoneNumber(), provider.getSpecialization(),
                provider.getVerificationStatus().toString().toLowerCase(), provider.getYearsOfExperience(),
                provider.getLastLogin(), provider.getLoginCount(),
                provider.getClinicAddress() != null ? provider.getClinicAddress().getCity() : null,
                provider.getClinicAddress() != null ? provider.getClinicAddress().getState() : null
            );

            auditService.logTokenRefresh(provider.getId(), clientIp, true, "SUCCESS");
            return response;

        } catch (Exception e) {
            log.error("Token refresh failed from IP: {}", clientIp, e);
            auditService.logTokenRefresh(null, clientIp, false, "INTERNAL_ERROR");
            return LoginResponse.internalError();
        }
    }

    /**
     * Logout provider by revoking refresh token
     */
    public boolean logout(String refreshTokenValue, String clientIp) {
        try {
            if (refreshTokenValue == null || refreshTokenValue.trim().isEmpty()) {
                return false;
            }

            String tokenHash = jwtUtils.createTokenHash(refreshTokenValue);
            int revokedCount = refreshTokenRepository.revokeTokenByHash(tokenHash, LocalDateTime.now());
            
            UUID providerId = null;
            try {
                providerId = jwtUtils.extractProviderId(refreshTokenValue);
            } catch (Exception e) {
                log.debug("Could not extract provider ID from refresh token during logout");
            }

            auditService.logLogout(providerId, clientIp, revokedCount > 0, "SINGLE_SESSION");
            return revokedCount > 0;

        } catch (Exception e) {
            log.error("Logout failed from IP: {}", clientIp, e);
            auditService.logLogout(null, clientIp, false, "INTERNAL_ERROR");
            return false;
        }
    }

    /**
     * Logout from all sessions for a provider
     */
    public boolean logoutAll(UUID providerId, String clientIp) {
        try {
            int revokedCount = refreshTokenRepository.revokeAllTokensByProviderId(providerId, LocalDateTime.now());
            auditService.logLogout(providerId, clientIp, revokedCount > 0, "ALL_SESSIONS");
            return revokedCount > 0;
        } catch (Exception e) {
            log.error("Logout all failed for provider: {} from IP: {}", providerId, clientIp, e);
            auditService.logLogout(providerId, clientIp, false, "INTERNAL_ERROR");
            return false;
        }
    }

    /**
     * Get active sessions for a provider
     */
    public List<RefreshToken> getActiveSessions(UUID providerId) {
        return refreshTokenRepository.findActiveTokensByProviderId(providerId, LocalDateTime.now());
    }

    /**
     * Revoke specific session
     */
    public boolean revokeSession(UUID sessionId, UUID providerId, String clientIp) {
        try {
            Optional<RefreshToken> tokenOpt = refreshTokenRepository.findById(sessionId);
            if (tokenOpt.isEmpty() || !tokenOpt.get().getProviderId().equals(providerId)) {
                return false;
            }

            RefreshToken token = tokenOpt.get();
            token.markAsRevoked();
            refreshTokenRepository.save(token);

            auditService.logSessionRevocation(providerId, sessionId, clientIp, true);
            return true;
        } catch (Exception e) {
            log.error("Session revocation failed for provider: {} session: {}", providerId, sessionId, e);
            auditService.logSessionRevocation(providerId, sessionId, clientIp, false);
            return false;
        }
    }

    // Private helper methods

    private void validateLoginRequest(LoginRequest request) {
        if (request.getIdentifier() == null || request.getIdentifier().trim().isEmpty()) {
            throw new ValidationException("Identifier (email or phone) is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new ValidationException("Password is required");
        }
    }

    private Provider findProviderByIdentifier(String identifier) {
        identifier = identifier.trim().toLowerCase();
        
        if (identifier.contains("@")) {
            // Email identifier
            return providerRepository.findByEmail(identifier).orElse(null);
        } else {
            // Phone number identifier
            return providerRepository.findByPhoneNumber(identifier).orElse(null);
        }
    }

    private LoginValidationResult validateProviderForLogin(Provider provider, String clientIp) {
        // Check if email is verified
        if (!provider.getEmailVerified()) {
            auditService.logFailedLogin(provider.getEmail(), clientIp, "EMAIL_NOT_VERIFIED");
            return LoginValidationResult.error(LoginResponse.accountNotVerified());
        }

        // Check if account is active
        if (!isProviderActive(provider)) {
            auditService.logFailedLogin(provider.getEmail(), clientIp, "ACCOUNT_INACTIVE");
            return LoginValidationResult.error(LoginResponse.accountInactive());
        }

        // Check if account is locked
        if (isAccountLocked(provider)) {
            auditService.logFailedLogin(provider.getEmail(), clientIp, "ACCOUNT_LOCKED");
            return LoginValidationResult.error(LoginResponse.accountLocked(provider.getLockedUntil()));
        }

        // Check concurrent sessions limit
        if (refreshTokenRepository.hasExceededMaxSessions(provider.getId(), maxConcurrentSessions)) {
            auditService.logFailedLogin(provider.getEmail(), clientIp, "MAX_SESSIONS_EXCEEDED");
            return LoginValidationResult.error(
                LoginResponse.error("Maximum concurrent sessions exceeded. Please logout from other devices.", "MAX_SESSIONS_EXCEEDED")
            );
        }

        return LoginValidationResult.valid();
    }

    private boolean isProviderActive(Provider provider) {
        return Provider.VerificationStatus.VERIFIED.equals(provider.getVerificationStatus());
    }

    private boolean isAccountLocked(Provider provider) {
        return provider.getLockedUntil() != null && provider.getLockedUntil().isAfter(LocalDateTime.now());
    }

    private void handleFailedLogin(Provider provider, String clientIp, String reason) {
        provider.setFailedLoginAttempts(provider.getFailedLoginAttempts() + 1);
        provider.setLastFailedAttempt(LocalDateTime.now());

        // Lock account if too many failed attempts
        if (provider.getFailedLoginAttempts() >= maxFailedAttempts) {
            provider.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
            log.warn("Account locked for provider: {} due to {} failed attempts", 
                    provider.getEmail(), provider.getFailedLoginAttempts());
        }

        // Increase suspicious activity score
        provider.setSuspiciousActivityScore(provider.getSuspiciousActivityScore() + 1);

        providerRepository.save(provider);
        auditService.logFailedLogin(provider.getEmail(), clientIp, reason);
    }

    private boolean detectSuspiciousActivity(Provider provider, String clientIp) {
        // Check for multiple recent login locations
        List<String> recentLocations = refreshTokenRepository.findRecentLoginLocationsByProviderId(
            provider.getId(), LocalDateTime.now().minusHours(24)
        );

        // Check for too many distinct IP addresses
        long distinctIps = refreshTokenRepository.countDistinctActiveIpsByProviderId(provider.getId(), LocalDateTime.now());

        return recentLocations.size() > 3 || distinctIps > 2 || 
               provider.getSuspiciousActivityScore() > suspiciousActivityThreshold;
    }

    private LoginResponse handleSuccessfulLogin(Provider provider, LoginRequest request, String clientIp, String userAgent) {
        // Reset failed login attempts
        provider.setFailedLoginAttempts(0);
        provider.setLastFailedAttempt(null);
        provider.setLockedUntil(null);
        provider.setLastLogin(LocalDateTime.now());
        provider.setLoginCount(provider.getLoginCount() + 1);

        // Reduce suspicious activity score gradually
        if (provider.getSuspiciousActivityScore() > 0) {
            provider.setSuspiciousActivityScore(Math.max(0, provider.getSuspiciousActivityScore() - 1));
        }

        providerRepository.save(provider);

        // Generate tokens
        String accessToken = jwtUtils.generateAccessToken(
            provider.getId(), 
            provider.getEmail(), 
            provider.getSpecialization(),
            provider.getVerificationStatus().toString().toLowerCase(),
            request.isRememberMe()
        );

        String refreshToken = jwtUtils.generateRefreshToken(provider.getId(), provider.getEmail());
        
        // Save refresh token to database
        String tokenHash = jwtUtils.createTokenHash(refreshToken);
        LocalDateTime refreshExpiration = jwtUtils.getRefreshTokenExpiration(request.isRememberMe());
        
        RefreshToken refreshTokenEntity = RefreshToken.createToken(
            provider.getId(), tokenHash, refreshExpiration,
            request.getDeviceType(), request.getDeviceName(), request.getAppVersion(),
            clientIp, userAgent
        );
        
        refreshTokenRepository.save(refreshTokenEntity);

        // Create response
        long expiresIn = jwtUtils.getTokenExpirationInSeconds(request.isRememberMe());
        LoginResponse response = LoginResponse.success(
            accessToken, refreshToken, expiresIn,
            provider.getId(), provider.getFirstName(), provider.getLastName(),
            provider.getEmail(), provider.getPhoneNumber(), provider.getSpecialization(),
            provider.getVerificationStatus().toString().toLowerCase(), provider.getYearsOfExperience(),
            provider.getLastLogin(), provider.getLoginCount(),
            provider.getClinicAddress() != null ? provider.getClinicAddress().getCity() : null,
            provider.getClinicAddress() != null ? provider.getClinicAddress().getState() : null
        );

        auditService.logSuccessfulLogin(provider.getId(), provider.getEmail(), clientIp);
        return response;
    }

    // Helper class for validation results
    private static class LoginValidationResult {
        private final boolean valid;
        private final LoginResponse errorResponse;

        private LoginValidationResult(boolean valid, LoginResponse errorResponse) {
            this.valid = valid;
            this.errorResponse = errorResponse;
        }

        public static LoginValidationResult valid() {
            return new LoginValidationResult(true, null);
        }

        public static LoginValidationResult error(LoginResponse errorResponse) {
            return new LoginValidationResult(false, errorResponse);
        }

        public boolean isValid() {
            return valid;
        }

        public LoginResponse getErrorResponse() {
            return errorResponse;
        }
    }
} 