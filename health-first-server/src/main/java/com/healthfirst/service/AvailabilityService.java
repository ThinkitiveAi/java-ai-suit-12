package com.healthfirst.service;

import com.healthfirst.dto.AvailabilityRequest;
import com.healthfirst.dto.AvailabilityResponse;
import com.healthfirst.entity.AvailabilitySlot;
import com.healthfirst.entity.Provider;
import com.healthfirst.entity.ProviderAvailability;
import com.healthfirst.repository.AvailabilitySlotRepository;
import com.healthfirst.repository.ProviderAvailabilityRepository;
import com.healthfirst.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing provider availability with advanced scheduling logic
 * Handles slot generation, conflict detection, and comprehensive business rules
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AvailabilityService {

    private final ProviderAvailabilityRepository availabilityRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final ProviderRepository providerRepository;
    private final AuditService auditService;

    @Value("${app.availability.max-advance-days:90}")
    private int maxAdvanceDays;

    @Value("${app.availability.auto-generate-slots:true}")
    private boolean autoGenerateSlots;

    @Value("${app.availability.default-timezone:America/New_York}")
    private String defaultTimeZone;

    // Create Operations

    /**
     * Create new provider availability
     */
    public AvailabilityResponse createAvailability(UUID providerId, AvailabilityRequest request, String clientIp) {
        try {
            // Validate provider exists and is active
            Optional<Provider> providerOpt = providerRepository.findById(providerId);
            if (providerOpt.isEmpty()) {
                return AvailabilityResponse.error("Provider not found", "PROVIDER_NOT_FOUND");
            }

            Provider provider = providerOpt.get();
            if (!provider.getIsActive()) {
                return AvailabilityResponse.error("Provider account is inactive", "PROVIDER_INACTIVE");
            }

            // Validate no conflicts
            List<ProviderAvailability> conflicts = findConflicts(providerId, request, null);
            if (!conflicts.isEmpty()) {
                String conflictDetails = conflicts.stream()
                        .map(pa -> String.format("%s (%s)", pa.getTitle(), pa.getTimeRangeString()))
                        .collect(Collectors.joining(", "));
                return AvailabilityResponse.conflictError("Schedule conflicts detected with: " + conflictDetails);
            }

            // Create availability entity
            ProviderAvailability availability = createAvailabilityEntity(providerId, request);
            availability = availabilityRepository.save(availability);

            // Generate slots if auto-generation is enabled
            if (autoGenerateSlots) {
                generateSlotsForAvailability(availability);
            }

            // Audit log
            auditService.logProviderAction(providerId, "AVAILABILITY_CREATED", availability.getId().toString(), clientIp);

            log.info("Created availability for provider {}: {}", providerId, availability.getTitle());
            return buildAvailabilityResponse(availability, provider.getFirstName() + " " + provider.getLastName());

        } catch (Exception e) {
            log.error("Error creating availability for provider {}", providerId, e);
            return AvailabilityResponse.internalError();
        }
    }

    /**
     * Update existing availability
     */
    public AvailabilityResponse updateAvailability(UUID providerId, UUID availabilityId, AvailabilityRequest request, String clientIp) {
        try {
            Optional<ProviderAvailability> availabilityOpt = availabilityRepository.findById(availabilityId);
            if (availabilityOpt.isEmpty()) {
                return AvailabilityResponse.notFound();
            }

            ProviderAvailability availability = availabilityOpt.get();
            if (!availability.getProviderId().equals(providerId)) {
                return AvailabilityResponse.accessDenied();
            }

            // Check for conflicts (excluding current availability)
            List<ProviderAvailability> conflicts = findConflicts(providerId, request, availabilityId);
            if (!conflicts.isEmpty()) {
                String conflictDetails = conflicts.stream()
                        .map(pa -> String.format("%s (%s)", pa.getTitle(), pa.getTimeRangeString()))
                        .collect(Collectors.joining(", "));
                return AvailabilityResponse.conflictError("Schedule conflicts detected with: " + conflictDetails);
            }

            // Update availability
            updateAvailabilityEntity(availability, request);
            availability = availabilityRepository.save(availability);

            // Regenerate slots if time/date changed
            if (hasSignificantChanges(availability, request)) {
                regenerateSlotsForAvailability(availability);
            }

            // Audit log
            auditService.logProviderAction(providerId, "AVAILABILITY_UPDATED", availabilityId.toString(), clientIp);

            Provider provider = providerRepository.findById(providerId).orElse(null);
            String providerName = provider != null ? provider.getFirstName() + " " + provider.getLastName() : "Unknown";
            
            return buildAvailabilityResponse(availability, providerName);

        } catch (Exception e) {
            log.error("Error updating availability {} for provider {}", availabilityId, providerId, e);
            return AvailabilityResponse.internalError();
        }
    }

    // Read Operations

    /**
     * Get availability by ID
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(UUID providerId, UUID availabilityId) {
        try {
            Optional<ProviderAvailability> availabilityOpt = availabilityRepository.findById(availabilityId);
            if (availabilityOpt.isEmpty()) {
                return AvailabilityResponse.notFound();
            }

            ProviderAvailability availability = availabilityOpt.get();
            if (!availability.getProviderId().equals(providerId)) {
                return AvailabilityResponse.accessDenied();
            }

            Provider provider = providerRepository.findById(providerId).orElse(null);
            String providerName = provider != null ? provider.getFirstName() + " " + provider.getLastName() : "Unknown";

            return buildAvailabilityResponse(availability, providerName);

        } catch (Exception e) {
            log.error("Error retrieving availability {} for provider {}", availabilityId, providerId, e);
            return AvailabilityResponse.internalError();
        }
    }

    /**
     * Get all availabilities for a provider
     */
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getProviderAvailabilities(UUID providerId) {
        try {
            List<ProviderAvailability> availabilities = availabilityRepository.findActiveByProviderId(providerId);
            Provider provider = providerRepository.findById(providerId).orElse(null);
            String providerName = provider != null ? provider.getFirstName() + " " + provider.getLastName() : "Unknown";

            return availabilities.stream()
                    .map(availability -> buildAvailabilityResponse(availability, providerName))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error retrieving availabilities for provider {}", providerId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get availabilities with pagination
     */
    @Transactional(readOnly = true)
    public Page<AvailabilityResponse> getProviderAvailabilities(UUID providerId, Pageable pageable) {
        try {
            Page<ProviderAvailability> availabilities = availabilityRepository.findByProviderIdPageable(providerId, pageable);
            Provider provider = providerRepository.findById(providerId).orElse(null);
            String providerName = provider != null ? provider.getFirstName() + " " + provider.getLastName() : "Unknown";

            return availabilities.map(availability -> buildAvailabilityResponse(availability, providerName));

        } catch (Exception e) {
            log.error("Error retrieving paginated availabilities for provider {}", providerId, e);
            throw new RuntimeException("Failed to retrieve availabilities", e);
        }
    }

    // Delete Operations

    /**
     * Delete availability (soft delete by deactivating)
     */
    public boolean deleteAvailability(UUID providerId, UUID availabilityId, String clientIp) {
        try {
            Optional<ProviderAvailability> availabilityOpt = availabilityRepository.findById(availabilityId);
            if (availabilityOpt.isEmpty()) {
                return false;
            }

            ProviderAvailability availability = availabilityOpt.get();
            if (!availability.getProviderId().equals(providerId)) {
                return false;
            }

            // Check if there are active bookings
            long activeBookings = slotRepository.countBookedSlots(providerId, LocalDate.now(), LocalDate.now().plusDays(maxAdvanceDays));
            if (activeBookings > 0) {
                log.warn("Cannot delete availability {} - has {} active bookings", availabilityId, activeBookings);
                return false;
            }

            // Soft delete by deactivating
            availabilityRepository.deactivateAvailability(availabilityId);

            // Audit log
            auditService.logProviderAction(providerId, "AVAILABILITY_DELETED", availabilityId.toString(), clientIp);

            log.info("Deleted availability {} for provider {}", availabilityId, providerId);
            return true;

        } catch (Exception e) {
            log.error("Error deleting availability {} for provider {}", availabilityId, providerId, e);
            return false;
        }
    }

    // Slot Management

    /**
     * Generate slots for availability
     */
    private void generateSlotsForAvailability(ProviderAvailability availability) {
        try {
            LocalDate startDate = availability.getStartDate();
            LocalDate endDate = availability.getEndDate() != null ? 
                    availability.getEndDate() : LocalDate.now().plusDays(maxAdvanceDays);

            List<AvailabilitySlot> slots = new ArrayList<>();
            LocalDate currentDate = startDate;

            while (!currentDate.isAfter(endDate)) {
                if (availability.isValidForDate(currentDate)) {
                    List<AvailabilitySlot> dailySlots = generateDailySlots(availability, currentDate);
                    slots.addAll(dailySlots);
                }
                currentDate = currentDate.plusDays(1);
            }

            if (!slots.isEmpty()) {
                slotRepository.saveAll(slots);
                log.info("Generated {} slots for availability {}", slots.size(), availability.getId());
            }

        } catch (Exception e) {
            log.error("Error generating slots for availability {}", availability.getId(), e);
        }
    }

    /**
     * Generate daily slots for a specific date
     */
    private List<AvailabilitySlot> generateDailySlots(ProviderAvailability availability, LocalDate date) {
        List<AvailabilitySlot> slots = new ArrayList<>();
        
        LocalTime currentTime = availability.getStartTime();
        LocalTime endTime = availability.getEndTime();
        int slotDuration = availability.getSlotDurationMinutes();
        int bufferTime = availability.getBufferTimeMinutes();
        int totalSlotTime = slotDuration + bufferTime;

        while (currentTime.plusMinutes(slotDuration).isBefore(endTime) || 
               currentTime.plusMinutes(slotDuration).equals(endTime)) {
            
            AvailabilitySlot slot = new AvailabilitySlot();
            slot.setAvailability(availability);
            slot.setProviderId(availability.getProviderId());
            slot.setSlotDate(date);
            slot.setStartTime(currentTime);
            slot.setEndTime(currentTime.plusMinutes(slotDuration));
            slot.setStatus(AvailabilitySlot.SlotStatus.AVAILABLE);
            slot.setIsAvailable(true);
            slot.setRequiresConfirmation(availability.getRequiresApproval());

            slots.add(slot);
            currentTime = currentTime.plusMinutes(totalSlotTime);
        }

        return slots;
    }

    /**
     * Regenerate slots when availability changes significantly
     */
    private void regenerateSlotsForAvailability(ProviderAvailability availability) {
        try {
            // Delete existing available slots
            List<AvailabilitySlot> existingSlots = slotRepository.findByAvailabilityId(availability.getId());
            List<AvailabilitySlot> availableSlots = existingSlots.stream()
                    .filter(slot -> slot.getStatus() == AvailabilitySlot.SlotStatus.AVAILABLE)
                    .collect(Collectors.toList());
            
            if (!availableSlots.isEmpty()) {
                slotRepository.deleteAll(availableSlots);
            }

            // Generate new slots
            generateSlotsForAvailability(availability);

        } catch (Exception e) {
            log.error("Error regenerating slots for availability {}", availability.getId(), e);
        }
    }

    // Helper Methods

    /**
     * Find conflicting availabilities
     */
    private List<ProviderAvailability> findConflicts(UUID providerId, AvailabilityRequest request, UUID excludeId) {
        UUID excludeIdSafe = excludeId != null ? excludeId : UUID.randomUUID();
        
        return availabilityRepository.findConflictingAvailabilities(
                providerId,
                request.getStartDate(),
                request.getEndDate() != null ? request.getEndDate() : request.getStartDate().plusYears(1),
                request.getStartTime(),
                request.getEndTime(),
                excludeIdSafe
        );
    }

    /**
     * Create availability entity from request
     */
    private ProviderAvailability createAvailabilityEntity(UUID providerId, AvailabilityRequest request) {
        ProviderAvailability availability = new ProviderAvailability();
        availability.setProviderId(providerId);
        availability.setTitle(request.getTitle());
        availability.setDescription(request.getDescription());
        availability.setRecurrenceType(request.getRecurrenceType());
        availability.setDayOfWeek(request.getDayOfWeek());
        availability.setStartDate(request.getStartDate());
        availability.setEndDate(request.getEndDate());
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());
        availability.setSlotDurationMinutes(request.getSlotDurationMinutes());
        availability.setBufferTimeMinutes(request.getBufferTimeMinutes());
        availability.setTimeZone(request.getTimeZone() != null ? request.getTimeZone() : defaultTimeZone);
        availability.setLocationType(request.getLocationType());
        availability.setLocationDetails(request.getLocationDetails());
        availability.setAppointmentType(request.getAppointmentType());
        availability.setMaxAdvanceBookingDays(request.getMaxAdvanceBookingDays() != null ? 
                request.getMaxAdvanceBookingDays() : maxAdvanceDays);
        availability.setMinAdvanceBookingHours(request.getMinAdvanceBookingHours() != null ? 
                request.getMinAdvanceBookingHours() : 2);
        availability.setIsActive(true);
        availability.setAllowOnlineBooking(request.getAllowOnlineBooking());
        availability.setRequiresApproval(request.getRequiresApproval());
        availability.setExcludedDates(request.getExcludedDates());
        
        return availability;
    }

    /**
     * Update availability entity from request
     */
    private void updateAvailabilityEntity(ProviderAvailability availability, AvailabilityRequest request) {
        availability.setTitle(request.getTitle());
        availability.setDescription(request.getDescription());
        availability.setRecurrenceType(request.getRecurrenceType());
        availability.setDayOfWeek(request.getDayOfWeek());
        availability.setStartDate(request.getStartDate());
        availability.setEndDate(request.getEndDate());
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());
        availability.setSlotDurationMinutes(request.getSlotDurationMinutes());
        availability.setBufferTimeMinutes(request.getBufferTimeMinutes());
        availability.setTimeZone(request.getTimeZone() != null ? request.getTimeZone() : availability.getTimeZone());
        availability.setLocationType(request.getLocationType());
        availability.setLocationDetails(request.getLocationDetails());
        availability.setAppointmentType(request.getAppointmentType());
        availability.setMaxAdvanceBookingDays(request.getMaxAdvanceBookingDays() != null ? 
                request.getMaxAdvanceBookingDays() : availability.getMaxAdvanceBookingDays());
        availability.setMinAdvanceBookingHours(request.getMinAdvanceBookingHours() != null ? 
                request.getMinAdvanceBookingHours() : availability.getMinAdvanceBookingHours());
        availability.setAllowOnlineBooking(request.getAllowOnlineBooking());
        availability.setRequiresApproval(request.getRequiresApproval());
        availability.setExcludedDates(request.getExcludedDates());
    }

    /**
     * Check if changes require slot regeneration
     */
    private boolean hasSignificantChanges(ProviderAvailability availability, AvailabilityRequest request) {
        return !availability.getStartTime().equals(request.getStartTime()) ||
               !availability.getEndTime().equals(request.getEndTime()) ||
               !availability.getSlotDurationMinutes().equals(request.getSlotDurationMinutes()) ||
               !availability.getBufferTimeMinutes().equals(request.getBufferTimeMinutes()) ||
               !availability.getStartDate().equals(request.getStartDate()) ||
               !Objects.equals(availability.getEndDate(), request.getEndDate());
    }

    /**
     * Build comprehensive availability response
     */
    private AvailabilityResponse buildAvailabilityResponse(ProviderAvailability availability, String providerName) {
        // Get slot statistics
        Object[] statsRaw = slotRepository.getProviderSlotStatsRaw(
                availability.getProviderId(),
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );

        long totalSlots = statsRaw[0] != null ? ((Number) statsRaw[0]).longValue() : 0;
        long availableSlots = statsRaw[1] != null ? ((Number) statsRaw[1]).longValue() : 0;
        long bookedSlots = statsRaw[2] != null ? ((Number) statsRaw[2]).longValue() : 0;

        double utilizationRate = totalSlots > 0 ? (double) bookedSlots / totalSlots : 0.0;

        // Build response using builder pattern
        return AvailabilityResponse.builder()
                .id(availability.getId())
                .providerId(availability.getProviderId())
                .providerName(providerName)
                .title(availability.getTitle())
                .description(availability.getDescription())
                .build("Availability retrieved successfully");
    }

    // Search and Filter Operations

    /**
     * Search availabilities by title or description
     */
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> searchAvailabilities(UUID providerId, String searchTerm) {
        try {
            List<ProviderAvailability> availabilities = availabilityRepository.searchByTitleOrDescription(providerId, searchTerm);
            Provider provider = providerRepository.findById(providerId).orElse(null);
            String providerName = provider != null ? provider.getFirstName() + " " + provider.getLastName() : "Unknown";

            return availabilities.stream()
                    .map(availability -> buildAvailabilityResponse(availability, providerName))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error searching availabilities for provider {} with term: {}", providerId, searchTerm, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get availability statistics for provider
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAvailabilityStatistics(UUID providerId) {
        try {
            Object[] statsRaw = availabilityRepository.getProviderAvailabilityStatsRaw(providerId);
            
            long totalAvailabilities = statsRaw[0] != null ? ((Number) statsRaw[0]).longValue() : 0;
            long activeAvailabilities = statsRaw[1] != null ? ((Number) statsRaw[1]).longValue() : 0;
            long bookableAvailabilities = statsRaw[2] != null ? ((Number) statsRaw[2]).longValue() : 0;
            double avgSlotDuration = statsRaw[3] != null ? ((Number) statsRaw[3]).doubleValue() : 0.0;

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalAvailabilities", totalAvailabilities);
            statistics.put("activeAvailabilities", activeAvailabilities);
            statistics.put("bookableAvailabilities", bookableAvailabilities);
            statistics.put("averageSlotDuration", avgSlotDuration);
            statistics.put("utilizationRate", calculateUtilizationRate(providerId));

            return statistics;

        } catch (Exception e) {
            log.error("Error getting availability statistics for provider {}", providerId, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Calculate utilization rate for provider
     */
    private double calculateUtilizationRate(UUID providerId) {
        try {
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusDays(30);
            
            long totalSlots = slotRepository.countAvailableSlots(providerId, startDate, endDate) +
                             slotRepository.countBookedSlots(providerId, startDate, endDate);
            long bookedSlots = slotRepository.countBookedSlots(providerId, startDate, endDate);
            
            return totalSlots > 0 ? (double) bookedSlots / totalSlots : 0.0;
        } catch (Exception e) {
            log.error("Error calculating utilization rate for provider {}", providerId, e);
            return 0.0;
        }
    }
} 