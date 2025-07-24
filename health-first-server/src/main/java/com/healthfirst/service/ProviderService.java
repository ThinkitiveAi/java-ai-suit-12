package com.healthfirst.service;

import com.healthfirst.dto.ProviderRegistrationRequest;
import com.healthfirst.dto.ProviderRegistrationResponse;
import com.healthfirst.entity.ClinicAddress;
import com.healthfirst.entity.Provider;
import com.healthfirst.exception.ValidationException;
import com.healthfirst.repository.ProviderRepository;
import com.healthfirst.util.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final ValidationService validationService;
    private final EmailService emailService;
    private final PasswordUtils passwordUtils;
    private final AuditService auditService;

    /**
     * Register a new healthcare provider
     */
    public ProviderRegistrationResponse registerProvider(ProviderRegistrationRequest request, String clientIp) {
        try {
            log.info("Starting provider registration for email: {}", request.getEmail());
            
            // Comprehensive validation
            validationService.validateProviderRegistration(request);
            
            // Create and save provider
            Provider provider = createProviderFromRequest(request);
            Provider savedProvider = providerRepository.save(provider);
            
            // TODO: Re-enable email verification when SMTP is configured
            // Send verification email
            /*
            emailService.sendProviderVerificationEmail(
                savedProvider.getEmail(),
                savedProvider.getFirstName(),
                savedProvider.getVerificationToken()
            );
            */
            
            // TEMPORARY: Auto-verify email for testing
            savedProvider.setEmailVerified(true);
            savedProvider.setVerificationStatus(Provider.VerificationStatus.VERIFIED);
            savedProvider.setVerificationToken(null);
            savedProvider.setVerificationTokenExpiresAt(null);
            providerRepository.save(savedProvider);
            
            // Audit log
            auditService.logProviderRegistration(savedProvider.getId(), savedProvider.getEmail(), clientIp, true);
            
            log.info("Provider registered successfully with ID: {} (Email auto-verified for testing)", savedProvider.getId());
            
            return ProviderRegistrationResponse.success(
                savedProvider.getId(),
                savedProvider.getEmail(),
                savedProvider.getVerificationStatus().toString().toLowerCase()
            );
            
        } catch (ValidationException e) {
            auditService.logProviderRegistration(null, request.getEmail(), clientIp, false);
            log.warn("Provider registration failed due to validation errors: {}", e.getErrors());
            throw e;
        } catch (Exception e) {
            auditService.logProviderRegistration(null, request.getEmail(), clientIp, false);
            log.error("Provider registration failed for email: {}", request.getEmail(), e);
            throw new RuntimeException("Registration failed due to an internal error. Please try again.", e);
        }
    }

    /**
     * Verify provider email using verification token
     */
    public boolean verifyProviderEmail(String token) {
        try {
            Provider provider = providerRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ValidationException("Invalid or expired verification token"));

            if (provider.isVerificationTokenExpired()) {
                throw new ValidationException("Verification token has expired");
            }

            if (provider.getEmailVerified()) {
                throw new ValidationException("Email is already verified");
            }

            // Mark email as verified
            provider.setEmailVerified(true);
            provider.setVerificationStatus(Provider.VerificationStatus.VERIFIED);
            provider.setVerificationToken(null);
            provider.setVerificationTokenExpiresAt(null);
            
            providerRepository.save(provider);
            
            // Send welcome email
            emailService.sendWelcomeEmail(
                provider.getEmail(),
                provider.getFirstName(),
                provider.getLastName()
            );
            
            log.info("Provider email verified successfully for ID: {}", provider.getId());
            return true;
            
        } catch (Exception e) {
            log.error("Email verification failed for token: {}", token, e);
            throw e;
        }
    }

    /**
     * Resend verification email
     */
    public void resendVerificationEmail(String email) {
        Provider provider = providerRepository.findByEmail(email)
            .orElseThrow(() -> new ValidationException("Provider not found with email: " + email));

        if (provider.getEmailVerified()) {
            throw new ValidationException("Email is already verified");
        }

        // Generate new verification token
        String newToken = passwordUtils.generateSecureToken(32);
        provider.setVerificationToken(newToken);
        provider.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        
        providerRepository.save(provider);

        // Send new verification email
        emailService.sendProviderVerificationEmail(
            provider.getEmail(),
            provider.getFirstName(),
            newToken
        );

        log.info("Verification email resent for provider: {}", email);
    }

    private Provider createProviderFromRequest(ProviderRegistrationRequest request) {
        Provider provider = new Provider();
        
        // Basic information with input sanitization
        provider.setFirstName(validationService.sanitizeInput(request.getFirstName()));
        provider.setLastName(validationService.sanitizeInput(request.getLastName()));
        provider.setEmail(validationService.normalizeEmail(request.getEmail()));
        provider.setPhoneNumber(validationService.normalizePhoneNumber(request.getPhoneNumber()));
        
        // Professional information
        provider.setSpecialization(validationService.sanitizeInput(request.getSpecialization()));
        provider.setLicenseNumber(request.getLicenseNumber().toUpperCase());
        provider.setYearsOfExperience(request.getYearsOfExperience());
        
        // Address information
        ClinicAddress address = new ClinicAddress(
            validationService.sanitizeInput(request.getClinicAddress().getStreet()),
            validationService.sanitizeInput(request.getClinicAddress().getCity()),
            validationService.sanitizeInput(request.getClinicAddress().getState()),
            request.getClinicAddress().getZip()
        );
        provider.setClinicAddress(address);
        
        // Security
        provider.setPasswordHash(passwordUtils.hashPassword(request.getPassword()));
        
        // Email verification - TEMPORARY: Auto-verify for testing
        String verificationToken = passwordUtils.generateSecureToken(32);
        provider.setVerificationToken(null); // No token needed for auto-verification
        provider.setVerificationTokenExpiresAt(null);
        provider.setEmailVerified(true); // Auto-verify for testing
        
        // Default values
        provider.setVerificationStatus(Provider.VerificationStatus.VERIFIED); // Auto-verified
        provider.setIsActive(true);
        provider.setFailedLoginAttempts(0);
        
        return provider;
    }

    /**
     * Get provider by ID
     */
    @Transactional(readOnly = true)
    public Provider getProviderById(UUID id) {
        return providerRepository.findById(id)
            .orElseThrow(() -> new ValidationException("Provider not found with ID: " + id));
    }

    /**
     * Check if provider exists by email
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return providerRepository.existsByEmail(email);
    }

    /**
     * Check if provider exists by phone number
     */
    @Transactional(readOnly = true)
    public boolean existsByPhoneNumber(String phoneNumber) {
        return providerRepository.existsByPhoneNumber(phoneNumber);
    }

    /**
     * Check if provider exists by license number
     */
    @Transactional(readOnly = true)
    public boolean existsByLicenseNumber(String licenseNumber) {
        return providerRepository.existsByLicenseNumber(licenseNumber);
    }

    /**
     * Update provider profile
     */
    public Provider updateProvider(UUID id, ProviderRegistrationRequest request) {
        Provider provider = getProviderById(id);
        
        // Update fields
        provider.setFirstName(validationService.sanitizeInput(request.getFirstName()));
        provider.setLastName(validationService.sanitizeInput(request.getLastName()));
        provider.setSpecialization(validationService.sanitizeInput(request.getSpecialization()));
        provider.setYearsOfExperience(request.getYearsOfExperience());
        
        // Update address
        ClinicAddress address = new ClinicAddress(
            validationService.sanitizeInput(request.getClinicAddress().getStreet()),
            validationService.sanitizeInput(request.getClinicAddress().getCity()),
            validationService.sanitizeInput(request.getClinicAddress().getState()),
            request.getClinicAddress().getZip()
        );
        provider.setClinicAddress(address);
        
        return providerRepository.save(provider);
    }

    /**
     * Deactivate provider account
     */
    public void deactivateProvider(UUID id) {
        Provider provider = getProviderById(id);
        provider.setIsActive(false);
        providerRepository.save(provider);
        
        log.info("Provider account deactivated: {}", id);
    }

    /**
     * Get verification status statistics
     */
    @Transactional(readOnly = true)
    public VerificationStats getVerificationStats() {
        long pending = providerRepository.countByVerificationStatus(Provider.VerificationStatus.PENDING);
        long verified = providerRepository.countByVerificationStatus(Provider.VerificationStatus.VERIFIED);
        long rejected = providerRepository.countByVerificationStatus(Provider.VerificationStatus.REJECTED);
        
        return new VerificationStats(pending, verified, rejected);
    }

    public record VerificationStats(long pending, long verified, long rejected) {}
} 