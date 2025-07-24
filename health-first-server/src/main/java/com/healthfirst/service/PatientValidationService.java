package com.healthfirst.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.healthfirst.dto.PatientRegistrationRequest;
import com.healthfirst.exception.ValidationException;
import com.healthfirst.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for comprehensive patient registration validation
 * Includes HIPAA compliance considerations and medical data validation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientValidationService {

    private final PatientRepository patientRepository;
    private final AgeVerificationService ageVerificationService;

    // Common medical conditions for validation (HIPAA-compliant - no specific patient data)
    private static final List<String> COMMON_MEDICAL_CONDITIONS = Arrays.asList(
        "diabetes", "hypertension", "high blood pressure", "asthma", "arthritis",
        "heart disease", "depression", "anxiety", "migraine", "allergies",
        "high cholesterol", "obesity", "osteoporosis", "copd", "thyroid disorder",
        "kidney disease", "liver disease", "cancer", "stroke", "epilepsy",
        "fibromyalgia", "lupus", "multiple sclerosis", "parkinson's disease",
        "alzheimer's disease", "bipolar disorder", "schizophrenia", "autism",
        "adhd", "sleep apnea", "acid reflux", "irritable bowel syndrome",
        "crohn's disease", "ulcerative colitis", "celiac disease", "psoriasis",
        "eczema", "chronic fatigue syndrome", "chronic pain", "back pain"
    );

    // Suspicious patterns that might indicate inappropriate data entry
    private static final List<Pattern> SUSPICIOUS_PATTERNS = Arrays.asList(
        Pattern.compile("(?i)test|fake|example|sample|dummy"),
        Pattern.compile("(?i)none|n/a|null|undefined"),
        Pattern.compile("^\\s*$"), // Empty or whitespace only
        Pattern.compile("^.{1}$"), // Single character
        Pattern.compile("(.)\\1{3,}") // Repeated characters (aaaa, bbbb, etc.)
    );

    /**
     * Comprehensive validation of patient registration request
     */
    public void validatePatientRegistration(PatientRegistrationRequest request) {
        List<String> errors = new ArrayList<>();

        try {
            // Basic field validation
            validateBasicFields(request, errors);

            // Age and COPPA compliance validation
            validateAge(request, errors);

            // Password validation
            validatePasswords(request, errors);

            // Contact information validation
            validateContactInformation(request, errors);

            // Address validation
            validateAddress(request, errors);

            // Medical data validation (if provided)
            validateMedicalData(request, errors);

            // Insurance information validation (if provided)
            validateInsuranceInformation(request, errors);

            // Emergency contact validation (if provided)
            validateEmergencyContact(request, errors);

            // Check for suspicious data patterns
            validateDataIntegrity(request, errors);

            // Check for duplicates
            validateUniqueness(request, errors);

            if (!errors.isEmpty()) {
                throw new ValidationException("Patient registration validation failed", errors);
            }

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during patient validation", e);
            throw new ValidationException("Validation failed due to an internal error");
        }
    }

    private void validateBasicFields(PatientRegistrationRequest request, List<String> errors) {
        // First name validation
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            errors.add("First name is required");
        } else if (request.getFirstName().length() < 2 || request.getFirstName().length() > 50) {
            errors.add("First name must be between 2 and 50 characters");
        } else if (!request.getFirstName().matches("^[a-zA-Z\\s'-]+$")) {
            errors.add("First name contains invalid characters");
        }

        // Last name validation
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            errors.add("Last name is required");
        } else if (request.getLastName().length() < 2 || request.getLastName().length() > 50) {
            errors.add("Last name must be between 2 and 50 characters");
        } else if (!request.getLastName().matches("^[a-zA-Z\\s'-]+$")) {
            errors.add("Last name contains invalid characters");
        }

        // Gender validation
        if (request.getGender() == null || request.getGender().trim().isEmpty()) {
            errors.add("Gender is required");
        } else if (!request.isValidGender()) {
            errors.add("Invalid gender value. Must be one of: male, female, other, prefer_not_to_say");
        }
    }

    private void validateAge(PatientRegistrationRequest request, List<String> errors) {
        if (request.getDateOfBirth() == null) {
            errors.add("Date of birth is required");
            return;
        }

        AgeVerificationService.ValidationResult ageResult = ageVerificationService.validateDateOfBirth(request.getDateOfBirth());
        if (!ageResult.isValid()) {
            errors.add(ageResult.getMessage());
        }

        // Additional age-related validations
        if (ageVerificationService.isPossibleDataEntryError(request.getDateOfBirth())) {
            errors.add("Date of birth appears to be invalid. Please verify the date is correct.");
        }
    }

    private void validatePasswords(PatientRegistrationRequest request, List<String> errors) {
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            errors.add("Password is required");
            return;
        }

        if (request.getConfirmPassword() == null || request.getConfirmPassword().trim().isEmpty()) {
            errors.add("Password confirmation is required");
            return;
        }

        if (!request.doPasswordsMatch()) {
            errors.add("Password and password confirmation do not match");
        }

        // Password strength validation
        if (request.getPassword().length() < 8) {
            errors.add("Password must be at least 8 characters long");
        }

        if (!request.getPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
            errors.add("Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character");
        }

        // Check for common weak passwords
        if (isWeakPassword(request.getPassword())) {
            errors.add("Password is too common or weak. Please choose a stronger password");
        }
    }

    private void validateContactInformation(PatientRegistrationRequest request, List<String> errors) {
        // Email validation
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            errors.add("Email is required");
        } else {
            String email = request.getNormalizedEmail();
            if (!isValidEmail(email)) {
                errors.add("Invalid email format");
            }
            if (email.length() > 100) {
                errors.add("Email cannot exceed 100 characters");
            }
        }

        // Phone number validation
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            errors.add("Phone number is required");
        } else {
            validatePhoneNumber(request.getPhoneNumber(), errors);
        }
    }

    private void validatePhoneNumber(String phoneNumber, List<String> errors) {
        try {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber number = phoneUtil.parse(phoneNumber, "US");
            
            if (!phoneUtil.isValidNumber(number)) {
                errors.add("Invalid phone number format");
            }
        } catch (NumberParseException e) {
            errors.add("Invalid phone number format");
        }
    }

    private void validateAddress(PatientRegistrationRequest request, List<String> errors) {
        if (request.getAddress() == null) {
            errors.add("Address is required");
            return;
        }

        PatientRegistrationRequest.AddressDto address = request.getAddress();

        if (address.getStreet() == null || address.getStreet().trim().isEmpty()) {
            errors.add("Street address is required");
        } else if (address.getStreet().length() > 200) {
            errors.add("Street address cannot exceed 200 characters");
        }

        if (address.getCity() == null || address.getCity().trim().isEmpty()) {
            errors.add("City is required");
        } else if (address.getCity().length() > 100) {
            errors.add("City cannot exceed 100 characters");
        } else if (!address.getCity().matches("^[a-zA-Z\\s\\-']+$")) {
            errors.add("City contains invalid characters");
        }

        if (address.getState() == null || address.getState().trim().isEmpty()) {
            errors.add("State is required");
        } else if (address.getState().length() > 50) {
            errors.add("State cannot exceed 50 characters");
        } else if (!address.getState().matches("^[a-zA-Z\\s\\-']+$")) {
            errors.add("State contains invalid characters");
        }

        if (address.getZip() == null || address.getZip().trim().isEmpty()) {
            errors.add("ZIP code is required");
        } else if (!address.getZip().matches("^\\d{5}(-\\d{4})?$")) {
            errors.add("Invalid ZIP code format. Use XXXXX or XXXXX-XXXX");
        }
    }

    private void validateMedicalData(PatientRegistrationRequest request, List<String> errors) {
        if (request.getMedicalHistory() != null && !request.getMedicalHistory().isEmpty()) {
            List<String> medicalHistory = request.getSanitizedMedicalHistory();
            
            for (String condition : medicalHistory) {
                if (condition.length() > 500) {
                    errors.add("Medical condition cannot exceed 500 characters");
                }
                
                // Check for suspicious patterns
                if (containsSuspiciousPattern(condition)) {
                    errors.add("Medical history contains inappropriate content");
                }
                
                // Basic validation for medical terms
                if (!isReasonableMedicalCondition(condition)) {
                    errors.add("Medical condition '" + condition + "' does not appear to be a valid medical term");
                }
            }
            
            // Limit number of conditions
            if (medicalHistory.size() > 20) {
                errors.add("Too many medical conditions listed. Please limit to 20 most relevant conditions.");
            }
        }
    }

    private void validateInsuranceInformation(PatientRegistrationRequest request, List<String> errors) {
        if (request.hasInsuranceInfo()) {
            PatientRegistrationRequest.InsuranceInfoDto insurance = request.getInsuranceInfo();
            
            if (insurance.getProvider() != null && !insurance.getProvider().trim().isEmpty()) {
                if (insurance.getProvider().length() > 100) {
                    errors.add("Insurance provider name cannot exceed 100 characters");
                }
                if (!insurance.getProvider().matches("^[a-zA-Z0-9\\s\\-&'.,]*$")) {
                    errors.add("Insurance provider contains invalid characters");
                }
            }
            
            if (insurance.getPolicyNumber() != null && !insurance.getPolicyNumber().trim().isEmpty()) {
                if (insurance.getPolicyNumber().length() > 50) {
                    errors.add("Policy number cannot exceed 50 characters");
                }
                if (!insurance.getPolicyNumber().matches("^[a-zA-Z0-9\\-]*$")) {
                    errors.add("Policy number contains invalid characters");
                }
            }
        }
    }

    private void validateEmergencyContact(PatientRegistrationRequest request, List<String> errors) {
        if (request.hasEmergencyContact()) {
            PatientRegistrationRequest.EmergencyContactDto contact = request.getEmergencyContact();
            
            if (contact.getName() != null && !contact.getName().trim().isEmpty()) {
                if (contact.getName().length() > 100) {
                    errors.add("Emergency contact name cannot exceed 100 characters");
                }
                if (!contact.getName().matches("^[a-zA-Z\\s\\-']*$")) {
                    errors.add("Emergency contact name contains invalid characters");
                }
            }
            
            if (contact.getPhone() != null && !contact.getPhone().trim().isEmpty()) {
                validatePhoneNumber(contact.getPhone(), errors);
            }
            
            if (contact.getRelationship() != null && !contact.getRelationship().trim().isEmpty()) {
                if (contact.getRelationship().length() > 50) {
                    errors.add("Relationship cannot exceed 50 characters");
                }
                if (!contact.getRelationship().matches("^[a-zA-Z\\s\\-']*$")) {
                    errors.add("Relationship contains invalid characters");
                }
            }
        }
    }

    private void validateDataIntegrity(PatientRegistrationRequest request, List<String> errors) {
        // Check for suspicious patterns in critical fields
        if (containsSuspiciousPattern(request.getFirstName())) {
            errors.add("First name contains inappropriate content");
        }
        
        if (containsSuspiciousPattern(request.getLastName())) {
            errors.add("Last name contains inappropriate content");
        }
        
        if (containsSuspiciousPattern(request.getEmail())) {
            errors.add("Email contains inappropriate content");
        }
    }

    private void validateUniqueness(PatientRegistrationRequest request, List<String> errors) {
        // Check email uniqueness
        if (request.getNormalizedEmail() != null && patientRepository.existsByEmail(request.getNormalizedEmail())) {
            errors.add("An account with this email address already exists");
        }
        
        // Check phone uniqueness
        if (request.getNormalizedPhoneNumber() != null && patientRepository.existsByPhoneNumber(request.getNormalizedPhoneNumber())) {
            errors.add("An account with this phone number already exists");
        }
    }

    // Helper methods

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    private boolean isWeakPassword(String password) {
        List<String> weakPasswords = Arrays.asList(
            "password", "123456", "password123", "admin", "qwerty",
            "12345678", "111111", "123123", "password1", "1234567890"
        );
        return weakPasswords.contains(password.toLowerCase());
    }

    private boolean containsSuspiciousPattern(String text) {
        if (text == null) return false;
        
        return SUSPICIOUS_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(text).find());
    }

    private boolean isReasonableMedicalCondition(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            return false;
        }
        
        String normalizedCondition = condition.toLowerCase().trim();
        
        // Check if it's a known common condition
        if (COMMON_MEDICAL_CONDITIONS.contains(normalizedCondition)) {
            return true;
        }
        
        // Check if it contains medical-sounding terms
        return normalizedCondition.matches(".*\\b(disease|disorder|syndrome|condition|pain|itis|osis|emia|uria)\\b.*") ||
               normalizedCondition.length() >= 3; // Allow other conditions if they're reasonable length
    }
} 