package com.healthfirst.middleware;

import com.healthfirst.service.AuditService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingMiddleware extends OncePerRequestFilter {

    private final AuditService auditService;
    
    @Value("${app.rate-limit.registration.attempts:5}")
    private int registrationAttempts;
    
    @Value("${app.rate-limit.registration.window-hours:1}")
    private int registrationWindowHours;
    
    @Value("${app.rate-limit.login.attempts:5}")
    private int loginAttempts;
    
    @Value("${app.rate-limit.login.window-minutes:15}")
    private int loginWindowMinutes;

    // Patient rate limiting configuration
    @Value("${app.rate-limit.patient.registration.attempts:3}")
    private int patientRegistrationAttempts;

    @Value("${app.rate-limit.patient.registration.window-hours:1}")
    private int patientRegistrationWindowHours;

    @Value("${app.rate-limit.patient.verification.attempts:5}")
    private int patientVerificationAttempts;

    @Value("${app.rate-limit.patient.verification.window-minutes:15}")
    private int patientVerificationWindowMinutes;

    @Value("${app.rate-limit.patient.login.attempts:5}")
    private int patientLoginAttempts;

    @Value("${app.rate-limit.patient.login.window-minutes:15}")
    private int patientLoginWindowMinutes;

    private final ConcurrentHashMap<String, Bucket> registrationBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> patientRegistrationBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> patientVerificationBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> patientLoginBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> violationCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIpAddress(request);
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // Apply rate limiting based on endpoint
        if (shouldApplyRateLimit(requestURI, method)) {
            Bucket bucket = getBucketForEndpoint(clientIp, requestURI);
            
            if (bucket != null && !bucket.tryConsume(1)) {
                handleRateLimitExceeded(clientIp, requestURI, response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldApplyRateLimit(String requestURI, String method) {
        // Provider endpoints
        if (("POST".equals(method) && requestURI.equals("/api/v1/provider/register")) ||
            ("POST".equals(method) && requestURI.equals("/api/v1/provider/login")) ||
            ("POST".equals(method) && requestURI.equals("/api/v1/provider/resend-verification"))) {
            return true;
        }
        
        // Patient endpoints
        if (("POST".equals(method) && requestURI.equals("/api/v1/patient/register")) ||
            ("POST".equals(method) && requestURI.equals("/api/v1/patient/login")) ||
            ("POST".equals(method) && requestURI.equals("/api/v1/patient/resend-verification")) ||
            ("POST".equals(method) && requestURI.equals("/api/v1/patient/verify-phone")) ||
            ("POST".equals(method) && requestURI.equals("/api/v1/patient/resend-phone-verification"))) {
            return true;
        }
        
        return false;
    }

    private Bucket getBucketForEndpoint(String clientIp, String requestURI) {
        // Provider endpoints
        if (requestURI.equals("/api/v1/provider/register") || requestURI.equals("/api/v1/provider/resend-verification")) {
            return getRegistrationBucket(clientIp);
        } else if (requestURI.equals("/api/v1/provider/login")) {
            return getLoginBucket(clientIp);
        }
        
        // Patient endpoints
        else if (requestURI.equals("/api/v1/patient/register")) {
            return getPatientRegistrationBucket(clientIp);
        } else if (requestURI.equals("/api/v1/patient/login")) {
            return getPatientLoginBucket(clientIp);
        } else if (requestURI.equals("/api/v1/patient/resend-verification") ||
                   requestURI.equals("/api/v1/patient/verify-phone") ||
                   requestURI.equals("/api/v1/patient/resend-phone-verification")) {
            return getPatientVerificationBucket(clientIp);
        }
        
        return null;
    }

    private Bucket getRegistrationBucket(String clientIp) {
        return registrationBuckets.computeIfAbsent(clientIp, key -> {
            Bandwidth limit = Bandwidth.classic(registrationAttempts, 
                Refill.intervally(registrationAttempts, Duration.ofHours(registrationWindowHours)));
            return Bucket.builder()
                .addLimit(limit)
                .build();
        });
    }

    private Bucket getLoginBucket(String clientIp) {
        return loginBuckets.computeIfAbsent(clientIp, key -> {
            Bandwidth limit = Bandwidth.classic(loginAttempts, 
                Refill.intervally(loginAttempts, Duration.ofMinutes(loginWindowMinutes)));
            return Bucket.builder()
                .addLimit(limit)
                .build();
        });
    }

    private void handleRateLimitExceeded(String clientIp, String endpoint, HttpServletResponse response) 
            throws IOException {
        
        // Increment violation count
        int violations = violationCounts.merge(clientIp, 1, Integer::sum);
        
        // Log the rate limit violation
        auditService.logRateLimitViolation(clientIp, endpoint, violations);
        
        // Determine blocking duration based on violation count
        int blockDurationMinutes = Math.min(violations * 15, 720); // Max 12 hours
        
        log.warn("Rate limit exceeded for IP: {} on endpoint: {}. Violation count: {}, Block duration: {} minutes", 
                clientIp, endpoint, violations, blockDurationMinutes);

        // Set response headers
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("X-Rate-Limit-Retry-After", String.valueOf(blockDurationMinutes * 60));
        response.setHeader("X-Rate-Limit-Limit", getEndpointLimit(endpoint));
        response.setHeader("X-Rate-Limit-Remaining", "0");

        // Write error response
        String errorResponse = createRateLimitErrorResponse(endpoint, blockDurationMinutes);
        response.getWriter().write(errorResponse);
    }

    private String getEndpointLimit(String endpoint) {
        if (endpoint.contains("/register") || endpoint.contains("/resend-verification")) {
            return String.valueOf(registrationAttempts);
        } else if (endpoint.contains("/login")) {
            return String.valueOf(loginAttempts);
        }
        return "5"; // Default
    }

    private String createRateLimitErrorResponse(String endpoint, int blockDurationMinutes) {
        String message;
        if (endpoint.contains("/register")) {
            message = String.format("Too many registration attempts. Please try again in %d minutes.", blockDurationMinutes);
        } else if (endpoint.contains("/login")) {
            message = String.format("Too many login attempts. Please try again in %d minutes.", blockDurationMinutes);
        } else {
            message = String.format("Rate limit exceeded. Please try again in %d minutes.", blockDurationMinutes);
        }

        return String.format("""
            {
                "success": false,
                "error_code": "RATE_LIMIT_EXCEEDED",
                "message": "%s",
                "retry_after_seconds": %d,
                "timestamp": %d
            }
            """, message, blockDurationMinutes * 60, System.currentTimeMillis());
    }

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

    private Bucket getPatientRegistrationBucket(String clientIp) {
        return patientRegistrationBuckets.computeIfAbsent(clientIp, key -> {
            Bandwidth limit = Bandwidth.classic(patientRegistrationAttempts, 
                Refill.intervally(patientRegistrationAttempts, Duration.ofHours(patientRegistrationWindowHours)));
            return Bucket.builder()
                .addLimit(limit)
                .build();
        });
    }

    private Bucket getPatientVerificationBucket(String clientIp) {
        return patientVerificationBuckets.computeIfAbsent(clientIp, key -> {
            Bandwidth limit = Bandwidth.classic(patientVerificationAttempts, 
                Refill.intervally(patientVerificationAttempts, Duration.ofMinutes(patientVerificationWindowMinutes)));
            return Bucket.builder()
                .addLimit(limit)
                .build();
        });
    }

    private Bucket getPatientLoginBucket(String clientIp) {
        return patientLoginBuckets.computeIfAbsent(clientIp, key -> {
            Bandwidth limit = Bandwidth.classic(patientLoginAttempts, 
                Refill.intervally(patientLoginAttempts, Duration.ofMinutes(patientLoginWindowMinutes)));
            return Bucket.builder()
                .addLimit(limit)
                .build();
        });
    }

    /**
     * Clean up old buckets and violation counts periodically
     */
    public void cleanup() {
        // In a production environment, you might want to implement this as a scheduled task
        // to prevent memory leaks by removing old entries
        log.info("Cleaning up rate limiting data structures");
        
        // Simple cleanup - in production, you'd want more sophisticated logic
        if (registrationBuckets.size() > 1000) {
            registrationBuckets.clear();
        }
        if (loginBuckets.size() > 1000) {
            loginBuckets.clear();
        }
        if (patientRegistrationBuckets.size() > 1000) {
            patientRegistrationBuckets.clear();
        }
        if (patientVerificationBuckets.size() > 1000) {
            patientVerificationBuckets.clear();
        }
        if (patientLoginBuckets.size() > 1000) {
            patientLoginBuckets.clear();
        }
        if (violationCounts.size() > 1000) {
            violationCounts.clear();
        }
    }
} 