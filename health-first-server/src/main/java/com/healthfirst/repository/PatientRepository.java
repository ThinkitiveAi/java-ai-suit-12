package com.healthfirst.repository;

import com.healthfirst.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Patient entity operations
 * Provides HIPAA-compliant methods for patient data access and management
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    // Basic lookup methods with privacy considerations

    /**
     * Find patient by email (case-insensitive)
     */
    @Query("SELECT p FROM Patient p WHERE LOWER(p.email) = LOWER(:email)")
    Optional<Patient> findByEmail(@Param("email") String email);

    /**
     * Find patient by phone number
     */
    Optional<Patient> findByPhoneNumber(String phoneNumber);

    /**
     * Check if email exists (for registration validation)
     */
    boolean existsByEmail(String email);

    /**
     * Check if phone number exists (for registration validation)
     */
    boolean existsByPhoneNumber(String phoneNumber);

    // Verification-related methods

    /**
     * Find patient by email verification token
     */
    @Query("SELECT p FROM Patient p WHERE p.emailVerificationToken = :token AND p.emailVerificationExpiresAt > :now")
    Optional<Patient> findByEmailVerificationToken(@Param("token") String token, @Param("now") LocalDateTime now);

    /**
     * Find patient by phone verification OTP
     */
    @Query("SELECT p FROM Patient p WHERE p.phoneVerificationOtp = :otp AND p.phoneVerificationExpiresAt > :now AND p.id = :patientId")
    Optional<Patient> findByPhoneVerificationOtp(@Param("otp") String otp, @Param("patientId") UUID patientId, @Param("now") LocalDateTime now);

    /**
     * Find patients with expired email verification tokens (for cleanup)
     */
    @Query("SELECT p FROM Patient p WHERE p.emailVerificationToken IS NOT NULL AND p.emailVerificationExpiresAt < :now")
    List<Patient> findPatientsWithExpiredEmailTokens(@Param("now") LocalDateTime now);

    /**
     * Find patients with expired phone verification OTPs (for cleanup)
     */
    @Query("SELECT p FROM Patient p WHERE p.phoneVerificationOtp IS NOT NULL AND p.phoneVerificationExpiresAt < :now")
    List<Patient> findPatientsWithExpiredPhoneOtps(@Param("now") LocalDateTime now);

    // Account status and verification queries

    /**
     * Find active patients by verification status
     */
    @Query("SELECT p FROM Patient p WHERE p.isActive = true AND p.emailVerified = :emailVerified")
    List<Patient> findActivePatientsByEmailVerification(@Param("emailVerified") Boolean emailVerified);

    /**
     * Find patients who are fully verified (both email and phone)
     */
    @Query("SELECT p FROM Patient p WHERE p.emailVerified = true AND p.phoneVerified = true AND p.isActive = true")
    List<Patient> findFullyVerifiedPatients();

    /**
     * Count patients by verification status
     */
    @Query("SELECT COUNT(p) FROM Patient p WHERE p.emailVerified = :emailVerified AND p.phoneVerified = :phoneVerified")
    long countByVerificationStatus(@Param("emailVerified") Boolean emailVerified, @Param("phoneVerified") Boolean phoneVerified);

    // Age and demographic queries (COPPA compliance)

    /**
     * Find patients by age range (for demographic analysis)
     */
    @Query("SELECT p FROM Patient p WHERE p.dateOfBirth BETWEEN :startDate AND :endDate AND p.isActive = true")
    List<Patient> findPatientsByAgeRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find patients under minimum age (COPPA compliance check)
     */
    @Query("SELECT p FROM Patient p WHERE p.dateOfBirth > :minimumBirthDate")
    List<Patient> findPatientsUnderAge(@Param("minimumBirthDate") LocalDate minimumBirthDate);

    /**
     * Count patients by gender (for demographic reporting)
     */
    @Query("SELECT p.gender, COUNT(p) FROM Patient p WHERE p.isActive = true GROUP BY p.gender")
    List<Object[]> countPatientsByGender();

    // Security and audit queries

    /**
     * Find patients with failed login attempts
     */
    @Query("SELECT p FROM Patient p WHERE p.failedLoginAttempts > 0 ORDER BY p.failedLoginAttempts DESC")
    List<Patient> findPatientsWithFailedLogins();

    /**
     * Find locked patient accounts
     */
    @Query("SELECT p FROM Patient p WHERE p.lockedUntil IS NOT NULL AND p.lockedUntil > :now")
    List<Patient> findLockedPatients(@Param("now") LocalDateTime now);

    /**
     * Find patients by last login range (for activity analysis)
     */
    @Query("SELECT p FROM Patient p WHERE p.lastLogin BETWEEN :startDate AND :endDate")
    List<Patient> findPatientsByLastLoginRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Update methods

    /**
     * Update email verification status
     */
    @Modifying
    @Transactional
    @Query("UPDATE Patient p SET p.emailVerified = true, p.emailVerificationToken = null, p.emailVerificationExpiresAt = null WHERE p.id = :patientId")
    int markEmailAsVerified(@Param("patientId") UUID patientId);

    /**
     * Update phone verification status
     */
    @Modifying
    @Transactional
    @Query("UPDATE Patient p SET p.phoneVerified = true, p.phoneVerificationOtp = null, p.phoneVerificationExpiresAt = null, p.phoneVerificationAttempts = 0 WHERE p.id = :patientId")
    int markPhoneAsVerified(@Param("patientId") UUID patientId);

    /**
     * Clear expired email verification tokens
     */
    @Modifying
    @Transactional
    @Query("UPDATE Patient p SET p.emailVerificationToken = null, p.emailVerificationExpiresAt = null WHERE p.emailVerificationExpiresAt < :now")
    int clearExpiredEmailTokens(@Param("now") LocalDateTime now);

    /**
     * Clear expired phone verification OTPs
     */
    @Modifying
    @Transactional
    @Query("UPDATE Patient p SET p.phoneVerificationOtp = null, p.phoneVerificationExpiresAt = null WHERE p.phoneVerificationExpiresAt < :now")
    int clearExpiredPhoneOtps(@Param("now") LocalDateTime now);

    /**
     * Update last login timestamp
     */
    @Modifying
    @Transactional
    @Query("UPDATE Patient p SET p.lastLogin = :loginTime, p.failedLoginAttempts = 0, p.lockedUntil = null WHERE p.id = :patientId")
    int updateLastLogin(@Param("patientId") UUID patientId, @Param("loginTime") LocalDateTime loginTime);

    /**
     * Increment failed login attempts
     */
    @Modifying
    @Transactional
    @Query("UPDATE Patient p SET p.failedLoginAttempts = p.failedLoginAttempts + 1 WHERE p.id = :patientId")
    int incrementFailedLoginAttempts(@Param("patientId") UUID patientId);

    /**
     * Lock patient account
     */
    @Modifying
    @Transactional
    @Query("UPDATE Patient p SET p.lockedUntil = :lockUntil WHERE p.id = :patientId")
    int lockPatientAccount(@Param("patientId") UUID patientId, @Param("lockUntil") LocalDateTime lockUntil);

    /**
     * Deactivate patient account
     */
    @Modifying
    @Transactional
    @Query("UPDATE Patient p SET p.isActive = false WHERE p.id = :patientId")
    int deactivatePatient(@Param("patientId") UUID patientId);

    // HIPAA-compliant search methods (limited information)

    /**
     * Search patients by partial name (for admin use only, limited results)
     */
    @Query("SELECT p.id, p.firstName, p.lastName, p.email, p.dateOfBirth, p.isActive " +
           "FROM Patient p WHERE " +
           "(LOWER(p.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "p.isActive = true")
    List<Object[]> searchPatientsByName(@Param("searchTerm") String searchTerm);

    /**
     * Get patient statistics for dashboard
     */
    @Query("SELECT " +
           "COUNT(p) as totalPatients, " +
           "SUM(CASE WHEN p.emailVerified = true THEN 1 ELSE 0 END) as emailVerifiedCount, " +
           "SUM(CASE WHEN p.phoneVerified = true THEN 1 ELSE 0 END) as phoneVerifiedCount, " +
           "SUM(CASE WHEN p.emailVerified = true AND p.phoneVerified = true THEN 1 ELSE 0 END) as fullyVerifiedCount " +
           "FROM Patient p WHERE p.isActive = true")
    Object[] getPatientStatistics();

    // Data retention and cleanup (HIPAA compliance)

    /**
     * Find inactive patients older than specified period
     */
    @Query("SELECT p FROM Patient p WHERE p.isActive = false AND p.updatedAt < :cutoffDate")
    List<Patient> findInactivePatientsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find patients who haven't logged in for a specified period
     */
    @Query("SELECT p FROM Patient p WHERE (p.lastLogin IS NULL OR p.lastLogin < :cutoffDate) AND p.createdAt < :cutoffDate")
    List<Patient> findInactivePatientsWithoutRecentLogin(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Bulk updates for patient management

    /**
     * Bulk activate patients by IDs (admin operation)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Patient p SET p.isActive = true WHERE p.id IN :ids")
    int bulkActivatePatients(@Param("ids") List<UUID> ids);

    /**
     * Bulk deactivate patients by IDs (admin operation)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Patient p SET p.isActive = false WHERE p.id IN :ids")
    int bulkDeactivatePatients(@Param("ids") List<UUID> ids);

    // Note: PatientSearchResult is returned as Object[] with the following structure:
    // [id, firstName, lastName, email, dateOfBirth, isActive]

    // Note: PatientStatistics is returned as Object[] with the following structure:
    // [totalPatients, emailVerifiedCount, phoneVerifiedCount, fullyVerifiedCount]
} 