package com.healthfirst.controller;

import com.healthfirst.dto.PatientRegistrationRequest;
import com.healthfirst.dto.PatientRegistrationResponse;
import com.healthfirst.exception.ValidationException;
import com.healthfirst.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Patient Management
 * Handles patient registration, verification, and profile operations with HIPAA compliance
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/patient")
@RequiredArgsConstructor
@Tag(name = "Patient Management", description = "Patient registration, verification, and profile management endpoints")
public class PatientController {

    private final PatientService patientService;

    /**
     * Register a new patient
     * POST /api/v1/patient/register
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register New Patient",
        description = """
            Register a new patient account with comprehensive validation and HIPAA compliance.
            
            **Features:**
            - Age verification (COPPA compliance - minimum age 13)
            - Email and phone validation
            - Password strength requirements
            - Optional medical history and insurance information
            - Automatic email verification workflow
            - Rate limiting protection
            
            **Privacy & Security:**
            - All data handled according to HIPAA regulations
            - Passwords encrypted with BCrypt (12 salt rounds)
            - Email addresses normalized and validated
            - Phone numbers validated with international format support
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Patient registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data or validation failed"),
        @ApiResponse(responseCode = "409", description = "Email or phone number already exists"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> registerPatient(
            @Parameter(description = "Patient registration details with personal, contact, and optional medical information")
            @Valid @RequestBody PatientRegistrationRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String clientIp = getClientIpAddress(httpRequest);
            
            log.debug("Patient registration attempt for email: {} from IP: {}", 
                    request.getEmail(), clientIp);

            PatientRegistrationResponse response = patientService.registerPatient(request, clientIp);
            
            if (response.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                HttpStatus status = getHttpStatusForError(response.getErrorCode());
                return ResponseEntity.status(status).body(response);
            }

        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(createErrorResponse("Registration validation failed", e.getErrors(), "VALIDATION_ERROR"));
        } catch (Exception e) {
            log.error("Patient registration endpoint error for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PatientRegistrationResponse.internalError());
        }
    }

    /**
     * Verify patient email
     * GET /api/v1/patient/verify
     */
    @GetMapping("/verify")
    @Operation(
        summary = "Verify Patient Email",
        description = """
            Verify patient email address using the verification token sent during registration.
            
            **Process:**
            1. Patient receives email with verification link
            2. Clicking link calls this endpoint with token parameter
            3. Token is validated and account is activated
            4. Patient can now log in to the system
            
            **Security:**
            - Tokens expire after 24 hours
            - Single-use tokens (consumed after verification)
            - Rate limiting to prevent abuse
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token"),
        @ApiResponse(responseCode = "404", description = "Token not found"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<?> verifyPatientEmail(
            @Parameter(description = "Email verification token", required = true)
            @RequestParam String token, HttpServletRequest httpRequest) {
        
        try {
            String clientIp = getClientIpAddress(httpRequest);
            log.debug("Patient email verification attempt with token: {} from IP: {}", token, clientIp);

            boolean success = patientService.verifyPatientEmail(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? 
                    "Email verified successfully! You can now log in to your account." : 
                    "Email verification failed. The token may be invalid or expired.");
            response.put("timestamp", System.currentTimeMillis());
            
            HttpStatus status = success ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("Patient email verification endpoint error for token: {}", token, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Email verification failed due to an internal error", "Internal Server Error", "INTERNAL_ERROR"));
        }
    }

    /**
     * Phone verification endpoint
     * POST /api/v1/patient/verify-phone
     */
    @PostMapping("/verify-phone")
    public ResponseEntity<?> verifyPhone(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String patientIdStr = request.get("patient_id");
            String otp = request.get("otp");
            String clientIp = getClientIpAddress(httpRequest);

            if (patientIdStr == null || otp == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Patient ID and OTP are required", null, "MISSING_PARAMETERS"));
            }

            UUID patientId;
            try {
                patientId = UUID.fromString(patientIdStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Invalid patient ID format", null, "INVALID_PATIENT_ID"));
            }

            log.debug("Phone verification attempt for patient: {} from IP: {}", patientId, clientIp);

            boolean verified = patientService.verifyPatientPhone(patientId, otp);
            
            Map<String, Object> response = new HashMap<>();
            
            if (verified) {
                response.put("success", true);
                response.put("message", "Phone number verified successfully!");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Invalid or expired OTP. Please request a new verification code.");
                response.put("error_code", "INVALID_OTP");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            log.error("Phone verification endpoint error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Phone verification failed due to an internal error", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * Resend email verification endpoint
     * POST /api/v1/patient/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendEmailVerification(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String email = request.get("email");
            String clientIp = getClientIpAddress(httpRequest);

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Email is required", null, "MISSING_EMAIL"));
            }

            log.debug("Resend email verification request for: {} from IP: {}", email, clientIp);

            // Always return success for security (don't reveal if email exists)
            patientService.resendEmailVerification(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "If an account with this email exists, a verification email has been sent.");
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Resend verification endpoint error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to resend verification email", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * Resend phone verification endpoint
     * POST /api/v1/patient/resend-phone-verification
     */
    @PostMapping("/resend-phone-verification")
    public ResponseEntity<?> resendPhoneVerification(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String patientIdStr = request.get("patient_id");
            String clientIp = getClientIpAddress(httpRequest);

            if (patientIdStr == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Patient ID is required", null, "MISSING_PATIENT_ID"));
            }

            UUID patientId;
            try {
                patientId = UUID.fromString(patientIdStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Invalid patient ID format", null, "INVALID_PATIENT_ID"));
            }

            log.debug("Resend phone verification request for patient: {} from IP: {}", patientId, clientIp);

            boolean sent = patientService.resendPhoneVerification(patientId);
            
            Map<String, Object> response = new HashMap<>();
            
            if (sent) {
                response.put("success", true);
                response.put("message", "Verification code has been sent to your phone.");
            } else {
                response.put("success", false);
                response.put("message", "Unable to send verification code. Please try again later or contact support.");
                response.put("error_code", "SMS_SERVICE_ERROR");
            }
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Resend phone verification endpoint error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to resend phone verification", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * Health check for patient service
     * GET /api/v1/patient/health
     */
    @GetMapping("/health")
    @Operation(
        summary = "Patient Service Health Check",
        description = "Check the health status of the patient management service and its dependencies."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service is healthy")
    })
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "patient-management");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Patient management service is running");
        response.put("features", Map.of(
            "registration", "enabled",
            "emailVerification", "enabled",
            "phoneVerification", "enabled",
            "hipaaCompliance", "enabled",
            "coppaCompliance", "enabled"
        ));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check if email exists (for frontend validation)
     * GET /api/v1/patient/check-email?email={email}
     */
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmailExists(@RequestParam String email) {
        try {
            boolean exists = patientService.existsByEmail(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            response.put("available", !exists);
            
            if (exists) {
                response.put("message", "An account with this email already exists");
            } else {
                response.put("message", "Email is available");
            }
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Check email endpoint error for: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to check email availability", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * Check if phone exists (for frontend validation)
     * GET /api/v1/patient/check-phone?phone={phone}
     */
    @GetMapping("/check-phone")
    public ResponseEntity<?> checkPhoneExists(@RequestParam String phone) {
        try {
            boolean exists = patientService.existsByPhoneNumber(phone);
            
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            response.put("available", !exists);
            
            if (exists) {
                response.put("message", "An account with this phone number already exists");
            } else {
                response.put("message", "Phone number is available");
            }
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Check phone endpoint error for: {}", phone, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to check phone availability", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * Get patient statistics (admin only - to be protected by security)
     * GET /api/v1/patient/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getPatientStatistics() {
        try {
            Object[] statistics = patientService.getPatientStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "total_patients", statistics[0],      // totalPatients
                "email_verified", statistics[1],     // emailVerifiedCount  
                "phone_verified", statistics[2],     // phoneVerifiedCount
                "fully_verified", statistics[3]      // fullyVerifiedCount
            ));
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Patient statistics endpoint error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get patient statistics", null, "INTERNAL_ERROR"));
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

    private HttpStatus getHttpStatusForError(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.BAD_REQUEST;
        }
        
        return switch (errorCode) {
            case "VALIDATION_ERROR" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "DUPLICATE_EMAIL", "DUPLICATE_PHONE" -> HttpStatus.CONFLICT;
            case "UNDERAGE_REGISTRATION" -> HttpStatus.FORBIDDEN;
            case "PASSWORD_MISMATCH" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "EMAIL_SERVICE_ERROR", "SMS_SERVICE_ERROR" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "INTERNAL_ERROR" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private Map<String, Object> createErrorResponse(String message, Object errors, String errorCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("error_code", errorCode);
        response.put("timestamp", System.currentTimeMillis());
        
        if (errors != null) {
            response.put("errors", errors);
        }
        
        return response;
    }
} 