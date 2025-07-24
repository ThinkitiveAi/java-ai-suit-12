package com.healthfirst.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for patient login request with comprehensive validation and HIPAA compliance
 * Supports login with email or phone number and includes device tracking
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientLoginRequest {

    @NotBlank(message = "Identifier (email or phone) is required")
    @Size(max = 100, message = "Identifier cannot exceed 100 characters")
    private String identifier; // email or phone_number

    @NotBlank(message = "Password is required")
    @Size(max = 255, message = "Password cannot exceed 255 characters")
    private String password;

    private boolean rememberMe = false; // optional, extends token expiry

    @Valid
    private DeviceInfo deviceInfo; // optional, for device tracking and HIPAA compliance

    /**
     * Inner DTO for device information tracking (HIPAA compliance)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceInfo {
        @Size(max = 50, message = "Device type cannot exceed 50 characters")
        private String deviceType; // mobile, desktop, tablet

        @Size(max = 100, message = "Device name cannot exceed 100 characters")
        private String deviceName; // iPhone 12, Chrome Browser, etc.

        @Size(max = 20, message = "App version cannot exceed 20 characters")
        private String appVersion; // 1.0.0

        @Size(max = 100, message = "Operating system cannot exceed 100 characters")
        private String operatingSystem; // iOS 15.0, Windows 11, etc.

        @Size(max = 100, message = "Browser cannot exceed 100 characters")
        private String browser; // Chrome 95.0, Safari 15.0, etc.

        @Size(max = 200, message = "Location info cannot exceed 200 characters")
        private String locationInfo; // Optional, for security monitoring

        /**
         * Check if device info has any information
         */
        public boolean hasAnyInformation() {
            return (deviceType != null && !deviceType.trim().isEmpty()) ||
                   (deviceName != null && !deviceName.trim().isEmpty()) ||
                   (appVersion != null && !appVersion.trim().isEmpty()) ||
                   (operatingSystem != null && !operatingSystem.trim().isEmpty()) ||
                   (browser != null && !browser.trim().isEmpty()) ||
                   (locationInfo != null && !locationInfo.trim().isEmpty());
        }

        /**
         * Get formatted device info string
         */
        public String getFormattedDeviceInfo() {
            StringBuilder info = new StringBuilder();
            
            if (deviceName != null && !deviceName.trim().isEmpty()) {
                info.append(deviceName);
            }
            
            if (operatingSystem != null && !operatingSystem.trim().isEmpty()) {
                if (info.length() > 0) info.append(" on ");
                info.append(operatingSystem);
            }
            
            if (browser != null && !browser.trim().isEmpty()) {
                if (!info.isEmpty()) info.append(" using ");
                info.append(browser);
            }
            
            return !info.isEmpty() ? info.toString() : "Unknown Device";
        }
    }

    // Utility methods

    /**
     * Check if identifier is an email format
     */
    public boolean isEmailIdentifier() {
        if (identifier == null) return false;
        return identifier.contains("@") && identifier.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    /**
     * Check if identifier is a phone number format
     */
    public boolean isPhoneIdentifier() {
        if (identifier == null) return false;
        // Remove all non-digit characters except +
        String cleaned = identifier.replaceAll("[^\\d+]", "");
        return cleaned.matches("^\\+?[1-9]\\d{1,14}$");
    }

    /**
     * Get normalized identifier (lowercase email or cleaned phone)
     */
    public String getNormalizedIdentifier() {
        if (identifier == null) return null;
        
        if (isEmailIdentifier()) {
            return identifier.toLowerCase().trim();
        } else if (isPhoneIdentifier()) {
            // Normalize phone number
            String cleaned = identifier.replaceAll("[^\\d+]", "");
            if (!cleaned.startsWith("+") && cleaned.length() > 10) {
                cleaned = "+" + cleaned;
            }
            return cleaned;
        }
        
        return identifier.trim();
    }

    /**
     * Get identifier type for logging purposes
     */
    public String getIdentifierType() {
        if (isEmailIdentifier()) {
            return "EMAIL";
        } else if (isPhoneIdentifier()) {
            return "PHONE";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Check if device info is provided
     */
    public boolean hasDeviceInfo() {
        return deviceInfo != null && deviceInfo.hasAnyInformation();
    }

    /**
     * Get masked identifier for logging (HIPAA compliance)
     */
    public String getMaskedIdentifier() {
        if (identifier == null) return "***";
        
        if (isEmailIdentifier()) {
            int atIndex = identifier.indexOf('@');
            if (atIndex <= 1) {
                return "***" + identifier.substring(atIndex);
            }
            return identifier.charAt(0) + "***" + identifier.substring(atIndex);
        } else if (isPhoneIdentifier()) {
            String cleaned = identifier.replaceAll("[^\\d]", "");
            if (cleaned.length() < 4) {
                return "***";
            }
            return "***-***-" + cleaned.substring(cleaned.length() - 4);
        }
        
        return "***";
    }

    /**
     * Validate the login request
     */
    public boolean isValidLoginRequest() {
        return identifier != null && !identifier.trim().isEmpty() &&
               password != null && !password.trim().isEmpty() &&
               (isEmailIdentifier() || isPhoneIdentifier());
    }

    /**
     * Get device fingerprint for security tracking
     */
    public String getDeviceFingerprint() {
        if (!hasDeviceInfo()) {
            return "unknown-device";
        }
        
        StringBuilder fingerprint = new StringBuilder();
        if (deviceInfo.getDeviceType() != null) {
            fingerprint.append(deviceInfo.getDeviceType()).append("-");
        }
        if (deviceInfo.getOperatingSystem() != null) {
            fingerprint.append(deviceInfo.getOperatingSystem()).append("-");
        }
        if (deviceInfo.getBrowser() != null) {
            fingerprint.append(deviceInfo.getBrowser());
        }
        
        return fingerprint.toString().replaceAll("[^a-zA-Z0-9-]", "").toLowerCase();
    }
} 