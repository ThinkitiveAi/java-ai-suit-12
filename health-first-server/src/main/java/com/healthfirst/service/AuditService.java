package com.healthfirst.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class AuditService {

    private static final String AUDIT_LOG_PATTERN = "AUDIT: {} | Time: {} | IP: {} | Success: {} | Details: {}";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Log provider registration attempts
     */
    public void logProviderRegistration(UUID providerId, String email, String clientIp, boolean success) {
        String event = success ? "PROVIDER_REGISTRATION_SUCCESS" : "PROVIDER_REGISTRATION_FAILED";
        String details = String.format("Email: %s, Success: %s", email, success);
        log.info("AUDIT: {} - Provider: {}, IP: {}, Details: {}", event, providerId, clientIp, details);
    }

    /**
     * Log email verification attempts
     */
    public void logEmailVerification(String email, String clientIp, boolean success, String failureReason) {
        String event = "EMAIL_VERIFICATION";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("Email: %s, FailureReason: %s", 
            maskEmail(email), 
            failureReason != null ? failureReason : "N/A");
        
        log.info(AUDIT_LOG_PATTERN, event, timestamp, clientIp, success, details);
    }

    /**
     * Log login attempts
     */
    public void logLoginAttempt(String identifier, String clientIp, boolean success, String failureReason) {
        String event = "LOGIN_ATTEMPT";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("Identifier: %s, FailureReason: %s", 
            maskIdentifier(identifier), 
            failureReason != null ? failureReason : "N/A");
        
        log.info(AUDIT_LOG_PATTERN, event, timestamp, clientIp, success, details);
    }

    /**
     * Log password reset requests
     */
    public void logPasswordReset(String email, String clientIp, boolean success, String action) {
        String event = "PASSWORD_RESET";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("Email: %s, Action: %s", maskEmail(email), action);
        
        log.info(AUDIT_LOG_PATTERN, event, timestamp, clientIp, success, details);
    }

    /**
     * Log account lockout events
     */
    public void logAccountLockout(String identifier, String clientIp, String reason) {
        String event = "ACCOUNT_LOCKOUT";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("Identifier: %s, Reason: %s", maskIdentifier(identifier), reason);
        
        log.warn(AUDIT_LOG_PATTERN, event, timestamp, clientIp, true, details);
    }

    /**
     * Log suspicious activity
     */
    public void logSuspiciousActivity(String identifier, String clientIp, String activity, String details) {
        String event = "SUSPICIOUS_ACTIVITY";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logDetails = String.format("Identifier: %s, Activity: %s, Details: %s", 
            maskIdentifier(identifier), activity, details);
        
        log.warn(AUDIT_LOG_PATTERN, event, timestamp, clientIp, false, logDetails);
    }

    /**
     * Log rate limiting violations
     */
    public void logRateLimitViolation(String clientIp, String endpoint, int attemptCount) {
        String event = "RATE_LIMIT_VIOLATION";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("Endpoint: %s, AttemptCount: %d", endpoint, attemptCount);
        
        log.warn(AUDIT_LOG_PATTERN, event, timestamp, clientIp, false, details);
    }

    /**
     * Log profile updates
     */
    public void logProfileUpdate(UUID providerId, String email, String clientIp, String updatedFields) {
        String event = "PROFILE_UPDATE";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("ProviderId: %s, Email: %s, UpdatedFields: %s", 
            providerId.toString(), maskEmail(email), updatedFields);
        
        log.info(AUDIT_LOG_PATTERN, event, timestamp, clientIp, true, details);
    }

    /**
     * Log account deactivation
     */
    public void logAccountDeactivation(UUID providerId, String email, String clientIp, String reason) {
        String event = "ACCOUNT_DEACTIVATION";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("ProviderId: %s, Email: %s, Reason: %s", 
            providerId.toString(), maskEmail(email), reason);
        
        log.warn(AUDIT_LOG_PATTERN, event, timestamp, clientIp, true, details);
    }

    /**
     * Log security configuration changes
     */
    public void logSecurityConfigChange(String adminId, String clientIp, String configType, String change) {
        String event = "SECURITY_CONFIG_CHANGE";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("AdminId: %s, ConfigType: %s, Change: %s", 
            adminId, configType, change);
        
        log.warn(AUDIT_LOG_PATTERN, event, timestamp, clientIp, true, details);
    }

    /**
     * Mask email for privacy in logs (keep first 2 chars and domain)
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) return "***";
        
        int atIndex = email.indexOf('@');
        if (atIndex < 2) return "***@" + email.substring(atIndex + 1);
        
        return email.substring(0, 2) + "***@" + email.substring(atIndex + 1);
    }

    /**
     * Mask identifier (email or phone) for privacy in logs
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 3) return "***";
        
        if (identifier.contains("@")) {
            return maskEmail(identifier);
        } else {
            // Phone number masking - show first 2 and last 2 digits
            if (identifier.length() > 4) {
                return identifier.substring(0, 2) + "***" + identifier.substring(identifier.length() - 2);
            }
            return "***";
        }
    }

    /**
     * Format provider details for logging (without sensitive information)
     */
    public String formatProviderDetails(UUID providerId, String email, String firstName, String lastName) {
        return String.format("ID: %s, Email: %s, Name: %s %s", 
            providerId, maskEmail(email), firstName, lastName.charAt(0) + "***");
    }

    /**
     * Log batch operations
     */
    public void logBatchOperation(String operationType, String clientIp, int itemCount, boolean success) {
        String event = "BATCH_OPERATION";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("Operation: %s, ItemCount: %d", operationType, itemCount);
        
        log.info(AUDIT_LOG_PATTERN, event, timestamp, clientIp, success, details);
    }

    /**
     * Log API access with endpoint details
     */
    public void logApiAccess(String endpoint, String method, String clientIp, int responseCode, long processingTime) {
        String event = "API_ACCESS";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("Endpoint: %s %s, ResponseCode: %d, ProcessingTime: %dms", 
            method, endpoint, responseCode, processingTime);
        
        if (responseCode >= 400) {
            log.warn(AUDIT_LOG_PATTERN, event, timestamp, clientIp, false, details);
        } else {
            log.debug(AUDIT_LOG_PATTERN, event, timestamp, clientIp, true, details);
        }
    }

    /**
     * Log successful login
     */
    public void logSuccessfulLogin(UUID providerId, String email, String clientIp) {
        String event = "LOGIN_SUCCESS";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("ProviderId: %s, Email: %s", 
            providerId != null ? providerId.toString() : "null", 
            maskEmail(email));
        
        log.info(AUDIT_LOG_PATTERN, event, timestamp, clientIp, true, details);
    }

    /**
     * Log failed login attempt
     */
    public void logFailedLogin(String identifier, String clientIp, String reason) {
        String event = "LOGIN_FAILED";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("Identifier: %s, Reason: %s", 
            maskIdentifier(identifier), reason);
        
        log.warn(AUDIT_LOG_PATTERN, event, timestamp, clientIp, false, details);
    }

    /**
     * Log token refresh attempts
     */
    public void logTokenRefresh(UUID providerId, String clientIp, boolean success, String details) {
        String event = "TOKEN_REFRESH";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logDetails = String.format("ProviderId: %s, Details: %s", 
            providerId != null ? providerId.toString() : "null", details);
        
        if (success) {
            log.info(AUDIT_LOG_PATTERN, event, timestamp, clientIp, success, logDetails);
        } else {
            log.warn(AUDIT_LOG_PATTERN, event, timestamp, clientIp, success, logDetails);
        }
    }

    /**
     * Log logout events
     */
    public void logLogout(UUID providerId, String clientIp, boolean success, String logoutType) {
        String event = "LOGOUT";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("ProviderId: %s, Type: %s", 
            providerId != null ? providerId.toString() : "null", logoutType);
        
        log.info(AUDIT_LOG_PATTERN, event, timestamp, clientIp, success, details);
    }

    /**
     * Log session revocation
     */
    public void logSessionRevocation(UUID providerId, UUID sessionId, String clientIp, boolean success) {
        String event = "SESSION_REVOCATION";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("ProviderId: %s, SessionId: %s", 
            providerId != null ? providerId.toString() : "null",
            sessionId != null ? sessionId.toString() : "null");
        
        log.info(AUDIT_LOG_PATTERN, event, timestamp, clientIp, success, details);
    }

    /**
     * Log account lockout events
     */
    public void logAccountLockout(UUID providerId, String email, String clientIp, int failedAttempts) {
        String event = "ACCOUNT_LOCKOUT";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("ProviderId: %s, Email: %s, FailedAttempts: %d", 
            providerId != null ? providerId.toString() : "null",
            maskEmail(email), failedAttempts);
        
        log.warn(AUDIT_LOG_PATTERN, event, timestamp, clientIp, true, details);
    }

    /**
     * Log suspicious authentication activity
     */
    public void logSuspiciousAuthActivity(UUID providerId, String email, String clientIp, String activityType) {
        String event = "SUSPICIOUS_AUTH_ACTIVITY";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("ProviderId: %s, Email: %s, ActivityType: %s", 
            providerId != null ? providerId.toString() : "null",
            maskEmail(email), activityType);
        
        log.warn(AUDIT_LOG_PATTERN, event, timestamp, clientIp, false, details);
    }

    // Patient-specific audit logging methods

    /**
     * Log patient registration
     */
    public void logPatientRegistration(UUID patientId, String email, String clientIp, boolean success) {
        String event = "PATIENT_REGISTRATION";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("PatientId: %s, Email: %s", 
            patientId != null ? patientId.toString() : "null", 
            maskEmail(email));
        
        log.info(AUDIT_LOG_PATTERN, event, timestamp, clientIp, success, details);
    }

    /**
     * Log patient email verification
     */
    public void logPatientEmailVerification(UUID patientId, String email, boolean success) {
        String event = "PATIENT_EMAIL_VERIFICATION";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("PatientId: %s, Email: %s", 
            patientId != null ? patientId.toString() : "null",
            maskEmail(email));
        
        if (success) {
            log.info(AUDIT_LOG_PATTERN, event, timestamp, "SYSTEM", success, details);
        } else {
            log.warn(AUDIT_LOG_PATTERN, event, timestamp, "SYSTEM", success, details);
        }
    }

    /**
     * Log patient phone verification
     */
    public void logPatientPhoneVerification(UUID patientId, String phoneNumber, boolean success) {
        String event = "PATIENT_PHONE_VERIFICATION";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("PatientId: %s, Phone: %s", 
            patientId != null ? patientId.toString() : "null",
            maskIdentifier(phoneNumber));
        
        if (success) {
            log.info(AUDIT_LOG_PATTERN, event, timestamp, "SYSTEM", success, details);
        } else {
            log.warn(AUDIT_LOG_PATTERN, event, timestamp, "SYSTEM", success, details);
        }
    }

    /**
     * Log patient email verification resent
     */
    public void logPatientEmailVerificationResent(UUID patientId, String email) {
        String event = "PATIENT_EMAIL_VERIFICATION_RESENT";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("PatientId: %s, Email: %s", 
            patientId.toString(), maskEmail(email));
        
        log.info(AUDIT_LOG_PATTERN, event, timestamp, "SYSTEM", true, details);
    }

    /**
     * Log patient phone verification resent
     */
    public void logPatientPhoneVerificationResent(UUID patientId, String phoneNumber) {
        String event = "PATIENT_PHONE_VERIFICATION_RESENT";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("PatientId: %s, Phone: %s", 
            patientId.toString(), maskIdentifier(phoneNumber));
        
        log.info(AUDIT_LOG_PATTERN, event, timestamp, "SYSTEM", true, details);
    }

    /**
     * Log patient account deactivation
     */
    public void logPatientDeactivation(UUID patientId, String reason) {
        String event = "PATIENT_DEACTIVATION";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("PatientId: %s, Reason: %s", 
            patientId.toString(), reason);
        
        log.warn(AUDIT_LOG_PATTERN, event, timestamp, "ADMIN", true, details);
    }

    /**
     * Log patient login (for when patient login is implemented)
     */
    public void logPatientLogin(UUID patientId, String email, String clientIp, boolean success) {
        String event = "PATIENT_LOGIN";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("PatientId: %s, Email: %s", 
            patientId != null ? patientId.toString() : "null",
            maskEmail(email));
        
        if (success) {
            log.info(AUDIT_LOG_PATTERN, event, timestamp, clientIp, success, details);
        } else {
            log.warn(AUDIT_LOG_PATTERN, event, timestamp, clientIp, success, details);
        }
    }

    /**
     * Log patient data access (HIPAA compliance)
     */
    public void logPatientDataAccess(UUID patientId, UUID accessorId, String accessorType, String dataType, String clientIp) {
        String event = "PATIENT_DATA_ACCESS";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("PatientId: %s, AccessorId: %s, AccessorType: %s, DataType: %s", 
            patientId.toString(), accessorId.toString(), accessorType, dataType);
        
        log.info(AUDIT_LOG_PATTERN, event, timestamp, clientIp, true, details);
    }

    /**
     * Log patient data modification (HIPAA compliance)
     */
    public void logPatientDataModification(UUID patientId, UUID modifierId, String modifierType, String changeType, String clientIp) {
        String event = "PATIENT_DATA_MODIFICATION";
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String details = String.format("PatientId: %s, ModifierId: %s, ModifierType: %s, ChangeType: %s", 
            patientId.toString(), modifierId.toString(), modifierType, changeType);
        
        log.warn(AUDIT_LOG_PATTERN, event, timestamp, clientIp, true, details);
    }

    // Provider availability audit logging methods
    public void logProviderAvailabilityCreated(UUID providerId, UUID availabilityId, String title, String clientIp) {
        String event = "AVAILABILITY_CREATED";
        String details = String.format("AvailabilityId: %s, Title: %s", availabilityId, title);
        log.info("AUDIT: {} - Provider: {}, IP: {}, Details: {}", event, providerId, clientIp, details);
    }

    public void logProviderAvailabilityUpdated(UUID providerId, UUID availabilityId, String title, String clientIp) {
        String event = "AVAILABILITY_UPDATED";
        String details = String.format("AvailabilityId: %s, Title: %s", availabilityId, title);
        log.info("AUDIT: {} - Provider: {}, IP: {}, Details: {}", event, providerId, clientIp, details);
    }

    public void logProviderAvailabilityDeleted(UUID providerId, UUID availabilityId, String clientIp) {
        String event = "AVAILABILITY_DELETED";
        String details = String.format("AvailabilityId: %s", availabilityId);
        log.info("AUDIT: {} - Provider: {}, IP: {}, Details: {}", event, providerId, clientIp, details);
    }

    public void logProviderSlotsGenerated(UUID providerId, UUID availabilityId, int slotCount, String clientIp) {
        String event = "AVAILABILITY_SLOTS_GENERATED";
        String details = String.format("AvailabilityId: %s, SlotCount: %d", availabilityId, slotCount);
        log.info("AUDIT: {} - Provider: {}, IP: {}, Details: {}", event, providerId, clientIp, details);
    }

    public void logProviderAction(UUID providerId, String action, String details, String clientIp) {
        log.info("AUDIT: {} - Provider: {}, IP: {}, Details: {}", action, providerId, clientIp, details);
    }
} 