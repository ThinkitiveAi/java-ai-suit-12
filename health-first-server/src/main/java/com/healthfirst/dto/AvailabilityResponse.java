package com.healthfirst.dto;

import com.healthfirst.entity.ProviderAvailability;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for provider availability responses
 * Provides comprehensive availability information with statistics and slot details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {

    private boolean success;
    private String message;
    private AvailabilityData data;
    private String errorCode;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailabilityData {
        private UUID id;
        private UUID providerId;
        private String providerName; // Cached for convenience
        private String title;
        private String description;
        private RecurrenceInfo recurrence;
        private TimeInfo timeInfo;
        private BookingInfo bookingInfo;
        private LocationInfo locationInfo;
        private SlotStatistics statistics;
        private List<ExcludedDate> excludedDates;
        private MetadataInfo metadata;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecurrenceInfo {
        private ProviderAvailability.RecurrenceType type;
        private String description;
        private Integer dayOfWeek;
        private String dayOfWeekName;
        private LocalDate startDate;
        private LocalDate endDate;
        private boolean isOneTime;
        private boolean isRecurring;
        private boolean isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeInfo {
        private LocalTime startTime;
        private LocalTime endTime;
        private String timeRange;
        private Integer slotDurationMinutes;
        private String slotDurationFormatted;
        private Integer bufferTimeMinutes;
        private Integer totalSlotDuration;
        private String timeZone;
        private Integer maxSlotsPerDay;
        private boolean isBusinessHours;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingInfo {
        private boolean allowOnlineBooking;
        private boolean requiresApproval;
        private Integer maxAdvanceBookingDays;
        private Integer minAdvanceBookingHours;
        private String bookingWindow;
        private boolean isCurrentlyBookable;
        private LocalDateTime nextAvailableSlot;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationInfo {
        private ProviderAvailability.LocationType type;
        private String typeDescription;
        private String details;
        private boolean requiresPhysicalLocation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotStatistics {
        private long totalSlots;
        private long availableSlots;
        private long bookedSlots;
        private long completedSlots;
        private long cancelledSlots;
        private double utilizationRate;
        private LocalDate lastBookedDate;
        private LocalDate nextAvailableDate;
        private List<DailySlotSummary> upcomingDays;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySlotSummary {
        private LocalDate date;
        private String dayOfWeek;
        private int totalSlots;
        private int availableSlots;
        private int bookedSlots;
        private boolean isToday;
        private boolean hasAvailability;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExcludedDate {
        private LocalDate date;
        private String reason;
        private boolean isPast;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetadataInfo {
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String status;
        private boolean canEdit;
        private boolean canDelete;
        private List<String> warnings;
        private List<String> recommendations;
    }

    // Static factory methods

    public static AvailabilityResponse success(AvailabilityData data) {
        return new AvailabilityResponse(true, "Availability retrieved successfully", data, null);
    }

    public static AvailabilityResponse success(AvailabilityData data, String message) {
        return new AvailabilityResponse(true, message, data, null);
    }

    public static AvailabilityResponse error(String message, String errorCode) {
        return new AvailabilityResponse(false, message, null, errorCode);
    }

    public static AvailabilityResponse notFound() {
        return error("Availability not found", "AVAILABILITY_NOT_FOUND");
    }

    public static AvailabilityResponse accessDenied() {
        return error("Access denied to this availability", "ACCESS_DENIED");
    }

    public static AvailabilityResponse internalError() {
        return error("Internal server error while retrieving availability", "INTERNAL_ERROR");
    }

    public static AvailabilityResponse validationError(String message) {
        return error(message, "VALIDATION_ERROR");
    }

    public static AvailabilityResponse conflictError(String message) {
        return error(message, "CONFLICT_ERROR");
    }

    // Builder class for complex availability responses
    public static class Builder {
        private final AvailabilityData data = new AvailabilityData();

        public Builder id(UUID id) {
            data.setId(id);
            return this;
        }

        public Builder providerId(UUID providerId) {
            data.setProviderId(providerId);
            return this;
        }

        public Builder providerName(String providerName) {
            data.setProviderName(providerName);
            return this;
        }

        public Builder title(String title) {
            data.setTitle(title);
            return this;
        }

        public Builder description(String description) {
            data.setDescription(description);
            return this;
        }

        public Builder recurrence(RecurrenceInfo recurrence) {
            data.setRecurrence(recurrence);
            return this;
        }

        public Builder timeInfo(TimeInfo timeInfo) {
            data.setTimeInfo(timeInfo);
            return this;
        }

        public Builder bookingInfo(BookingInfo bookingInfo) {
            data.setBookingInfo(bookingInfo);
            return this;
        }

        public Builder locationInfo(LocationInfo locationInfo) {
            data.setLocationInfo(locationInfo);
            return this;
        }

        public Builder statistics(SlotStatistics statistics) {
            data.setStatistics(statistics);
            return this;
        }

        public Builder excludedDates(List<ExcludedDate> excludedDates) {
            data.setExcludedDates(excludedDates);
            return this;
        }

        public Builder metadata(MetadataInfo metadata) {
            data.setMetadata(metadata);
            return this;
        }

        public AvailabilityResponse build() {
            return AvailabilityResponse.success(data);
        }

        public AvailabilityResponse build(String message) {
            return AvailabilityResponse.success(data, message);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Utility methods

    /**
     * Check if the availability is currently active and bookable
     */
    public boolean isCurrentlyAvailable() {
        if (!success || data == null || data.getRecurrence() == null || data.getBookingInfo() == null) {
            return false;
        }

        return data.getRecurrence().isActive() && 
               data.getBookingInfo().isAllowOnlineBooking() &&
               data.getBookingInfo().isCurrentlyBookable();
    }

    /**
     * Get the availability summary for display
     */
    public String getAvailabilitySummary() {
        if (!success || data == null) {
            return "Availability information not available";
        }

        StringBuilder summary = new StringBuilder();
        
        if (data.getTitle() != null) {
            summary.append(data.getTitle());
        }

        if (data.getTimeInfo() != null) {
            summary.append(" - ").append(data.getTimeInfo().getTimeRange());
        }

        if (data.getRecurrence() != null) {
            summary.append(" (").append(data.getRecurrence().getDescription()).append(")");
        }

        return summary.toString();
    }

    /**
     * Get next available booking opportunity
     */
    public String getNextAvailableInfo() {
        if (!success || data == null || data.getBookingInfo() == null) {
            return "No availability information";
        }

        BookingInfo booking = data.getBookingInfo();
        if (!booking.isCurrentlyBookable()) {
            return "Online booking not available";
        }

        if (booking.getNextAvailableSlot() != null) {
            return "Next available: " + booking.getNextAvailableSlot().toLocalDate();
        }

        return "Check availability calendar";
    }

    /**
     * Get utilization percentage as a formatted string
     */
    public String getUtilizationDisplay() {
        if (!success || data == null || data.getStatistics() == null) {
            return "N/A";
        }

        double rate = data.getStatistics().getUtilizationRate();
        return String.format("%.1f%%", rate * 100);
    }

    /**
     * Check if the availability needs attention (low utilization, conflicts, etc.)
     */
    public boolean needsAttention() {
        if (!success || data == null) {
            return false;
        }

        // Check for warnings or low utilization
        if (data.getMetadata() != null && data.getMetadata().getWarnings() != null && 
            !data.getMetadata().getWarnings().isEmpty()) {
            return true;
        }

        // Check for very low utilization
        if (data.getStatistics() != null && data.getStatistics().getUtilizationRate() < 0.2) {
            return true;
        }

        return false;
    }

    /**
     * Get color coding for UI display based on utilization and status
     */
    public String getStatusColor() {
        if (!success || data == null) {
            return "gray";
        }

        if (!data.getRecurrence().isActive()) {
            return "gray";
        }

        if (data.getStatistics() != null) {
            double utilization = data.getStatistics().getUtilizationRate();
            if (utilization >= 0.8) {
                return "red"; // High utilization
            } else if (utilization >= 0.5) {
                return "yellow"; // Medium utilization
            } else if (utilization >= 0.2) {
                return "green"; // Good availability
            } else {
                return "blue"; // Low utilization, may need promotion
            }
        }

        return "green";
    }
} 