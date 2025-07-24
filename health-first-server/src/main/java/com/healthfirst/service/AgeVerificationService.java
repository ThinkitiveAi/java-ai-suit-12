package com.healthfirst.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Service for age verification and COPPA compliance
 * Ensures patients meet minimum age requirements for healthcare services
 */
@Slf4j
@Service
public class AgeVerificationService {

    @Value("${app.patient.minimum-age:13}")
    private int minimumAge;

    @Value("${app.patient.adult-age:18}")
    private int adultAge;

    @Value("${app.patient.senior-age:65}")
    private int seniorAge;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Verify if patient meets minimum age requirement (COPPA compliance)
     */
    public boolean meetsMinimumAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            log.warn("Date of birth is null for age verification");
            return false;
        }

        int age = calculateAge(dateOfBirth);
        boolean meetsMinimum = age >= minimumAge;
        
        if (!meetsMinimum) {
            log.info("Age verification failed: age {} is below minimum required age {}", age, minimumAge);
        }
        
        return meetsMinimum;
    }

    /**
     * Calculate exact age from date of birth
     */
    public int calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return 0;
        }
        
        LocalDate currentDate = LocalDate.now();
        
        // Validate date of birth is in the past
        if (dateOfBirth.isAfter(currentDate)) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }
        
        return Period.between(dateOfBirth, currentDate).getYears();
    }

    /**
     * Get age category for the patient
     */
    public AgeCategory getAgeCategory(LocalDate dateOfBirth) {
        int age = calculateAge(dateOfBirth);
        
        if (age < minimumAge) {
            return AgeCategory.UNDERAGE;
        } else if (age < adultAge) {
            return AgeCategory.MINOR;
        } else if (age < seniorAge) {
            return AgeCategory.ADULT;
        } else {
            return AgeCategory.SENIOR;
        }
    }

    /**
     * Validate date of birth format and constraints
     */
    public ValidationResult validateDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return ValidationResult.error("Date of birth is required");
        }

        LocalDate currentDate = LocalDate.now();
        
        // Check if date is in the future
        if (dateOfBirth.isAfter(currentDate)) {
            return ValidationResult.error("Date of birth cannot be in the future");
        }

        // Check if date is too far in the past (unrealistic age)
        LocalDate minimumDate = currentDate.minusYears(150);
        if (dateOfBirth.isBefore(minimumDate)) {
            return ValidationResult.error("Date of birth is too far in the past");
        }

        // Calculate age
        int age = calculateAge(dateOfBirth);
        
        // Check COPPA compliance
        if (age < minimumAge) {
            return ValidationResult.error(
                String.format("You must be at least %d years old to register. Your age: %d", minimumAge, age)
            );
        }

        return ValidationResult.success(String.format("Age verification passed. Age: %d", age));
    }

    /**
     * Check if patient requires parental consent (for minors)
     */
    public boolean requiresParentalConsent(LocalDate dateOfBirth) {
        int age = calculateAge(dateOfBirth);
        return age >= minimumAge && age < adultAge;
    }

    /**
     * Get special considerations based on age
     */
    public AgeConsiderations getAgeConsiderations(LocalDate dateOfBirth) {
        AgeCategory category = getAgeCategory(dateOfBirth);
        int age = calculateAge(dateOfBirth);
        
        return switch (category) {
            case UNDERAGE -> new AgeConsiderations(
                false, // Cannot register
                false, // No parental consent needed (cannot register)
                true,  // Special privacy protections
                "Registration not allowed for users under " + minimumAge
            );
            case MINOR -> new AgeConsiderations(
                true,  // Can register
                true,  // Requires parental consent
                true,  // Special privacy protections
                "Requires parental consent and enhanced privacy protections"
            );
            case ADULT -> new AgeConsiderations(
                true,  // Can register
                false, // No parental consent needed
                false, // Standard privacy protections
                "Standard registration and privacy protections"
            );
            case SENIOR -> new AgeConsiderations(
                true,  // Can register
                false, // No parental consent needed
                false, // Standard privacy protections
                "May benefit from accessibility features and additional support"
            );
        };
    }

    /**
     * Format age for display purposes
     */
    public String formatAgeDisplay(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return "Unknown";
        }
        
        int age = calculateAge(dateOfBirth);
        AgeCategory category = getAgeCategory(dateOfBirth);
        
        return String.format("%d years old (%s)", age, category.getDisplayName());
    }

    /**
     * Check if date of birth indicates a possible data entry error
     */
    public boolean isPossibleDataEntryError(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return true;
        }
        
        int age = calculateAge(dateOfBirth);
        
        // Flag potential errors
        return age > 120 || // Unrealistically old
               age < 0 ||   // Future date
               dateOfBirth.equals(LocalDate.of(1900, 1, 1)) || // Common default date
               dateOfBirth.equals(LocalDate.of(2000, 1, 1));   // Common default date
    }

    /**
     * Get age-appropriate terms of service version
     */
    public String getAgeAppropriateTermsVersion(LocalDate dateOfBirth) {
        AgeCategory category = getAgeCategory(dateOfBirth);
        
        return switch (category) {
            case UNDERAGE -> null; // Cannot register
            case MINOR -> "terms_minor_v1.0";
            case ADULT, SENIOR -> "terms_adult_v1.0";
        };
    }

    // Enums and inner classes

    public enum AgeCategory {
        UNDERAGE("Under " + 13), // Below minimum age
        MINOR("Minor"),          // 13-17
        ADULT("Adult"),          // 18-64
        SENIOR("Senior");        // 65+

        private final String displayName;

        AgeCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult success(String message) {
            return new ValidationResult(true, message);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class AgeConsiderations {
        private final boolean canRegister;
        private final boolean requiresParentalConsent;
        private final boolean requiresEnhancedPrivacy;
        private final String specialInstructions;

        public AgeConsiderations(boolean canRegister, boolean requiresParentalConsent, 
                               boolean requiresEnhancedPrivacy, String specialInstructions) {
            this.canRegister = canRegister;
            this.requiresParentalConsent = requiresParentalConsent;
            this.requiresEnhancedPrivacy = requiresEnhancedPrivacy;
            this.specialInstructions = specialInstructions;
        }

        // Getters
        public boolean canRegister() { return canRegister; }
        public boolean requiresParentalConsent() { return requiresParentalConsent; }
        public boolean requiresEnhancedPrivacy() { return requiresEnhancedPrivacy; }
        public String getSpecialInstructions() { return specialInstructions; }
    }
} 