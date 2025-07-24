package com.healthfirst.repository;

import com.healthfirst.entity.PatientRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for PatientRefreshToken entity operations
 * Provides HIPAA-compliant methods for patient session management and security monitoring
 */
@Repository
public interface PatientRefreshTokenRepository extends JpaRepository<PatientRefreshToken, UUID> {

    // Basic token operations

    /**
     * Find active token by hash
     */
    @Query("SELECT prt FROM PatientRefreshToken prt WHERE prt.tokenHash = :tokenHash AND prt.isRevoked = false AND prt.expiresAt > :now")
    Optional<PatientRefreshToken> findActiveTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    /**
     * Find all active tokens for a patient
     */
    @Query("SELECT prt FROM PatientRefreshToken prt WHERE prt.patientId = :patientId AND prt.isRevoked = false AND prt.expiresAt > :now ORDER BY prt.lastActivity DESC")
    List<PatientRefreshToken> findActiveTokensByPatientId(@Param("patientId") UUID patientId, @Param("now") LocalDateTime now);

    /**
     * Count active tokens for a patient
     */
    @Query("SELECT COUNT(prt) FROM PatientRefreshToken prt WHERE prt.patientId = :patientId AND prt.isRevoked = false AND prt.expiresAt > :now")
    long countActiveTokensByPatientId(@Param("patientId") UUID patientId, @Param("now") LocalDateTime now);

    /**
     * Find tokens by device fingerprint for the patient
     */
    @Query("SELECT prt FROM PatientRefreshToken prt WHERE prt.patientId = :patientId AND prt.deviceFingerprint = :deviceFingerprint AND prt.isRevoked = false ORDER BY prt.createdAt DESC")
    List<PatientRefreshToken> findTokensByPatientAndDevice(@Param("patientId") UUID patientId, @Param("deviceFingerprint") String deviceFingerprint);

    // Session management operations

    /**
     * Update last used timestamp and activity count
     */
    @Modifying
    @Transactional
    @Query("UPDATE PatientRefreshToken prt SET prt.lastUsedAt = :now, prt.lastActivity = :now, prt.activityCount = prt.activityCount + 1, prt.updatedAt = :now WHERE prt.tokenHash = :tokenHash")
    int updateTokenActivity(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    /**
     * Revoke a single token
     */
    @Modifying
    @Transactional
    @Query("UPDATE PatientRefreshToken prt SET prt.isRevoked = true, prt.revokedAt = :now, prt.revokedReason = :reason, prt.updatedAt = :now WHERE prt.tokenHash = :tokenHash")
    int revokeTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now, @Param("reason") String reason);

    /**
     * Revoke all tokens for a patient
     */
    @Modifying
    @Transactional
    @Query("UPDATE PatientRefreshToken prt SET prt.isRevoked = true, prt.revokedAt = :now, prt.revokedReason = :reason, prt.updatedAt = :now WHERE prt.patientId = :patientId AND prt.isRevoked = false")
    int revokeAllTokensByPatientId(@Param("patientId") UUID patientId, @Param("now") LocalDateTime now, @Param("reason") String reason);

    /**
     * Revoke all tokens except current one
     */
    @Modifying
    @Transactional
    @Query("UPDATE PatientRefreshToken prt SET prt.isRevoked = true, prt.revokedAt = :now, prt.revokedReason = :reason, prt.updatedAt = :now WHERE prt.patientId = :patientId AND prt.tokenHash != :currentTokenHash AND prt.isRevoked = false")
    int revokeAllTokensExceptCurrent(@Param("patientId") UUID patientId, @Param("currentTokenHash") String currentTokenHash, @Param("now") LocalDateTime now, @Param("reason") String reason);

    /**
     * Revoke tokens by device fingerprint
     */
    @Modifying
    @Transactional
    @Query("UPDATE PatientRefreshToken prt SET prt.isRevoked = true, prt.revokedAt = :now, prt.revokedReason = :reason, prt.updatedAt = :now WHERE prt.patientId = :patientId AND prt.deviceFingerprint = :deviceFingerprint AND prt.isRevoked = false")
    int revokeTokensByDevice(@Param("patientId") UUID patientId, @Param("deviceFingerprint") String deviceFingerprint, @Param("now") LocalDateTime now, @Param("reason") String reason);

    // Cleanup operations

    /**
     * Find expired tokens for cleanup
     */
    @Query("SELECT prt FROM PatientRefreshToken prt WHERE prt.expiresAt < :cutoffTime")
    List<PatientRefreshToken> findExpiredTokens(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Delete expired tokens older than specified period
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PatientRefreshToken prt WHERE prt.expiresAt < :cutoffTime")
    int deleteExpiredTokens(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find stale tokens (no activity for extended period)
     */
    @Query("SELECT prt FROM PatientRefreshToken prt WHERE prt.lastActivity < :staleThreshold AND prt.isRevoked = false")
    List<PatientRefreshToken> findStaleTokens(@Param("staleThreshold") LocalDateTime staleThreshold);

    /**
     * Revoke stale tokens
     */
    @Modifying
    @Transactional
    @Query("UPDATE PatientRefreshToken prt SET prt.isRevoked = true, prt.revokedAt = :now, prt.revokedReason = 'Stale session', prt.updatedAt = :now WHERE prt.lastActivity < :staleThreshold AND prt.isRevoked = false")
    int revokeStaleTokens(@Param("staleThreshold") LocalDateTime staleThreshold, @Param("now") LocalDateTime now);

    // Security monitoring queries

    /**
     * Find tokens with suspicious activity
     */
    @Query("SELECT prt FROM PatientRefreshToken prt WHERE prt.isSuspicious = true OR prt.securityScore > :suspiciousScore")
    List<PatientRefreshToken> findSuspiciousTokens(@Param("suspiciousScore") Integer suspiciousScore);

    /**
     * Find tokens from the same IP address for a patient
     */
    @Query("SELECT prt FROM PatientRefreshToken prt WHERE prt.patientId = :patientId AND prt.ipAddress = :ipAddress AND prt.isRevoked = false ORDER BY prt.createdAt DESC")
    List<PatientRefreshToken> findTokensByPatientAndIp(@Param("patientId") UUID patientId, @Param("ipAddress") String ipAddress);

    /**
     * Find recent login locations for a patient (last 30 days)
     */
    @Query("SELECT DISTINCT prt.ipAddress, prt.locationInfo FROM PatientRefreshToken prt WHERE prt.patientId = :patientId AND prt.createdAt > :since ORDER BY prt.createdAt DESC")
    List<Object[]> findRecentLoginLocations(@Param("patientId") UUID patientId, @Param("since") LocalDateTime since);

    /**
     * Find tokens with unusual device patterns
     */
    @Query("SELECT prt FROM PatientRefreshToken prt WHERE prt.patientId = :patientId AND prt.deviceFingerprint != :knownFingerprint AND prt.createdAt > :recentThreshold AND prt.isRevoked = false")
    List<PatientRefreshToken> findTokensFromNewDevices(@Param("patientId") UUID patientId, @Param("knownFingerprint") String knownFingerprint, @Param("recentThreshold") LocalDateTime recentThreshold);

    /**
     * Count distinct IP addresses for a patient in recent period
     */
    @Query("SELECT COUNT(DISTINCT prt.ipAddress) FROM PatientRefreshToken prt WHERE prt.patientId = :patientId AND prt.createdAt > :since")
    long countDistinctIpAddresses(@Param("patientId") UUID patientId, @Param("since") LocalDateTime since);

    /**
     * Count distinct devices for a patient in recent period
     */
    @Query("SELECT COUNT(DISTINCT prt.deviceFingerprint) FROM PatientRefreshToken prt WHERE prt.patientId = :patientId AND prt.createdAt > :since")
    long countDistinctDevices(@Param("patientId") UUID patientId, @Param("since") LocalDateTime since);

    // Analytics and reporting queries

    /**
     * Get session statistics for a patient
     */
    @Query("SELECT " +
           "COUNT(prt) as totalSessions, " +
           "SUM(CASE WHEN prt.isRevoked = false AND prt.expiresAt > :now THEN 1 ELSE 0 END) as activeSessions, " +
           "AVG(prt.activityCount) as avgActivityCount, " +
           "MAX(prt.lastActivity) as lastActivity " +
           "FROM PatientRefreshToken prt WHERE prt.patientId = :patientId")
    PatientSessionStatistics getPatientSessionStatistics(@Param("patientId") UUID patientId, @Param("now") LocalDateTime now);

    /**
     * Get session duration statistics
     */
    @Query("SELECT prt FROM PatientRefreshToken prt WHERE prt.patientId = :patientId AND prt.isRevoked = true ORDER BY prt.revokedAt DESC")
    List<PatientRefreshToken> findCompletedSessions(@Param("patientId") UUID patientId);

    /**
     * Find concurrent sessions for security monitoring
     */
    @Query("SELECT prt FROM PatientRefreshToken prt WHERE prt.patientId = :patientId AND prt.isRevoked = false AND prt.expiresAt > :now AND prt.lastActivity > :concurrentThreshold")
    List<PatientRefreshToken> findConcurrentSessions(@Param("patientId") UUID patientId, @Param("now") LocalDateTime now, @Param("concurrentThreshold") LocalDateTime concurrentThreshold);

    /**
     * Mark tokens as suspicious based on security rules
     */
    @Modifying
    @Transactional
    @Query("UPDATE PatientRefreshToken prt SET prt.isSuspicious = true, prt.securityScore = prt.securityScore + :scoreIncrease, prt.revokedReason = :reason, prt.updatedAt = :now WHERE prt.id IN :tokenIds")
    int markTokensAsSuspicious(@Param("tokenIds") List<UUID> tokenIds, @Param("scoreIncrease") Integer scoreIncrease, @Param("reason") String reason, @Param("now") LocalDateTime now);

    /**
     * Get token count by device type for analytics
     */
    @Query("SELECT prt.deviceType, COUNT(prt) FROM PatientRefreshToken prt WHERE prt.patientId = :patientId AND prt.createdAt > :since GROUP BY prt.deviceType")
    List<Object[]> getTokenCountByDeviceType(@Param("patientId") UUID patientId, @Param("since") LocalDateTime since);

    /**
     * Get login frequency by hour of day for analytics
     */
    @Query("SELECT HOUR(prt.createdAt), COUNT(prt) FROM PatientRefreshToken prt WHERE prt.patientId = :patientId AND prt.createdAt > :since GROUP BY HOUR(prt.createdAt) ORDER BY HOUR(prt.createdAt)")
    List<Object[]> getLoginFrequencyByHour(@Param("patientId") UUID patientId, @Param("since") LocalDateTime since);

    // Custom query for checking device familiarity
    /**
     * Check if a device fingerprint has been used before by the patient
     */
    @Query("SELECT CASE WHEN COUNT(prt) > 0 THEN true ELSE false END FROM PatientRefreshToken prt WHERE prt.patientId = :patientId AND prt.deviceFingerprint = :deviceFingerprint")
    boolean isKnownDevice(@Param("patientId") UUID patientId, @Param("deviceFingerprint") String deviceFingerprint);

    /**
     * Get the most recent login from a specific device
     */
    @Query("SELECT prt FROM PatientRefreshToken prt WHERE prt.patientId = :patientId AND prt.deviceFingerprint = :deviceFingerprint ORDER BY prt.createdAt DESC LIMIT 1")
    Optional<PatientRefreshToken> findLatestTokenByDevice(@Param("patientId") UUID patientId, @Param("deviceFingerprint") String deviceFingerprint);

    // Projection interface for session statistics
    interface PatientSessionStatistics {
        Long getTotalSessions();
        Long getActiveSessions();
        Double getAvgActivityCount();
        LocalDateTime getLastActivity();
    }
} 