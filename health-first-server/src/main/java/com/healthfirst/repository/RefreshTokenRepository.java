package com.healthfirst.repository;

import com.healthfirst.entity.RefreshToken;
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
 * Repository interface for RefreshToken entity operations
 * Provides methods for token validation, cleanup, and security management
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find an active refresh token by its hash
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash AND rt.isRevoked = false AND rt.expiresAt > :now")
    Optional<RefreshToken> findActiveTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    /**
     * Find all active tokens for a specific provider
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.providerId = :providerId AND rt.isRevoked = false AND rt.expiresAt > :now ORDER BY rt.lastUsedAt DESC")
    List<RefreshToken> findActiveTokensByProviderId(@Param("providerId") UUID providerId, @Param("now") LocalDateTime now);

    /**
     * Count active sessions for a provider
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.providerId = :providerId AND rt.isRevoked = false AND rt.expiresAt > :now")
    long countActiveTokensByProviderId(@Param("providerId") UUID providerId, @Param("now") LocalDateTime now);

    /**
     * Find tokens by provider ID and device info for duplicate session detection
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.providerId = :providerId AND rt.deviceType = :deviceType AND rt.deviceName = :deviceName AND rt.isRevoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findActiveTokensByProviderAndDevice(@Param("providerId") UUID providerId, 
                                                          @Param("deviceType") String deviceType,
                                                          @Param("deviceName") String deviceName,
                                                          @Param("now") LocalDateTime now);

    /**
     * Revoke all tokens for a specific provider (logout all)
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true, rt.updatedAt = :now WHERE rt.providerId = :providerId AND rt.isRevoked = false")
    int revokeAllTokensByProviderId(@Param("providerId") UUID providerId, @Param("now") LocalDateTime now);

    /**
     * Revoke a specific token by hash
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true, rt.updatedAt = :now WHERE rt.tokenHash = :tokenHash")
    int revokeTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    /**
     * Update last used timestamp for a token
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.lastUsedAt = :now WHERE rt.tokenHash = :tokenHash")
    int updateLastUsedByHash(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    /**
     * Clean up expired tokens (scheduled task)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Clean up revoked tokens older than specified days
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.isRevoked = true AND rt.updatedAt < :cutoffDate")
    int deleteOldRevokedTokens(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find tokens that haven't been used recently (for security monitoring)
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.lastUsedAt < :cutoffDate AND rt.isRevoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findUnusedTokens(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("now") LocalDateTime now);

    /**
     * Get session statistics for a provider
     */
    @Query("SELECT COUNT(rt), COUNT(CASE WHEN rt.expiresAt > :now THEN 1 END) FROM RefreshToken rt WHERE rt.providerId = :providerId AND rt.isRevoked = false")
    Object[] getSessionStats(@Param("providerId") UUID providerId, @Param("now") LocalDateTime now);

    /**
     * Find tokens by IP address for security monitoring
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.ipAddress = :ipAddress AND rt.isRevoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findActiveTokensByIpAddress(@Param("ipAddress") String ipAddress, @Param("now") LocalDateTime now);

    /**
     * Find recent login attempts from different locations for the same provider
     */
    @Query("SELECT DISTINCT rt.ipAddress FROM RefreshToken rt WHERE rt.providerId = :providerId AND rt.createdAt > :since")
    List<String> findRecentLoginLocationsByProviderId(@Param("providerId") UUID providerId, @Param("since") LocalDateTime since);

    /**
     * Check if provider has exceeded maximum concurrent sessions
     */
    default boolean hasExceededMaxSessions(UUID providerId, int maxSessions) {
        long activeSessionCount = countActiveTokensByProviderId(providerId, LocalDateTime.now());
        return activeSessionCount >= maxSessions;
    }

    /**
     * Check if there are multiple active sessions from different IP addresses
     */
    @Query("SELECT COUNT(DISTINCT rt.ipAddress) FROM RefreshToken rt WHERE rt.providerId = :providerId AND rt.isRevoked = false AND rt.expiresAt > :now")
    long countDistinctActiveIpsByProviderId(@Param("providerId") UUID providerId, @Param("now") LocalDateTime now);
} 