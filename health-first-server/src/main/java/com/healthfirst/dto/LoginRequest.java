package com.healthfirst.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for provider login request
 * Supports login with email or phone number + password
 * Includes optional device information and remember me functionality
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Identifier (email or phone) is required")
    @Size(max = 100, message = "Identifier cannot exceed 100 characters")
    private String identifier; // email or phone_number

    @NotBlank(message = "Password is required")
    @Size(max = 255, message = "Password cannot exceed 255 characters")
    private String password;

    private boolean rememberMe = false; // optional, extends token expiry

    @Valid
    private DeviceInfo deviceInfo; // optional, for device tracking

    /**
     * Inner class for device information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceInfo {
        
        @Size(max = 50, message = "Device type cannot exceed 50 characters")
        private String deviceType; // mobile, desktop, tablet, etc.

        @Size(max = 100, message = "Device name cannot exceed 100 characters")
        private String deviceName; // iPhone 12, Chrome Browser, etc.

        @Size(max = 20, message = "App version cannot exceed 20 characters")
        private String appVersion; // 1.0.0, etc.

        @Size(max = 100, message = "Operating system cannot exceed 100 characters")
        private String operatingSystem; // iOS 15.0, Windows 11, etc.

        @Size(max = 100, message = "Browser cannot exceed 100 characters")
        private String browser; // Chrome 96.0, Safari 15.0, etc.
    }

    // Utility methods
    public boolean hasDeviceInfo() {
        return deviceInfo != null;
    }

    public String getDeviceType() {
        return hasDeviceInfo() ? deviceInfo.getDeviceType() : null;
    }

    public String getDeviceName() {
        return hasDeviceInfo() ? deviceInfo.getDeviceName() : null;
    }

    public String getAppVersion() {
        return hasDeviceInfo() ? deviceInfo.getAppVersion() : null;
    }

    public String getOperatingSystem() {
        return hasDeviceInfo() ? deviceInfo.getOperatingSystem() : null;
    }

    public String getBrowser() {
        return hasDeviceInfo() ? deviceInfo.getBrowser() : null;
    }

    // Helper methods for validation
    public boolean isEmailIdentifier() {
        return identifier != null && identifier.contains("@");
    }

    public boolean isPhoneIdentifier() {
        return identifier != null && !identifier.contains("@") && 
               (identifier.startsWith("+") || identifier.matches("\\d+"));
    }
} 