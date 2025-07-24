package com.healthfirst.entity;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

/**
 * Patient entity representing healthcare patients with comprehensive validation
 * Includes HIPAA compliance considerations and medical data protection
 */
@Entity
@Table(name = "patients", indexes = {
    @Index(name = "idx_patient_email", columnList = "email"),
    @Index(name = "idx_patient_phone", columnList = "phone_number"),
    @Index(name = "idx_patient_active", columnList = "is_active"),
    @Index(name = "idx_patient_created", columnList = "created_at"),
    @Index(name = "idx_patient_email_verified", columnList = "email_verified"),
    @Index(name = "idx_patient_phone_verified", columnList = "phone_verified")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "First name contains invalid characters")
    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Last name contains invalid characters")
    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Column(name = "phone_number", length = 20, nullable = false, unique = true)
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 30, nullable = false)
    private Gender gender;

    // Address information (embedded)
    @Valid
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "address_street")),
        @AttributeOverride(name = "city", column = @Column(name = "address_city")),
        @AttributeOverride(name = "state", column = @Column(name = "address_state")),
        @AttributeOverride(name = "zip", column = @Column(name = "address_zip"))
    })
    private PatientAddress address;

    // Emergency contact information (embedded, optional)
    @Valid
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "name", column = @Column(name = "emergency_contact_name")),
        @AttributeOverride(name = "phone", column = @Column(name = "emergency_contact_phone")),
        @AttributeOverride(name = "relationship", column = @Column(name = "emergency_contact_relationship"))
    })
    private EmergencyContact emergencyContact;

    // Medical history (stored as JSON array or separate table for HIPAA compliance)
    @ElementCollection
    @CollectionTable(name = "patient_medical_history", 
                    joinColumns = @JoinColumn(name = "patient_id"))
    @Column(name = "medical_condition", length = 500)
    private List<String> medicalHistory;

    // Insurance information (embedded, optional)
    @Valid
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "provider", column = @Column(name = "insurance_provider")),
        @AttributeOverride(name = "policyNumber", column = @Column(name = "insurance_policy_number")),
        @AttributeOverride(name = "groupNumber", column = @Column(name = "insurance_group_number"))
    })
    private InsuranceInfo insuranceInfo;

    // Verification status
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    private Boolean phoneVerified = false;

    // Account status
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Email verification token and expiry
    @Column(name = "email_verification_token", length = 255)
    private String emailVerificationToken;

    @Column(name = "email_verification_expires_at")
    private LocalDateTime emailVerificationExpiresAt;

    // Phone verification OTP and expiry
    @Column(name = "phone_verification_otp", length = 10)
    private String phoneVerificationOtp;

    @Column(name = "phone_verification_expires_at")
    private LocalDateTime phoneVerificationExpiresAt;

    @Column(name = "phone_verification_attempts", nullable = false)
    private Integer phoneVerificationAttempts = 0;

    // Privacy and compliance flags
    @Column(name = "marketing_consent", nullable = false)
    private Boolean marketingConsent = false;

    @Column(name = "data_sharing_consent", nullable = false)
    private Boolean dataSharingConsent = false;

    // Login tracking (similar to Provider)
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "login_count", nullable = false)
    private Integer loginCount = 0;

    @Column(name = "last_failed_attempt")
    private LocalDateTime lastFailedAttempt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum Gender {
        MALE("male"),
        FEMALE("female"),
        OTHER("other"),
        PREFER_NOT_TO_SAY("prefer_not_to_say");

        private final String value;

        Gender(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Gender fromString(String text) {
            for (Gender gender : Gender.values()) {
                if (gender.value.equalsIgnoreCase(text)) {
                    return gender;
                }
            }
            throw new IllegalArgumentException("No constant with text " + text + " found");
        }
    }

    // Utility methods

    /**
     * Calculate age from date of birth
     */
    public int getAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * Check if patient meets minimum age requirement (COPPA compliance)
     */
    public boolean meetsMinimumAge() {
        return getAge() >= 13;
    }

    /**
     * Check if email verification token is expired
     */
    public boolean isEmailVerificationTokenExpired() {
        return emailVerificationExpiresAt != null && 
               LocalDateTime.now().isAfter(emailVerificationExpiresAt);
    }

    /**
     * Check if phone verification OTP is expired
     */
    public boolean isPhoneVerificationOtpExpired() {
        return phoneVerificationExpiresAt != null && 
               LocalDateTime.now().isAfter(phoneVerificationExpiresAt);
    }

    /**
     * Check if account is locked due to failed login attempts
     */
    public boolean isAccountLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    /**
     * Check if patient is fully verified (both email and phone)
     */
    public boolean isFullyVerified() {
        return emailVerified && phoneVerified;
    }

    /**
     * Check if patient can login (active, email verified, not locked)
     */
    public boolean canLogin() {
        return isActive && emailVerified && !isAccountLocked();
    }

    /**
     * Get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Generate email verification token
     */
    public void generateEmailVerificationToken() {
        this.emailVerificationToken = UUID.randomUUID().toString();
        this.emailVerificationExpiresAt = LocalDateTime.now().plusHours(24);
    }

    /**
     * Generate phone verification OTP
     */
    public void generatePhoneVerificationOtp() {
        // Generate 6-digit OTP
        this.phoneVerificationOtp = String.format("%06d", (int)(Math.random() * 1000000));
        this.phoneVerificationExpiresAt = LocalDateTime.now().plusMinutes(5);
        this.phoneVerificationAttempts = 0;
    }

    /**
     * Increment phone verification attempts
     */
    public void incrementPhoneVerificationAttempts() {
        this.phoneVerificationAttempts = (this.phoneVerificationAttempts == null ? 0 : this.phoneVerificationAttempts) + 1;
    }

    /**
     * Reset verification tokens
     */
    public void clearVerificationTokens() {
        this.emailVerificationToken = null;
        this.emailVerificationExpiresAt = null;
        this.phoneVerificationOtp = null;
        this.phoneVerificationExpiresAt = null;
        this.phoneVerificationAttempts = 0;
    }
} 