package com.healthfirst.controller;

import com.healthfirst.dto.PatientLoginRequest;
import com.healthfirst.dto.PatientLoginResponse;
import com.healthfirst.entity.PatientRefreshToken;
import com.healthfirst.service.PatientAuthService;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for patient authentication endpoints with HIPAA compliance
 * Handles patient login, token refresh, logout, and session management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/patient")
@RequiredArgsConstructor
public class PatientAuthController {

    private final PatientAuthService patientAuthService;
    private final JwtUtils jwtUtils;

    /**
     * Patient login endpoint
     * POST /api/v1/patient/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody PatientLoginRequest loginRequest,
            HttpServletRequest request) {
        
        try {
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            log.debug("Patient login attempt for identifier: {} from IP: {}", 
                    loginRequest.getMaskedIdentifier(), clientIp);

            PatientLoginResponse response = patientAuthService.login(loginRequest, clientIp, userAgent);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                HttpStatus status = getHttpStatusForError(response.getErrorCode());
                return ResponseEntity.status(status).body(response);
            }

        } catch (Exception e) {
            log.error("Patient login endpoint error for identifier: {}", 
                    loginRequest.getMaskedIdentifier(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PatientLoginResponse.internalError());
        }
    }

    /**
     * Token refresh endpoint
     * POST /api/v1/patient/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String refreshToken = request.get("refresh_token");
            String clientIp = getClientIpAddress(httpRequest);

            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Refresh token is required", "MISSING_REFRESH_TOKEN"));
            }

            log.debug("Patient token refresh attempt from IP: {}", clientIp);

            PatientLoginResponse response = patientAuthService.refreshToken(refreshToken, clientIp);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                HttpStatus status = getHttpStatusForError(response.getErrorCode());
                return ResponseEntity.status(status).body(response);
            }

        } catch (Exception e) {
            log.error("Patient token refresh endpoint error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PatientLoginResponse.tokenRefreshError());
        }
    }

    /**
     * Logout endpoint (single session)
     * POST /api/v1/patient/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String refreshToken = request.get("refresh_token");
            String clientIp = getClientIpAddress(httpRequest);

            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Refresh token is required", "MISSING_REFRESH_TOKEN"));
            }

            log.debug("Patient logout attempt from IP: {}", clientIp);

            boolean success = patientAuthService.logout(refreshToken, clientIp);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Logged out successfully" : "Logout failed");
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Patient logout endpoint error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Logout failed due to an internal error", "INTERNAL_ERROR"));
        }
    }

    /**
     * Logout from all devices
     * POST /api/v1/patient/logout-all
     */
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(
            @RequestBody(required = false) Map<String, String> request,
            HttpServletRequest httpRequest,
            Authentication authentication) {
        
        try {
            String clientIp = getClientIpAddress(httpRequest);
            UUID patientId = getCurrentPatientId(authentication);

            if (patientId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentication required", "UNAUTHORIZED"));
            }

            log.debug("Patient logout all attempt for patient: {} from IP: {}", patientId, clientIp);

            boolean success = patientAuthService.logoutAll(patientId, clientIp);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Logged out from all devices successfully" : "Logout from all devices failed");
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Patient logout all endpoint error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Logout all failed due to an internal error", "INTERNAL_ERROR"));
        }
    }

    /**
     * Get active sessions
     * GET /api/v1/patient/sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getActiveSessions(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        try {
            String clientIp = getClientIpAddress(httpRequest);
            UUID patientId = getCurrentPatientId(authentication);

            if (patientId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentication required", "UNAUTHORIZED"));
            }

            log.debug("Get active sessions request for patient: {} from IP: {}", patientId, clientIp);

            List<PatientRefreshToken> activeSessions = patientAuthService.getActiveSessions(patientId);
            
            List<Map<String, Object>> sessionData = activeSessions.stream()
                    .map(this::mapSessionToResponse)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessions", sessionData);
            response.put("total_sessions", sessionData.size());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Get sessions endpoint error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get sessions", "INTERNAL_ERROR"));
        }
    }

    /**
     * Revoke specific session
     * DELETE /api/v1/patient/sessions/{sessionId}
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> revokeSession(
            @PathVariable UUID sessionId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        try {
            String clientIp = getClientIpAddress(httpRequest);
            UUID patientId = getCurrentPatientId(authentication);

            if (patientId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentication required", "UNAUTHORIZED"));
            }

            log.debug("Revoke session request for session: {} by patient: {} from IP: {}", 
                    sessionId, patientId, clientIp);

            boolean success = patientAuthService.revokeSession(sessionId, patientId, clientIp);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Session revoked successfully" : "Failed to revoke session");
            
            HttpStatus status = success ? HttpStatus.OK : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("Revoke session endpoint error for session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to revoke session", "INTERNAL_ERROR"));
        }
    }

    /**
     * Get authentication status
     * GET /api/v1/patient/auth/status
     */
    @GetMapping("/auth/status")
    public ResponseEntity<?> getAuthStatus(Authentication authentication) {
        try {
            UUID patientId = getCurrentPatientId(authentication);
            
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", patientId != null);
            
            if (patientId != null) {
                response.put("patient_id", patientId.toString());
                response.put("email", authentication.getName());
                response.put("authorities", authentication.getAuthorities());
            }
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Auth status endpoint error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get auth status", "INTERNAL_ERROR"));
        }
    }

    /**
     * Health check for patient auth service
     * GET /api/v1/patient/auth/health
     */
    @GetMapping("/auth/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "patient-auth");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Patient authentication service is running");
        
        return ResponseEntity.ok(response);
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

    private UUID getCurrentPatientId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        // Extract patient ID from JWT token in credentials
        String credentials = (String) authentication.getCredentials();
        if (credentials != null) {
            try {
                return jwtUtils.extractPatientId(credentials);
            } catch (Exception e) {
                log.debug("Failed to extract patient ID from token", e);
            }
        }
        
        return null;
    }

    private HttpStatus getHttpStatusForError(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.BAD_REQUEST;
        }
        
        return switch (errorCode) {
            case "INVALID_CREDENTIALS" -> HttpStatus.UNAUTHORIZED;
            case "ACCOUNT_NOT_VERIFIED" -> HttpStatus.FORBIDDEN;
            case "ACCOUNT_LOCKED" -> HttpStatus.LOCKED;
            case "ACCOUNT_INACTIVE" -> HttpStatus.FORBIDDEN;
            case "RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "SUSPICIOUS_ACTIVITY" -> HttpStatus.FORBIDDEN;
            case "TOKEN_REFRESH_FAILED", "SESSION_EXPIRED" -> HttpStatus.UNAUTHORIZED;
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

    private Map<String, Object> mapSessionToResponse(PatientRefreshToken session) {
        Map<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("session_id", session.getId().toString());
        sessionMap.put("device_type", session.getDeviceType());
        sessionMap.put("device_name", session.getDeviceName());
        sessionMap.put("device_info", session.getFormattedDeviceInfo());
        sessionMap.put("ip_address", session.getMaskedIpAddress()); // Masked for privacy
        sessionMap.put("location_info", session.getLocationInfo());
        sessionMap.put("login_time", session.getSessionStart());
        sessionMap.put("last_activity", session.getLastActivity());
        sessionMap.put("activity_count", session.getActivityCount());
        sessionMap.put("is_current", isCurrentSession(session));
        sessionMap.put("expires_at", session.getExpiresAt());
        sessionMap.put("minutes_until_expiry", session.getMinutesUntilExpiration());
        sessionMap.put("session_duration_minutes", session.getSessionDurationMinutes());
        sessionMap.put("login_method", session.getLoginMethod());
        
        return sessionMap;
    }

    private boolean isCurrentSession(PatientRefreshToken session) {
        // This would typically check if the current request's session matches this token
        // For now, we'll check if it's the most recently used session
        return session.getLastActivity() != null && 
               session.getLastActivity().isAfter(
                   java.time.LocalDateTime.now().minusMinutes(5)
               );
    }
} 