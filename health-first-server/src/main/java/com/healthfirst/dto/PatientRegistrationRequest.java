package com.healthfirst.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for patient registration request with comprehensive validation
 * Includes COPPA compliance for age verification and HIPAA considerations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientRegistrationRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "First name contains invalid characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Last name contains invalid characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
             message = "Password must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "^(male|female|other|prefer_not_to_say)$", message = "Invalid gender value")
    private String gender;

    // Address information (required)
    @NotNull(message = "Address is required")
    @Valid
    private AddressDto address;

    // Emergency contact information (optional)
    @Valid
    private EmergencyContactDto emergencyContact;

    // Medical history (optional)
    private List<@Size(max = 500, message = "Medical condition cannot exceed 500 characters") String> medicalHistory;

    // Insurance information (optional)
    @Valid
    private InsuranceInfoDto insuranceInfo;

    // Privacy and consent flags
    private Boolean marketingConsent = false;
    private Boolean dataSharingConsent = false;

    /**
     * Inner DTO for address information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        
        @NotBlank(message = "Street address is required")
        @Size(max = 200, message = "Street address cannot exceed 200 characters")
        private String street;

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "City contains invalid characters")
        private String city;

        @NotBlank(message = "State is required")
        @Size(max = 50, message = "State cannot exceed 50 characters")
        @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "State contains invalid characters")
        private String state;

        @NotBlank(message = "ZIP code is required")
        @Pattern(regexp = "^\\d{5}(-\\d{4})?$", message = "Invalid ZIP code format (use XXXXX or XXXXX-XXXX)")
        private String zip;
    }

    /**
     * Inner DTO for emergency contact information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmergencyContactDto {
        
        @Size(max = 100, message = "Emergency contact name cannot exceed 100 characters")
        @Pattern(regexp = "^[a-zA-Z\\s\\-']*$", message = "Emergency contact name contains invalid characters")
        private String name;

        @Pattern(regexp = "^(\\+?[1-9]\\d{1,14})?$", message = "Invalid emergency contact phone number format")
        private String phone;

        @Size(max = 50, message = "Relationship cannot exceed 50 characters")
        @Pattern(regexp = "^[a-zA-Z\\s\\-']*$", message = "Relationship contains invalid characters")
        private String relationship;

        // Utility method to check if emergency contact has any information
        public boolean hasAnyInformation() {
            return (name != null && !name.trim().isEmpty()) ||
                   (phone != null && !phone.trim().isEmpty()) ||
                   (relationship != null && !relationship.trim().isEmpty());
        }

        // Utility method to check if emergency contact is complete
        public boolean isComplete() {
            return name != null && !name.trim().isEmpty() &&
                   phone != null && !phone.trim().isEmpty() &&
                   relationship != null && !relationship.trim().isEmpty();
        }
    }

    /**
     * Inner DTO for insurance information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsuranceInfoDto {
        
        @Size(max = 100, message = "Insurance provider name cannot exceed 100 characters")
        @Pattern(regexp = "^[a-zA-Z0-9\\s\\-&'.,]*$", message = "Insurance provider contains invalid characters")
        private String provider;

        @Size(max = 50, message = "Policy number cannot exceed 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9\\-]*$", message = "Policy number contains invalid characters")
        private String policyNumber;

        @Size(max = 50, message = "Group number cannot exceed 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9\\-]*$", message = "Group number contains invalid characters")
        private String groupNumber;

        @Size(max = 100, message = "Plan name cannot exceed 100 characters")
        private String planName;

        @Size(max = 100, message = "Member ID cannot exceed 100 characters")
        @Pattern(regexp = "^[a-zA-Z0-9\\-]*$", message = "Member ID contains invalid characters")
        private String memberId;

        // Utility method to check if insurance has any information
        public boolean hasAnyInformation() {
            return (provider != null && !provider.trim().isEmpty()) ||
                   (policyNumber != null && !policyNumber.trim().isEmpty()) ||
                   (groupNumber != null && !groupNumber.trim().isEmpty()) ||
                   (planName != null && !planName.trim().isEmpty()) ||
                   (memberId != null && !memberId.trim().isEmpty());
        }

        // Utility method to check if insurance information is complete
        public boolean isComplete() {
            return provider != null && !provider.trim().isEmpty() &&
                   policyNumber != null && !policyNumber.trim().isEmpty();
        }
    }

    // Utility methods for the main DTO

    /**
     * Check if passwords match
     */
    public boolean doPasswordsMatch() {
        return password != null && password.equals(confirmPassword);
    }

    /**
     * Calculate age from date of birth
     */
    public int getAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return java.time.Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * Check if patient meets minimum age requirement (COPPA compliance)
     */
    public boolean meetsMinimumAge() {
        return getAge() >= 13;
    }

    /**
     * Get normalized email (lowercase, trimmed)
     */
    public String getNormalizedEmail() {
        return email != null ? email.toLowerCase().trim() : null;
    }

    /**
     * Get normalized phone number
     */
    public String getNormalizedPhoneNumber() {
        if (phoneNumber == null) return null;
        
        String normalized = phoneNumber.trim();
        // Remove any non-digit characters except +
        normalized = normalized.replaceAll("[^\\d+]", "");
        
        // Ensure it starts with + if it's an international number
        if (!normalized.startsWith("+") && normalized.length() > 10) {
            normalized = "+" + normalized;
        }
        
        return normalized;
    }

    /**
     * Check if emergency contact information is provided
     */
    public boolean hasEmergencyContact() {
        return emergencyContact != null && emergencyContact.hasAnyInformation();
    }

    /**
     * Check if insurance information is provided
     */
    public boolean hasInsuranceInfo() {
        return insuranceInfo != null && insuranceInfo.hasAnyInformation();
    }

    /**
     * Check if medical history is provided
     */
    public boolean hasMedicalHistory() {
        return medicalHistory != null && !medicalHistory.isEmpty() &&
               medicalHistory.stream().anyMatch(condition -> condition != null && !condition.trim().isEmpty());
    }

    /**
     * Get sanitized medical history (remove empty entries)
     */
    public List<String> getSanitizedMedicalHistory() {
        if (medicalHistory == null) return List.of();
        
        return medicalHistory.stream()
                .filter(condition -> condition != null && !condition.trim().isEmpty())
                .map(String::trim)
                .toList();
    }

    /**
     * Validate gender enum value
     */
    public boolean isValidGender() {
        if (gender == null) return false;
        return gender.matches("^(male|female|other|prefer_not_to_say)$");
    }

    /**
     * Convert gender string to enum format
     */
    public String getGenderForEntity() {
        if (gender == null) return null;
        
        return switch (gender.toLowerCase()) {
            case "male" -> "MALE";
            case "female" -> "FEMALE";
            case "other" -> "OTHER";
            case "prefer_not_to_say" -> "PREFER_NOT_TO_SAY";
            default -> throw new IllegalArgumentException("Invalid gender: " + gender);
        };
    }

    /**
     * Check if the request has all required information for registration
     */
    public boolean isValidForRegistration() {
        return firstName != null && !firstName.trim().isEmpty() &&
               lastName != null && !lastName.trim().isEmpty() &&
               email != null && !email.trim().isEmpty() &&
               phoneNumber != null && !phoneNumber.trim().isEmpty() &&
               password != null && !password.trim().isEmpty() &&
               confirmPassword != null && !confirmPassword.trim().isEmpty() &&
               dateOfBirth != null &&
               gender != null && !gender.trim().isEmpty() &&
               address != null &&
               doPasswordsMatch() &&
               meetsMinimumAge() &&
               isValidGender();
    }
} 