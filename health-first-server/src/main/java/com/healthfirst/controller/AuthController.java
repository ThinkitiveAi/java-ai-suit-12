package com.healthfirst.controller;

import com.healthfirst.dto.LoginRequest;
import com.healthfirst.dto.LoginResponse;
import com.healthfirst.entity.RefreshToken;
import com.healthfirst.service.AuthService;
import com.healthfirst.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for provider authentication endpoints
 * Handles login, logout, token refresh, and session management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/provider")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    /**
     * Provider login endpoint
     * POST /api/v1/provider/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        
        try {
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            log.debug("Login attempt for identifier: {} from IP: {}", 
                    loginRequest.getIdentifier(), clientIp);

            LoginResponse response = authService.login(loginRequest, clientIp, userAgent);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                HttpStatus status = getHttpStatusForError(response.getErrorCode());
                return ResponseEntity.status(status).body(response);
            }

        } catch (Exception e) {
            log.error("Login endpoint error for identifier: {}", loginRequest.getIdentifier(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponse.internalError());
        }
    }

    /**
     * Refresh access token endpoint
     * POST /api/v1/provider/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String refreshToken = request.get("refresh_token");
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Refresh token is required", "MISSING_REFRESH_TOKEN"));
            }

            String clientIp = getClientIpAddress(httpRequest);
            LoginResponse response = authService.refreshToken(refreshToken, clientIp);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                HttpStatus status = getHttpStatusForError(response.getErrorCode());
                return ResponseEntity.status(status).body(response);
            }

        } catch (Exception e) {
            log.error("Token refresh endpoint error from IP: {}", getClientIpAddress(httpRequest), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponse.internalError());
        }
    }

    /**
     * Logout endpoint (single session)
     * POST /api/v1/provider/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String refreshToken = request.get("refresh_token");
            String clientIp = getClientIpAddress(httpRequest);
            
            boolean success = authService.logout(refreshToken, clientIp);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Logged out successfully" : "Logout failed");
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Logout endpoint error from IP: {}", getClientIpAddress(httpRequest), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Logout failed due to an internal error", "INTERNAL_ERROR"));
        }
    }

    /**
     * Logout from all sessions endpoint
     * POST /api/v1/provider/logout-all
     */
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(
            @RequestBody(required = false) Map<String, String> request,
            HttpServletRequest httpRequest,
            Authentication authentication) {
        
        try {
            UUID providerId = getCurrentProviderId(authentication);
            if (providerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentication required", "UNAUTHORIZED"));
            }

            // Optional password verification for enhanced security
            if (request != null && request.containsKey("password")) {
                // Password verification could be implemented here for additional security
                log.debug("Password verification requested for logout-all");
            }

            String clientIp = getClientIpAddress(httpRequest);
            boolean success = authService.logoutAll(providerId, clientIp);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Logged out from all sessions successfully" : "Logout from all sessions failed");
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Logout all endpoint error from IP: {}", getClientIpAddress(httpRequest), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Logout all failed due to an internal error", "INTERNAL_ERROR"));
        }
    }

    /**
     * Get active sessions for current provider
     * GET /api/v1/provider/sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getActiveSessions(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        try {
            UUID providerId = getCurrentProviderId(authentication);
            if (providerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentication required", "UNAUTHORIZED"));
            }

            List<RefreshToken> activeSessions = authService.getActiveSessions(providerId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Active sessions retrieved successfully");
            response.put("data", createSessionsResponse(activeSessions));
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Get sessions endpoint error from IP: {}", getClientIpAddress(httpRequest), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve sessions", "INTERNAL_ERROR"));
        }
    }

    /**
     * Revoke specific session
     * DELETE /api/v1/provider/sessions/{sessionId}
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> revokeSession(
            @PathVariable UUID sessionId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        try {
            UUID providerId = getCurrentProviderId(authentication);
            if (providerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentication required", "UNAUTHORIZED"));
            }

            String clientIp = getClientIpAddress(httpRequest);
            boolean success = authService.revokeSession(sessionId, providerId, clientIp);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Session revoked successfully" : "Failed to revoke session");
            
            HttpStatus status = success ? HttpStatus.OK : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("Revoke session endpoint error for session: {} from IP: {}", 
                    sessionId, getClientIpAddress(httpRequest), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to revoke session", "INTERNAL_ERROR"));
        }
    }

    /**
     * Check authentication status
     * GET /api/v1/provider/auth/status
     */
    @GetMapping("/auth/status")
    public ResponseEntity<?> getAuthStatus(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Not authenticated", "UNAUTHORIZED"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("authenticated", true);
            response.put("provider_id", getCurrentProviderId(authentication));
            response.put("email", authentication.getName());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Auth status endpoint error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get auth status", "INTERNAL_ERROR"));
        }
    }

    // Helper methods

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private UUID getCurrentProviderId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        
        try {
            // Assuming the principal contains the provider ID
            String principal = authentication.getName();
            if (principal != null) {
                // If using JWT, extract provider ID from token
                String authHeader = (String) authentication.getCredentials();
                if (authHeader != null) {
                    String token = jwtUtils.extractTokenFromHeader("Bearer " + authHeader);
                    if (token != null) {
                        return jwtUtils.extractProviderId(token);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract provider ID from authentication", e);
        }
        
        return null;
    }

    private HttpStatus getHttpStatusForError(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.BAD_REQUEST;
        }
        
        return switch (errorCode) {
            case "INVALID_CREDENTIALS" -> HttpStatus.UNAUTHORIZED;
            case "EMAIL_NOT_VERIFIED", "ACCOUNT_INACTIVE" -> HttpStatus.FORBIDDEN;
            case "ACCOUNT_LOCKED" -> HttpStatus.LOCKED;
            case "RATE_LIMIT_EXCEEDED", "MAX_SESSIONS_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "INVALID_REFRESH_TOKEN", "TOKEN_NOT_FOUND" -> HttpStatus.UNAUTHORIZED;
            case "PROVIDER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "INTERNAL_ERROR" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private Map<String, Object> createErrorResponse(String message, String errorCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("error_code", errorCode);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    private Map<String, Object> createSessionsResponse(List<RefreshToken> sessions) {
        Map<String, Object> response = new HashMap<>();
        response.put("total_sessions", sessions.size());
        response.put("sessions", sessions.stream().map(this::mapSessionToResponse).toList());
        return response;
    }

    private Map<String, Object> mapSessionToResponse(RefreshToken session) {
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("session_id", session.getId());
        sessionData.put("device_type", session.getDeviceType());
        sessionData.put("device_name", session.getDeviceName());
        sessionData.put("ip_address", session.getIpAddress());
        sessionData.put("created_at", session.getCreatedAt());
        sessionData.put("last_used_at", session.getLastUsedAt());
        sessionData.put("expires_at", session.getExpiresAt());
        sessionData.put("is_current", false); // Could be enhanced to detect current session
        return sessionData;
    }
} 