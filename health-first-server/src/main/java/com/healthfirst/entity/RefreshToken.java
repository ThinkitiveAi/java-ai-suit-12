package com.healthfirst.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RefreshToken entity for managing JWT refresh tokens with security features
 * Supports device tracking, automatic rotation, and revocation
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token_provider", columnList = "provider_id"),
    @Index(name = "idx_refresh_token_hash", columnList = "token_hash"),
    @Index(name = "idx_refresh_token_expires", columnList = "expires_at"),
    @Index(name = "idx_refresh_token_revoked", columnList = "is_revoked")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @NotNull(message = "Provider ID is required")
    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @NotBlank(message = "Token hash is required")
    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @NotNull(message = "Expires at is required")
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked = false;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "app_version", length = 20)
    private String appVersion;

    @Column(name = "ip_address", length = 45) // Supports IPv6
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "location_info", columnDefinition = "TEXT")
    private String locationInfo; // JSON string for location data

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    // Utility methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isActive() {
        return !this.isRevoked && !this.isExpired();
    }

    public void markAsRevoked() {
        this.isRevoked = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * Static factory method to create a new refresh token
     */
    public static RefreshToken createToken(UUID providerId, String tokenHash, LocalDateTime expiresAt,
                                         String deviceType, String deviceName, String appVersion,
                                         String ipAddress, String userAgent) {
        RefreshToken token = new RefreshToken();
        token.setProviderId(providerId);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(expiresAt);
        token.setIsRevoked(false);
        token.setDeviceType(deviceType);
        token.setDeviceName(deviceName);
        token.setAppVersion(appVersion);
        token.setIpAddress(ipAddress);
        token.setUserAgent(userAgent);
        token.setCreatedAt(LocalDateTime.now());
        token.setUpdatedAt(LocalDateTime.now());
        token.setLastUsedAt(LocalDateTime.now());
        
        return token;
    }
} 