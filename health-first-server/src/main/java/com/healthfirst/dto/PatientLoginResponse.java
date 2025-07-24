package com.healthfirst.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for patient login response with HIPAA compliance and privacy considerations
 * Contains patient-specific information with appropriate masking
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientLoginResponse {

    private boolean success;
    private String message;
    private PatientLoginData data;
    private String errorCode; // For error responses

    /**
     * Inner class for successful login data
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientLoginData {
        private String accessToken;
        private String refreshToken;
        private long expiresIn; // seconds until access token expires
        private String tokenType = "Bearer";
        private PatientInfo patient;
        private SessionInfo session;
    }

    /**
     * Patient information with privacy considerations (HIPAA compliance)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientInfo {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email; // Masked
        private String phoneNumber; // Masked
        private LocalDate dateOfBirth; // May be masked for minors
        private String gender;
        private boolean emailVerified;
        private boolean phoneVerified;
        private boolean isActive;
        private boolean meetsMinimumAge;
        private String ageCategory; // ADULT, MINOR, SENIOR
        private LocalDateTime lastLogin;
        private Integer loginCount;
        
        // Address info (limited for privacy)
        private AddressInfo address;
        
        // Medical info flags (no actual medical data)
        private MedicalFlags medicalFlags;
        
        // Insurance status (no actual insurance data)
        private InsuranceFlags insuranceFlags;
    }

    /**
     * Limited address information for privacy
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressInfo {
        private String city;
        private String state;
        private String zip; // First 3 digits only for privacy
    }

    /**
     * Medical information flags (no actual medical data for privacy)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicalFlags {
        private boolean hasMedicalHistory;
        private boolean hasEmergencyContact;
        private int medicalConditionsCount;
    }

    /**
     * Insurance information flags (no actual insurance data for privacy)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsuranceFlags {
        private boolean hasInsurance;
        private boolean isInsuranceVerified;
        private String insuranceProvider; // Provider name only, no policy details
    }

    /**
     * Session information for security tracking
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionInfo {
        private UUID sessionId;
        private String deviceFingerprint;
        private String ipAddress; // Masked
        private LocalDateTime loginTime;
        private LocalDateTime lastActivity;
        private boolean isNewDevice;
        private String deviceInfo; // Formatted device information
    }

    // Static factory methods for creating responses

    /**
     * Create successful login response
     */
    public static PatientLoginResponse success(
            UUID patientId,
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            LocalDate dateOfBirth,
            String gender,
            boolean emailVerified,
            boolean phoneVerified,
            boolean isActive,
            boolean meetsMinimumAge,
            String ageCategory,
            LocalDateTime lastLogin,
            Integer loginCount,
            String accessToken,
            String refreshToken,
            long expiresIn,
            UUID sessionId,
            String deviceFingerprint,
            String ipAddress,
            String deviceInfo,
            boolean isNewDevice) {

        // Create patient info with masked sensitive data
        PatientInfo patientInfo = new PatientInfo();
        patientInfo.setId(patientId);
        patientInfo.setFirstName(firstName);
        patientInfo.setLastName(lastName);
        patientInfo.setEmail(maskEmail(email));
        patientInfo.setPhoneNumber(maskPhoneNumber(phoneNumber));
        patientInfo.setDateOfBirth(dateOfBirth); // May be further masked for minors
        patientInfo.setGender(gender);
        patientInfo.setEmailVerified(emailVerified);
        patientInfo.setPhoneVerified(phoneVerified);
        patientInfo.setActive(isActive);
        patientInfo.setMeetsMinimumAge(meetsMinimumAge);
        patientInfo.setAgeCategory(ageCategory);
        patientInfo.setLastLogin(lastLogin);
        patientInfo.setLoginCount(loginCount);

        // Create session info
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setSessionId(sessionId);
        sessionInfo.setDeviceFingerprint(deviceFingerprint);
        sessionInfo.setIpAddress(maskIpAddress(ipAddress));
        sessionInfo.setLoginTime(LocalDateTime.now());
        sessionInfo.setLastActivity(LocalDateTime.now());
        sessionInfo.setNewDevice(isNewDevice);
        sessionInfo.setDeviceInfo(deviceInfo);

        // Create login data
        PatientLoginData loginData = new PatientLoginData();
        loginData.setAccessToken(accessToken);
        loginData.setRefreshToken(refreshToken);
        loginData.setExpiresIn(expiresIn);
        loginData.setTokenType("Bearer");
        loginData.setPatient(patientInfo);
        loginData.setSession(sessionInfo);

        return new PatientLoginResponse(
            true,
            String.format("Welcome back, %s! Login successful.", firstName),
            loginData,
            null
        );
    }

    /**
     * Create error response with error code
     */
    public static PatientLoginResponse error(String message, String errorCode) {
        return new PatientLoginResponse(false, message, null, errorCode);
    }

    /**
     * Create invalid credentials error
     */
    public static PatientLoginResponse invalidCredentials() {
        return error("Invalid email/phone or password. Please check your credentials and try again.", "INVALID_CREDENTIALS");
    }

    /**
     * Create account not verified error
     */
    public static PatientLoginResponse accountNotVerified() {
        return error("Account not verified. Please check your email for verification instructions.", "ACCOUNT_NOT_VERIFIED");
    }

    /**
     * Create account locked error
     */
    public static PatientLoginResponse accountLocked(long lockDurationMinutes) {
        return error(
            String.format("Account temporarily locked due to multiple failed login attempts. Try again in %d minutes.", lockDurationMinutes),
            "ACCOUNT_LOCKED"
        );
    }

    /**
     * Create account inactive error
     */
    public static PatientLoginResponse accountInactive() {
        return error("Account is inactive. Please contact support for assistance.", "ACCOUNT_INACTIVE");
    }

    /**
     * Create rate limit exceeded error
     */
    public static PatientLoginResponse rateLimitExceeded() {
        return error("Too many login attempts. Please try again later.", "RATE_LIMIT_EXCEEDED");
    }

    /**
     * Create internal server error
     */
    public static PatientLoginResponse internalError() {
        return error("Login failed due to an internal error. Please try again later.", "INTERNAL_ERROR");
    }

    /**
     * Create token refresh error
     */
    public static PatientLoginResponse tokenRefreshError() {
        return error("Token refresh failed. Please login again.", "TOKEN_REFRESH_FAILED");
    }

    /**
     * Create session expired error
     */
    public static PatientLoginResponse sessionExpired() {
        return error("Session has expired. Please login again.", "SESSION_EXPIRED");
    }

    /**
     * Create suspicious activity error
     */
    public static PatientLoginResponse suspiciousActivity() {
        return error("Suspicious activity detected. Account temporarily restricted for security.", "SUSPICIOUS_ACTIVITY");
    }

    // Privacy utility methods

    /**
     * Mask email address for privacy (HIPAA compliance)
     */
    private static String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        
        // Show first character and everything after @
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    /**
     * Mask phone number for privacy (HIPAA compliance)
     */
    private static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "***";
        }
        
        // Remove all non-digit characters
        String digitsOnly = phoneNumber.replaceAll("[^\\d]", "");
        
        if (digitsOnly.length() < 4) {
            return "***";
        }
        
        // Show last 4 digits only
        String lastFour = digitsOnly.substring(digitsOnly.length() - 4);
        return "***-***-" + lastFour;
    }

    /**
     * Mask IP address for privacy
     */
    private static String maskIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return "***";
        }
        
        // For IPv4, mask last octet
        if (ipAddress.contains(".")) {
            String[] parts = ipAddress.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
            }
        }
        
        // For IPv6 or other formats, mask everything after first part
        if (ipAddress.contains(":")) {
            String[] parts = ipAddress.split(":");
            if (parts.length > 1) {
                return parts[0] + ":***";
            }
        }
        
        return "***";
    }

    // Utility methods for response validation

    /**
     * Check if response indicates success
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Check if response has error
     */
    public boolean hasError() {
        return !success && errorCode != null;
    }

    /**
     * Get patient ID from response data
     */
    public UUID getPatientId() {
        return data != null && data.getPatient() != null ? data.getPatient().getId() : null;
    }

    /**
     * Get session ID from response data
     */
    public UUID getSessionId() {
        return data != null && data.getSession() != null ? data.getSession().getSessionId() : null;
    }

    /**
     * Check if this is a new device login
     */
    public boolean isNewDeviceLogin() {
        return data != null && data.getSession() != null && data.getSession().isNewDevice();
    }

    /**
     * Get access token from response
     */
    public String getAccessToken() {
        return data != null ? data.getAccessToken() : null;
    }

    /**
     * Get refresh token from response
     */
    public String getRefreshToken() {
        return data != null ? data.getRefreshToken() : null;
    }

    /**
     * Create response for successful token refresh
     */
    public static PatientLoginResponse refreshSuccess(String newAccessToken, long expiresIn) {
        PatientLoginData loginData = new PatientLoginData();
        loginData.setAccessToken(newAccessToken);
        loginData.setExpiresIn(expiresIn);
        loginData.setTokenType("Bearer");
        
        return new PatientLoginResponse(
            true,
            "Token refreshed successfully",
            loginData,
            null
        );
    }

    /**
     * Create response for successful logout
     */
    public static PatientLoginResponse logoutSuccess() {
        return new PatientLoginResponse(
            true,
            "Logged out successfully",
            null,
            null
        );
    }
} 