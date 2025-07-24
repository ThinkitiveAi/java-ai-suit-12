package com.healthfirst.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for patient registration response with privacy considerations
 * Designed with HIPAA compliance in mind - limited information exposure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientRegistrationResponse {

    private boolean success;
    private String message;
    private PatientData data;
    private String errorCode; // For error responses

    /**
     * Inner class for successful registration data
     * Contains minimal information for privacy/security
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientData {
        private UUID patientId;
        private String email;
        private String phoneNumber; // Masked for privacy
        private boolean emailVerified;
        private boolean phoneVerified;
        private boolean meetsMinimumAge; // For COPPA compliance indication
        private LocalDateTime registrationTime;
        
        // Privacy-conscious fields (limited exposure)
        private String firstName; // For personalization in responses
        private VerificationInfo verificationInfo;
    }

    /**
     * Inner class for verification information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationInfo {
        private boolean emailVerificationSent;
        private boolean phoneVerificationRequired;
        private String emailVerificationMessage;
        private String phoneVerificationMessage;
        private int phoneVerificationAttempts;
        private int maxPhoneVerificationAttempts = 3;
    }

    // Static factory methods for creating responses

    /**
     * Create successful registration response with minimal information
     */
    public static PatientRegistrationResponse success(
            UUID patientId, 
            String firstName,
            String email, 
            String phoneNumber,
            boolean emailVerified, 
            boolean phoneVerified,
            boolean meetsMinimumAge,
            boolean emailVerificationSent,
            boolean phoneVerificationRequired) {
        
        // Mask phone number for privacy (show only last 4 digits)
        String maskedPhone = maskPhoneNumber(phoneNumber);
        
        VerificationInfo verificationInfo = new VerificationInfo(
            emailVerificationSent,
            phoneVerificationRequired,
            emailVerificationSent ? "Verification email sent to " + maskEmail(email) : null,
            phoneVerificationRequired ? "SMS verification will be sent to " + maskedPhone : null,
            0,
            3
        );

        PatientData data = new PatientData(
            patientId,
            maskEmail(email), // Mask email for response
            maskedPhone,
            emailVerified,
            phoneVerified,
            meetsMinimumAge,
            LocalDateTime.now(),
            firstName, // First name for personalization
            verificationInfo
        );

        return new PatientRegistrationResponse(
            true, 
            String.format("Registration successful! Welcome, %s. Please check your email for verification.", firstName), 
            data, 
            null
        );
    }

    /**
     * Create error response with error code
     */
    public static PatientRegistrationResponse error(String message, String errorCode) {
        return new PatientRegistrationResponse(false, message, null, errorCode);
    }

    /**
     * Create validation error response
     */
    public static PatientRegistrationResponse validationError(String message) {
        return error(message, "VALIDATION_ERROR");
    }

    /**
     * Create duplicate email error
     */
    public static PatientRegistrationResponse duplicateEmail() {
        return error("An account with this email already exists. Please use a different email or try logging in.", "DUPLICATE_EMAIL");
    }

    /**
     * Create duplicate phone error
     */
    public static PatientRegistrationResponse duplicatePhone() {
        return error("An account with this phone number already exists. Please use a different phone number or try logging in.", "DUPLICATE_PHONE");
    }

    /**
     * Create age verification error (COPPA compliance)
     */
    public static PatientRegistrationResponse underageError() {
        return error("You must be at least 13 years old to register for an account.", "UNDERAGE_REGISTRATION");
    }

    /**
     * Create password mismatch error
     */
    public static PatientRegistrationResponse passwordMismatchError() {
        return error("Password and password confirmation do not match.", "PASSWORD_MISMATCH");
    }

    /**
     * Create internal server error
     */
    public static PatientRegistrationResponse internalError() {
        return error("Registration failed due to an internal error. Please try again later.", "INTERNAL_ERROR");
    }

    /**
     * Create rate limit exceeded error
     */
    public static PatientRegistrationResponse rateLimitExceeded() {
        return error("Too many registration attempts. Please try again later.", "RATE_LIMIT_EXCEEDED");
    }

    /**
     * Create email service error
     */
    public static PatientRegistrationResponse emailServiceError() {
        return error("Registration completed, but verification email could not be sent. Please try resending verification email.", "EMAIL_SERVICE_ERROR");
    }

    /**
     * Create SMS service error
     */
    public static PatientRegistrationResponse smsServiceError() {
        return error("Registration completed, but SMS verification could not be sent. Please try phone verification later.", "SMS_SERVICE_ERROR");
    }

    // Utility methods for privacy protection

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
        
        // Remove all non-digit characters for processing
        String digitsOnly = phoneNumber.replaceAll("[^\\d]", "");
        
        if (digitsOnly.length() < 4) {
            return "***";
        }
        
        // Show last 4 digits only
        String lastFour = digitsOnly.substring(digitsOnly.length() - 4);
        return "***-***-" + lastFour;
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
        return data != null ? data.getPatientId() : null;
    }

    /**
     * Check if email verification was sent
     */
    public boolean wasEmailVerificationSent() {
        return data != null && 
               data.getVerificationInfo() != null && 
               data.getVerificationInfo().isEmailVerificationSent();
    }

    /**
     * Check if phone verification is required
     */
    public boolean isPhoneVerificationRequired() {
        return data != null && 
               data.getVerificationInfo() != null && 
               data.getVerificationInfo().isPhoneVerificationRequired();
    }

    /**
     * Get verification instructions for user
     */
    public String getVerificationInstructions() {
        if (data == null || data.getVerificationInfo() == null) {
            return null;
        }
        
        VerificationInfo info = data.getVerificationInfo();
        StringBuilder instructions = new StringBuilder();
        
        if (info.isEmailVerificationSent()) {
            instructions.append("Please check your email for a verification link. ");
        }
        
        if (info.isPhoneVerificationRequired()) {
            instructions.append("You will receive an SMS with a verification code. ");
        }
        
        if (instructions.length() == 0) {
            return "Your account has been created successfully.";
        }
        
        instructions.append("Complete verification to access all features.");
        return instructions.toString();
    }

    /**
     * Create response for verification step completion
     */
    public static PatientRegistrationResponse verificationStepCompleted(String stepName, String nextStep) {
        String message = String.format("%s verification completed successfully.", stepName);
        if (nextStep != null) {
            message += " Next step: " + nextStep;
        }
        return new PatientRegistrationResponse(true, message, null, null);
    }

    /**
     * Create response for verification step failure
     */
    public static PatientRegistrationResponse verificationStepFailed(String stepName, String reason) {
        return error(String.format("%s verification failed: %s", stepName, reason), "VERIFICATION_FAILED");
    }
} 