package com.healthfirst.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT utility class for token generation, validation, and payload extraction
 * Supports both access and refresh tokens with enhanced security features
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token.expiration-hours:1}")
    private int accessTokenExpirationHours;

    @Value("${app.jwt.access-token.remember-me-hours:24}")
    private int accessTokenRememberMeHours;

    @Value("${app.jwt.refresh-token.expiration-days:7}")
    private int refreshTokenExpirationDays;

    @Value("${app.jwt.refresh-token.remember-me-days:30}")
    private int refreshTokenRememberMeDays;

    @Value("${app.jwt.issuer:HealthFirst}")
    private String issuer;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate access token for authenticated provider
     */
    public String generateAccessToken(UUID providerId, String email, String specialization, 
                                    String verificationStatus, boolean rememberMe) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("provider_id", providerId.toString());
        claims.put("email", email);
        claims.put("specialization", specialization);
        claims.put("verification_status", verificationStatus);
        claims.put("role", "PROVIDER");
        claims.put("token_type", "access");

        int expirationHours = rememberMe ? accessTokenRememberMeHours : accessTokenExpirationHours;
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(expirationHours);

        return createToken(claims, email, expirationTime);
    }

    /**
     * Generate refresh token for session management
     */
    public String generateRefreshToken(UUID providerId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("provider_id", providerId.toString());
        claims.put("email", email);
        claims.put("token_type", "refresh");

        // Use shorter expiration for refresh tokens in JWT (actual expiration managed in DB)
        LocalDateTime expirationTime = LocalDateTime.now().plusDays(1);
        return createToken(claims, email, expirationTime);
    }

    /**
     * Generate access token for authenticated patient with HIPAA compliance
     */
    public String generatePatientAccessToken(UUID patientId, String email, String ageCategory, 
                                           String verificationStatus, boolean rememberMe) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("patient_id", patientId.toString());
        claims.put("email", email);
        claims.put("age_category", ageCategory); // MINOR, ADULT, SENIOR
        claims.put("verification_status", verificationStatus);
        claims.put("role", "PATIENT");
        claims.put("user_type", "PATIENT");
        claims.put("token_type", "access");

        int expirationHours = rememberMe ? accessTokenRememberMeHours : accessTokenExpirationHours;
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(expirationHours);

        return createToken(claims, email, expirationTime);
    }

    /**
     * Generate refresh token for patient session management
     */
    public String generatePatientRefreshToken(UUID patientId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("patient_id", patientId.toString());
        claims.put("email", email);
        claims.put("role", "PATIENT");
        claims.put("user_type", "PATIENT");
        claims.put("token_type", "refresh");

        // Use shorter expiration for refresh tokens in JWT (actual expiration managed in DB)
        LocalDateTime expirationTime = LocalDateTime.now().plusDays(1);
        return createToken(claims, email, expirationTime);
    }

    /**
     * Create JWT token with specified claims and expiration
     */
    private String createToken(Map<String, Object> claims, String subject, LocalDateTime expiration) {
        Date expirationDate = Date.from(expiration.atZone(ZoneId.systemDefault()).toInstant());
        Date issuedAt = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(issuedAt)
                .setExpiration(expirationDate)
                .setId(UUID.randomUUID().toString()) // Unique token ID for tracking
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract username (email) from token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract provider ID from token
     */
    public UUID extractProviderId(String token) {
        String providerIdStr = extractClaim(token, claims -> claims.get("provider_id", String.class));
        return providerIdStr != null ? UUID.fromString(providerIdStr) : null;
    }

    /**
     * Extract specialization from token
     */
    public String extractSpecialization(String token) {
        return extractClaim(token, claims -> claims.get("specialization", String.class));
    }

    /**
     * Extract verification status from token
     */
    public String extractVerificationStatus(String token) {
        return extractClaim(token, claims -> claims.get("verification_status", String.class));
    }

    /**
     * Extract role from token
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extract token type from token
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("token_type", String.class));
    }

    /**
     * Extract patient ID from token
     */
    public UUID extractPatientId(String token) {
        String patientIdStr = extractClaim(token, claims -> claims.get("patient_id", String.class));
        return patientIdStr != null ? UUID.fromString(patientIdStr) : null;
    }

    /**
     * Extract age category from patient token
     */
    public String extractAgeCategory(String token) {
        return extractClaim(token, claims -> claims.get("age_category", String.class));
    }

    /**
     * Extract user type from token (PROVIDER or PATIENT)
     */
    public String extractUserType(String token) {
        return extractClaim(token, claims -> claims.get("user_type", String.class));
    }

    /**
     * Check if token is for a patient
     */
    public boolean isPatientToken(String token) {
        String userType = extractUserType(token);
        String role = extractRole(token);
        return "PATIENT".equals(userType) || "PATIENT".equals(role);
    }

    /**
     * Check if token is for a provider
     */
    public boolean isProviderToken(String token) {
        String userType = extractUserType(token);
        String role = extractRole(token);
        return "PROVIDER".equals(userType) || "PROVIDER".equals(role);
    }

    /**
     * Extract JTI (token ID) from token
     */
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract issued at date from token
     */
    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extract claims with specific verification key (for patient tokens)
     */
    private Claims extractAllClaimsWithKey(String token, SecretKey key) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token without username check (for refresh tokens)
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is an access token
     */
    public boolean isAccessToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "access".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if token is a refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get token expiration in seconds (for API response)
     */
    public long getTokenExpirationInSeconds(boolean rememberMe) {
        int hours = rememberMe ? accessTokenRememberMeHours : accessTokenExpirationHours;
        return hours * 3600L; // Convert hours to seconds
    }

    /**
     * Get refresh token expiration date
     */
    public LocalDateTime getRefreshTokenExpiration(boolean rememberMe) {
        int days = rememberMe ? refreshTokenRememberMeDays : refreshTokenExpirationDays;
        return LocalDateTime.now().plusDays(days);
    }

    /**
     * Check if token will expire soon (within 5 minutes)
     */
    public boolean willExpireSoon(String token) {
        try {
            Date expiration = extractExpiration(token);
            long currentTime = System.currentTimeMillis();
            long expirationTime = expiration.getTime();
            long fiveMinutesInMs = 5 * 60 * 1000; // 5 minutes in milliseconds
            
            return (expirationTime - currentTime) < fiveMinutesInMs;
        } catch (Exception e) {
            return true; // If we can't parse it, consider it expiring soon
        }
    }

    /**
     * Extract bearer token from Authorization header
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Create token hash for secure storage (for refresh tokens)
     */
    public String createTokenHash(String token) {
        // Use a portion of the token as hash for storage
        // In production, you might want to use a proper hashing algorithm
        try {
            String jti = extractJti(token);
            return jti != null ? jti : token.substring(token.length() - 32);
        } catch (Exception e) {
            // Fallback to last 32 characters if JTI extraction fails
            return token.length() >= 32 ? token.substring(token.length() - 32) : token;
        }
    }
} 