package com.healthfirst.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.healthfirst.dto.ProviderRegistrationRequest;
import com.healthfirst.exception.ValidationException;
import com.healthfirst.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final ProviderRepository providerRepository;
    
    // Predefined specializations list
    private static final List<String> VALID_SPECIALIZATIONS = Arrays.asList(
        "cardiology", "dermatology", "endocrinology", "gastroenterology", "hematology",
        "infectious diseases", "nephrology", "neurology", "oncology", "pulmonology",
        "rheumatology", "family medicine", "internal medicine", "pediatrics", "obstetrics and gynecology",
        "psychiatry", "surgery", "orthopedics", "anesthesiology", "radiology", "pathology",
        "emergency medicine", "urology", "ophthalmology", "otolaryngology", "plastic surgery"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern LICENSE_PATTERN = Pattern.compile("^[A-Za-z0-9]+$");

    /**
     * Validate provider registration request
     */
    public void validateProviderRegistration(ProviderRegistrationRequest request) {
        List<String> errors = new ArrayList<>();

        // Basic field validation
        validateBasicFields(request, errors);
        
        // Password confirmation validation
        validatePasswordConfirmation(request, errors);
        
        // Uniqueness validation
        validateUniqueness(request, errors);
        
        // Phone number validation
        validatePhoneNumber(request.getPhoneNumber(), errors);
        
        // Specialization validation
        validateSpecialization(request.getSpecialization(), errors);

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed", errors);
        }
    }

    private void validateBasicFields(ProviderRegistrationRequest request, List<String> errors) {
        // Email format validation (additional to annotation)
        if (request.getEmail() != null && !EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            errors.add("Email format is invalid");
        }

        // License number validation
        if (request.getLicenseNumber() != null && !LICENSE_PATTERN.matcher(request.getLicenseNumber()).matches()) {
            errors.add("License number must contain only alphanumeric characters");
        }

        // Years of experience validation
        if (request.getYearsOfExperience() != null && 
            (request.getYearsOfExperience() < 0 || request.getYearsOfExperience() > 50)) {
            errors.add("Years of experience must be between 0 and 50");
        }
    }

    private void validatePasswordConfirmation(ProviderRegistrationRequest request, List<String> errors) {
        if (request.getPassword() != null && request.getConfirmPassword() != null) {
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                errors.add("Password and confirmation password do not match");
            }
        }
    }

    private void validateUniqueness(ProviderRegistrationRequest request, List<String> errors) {
        // Check email uniqueness
        if (request.getEmail() != null && providerRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            errors.add("Email is already registered");
        }

        // Check phone number uniqueness
        if (request.getPhoneNumber() != null && providerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            errors.add("Phone number is already registered");
        }

        // Check license number uniqueness
        if (request.getLicenseNumber() != null && providerRepository.existsByLicenseNumber(request.getLicenseNumber().toUpperCase())) {
            errors.add("License number is already registered");
        }
    }

    private void validatePhoneNumber(String phoneNumber, List<String> errors) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return; // Will be caught by @NotBlank
        }

        try {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber number = phoneUtil.parse(phoneNumber, null);
            
            if (!phoneUtil.isValidNumber(number)) {
                errors.add("Phone number is not valid");
            }
        } catch (NumberParseException e) {
            errors.add("Phone number format is invalid");
        }
    }

    private void validateSpecialization(String specialization, List<String> errors) {
        if (specialization == null || specialization.trim().isEmpty()) {
            return; // Will be caught by @NotBlank
        }

        String lowerSpecialization = specialization.toLowerCase().trim();
        if (!VALID_SPECIALIZATIONS.contains(lowerSpecialization)) {
            errors.add("Specialization must be from the predefined list: " + String.join(", ", VALID_SPECIALIZATIONS));
        }
    }

    /**
     * Sanitize input data to prevent injection attacks
     */
    public String sanitizeInput(String input) {
        if (input == null) return null;
        
        // Remove potentially dangerous characters
        return input.trim()
                   .replaceAll("[<>\"'&]", "") // Remove HTML/SQL injection chars
                   .replaceAll("\\s+", " "); // Normalize whitespace
    }

    /**
     * Normalize email address
     */
    public String normalizeEmail(String email) {
        if (email == null) return null;
        return sanitizeInput(email).toLowerCase();
    }

    /**
     * Normalize phone number to international format
     */
    public String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;
        
        try {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber number = phoneUtil.parse(phoneNumber, null);
            return phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            return sanitizeInput(phoneNumber); // Return sanitized version if parsing fails
        }
    }

    /**
     * Validate ZIP code format
     */
    public boolean isValidZipCode(String zipCode) {
        if (zipCode == null) return false;
        return zipCode.matches("^\\d{5}(-\\d{4})?$");
    }

    /**
     * Get list of valid specializations
     */
    public List<String> getValidSpecializations() {
        return new ArrayList<>(VALID_SPECIALIZATIONS);
    }
} 