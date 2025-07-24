package com.healthfirst.controller;

import com.healthfirst.dto.AvailabilityRequest;
import com.healthfirst.dto.AvailabilityResponse;
import com.healthfirst.service.AvailabilityService;
import com.healthfirst.util.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Provider Availability Management
 * Handles provider scheduling, slot management, and availability operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/provider/availability")
@RequiredArgsConstructor
@Tag(name = "Provider Availability", description = "Provider scheduling and availability management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class ProviderAvailabilityController {

    private final AvailabilityService availabilityService;
    private final JwtUtils jwtUtils;

    /**
     * Create new provider availability
     * POST /api/v1/provider/availability
     */
    @PostMapping
    @Operation(
        summary = "Create Provider Availability",
        description = "Create a new availability schedule for the authenticated provider. Supports recurring and one-time schedules with conflict detection."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Availability created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "409", description = "Schedule conflict detected"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createAvailability(
            @Valid @RequestBody AvailabilityRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        try {
            UUID providerId = getCurrentProviderId(authentication);
            if (providerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentication required", "UNAUTHORIZED"));
            }

            String clientIp = getClientIpAddress(httpRequest);
            
            log.debug("Creating availability for provider: {} - {}", providerId, request.getTitle());

            AvailabilityResponse response = availabilityService.createAvailability(providerId, request, clientIp);
            
            if (response.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                HttpStatus status = getHttpStatusForError(response.getErrorCode());
                return ResponseEntity.status(status).body(response);
            }

        } catch (Exception e) {
            log.error("Error creating availability", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to create availability", "INTERNAL_ERROR"));
        }
    }

    /**
     * Get provider availability by ID
     * GET /api/v1/provider/availability/{id}
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get Availability by ID",
        description = "Retrieve detailed information about a specific availability schedule including slot statistics and booking details."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Availability retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Availability not found")
    })
    public ResponseEntity<?> getAvailability(
            @Parameter(description = "Availability ID") @PathVariable UUID id,
            Authentication authentication) {
        
        try {
            UUID providerId = getCurrentProviderId(authentication);
            if (providerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentication required", "UNAUTHORIZED"));
            }

            AvailabilityResponse response = availabilityService.getAvailability(providerId, id);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                HttpStatus status = getHttpStatusForError(response.getErrorCode());
                return ResponseEntity.status(status).body(response);
            }

        } catch (Exception e) {
            log.error("Error retrieving availability {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve availability", "INTERNAL_ERROR"));
        }
    }

    /**
     * Get all availabilities for the authenticated provider
     * GET /api/v1/provider/availability
     */
    @GetMapping
    @Operation(
        summary = "Get Provider Availabilities",
        description = "Retrieve all active availability schedules for the authenticated provider with optional pagination."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Availabilities retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<?> getProviderAvailabilities(
            @Parameter(description = "Pagination parameters") @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Include pagination") @RequestParam(defaultValue = "false") boolean paginated,
            Authentication authentication) {
        
        try {
            UUID providerId = getCurrentProviderId(authentication);
            if (providerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentication required", "UNAUTHORIZED"));
            }

            if (paginated) {
                Page<AvailabilityResponse> availabilities = availabilityService.getProviderAvailabilities(providerId, pageable);
                return ResponseEntity.ok(availabilities);
            } else {
                List<AvailabilityResponse> availabilities = availabilityService.getProviderAvailabilities(providerId);
                return ResponseEntity.ok(createSuccessResponse(availabilities, "Availabilities retrieved successfully"));
            }

        } catch (Exception e) {
            log.error("Error retrieving availabilities for provider", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve availabilities", "INTERNAL_ERROR"));
        }
    }

    /**
     * Update provider availability
     * PUT /api/v1/provider/availability/{id}
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Update Provider Availability",
        description = "Update an existing availability schedule. Changes to time or date will regenerate associated slots."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Availability updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Availability not found"),
        @ApiResponse(responseCode = "409", description = "Schedule conflict detected")
    })
    public ResponseEntity<?> updateAvailability(
            @Parameter(description = "Availability ID") @PathVariable UUID id,
            @Valid @RequestBody AvailabilityRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        try {
            UUID providerId = getCurrentProviderId(authentication);
            if (providerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentication required", "UNAUTHORIZED"));
            }

            String clientIp = getClientIpAddress(httpRequest);
            
            log.debug("Updating availability {} for provider: {} - {}", id, providerId, request.getTitle());

            AvailabilityResponse response = availabilityService.updateAvailability(providerId, id, request, clientIp);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                HttpStatus status = getHttpStatusForError(response.getErrorCode());
                return ResponseEntity.status(status).body(response);
            }

        } catch (Exception e) {
            log.error("Error updating availability {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to update availability", "INTERNAL_ERROR"));
        }
    }

    /**
     * Delete provider availability
     * DELETE /api/v1/provider/availability/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete Provider Availability",
        description = "Soft delete an availability schedule. Cannot delete if there are active bookings."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Availability deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Access denied or has active bookings"),
        @ApiResponse(responseCode = "404", description = "Availability not found")
    })
    public ResponseEntity<?> deleteAvailability(
            @Parameter(description = "Availability ID") @PathVariable UUID id,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        try {
            UUID providerId = getCurrentProviderId(authentication);
            if (providerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentication required", "UNAUTHORIZED"));
            }

            String clientIp = getClientIpAddress(httpRequest);
            
            log.debug("Deleting availability {} for provider: {}", id, providerId);

            boolean success = availabilityService.deleteAvailability(providerId, id, clientIp);
            
            if (success) {
                return ResponseEntity.ok(createSuccessResponse("Availability deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Cannot delete availability - may have active bookings", "DELETE_FORBIDDEN"));
            }

        } catch (Exception e) {
            log.error("Error deleting availability {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete availability", "INTERNAL_ERROR"));
        }
    }

    /**
     * Search provider availabilities
     * GET /api/v1/provider/availability/search
     */
    @GetMapping("/search")
    @Operation(
        summary = "Search Provider Availabilities",
        description = "Search availability schedules by title or description keywords."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<?> searchAvailabilities(
            @Parameter(description = "Search term") @RequestParam String q,
            Authentication authentication) {
        
        try {
            UUID providerId = getCurrentProviderId(authentication);
            if (providerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentication required", "UNAUTHORIZED"));
            }

            List<AvailabilityResponse> availabilities = availabilityService.searchAvailabilities(providerId, q);
            return ResponseEntity.ok(createSuccessResponse(availabilities, "Search completed successfully"));

        } catch (Exception e) {
            log.error("Error searching availabilities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Search failed", "INTERNAL_ERROR"));
        }
    }

    /**
     * Get provider availability statistics
     * GET /api/v1/provider/availability/stats
     */
    @GetMapping("/stats")
    @Operation(
        summary = "Get Availability Statistics",
        description = "Retrieve comprehensive statistics about provider availability including utilization rates and booking metrics."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<?> getAvailabilityStatistics(Authentication authentication) {
        try {
            UUID providerId = getCurrentProviderId(authentication);
            if (providerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Authentication required", "UNAUTHORIZED"));
            }

            Map<String, Object> statistics = availabilityService.getAvailabilityStatistics(providerId);
            return ResponseEntity.ok(createSuccessResponse(statistics, "Statistics retrieved successfully"));

        } catch (Exception e) {
            log.error("Error retrieving availability statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to retrieve statistics", "INTERNAL_ERROR"));
        }
    }

    /**
     * Health check for availability service
     * GET /api/v1/provider/availability/health
     */
    @GetMapping("/health")
    @Operation(
        summary = "Availability Service Health Check",
        description = "Check the health status of the availability management service."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service is healthy")
    })
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "provider-availability");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Provider availability service is running");
        
        return ResponseEntity.ok(response);
    }

    // Helper methods

    private UUID getCurrentProviderId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        String credentials = (String) authentication.getCredentials();
        if (credentials != null) {
            try {
                return jwtUtils.extractProviderId(credentials);
            } catch (Exception e) {
                log.debug("Failed to extract provider ID from token", e);
            }
        }
        
        return null;
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

    private HttpStatus getHttpStatusForError(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.BAD_REQUEST;
        }
        
        return switch (errorCode) {
            case "PROVIDER_NOT_FOUND", "AVAILABILITY_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "ACCESS_DENIED", "PROVIDER_INACTIVE" -> HttpStatus.FORBIDDEN;
            case "CONFLICT_ERROR" -> HttpStatus.CONFLICT;
            case "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
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

    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    private Map<String, Object> createSuccessResponse(Object data, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }
} 