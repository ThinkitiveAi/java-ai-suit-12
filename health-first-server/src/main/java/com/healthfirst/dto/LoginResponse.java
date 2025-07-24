package com.healthfirst.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for provider login response
 * Contains authentication tokens and provider information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    // Utility methods
    private boolean success;
    private String message;
    private LoginData data;
    private String errorCode; // For error responses

    /**
     * Inner class for successful login data
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginData {
        private String accessToken;
        private String refreshToken;
        private long expiresIn; // seconds until access token expires
        private String tokenType = "Bearer";
        private ProviderInfo provider;
    }

    /**
     * Inner class for provider information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderInfo {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String specialization;
        private String verificationStatus;
        private Integer yearsOfExperience;
        private boolean isActive = true;
        private LocalDateTime lastLogin;
        private Integer loginCount;
        
        // Address information (optional - can be null for privacy)
        private AddressInfo clinicAddress;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AddressInfo {
            private String city;
            private String state;
            // Note: We don't include full address for privacy/security
        }
    }

    // Static factory methods for creating responses

    /**
     * Create successful login response
     */
    public static LoginResponse success(String accessToken, String refreshToken, long expiresIn, 
                                      UUID providerId, String firstName, String lastName, 
                                      String email, String phoneNumber, String specialization, 
                                      String verificationStatus, Integer yearsOfExperience, 
                                      LocalDateTime lastLogin, Integer loginCount,
                                      String city, String state) {
        
        ProviderInfo.AddressInfo addressInfo = null;
        if (city != null && state != null) {
            addressInfo = new ProviderInfo.AddressInfo(city, state);
        }

        ProviderInfo provider = new ProviderInfo(
            providerId, firstName, lastName, email, phoneNumber, 
            specialization, verificationStatus, yearsOfExperience, 
            true, lastLogin, loginCount, addressInfo
        );

        LoginData data = new LoginData(accessToken, refreshToken, expiresIn, "Bearer", provider);
        
        return new LoginResponse(true, "Login successful", data, null);
    }

    /**
     * Create error response with error code
     */
    public static LoginResponse error(String message, String errorCode) {
        return new LoginResponse(false, message, null, errorCode);
    }

    /**
     * Create invalid credentials error
     */
    public static LoginResponse invalidCredentials() {
        return error("Invalid email/phone or password", "INVALID_CREDENTIALS");
    }

    /**
     * Create account not verified error
     */
    public static LoginResponse accountNotVerified() {
        return error("Please verify your email before logging in", "EMAIL_NOT_VERIFIED");
    }

    /**
     * Create account locked error
     */
    public static LoginResponse accountLocked(LocalDateTime lockedUntil) {
        String message = "Account temporarily locked due to failed login attempts";
        if (lockedUntil != null) {
            message += ". Try again after " + lockedUntil.toString();
        }
        return error(message, "ACCOUNT_LOCKED");
    }

    /**
     * Create account inactive error
     */
    public static LoginResponse accountInactive() {
        return error("Account is inactive. Please contact support", "ACCOUNT_INACTIVE");
    }

    /**
     * Create rate limit exceeded error
     */
    public static LoginResponse rateLimitExceeded() {
        return error("Too many login attempts. Please try again later", "RATE_LIMIT_EXCEEDED");
    }

    /**
     * Create internal server error
     */
    public static LoginResponse internalError() {
        return error("Login failed due to an internal error. Please try again", "INTERNAL_ERROR");
    }

    public boolean hasError() {
        return !success && errorCode != null;
    }

    public String getAccessToken() {
        return data != null ? data.getAccessToken() : null;
    }

    public String getRefreshToken() {
        return data != null ? data.getRefreshToken() : null;
    }

    public ProviderInfo getProviderInfo() {
        return data != null ? data.getProvider() : null;
    }
} 