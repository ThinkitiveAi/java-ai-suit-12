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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "providers", indexes = {
    @Index(name = "idx_provider_email", columnList = "email"),
    @Index(name = "idx_provider_phone", columnList = "phone_number"),
    @Index(name = "idx_provider_license", columnList = "license_number"),
    @Index(name = "idx_provider_active", columnList = "is_active"),
    @Index(name = "idx_provider_verification", columnList = "verification_status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "First name can only contain letters and spaces")
    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Last name can only contain letters and spaces")
    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Please provide a valid international phone number")
    @Column(name = "phone_number", length = 20, nullable = false, unique = true)
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotBlank(message = "Specialization is required")
    @Size(min = 3, max = 100, message = "Specialization must be between 3 and 100 characters")
    @Column(name = "specialization", length = 100, nullable = false)
    private String specialization;

    @NotBlank(message = "License number is required")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "License number must be alphanumeric")
    @Size(min = 5, max = 50, message = "License number must be between 5 and 50 characters")
    @Column(name = "license_number", length = 50, nullable = false, unique = true)
    private String licenseNumber;

    @NotNull(message = "Years of experience is required")
    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 50, message = "Years of experience cannot exceed 50")
    @Column(name = "years_of_experience", nullable = false)
    private Integer yearsOfExperience;

    @Valid
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "clinic_street")),
        @AttributeOverride(name = "city", column = @Column(name = "clinic_city")),
        @AttributeOverride(name = "state", column = @Column(name = "clinic_state")),
        @AttributeOverride(name = "zip", column = @Column(name = "clinic_zip"))
    })
    private ClinicAddress clinicAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Size(max = 500, message = "License document URL cannot exceed 500 characters")
    @Column(name = "license_document_url", length = 500)
    private String licenseDocumentUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Email verification fields
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "verification_token_expires_at")
    private LocalDateTime verificationTokenExpiresAt;

    // Security fields for login attempts
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // Add new login tracking fields for enhanced security
    @Column(name = "login_count", nullable = false)
    private Integer loginCount = 0;
    
    @Column(name = "last_failed_attempt")
    private LocalDateTime lastFailedAttempt;
    
    @Column(name = "suspicious_activity_score", nullable = false)
    private Integer suspiciousActivityScore = 0;

    public enum VerificationStatus {
        PENDING, VERIFIED, REJECTED
    }

    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isVerificationTokenExpired() {
        return verificationTokenExpiresAt == null || verificationTokenExpiresAt.isBefore(LocalDateTime.now());
    }
} 