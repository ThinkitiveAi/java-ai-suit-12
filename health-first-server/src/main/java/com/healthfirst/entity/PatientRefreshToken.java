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
 * Entity for managing patient JWT refresh tokens and session tracking
 * Includes comprehensive device tracking and security monitoring for HIPAA compliance
 */
@Entity
@Table(name = "patient_refresh_tokens", indexes = {
    @Index(name = "idx_patient_refresh_token_patient", columnList = "patient_id"),
    @Index(name = "idx_patient_refresh_token_hash", columnList = "token_hash"),
    @Index(name = "idx_patient_refresh_token_expires", columnList = "expires_at"),
    @Index(name = "idx_patient_refresh_token_revoked", columnList = "is_revoked"),
    @Index(name = "idx_patient_refresh_token_device", columnList = "device_fingerprint"),
    @Index(name = "idx_patient_refresh_token_ip", columnList = "ip_address")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @NotNull(message = "Patient ID is required")
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @NotBlank(message = "Token hash is required")
    @Column(name = "token_hash", nullable = false, length = 255, unique = true)
    private String tokenHash;

    @NotNull(message = "Expires at is required")
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked = false;

    // Device tracking information for HIPAA compliance and security
    @Column(name = "device_type", length = 50)
    private String deviceType; // mobile, desktop, tablet

    @Column(name = "device_name", length = 100)
    private String deviceName; // iPhone 12, Chrome Browser, etc.

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint; // Unique device identifier

    @Column(name = "app_version", length = 20)
    private String appVersion; // Application version

    @Column(name = "operating_system", length = 100)
    private String operatingSystem; // iOS 15.0, Windows 11, etc.

    @Column(name = "browser", length = 100)
    private String browser; // Chrome 95.0, Safari 15.0, etc.

    // Network and location information
    @Column(name = "ip_address", length = 45)
    private String ipAddress; // IPv4 or IPv6

    @Column(name = "user_agent", length = 500)
    private String userAgent; // Full user agent string

    @Column(name = "location_info", columnDefinition = "TEXT")
    private String locationInfo; // Geographic location (if permitted)

    // Session tracking
    @Column(name = "session_start", nullable = false)
    private LocalDateTime sessionStart;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @Column(name = "activity_count", nullable = false)
    private Integer activityCount = 0;

    // Security flags
    @Column(name = "is_suspicious", nullable = false)
    private Boolean isSuspicious = false;

    @Column(name = "security_score", nullable = false)
    private Integer securityScore = 0; // 0-100, higher is more suspicious

    @Column(name = "login_method", length = 20)
    private String loginMethod; // EMAIL, PHONE, SSO, etc.

    // Audit timestamps
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason", length = 100)
    private String revokedReason;

    // Utility methods

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if token is active (not revoked and not expired)
     */
    public boolean isActive() {
        return !isRevoked && !isExpired();
    }

    /**
     * Check if token is valid for use
     */
    public boolean isValid() {
        return isActive() && !isSuspicious;
    }

    /**
     * Check if session is stale (no activity for extended period)
     */
    public boolean isStale() {
        if (lastActivity == null) {
            lastActivity = createdAt;
        }
        // Consider stale if no activity for 24 hours
        return lastActivity.isBefore(LocalDateTime.now().minusHours(24));
    }

    /**
     * Update last activity and increment activity count
     */
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
        this.lastUsedAt = LocalDateTime.now();
        this.activityCount = (this.activityCount == null ? 0 : this.activityCount) + 1;
    }

    /**
     * Revoke the token with reason
     */
    public void revoke(String reason) {
        this.isRevoked = true;
        this.revokedAt = LocalDateTime.now();
        this.revokedReason = reason;
    }

    /**
     * Mark as suspicious
     */
    public void markSuspicious(int scoreIncrease, String reason) {
        this.isSuspicious = true;
        this.securityScore = Math.min(100, (this.securityScore == null ? 0 : this.securityScore) + scoreIncrease);
        this.revokedReason = reason;
    }

    /**
     * Reactivate a revoked token (admin function)
     */
    public void reactivate() {
        if (!isRevoked) {
            throw new IllegalStateException("Token is not revoked");
        }
        
        this.isRevoked = false;
        this.revokedAt = null;
    }

    /**
     * Create a new refresh token with default values
     */
    public static PatientRefreshToken createNew(UUID patientId, String tokenHash, LocalDateTime expiresAt) {
        PatientRefreshToken token = new PatientRefreshToken();
        token.setPatientId(patientId);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(expiresAt);
        token.setIsRevoked(false);
        token.setIsSuspicious(false);
        token.setSessionStart(LocalDateTime.now());
        token.setLastActivity(LocalDateTime.now());
        token.setActivityCount(1);
        return token;
    }

    /**
     * Get formatted device information
     */
    public String getFormattedDeviceInfo() {
        StringBuilder info = new StringBuilder();
        
        if (deviceName != null && !deviceName.trim().isEmpty()) {
            info.append(deviceName);
        }
        
        if (operatingSystem != null && !operatingSystem.trim().isEmpty()) {
            if (info.length() > 0) info.append(" on ");
            info.append(operatingSystem);
        }
        
        if (browser != null && !browser.trim().isEmpty()) {
            if (info.length() > 0) info.append(" using ");
            info.append(browser);
        }
        
        return info.length() > 0 ? info.toString() : "Unknown Device";
    }

    /**
     * Get session duration in minutes
     */
    public long getSessionDurationMinutes() {
        LocalDateTime end = revokedAt != null ? revokedAt : LocalDateTime.now();
        return java.time.Duration.between(sessionStart, end).toMinutes();
    }

    /**
     * Get time until expiration in minutes
     */
    public long getMinutesUntilExpiration() {
        if (isExpired()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
    }

    /**
     * Create a new token instance with essential information
     */
    public static PatientRefreshToken create(
            UUID patientId,
            String tokenHash,
            LocalDateTime expiresAt,
            String deviceType,
            String deviceName,
            String deviceFingerprint,
            String appVersion,
            String operatingSystem,
            String browser,
            String ipAddress,
            String userAgent,
            String loginMethod) {
        
        PatientRefreshToken token = new PatientRefreshToken();
        token.setPatientId(patientId);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(expiresAt);
        token.setDeviceType(deviceType != null ? deviceType : "unknown");
        token.setDeviceName(deviceName);
        token.setDeviceFingerprint(deviceFingerprint);
        token.setAppVersion(appVersion);
        token.setOperatingSystem(operatingSystem);
        token.setBrowser(browser);
        token.setIpAddress(ipAddress);
        token.setUserAgent(userAgent);
        token.setLoginMethod(loginMethod);
        token.setSessionStart(LocalDateTime.now());
        token.setLastActivity(LocalDateTime.now());
        token.setActivityCount(1);
        token.setIsRevoked(false);
        token.setIsSuspicious(false);
        token.setSecurityScore(0);
        
        return token;
    }

    /**
     * Check if this token represents a new device for the patient
     */
    public boolean isNewDevice() {
        // This would typically be determined by comparing with existing sessions
        // For now, return false as a placeholder
        return false;
    }

    /**
     * Get masked IP address for logging (privacy)
     */
    public String getMaskedIpAddress() {
        if (ipAddress == null) {
            return "***";
        }
        
        // For IPv4, mask last octet
        if (ipAddress.contains(".")) {
            String[] parts = ipAddress.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
            }
        }
        
        // For IPv6 or other formats, mask everything after first part
        if (ipAddress.contains(":")) {
            String[] parts = ipAddress.split(":");
            if (parts.length > 1) {
                return parts[0] + ":***";
            }
        }
        
        return "***";
    }

    /**
     * Generate session summary for audit logging
     */
    public String getSessionSummary() {
        return String.format(
            "Patient: %s, Device: %s, IP: %s, Duration: %d mins, Activities: %d, Security Score: %d",
            patientId.toString(),
            getFormattedDeviceInfo(),
            getMaskedIpAddress(),
            getSessionDurationMinutes(),
            activityCount,
            securityScore
        );
    }

    /**
     * Check if token needs cleanup (expired and old)
     */
    public boolean needsCleanup() {
        if (!isExpired()) {
            return false;
        }
        
        // Clean up expired tokens older than 30 days
        LocalDateTime cleanupThreshold = LocalDateTime.now().minusDays(30);
        return expiresAt.isBefore(cleanupThreshold);
    }
} 