package com.healthfirst.controller;

import com.healthfirst.dto.ProviderRegistrationRequest;
import com.healthfirst.dto.ProviderRegistrationResponse;
import com.healthfirst.exception.ValidationException;
import com.healthfirst.service.ProviderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/provider")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService providerService;

    /**
     * Register a new healthcare provider
     * POST /api/v1/provider/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerProvider(
            @Valid @RequestBody ProviderRegistrationRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String clientIp = getClientIpAddress(httpRequest);
            log.info("Provider registration request received from IP: {} for email: {}", clientIp, request.getEmail());
            
            ProviderRegistrationResponse response = providerService.registerProvider(request, clientIp);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (ValidationException e) {
            log.warn("Provider registration validation failed: {}", e.getErrors());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(createErrorResponse("Validation failed", e.getErrors()));
                
        } catch (Exception e) {
            log.error("Provider registration failed with unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Registration failed due to an internal error. Please try again.", null));
        }
    }

    /**
     * Verify provider email
     * GET /api/v1/provider/verify
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token, HttpServletRequest httpRequest) {
        try {
            String clientIp = getClientIpAddress(httpRequest);
            log.info("Email verification request received from IP: {} for token: {}", clientIp, token);
            
            boolean verified = providerService.verifyProviderEmail(token);
            
            if (verified) {
                return ResponseEntity.ok(createSuccessResponse("Email verified successfully. You can now log in to your account."));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Email verification failed", null));
            }
            
        } catch (ValidationException e) {
            log.warn("Email verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(e.getMessage(), null));
                
        } catch (Exception e) {
            log.error("Email verification failed with unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Verification failed due to an internal error. Please try again.", null));
        }
    }

    /**
     * Resend verification email
     * POST /api/v1/provider/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(
            @RequestParam String email,
            HttpServletRequest httpRequest) {
        
        try {
            String clientIp = getClientIpAddress(httpRequest);
            log.info("Resend verification email request from IP: {} for email: {}", clientIp, email);
            
            providerService.resendVerificationEmail(email);
            
            return ResponseEntity.ok(createSuccessResponse("Verification email sent successfully."));
            
        } catch (ValidationException e) {
            log.warn("Resend verification email failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(e.getMessage(), null));
                
        } catch (Exception e) {
            log.error("Resend verification email failed with unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to send verification email. Please try again.", null));
        }
    }

    /**
     * Get provider verification statistics (admin endpoint)
     * GET /api/v1/provider/stats/verification
     */
    @GetMapping("/stats/verification")
    public ResponseEntity<?> getVerificationStats() {
        try {
            ProviderService.VerificationStats stats = providerService.getVerificationStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "pending", stats.pending(),
                "verified", stats.verified(),
                "rejected", stats.rejected(),
                "total", stats.pending() + stats.verified() + stats.rejected()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get verification statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Failed to retrieve statistics", null));
        }
    }

    /**
     * Health check endpoint
     * GET /api/v1/provider/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "provider-service");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }

    /**
     * Extract client IP address from request
     */
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

    /**
     * Create standardized error response
     */
    private Map<String, Object> createErrorResponse(String message, Object errors) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        
        if (errors != null) {
            response.put("errors", errors);
        }
        
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    /**
     * Create standardized success response
     */
    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }
} 