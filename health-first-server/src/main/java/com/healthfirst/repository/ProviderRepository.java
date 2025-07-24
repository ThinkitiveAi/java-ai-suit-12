package com.healthfirst.repository;

import com.healthfirst.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, UUID> {

    // Uniqueness checks
    boolean existsByEmail(String email);
    
    boolean existsByPhoneNumber(String phoneNumber);
    
    boolean existsByLicenseNumber(String licenseNumber);

    // Find by email or phone for login
    Optional<Provider> findByEmail(String email);
    
    Optional<Provider> findByPhoneNumber(String phoneNumber);

    // Find by email or phone number (for login with either identifier)
    @Query("SELECT p FROM Provider p WHERE p.email = :identifier OR p.phoneNumber = :identifier")
    Optional<Provider> findByEmailOrPhoneNumber(@Param("identifier") String identifier);

    // Email verification
    Optional<Provider> findByVerificationToken(String verificationToken);

    // Active providers
    @Query("SELECT p FROM Provider p WHERE p.isActive = true")
    Optional<Provider> findByIdAndIsActiveTrue(UUID id);

    // Security - failed login attempts
    @Query("SELECT p FROM Provider p WHERE p.email = :email AND p.failedLoginAttempts >= :maxAttempts")
    Optional<Provider> findByEmailWithFailedAttempts(@Param("email") String email, @Param("maxAttempts") int maxAttempts);

    // Clean up expired verification tokens
    @Query("SELECT p FROM Provider p WHERE p.verificationTokenExpiresAt < :expiredBefore AND p.emailVerified = false")
    Optional<Provider> findProvidersWithExpiredTokens(@Param("expiredBefore") LocalDateTime expiredBefore);

    // Specialization search
    @Query("SELECT p FROM Provider p WHERE LOWER(p.specialization) LIKE LOWER(CONCAT('%', :specialization, '%')) AND p.isActive = true AND p.verificationStatus = 'VERIFIED'")
    Optional<Provider> findBySpecializationContainingIgnoreCase(@Param("specialization") String specialization);

    // Count by verification status
    @Query("SELECT COUNT(p) FROM Provider p WHERE p.verificationStatus = :status")
    long countByVerificationStatus(@Param("status") Provider.VerificationStatus status);
} 