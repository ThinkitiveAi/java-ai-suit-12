package com.healthfirst.filter;

import com.healthfirst.entity.Patient;
import com.healthfirst.entity.Provider;
import com.healthfirst.repository.PatientRepository;
import com.healthfirst.repository.ProviderRepository;
import com.healthfirst.service.AuditService;
import com.healthfirst.util.JwtUtils;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT Authentication Filter for validating access tokens for both providers and patients
 * Processes JWT tokens from Authorization header and sets Spring Security context
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final ProviderRepository providerRepository;
    private final PatientRepository patientRepository;
    private final AuditService auditService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");
            String token = jwtUtils.extractTokenFromHeader(authHeader);

            // Skip processing if no token or user already authenticated
            if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Validate and process JWT token
            if (processJwtToken(token, request)) {
                log.debug("Successfully authenticated request with JWT token");
            } else {
                log.debug("JWT token validation failed");
            }

        } catch (ExpiredJwtException e) {
            log.debug("JWT token is expired: {}", e.getMessage());
            handleTokenError(response, "Token expired", "TOKEN_EXPIRED", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (JwtException e) {
            log.debug("JWT token validation failed: {}", e.getMessage());
            handleTokenError(response, "Invalid token", "INVALID_TOKEN", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (Exception e) {
            log.error("Error processing JWT token", e);
            handleTokenError(response, "Authentication error", "AUTH_ERROR", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Process and validate JWT token, set authentication context if valid
     */
    private boolean processJwtToken(String token, HttpServletRequest request) {
        try {
            // Validate token format and signature
            if (!jwtUtils.validateToken(token)) {
                return false;
            }

            // Verify it's an access token
            if (!jwtUtils.isAccessToken(token)) {
                log.debug("Token is not an access token");
                return false;
            }

            // Check if it's a patient or provider token
            if (jwtUtils.isPatientToken(token)) {
                return processPatientToken(token, request);
            } else if (jwtUtils.isProviderToken(token)) {
                return processProviderToken(token, request);
            } else {
                log.debug("Token does not contain valid user type");
                return false;
            }

        } catch (Exception e) {
            log.debug("Error processing JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Process provider JWT token
     */
    private boolean processProviderToken(String token, HttpServletRequest request) {
        try {
            // Extract provider information from token
            String email = jwtUtils.extractUsername(token);
            UUID providerId = jwtUtils.extractProviderId(token);
            String role = jwtUtils.extractRole(token);
            String verificationStatus = jwtUtils.extractVerificationStatus(token);

            if (email == null || providerId == null) {
                log.debug("Provider token missing required claims");
                return false;
            }

            // Verify provider still exists and is active
            Optional<Provider> providerOpt = providerRepository.findById(providerId);
            if (providerOpt.isEmpty()) {
                log.debug("Provider not found for ID: {}", providerId);
                return false;
            }

            Provider provider = providerOpt.get();

            // Check if provider is still active and verified
            if (!isProviderActiveAndValid(provider)) {
                log.debug("Provider is not active or verified: {}", provider.getEmail());
                return false;
            }

            // Validate email matches
            if (!provider.getEmail().equals(email)) {
                log.debug("Token email does not match provider email");
                return false;
            }

            // Create authentication object
            List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "PROVIDER"))
            );

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                email, // principal
                token,  // credentials (for potential extraction of provider ID later)
                authorities
            );

            // Set additional details
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // Log successful authentication (debug level to avoid spam)
            log.debug("Successfully authenticated provider: {} with JWT", email);
            
            return true;

        } catch (Exception e) {
            log.debug("Error processing provider token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Process patient JWT token
     */
    private boolean processPatientToken(String token, HttpServletRequest request) {
        try {
            // Extract patient information from token
            String email = jwtUtils.extractUsername(token);
            UUID patientId = jwtUtils.extractPatientId(token);
            String role = jwtUtils.extractRole(token);
            String ageCategory = jwtUtils.extractAgeCategory(token);
            String verificationStatus = jwtUtils.extractVerificationStatus(token);

            if (email == null || patientId == null) {
                log.debug("Patient token missing required claims");
                return false;
            }

            // Verify patient still exists and is active
            Optional<Patient> patientOpt = patientRepository.findById(patientId);
            if (patientOpt.isEmpty()) {
                log.debug("Patient not found for ID: {}", patientId);
                return false;
            }

            Patient patient = patientOpt.get();

            // Check if patient is still active and verified
            if (!isPatientActiveAndValid(patient)) {
                log.debug("Patient is not active or verified: {}", patient.getEmail());
                return false;
            }

            // Validate email matches
            if (!patient.getEmail().equals(email)) {
                log.debug("Token email does not match patient email");
                return false;
            }

            // Create authentication object with patient-specific authorities
            List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "PATIENT"))
            );

            // Add age-specific authorities for HIPAA compliance
            if (ageCategory != null) {
                authorities.add(new SimpleGrantedAuthority("AGE_" + ageCategory));
            }

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                email, // principal
                token,  // credentials (for potential extraction of patient ID later)
                authorities
            );

            // Set additional details
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // Log successful authentication (debug level to avoid spam)
            log.debug("Successfully authenticated patient: {} with JWT (Age: {})", email, ageCategory);
            
            return true;

        } catch (Exception e) {
            log.debug("Error processing patient token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if provider is active and valid for authentication
     */
    private boolean isProviderActiveAndValid(Provider provider) {
        // Check if provider is active
        if (!provider.getIsActive()) {
            return false;
        }

        // Check if email is verified (required for login)
        if (!provider.getEmailVerified()) {
            return false;
        }

        // Check if account is not locked
        if (provider.isAccountLocked()) {
            return false;
        }

        return true;
    }

    /**
     * Check if patient is active and valid for authentication
     */
    private boolean isPatientActiveAndValid(Patient patient) {
        // Check if patient is active
        if (!patient.getIsActive()) {
            return false;
        }

        // Check if email is verified (required for login)
        if (!patient.getEmailVerified()) {
            return false;
        }

        // Check if account is not locked
        if (patient.isAccountLocked()) {
            return false;
        }

        // Check minimum age requirement (COPPA compliance)
        if (!patient.meetsMinimumAge()) {
            return false;
        }

        return true;
    }

    /**
     * Handle token validation errors
     */
    private void handleTokenError(HttpServletResponse response, String message, String errorCode, int statusCode) 
            throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format(
            """
            {
                "success": false,
                "error_code": "%s",
                "message": "%s",
                "timestamp": %d
            }
            """, errorCode, message, System.currentTimeMillis());
        
        response.getWriter().write(jsonResponse);
    }

    /**
     * Check if the request should be excluded from JWT processing
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Skip JWT validation for public endpoints
        List<String> publicPaths = List.of(
            "/api/v1/provider/register",
            "/api/v1/provider/verify",
            "/api/v1/provider/resend-verification",
            "/api/v1/provider/health",
            "/api/v1/provider/login",
            "/api/v1/provider/refresh",
            "/api/v1/patient/register",
            "/api/v1/patient/verify",
            "/api/v1/patient/verify-phone",
            "/api/v1/patient/resend-verification",
            "/api/v1/patient/resend-phone-verification",
            "/api/v1/patient/health",
            "/api/v1/patient/check-email",
            "/api/v1/patient/check-phone",
            "/api/v1/patient/login",
            "/api/v1/patient/refresh",
            "/h2-console",
            "/actuator/health",
            "/error"
        );
        
        // Check if path is in public paths or starts with public prefix
        return publicPaths.stream().anyMatch(path::startsWith) ||
               path.startsWith("/h2-console/") ||
               path.startsWith("/actuator/");
    }
} 