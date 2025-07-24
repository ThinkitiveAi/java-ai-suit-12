package com.healthfirst.service;

import com.healthfirst.dto.PatientRegistrationRequest;
import com.healthfirst.dto.PatientRegistrationResponse;
import com.healthfirst.entity.*;
import com.healthfirst.exception.ValidationException;
import com.healthfirst.repository.PatientRepository;
import com.healthfirst.util.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for patient registration and management with HIPAA compliance
 * Handles patient registration, verification, and data management
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientValidationService patientValidationService;
    private final AgeVerificationService ageVerificationService;
    private final EmailService emailService;
    private final PasswordUtils passwordUtils;
    private final AuditService auditService;

    @Value("${app.patient.email-verification-required:true}")
    private boolean emailVerificationRequired;

    @Value("${app.patient.phone-verification-required:false}")
    private boolean phoneVerificationRequired;

    @Value("${app.patient.auto-activate:false}")
    private boolean autoActivateAccount;

    /**
     * Register a new patient with comprehensive validation and security
     */
    public PatientRegistrationResponse registerPatient(PatientRegistrationRequest request, String clientIp) {
        try {
            // Comprehensive validation
            patientValidationService.validatePatientRegistration(request);

            // Create patient entity from request
            Patient patient = createPatientFromRequest(request);

            // Save patient to database
            Patient savedPatient = patientRepository.save(patient);

            // TODO: Re-enable email/SMS verification when services are configured
            // Send verification emails/SMS
            /*
            boolean emailVerificationSent = sendEmailVerification(savedPatient);
            boolean phoneVerificationSent = sendPhoneVerification(savedPatient);
            */
            
            // TEMPORARY: Auto-verify for testing
            boolean emailVerificationSent = true; // Simulated
            boolean phoneVerificationSent = true; // Simulated
            
            savedPatient.setEmailVerified(true);
            savedPatient.setPhoneVerified(true);
            savedPatient.setIsActive(true);
            patientRepository.save(savedPatient);

            // Log successful registration
            auditService.logPatientRegistration(
                savedPatient.getId(), 
                savedPatient.getEmail(), 
                clientIp, 
                true
            );

            // Create success response
            return PatientRegistrationResponse.success(
                savedPatient.getId(),
                savedPatient.getFirstName(),
                savedPatient.getEmail(),
                savedPatient.getPhoneNumber(),
                savedPatient.getEmailVerified(),
                savedPatient.getPhoneVerified(),
                savedPatient.meetsMinimumAge(),
                emailVerificationSent,
                phoneVerificationRequired && phoneVerificationSent
            );

        } catch (ValidationException e) {
            auditService.logPatientRegistration(null, request.getEmail(), clientIp, false);
            throw e;
        } catch (Exception e) {
            log.error("Patient registration failed for email: {} from IP: {}", request.getEmail(), clientIp, e);
            auditService.logPatientRegistration(null, request.getEmail(), clientIp, false);
            return PatientRegistrationResponse.internalError();
        }
    }

    /**
     * Verify patient email using verification token
     */
    public boolean verifyPatientEmail(String token) {
        try {
            Optional<Patient> patientOpt = patientRepository.findByEmailVerificationToken(token, LocalDateTime.now());
            
            if (patientOpt.isEmpty()) {
                log.warn("Invalid or expired email verification token: {}", token);
                return false;
            }

            Patient patient = patientOpt.get();
            
            // Mark email as verified
            int updatedRows = patientRepository.markEmailAsVerified(patient.getId());
            
            if (updatedRows > 0) {
                // Send welcome email
                try {
                    emailService.sendWelcomeEmail(patient.getEmail(), patient.getFirstName(), patient.getLastName());
                } catch (Exception e) {
                    log.warn("Failed to send welcome email to: {}", patient.getEmail(), e);
                }

                auditService.logPatientEmailVerification(patient.getId(), patient.getEmail(), true);
                log.info("Email verified successfully for patient: {}", patient.getId());
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("Email verification failed for token: {}", token, e);
            auditService.logPatientEmailVerification(null, null, false);
            return false;
        }
    }

    /**
     * Verify patient phone using OTP
     */
    public boolean verifyPatientPhone(UUID patientId, String otp) {
        try {
            Optional<Patient> patientOpt = patientRepository.findByPhoneVerificationOtp(otp, patientId, LocalDateTime.now());
            
            if (patientOpt.isEmpty()) {
                log.warn("Invalid or expired phone verification OTP for patient: {}", patientId);
                return false;
            }

            Patient patient = patientOpt.get();
            
            // Check if too many attempts
            if (patient.getPhoneVerificationAttempts() >= 3) {
                log.warn("Too many phone verification attempts for patient: {}", patientId);
                return false;
            }

            // Mark phone as verified
            int updatedRows = patientRepository.markPhoneAsVerified(patient.getId());
            
            if (updatedRows > 0) {
                auditService.logPatientPhoneVerification(patient.getId(), patient.getPhoneNumber(), true);
                log.info("Phone verified successfully for patient: {}", patient.getId());
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("Phone verification failed for patient: {} with OTP: {}", patientId, otp, e);
            auditService.logPatientPhoneVerification(patientId, null, false);
            return false;
        }
    }

    /**
     * Resend email verification
     */
    public void resendEmailVerification(String email) {
        try {
            Optional<Patient> patientOpt = patientRepository.findByEmail(email.toLowerCase().trim());
            
            if (patientOpt.isEmpty()) {
                log.warn("Attempted to resend verification for non-existent email: {}", email);
                return; // Don't reveal if email exists
            }

            Patient patient = patientOpt.get();
            
            if (patient.getEmailVerified()) {
                log.info("Email already verified for patient: {}", patient.getId());
                return;
            }

            // Generate new verification token
            patient.generateEmailVerificationToken();
            patientRepository.save(patient);

            // Send verification email
            sendEmailVerification(patient);

            auditService.logPatientEmailVerificationResent(patient.getId(), patient.getEmail());

        } catch (Exception e) {
            log.error("Failed to resend email verification for: {}", email, e);
        }
    }

    /**
     * Resend phone verification
     */
    public boolean resendPhoneVerification(UUID patientId) {
        try {
            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            
            if (patientOpt.isEmpty()) {
                log.warn("Attempted to resend phone verification for non-existent patient: {}", patientId);
                return false;
            }

            Patient patient = patientOpt.get();
            
            if (patient.getPhoneVerified()) {
                log.info("Phone already verified for patient: {}", patient.getId());
                return false;
            }

            // Check if too many attempts
            if (patient.getPhoneVerificationAttempts() >= 3) {
                log.warn("Too many phone verification attempts for patient: {}", patientId);
                return false;
            }

            // Generate new OTP
            patient.generatePhoneVerificationOtp();
            patientRepository.save(patient);

            // Send SMS verification
            boolean sent = sendPhoneVerification(patient);

            if (sent) {
                auditService.logPatientPhoneVerificationResent(patient.getId(), patient.getPhoneNumber());
            }

            return sent;

        } catch (Exception e) {
            log.error("Failed to resend phone verification for patient: {}", patientId, e);
            return false;
        }
    }

    /**
     * Get patient by ID (HIPAA-compliant - limited information)
     */
    public Optional<Patient> getPatientById(UUID patientId) {
        return patientRepository.findById(patientId);
    }

    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) {
        return patientRepository.existsByEmail(email.toLowerCase().trim());
    }

    /**
     * Check if phone exists
     */
    public boolean existsByPhoneNumber(String phoneNumber) {
        return patientRepository.existsByPhoneNumber(phoneNumber.trim());
    }

    /**
     * Get patient statistics (for admin dashboard)
     */
    public Object[] getPatientStatistics() {
        return patientRepository.getPatientStatistics();
    }

    /**
     * Deactivate patient account
     */
    public boolean deactivatePatient(UUID patientId, String reason) {
        try {
            int updatedRows = patientRepository.deactivatePatient(patientId);
            
            if (updatedRows > 0) {
                auditService.logPatientDeactivation(patientId, reason);
                log.info("Patient deactivated: {} - Reason: {}", patientId, reason);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("Failed to deactivate patient: {}", patientId, e);
            return false;
        }
    }

    // Private helper methods

    private Patient createPatientFromRequest(PatientRegistrationRequest request) {
        Patient patient = new Patient();
        
        // Basic information
        patient.setFirstName(sanitizeInput(request.getFirstName()));
        patient.setLastName(sanitizeInput(request.getLastName()));
        patient.setEmail(request.getNormalizedEmail());
        patient.setPhoneNumber(request.getNormalizedPhoneNumber());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(Patient.Gender.valueOf(request.getGenderForEntity()));

        // Hash password
        patient.setPasswordHash(passwordUtils.hashPassword(request.getPassword()));

        // Address
        if (request.getAddress() != null) {
            PatientAddress address = new PatientAddress(
                sanitizeInput(request.getAddress().getStreet()),
                sanitizeInput(request.getAddress().getCity()),
                sanitizeInput(request.getAddress().getState()),
                sanitizeInput(request.getAddress().getZip())
            );
            patient.setAddress(address);
        }

        // Emergency contact
        if (request.hasEmergencyContact() && request.getEmergencyContact().isComplete()) {
            EmergencyContact emergencyContact = new EmergencyContact(
                sanitizeInput(request.getEmergencyContact().getName()),
                sanitizeInput(request.getEmergencyContact().getPhone()),
                sanitizeInput(request.getEmergencyContact().getRelationship())
            );
            patient.setEmergencyContact(emergencyContact);
        }

        // Medical history
        if (request.hasMedicalHistory()) {
            patient.setMedicalHistory(request.getSanitizedMedicalHistory());
        }

        // Insurance information
        if (request.hasInsuranceInfo() && request.getInsuranceInfo().isComplete()) {
            InsuranceInfo insuranceInfo = new InsuranceInfo(
                sanitizeInput(request.getInsuranceInfo().getProvider()),
                sanitizeInput(request.getInsuranceInfo().getPolicyNumber()),
                sanitizeInput(request.getInsuranceInfo().getGroupNumber()),
                sanitizeInput(request.getInsuranceInfo().getPlanName()),
                sanitizeInput(request.getInsuranceInfo().getMemberId())
            );
            patient.setInsuranceInfo(insuranceInfo);
        }

        // Privacy and consent
        patient.setMarketingConsent(request.getMarketingConsent() != null ? request.getMarketingConsent() : false);
        patient.setDataSharingConsent(request.getDataSharingConsent() != null ? request.getDataSharingConsent() : false);

        // Verification settings
        patient.setEmailVerified(!emailVerificationRequired);
        patient.setPhoneVerified(!phoneVerificationRequired);
        patient.setIsActive(autoActivateAccount || !emailVerificationRequired);

        // Generate verification tokens if needed
        if (emailVerificationRequired) {
            patient.generateEmailVerificationToken();
        }

        if (phoneVerificationRequired) {
            patient.generatePhoneVerificationOtp();
        }

        return patient;
    }

    private boolean sendEmailVerification(Patient patient) {
        if (!emailVerificationRequired || patient.getEmailVerificationToken() == null) {
            return false;
        }

        try {
            emailService.sendPatientVerificationEmail(
                patient.getEmail(),
                patient.getFirstName(),
                patient.getEmailVerificationToken()
            );
            return true;
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", patient.getEmail(), e);
            return false;
        }
    }

    private boolean sendPhoneVerification(Patient patient) {
        if (!phoneVerificationRequired || patient.getPhoneVerificationOtp() == null) {
            return false;
        }

        try {
            // SMS service integration would go here
            // For now, we'll log the OTP (in production, this should send SMS)
            log.info("Phone verification OTP for patient {}: {}", patient.getId(), patient.getPhoneVerificationOtp());
            return true;
        } catch (Exception e) {
            log.error("Failed to send phone verification to: {}", patient.getPhoneNumber(), e);
            return false;
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.trim().replaceAll("[<>\"'&]", "");
    }
} 