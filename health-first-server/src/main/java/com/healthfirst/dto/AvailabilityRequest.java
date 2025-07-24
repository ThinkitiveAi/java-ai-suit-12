package com.healthfirst.dto;

import com.healthfirst.entity.ProviderAvailability;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO for creating and updating provider availability
 * Includes comprehensive validation for scheduling rules and business constraints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_.,()]+$", message = "Title contains invalid characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Recurrence type is required")
    private ProviderAvailability.RecurrenceType recurrenceType;

    @Min(value = 1, message = "Day of week must be between 1 (Monday) and 7 (Sunday)")
    @Max(value = 7, message = "Day of week must be between 1 (Monday) and 7 (Sunday)")
    private Integer dayOfWeek; // Required for weekly recurrence

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDate startDate;

    @Future(message = "End date must be in the future")
    private LocalDate endDate; // Optional for open-ended availability

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Slot duration is required")
    @Min(value = 5, message = "Slot duration must be at least 5 minutes")
    @Max(value = 480, message = "Slot duration cannot exceed 8 hours")
    private Integer slotDurationMinutes;

    @NotNull(message = "Buffer time is required")
    @Min(value = 0, message = "Buffer time cannot be negative")
    @Max(value = 120, message = "Buffer time cannot exceed 2 hours")
    private Integer bufferTimeMinutes;

    @NotBlank(message = "Time zone is required")
    @Pattern(regexp = "^[A-Za-z]+/[A-Za-z_]+$", message = "Invalid timezone format")
    private String timeZone;

    @NotNull(message = "Location type is required")
    private ProviderAvailability.LocationType locationType;

    @Size(max = 200, message = "Location details cannot exceed 200 characters")
    private String locationDetails;

    @NotNull(message = "Appointment type is required")
    private ProviderAvailability.AppointmentType appointmentType;

    @Min(value = 1, message = "Maximum advance booking days must be at least 1")
    @Max(value = 365, message = "Maximum advance booking days cannot exceed 365")
    private Integer maxAdvanceBookingDays;

    @Min(value = 0, message = "Minimum advance booking hours cannot be negative")
    @Max(value = 168, message = "Minimum advance booking hours cannot exceed 168 (1 week)")
    private Integer minAdvanceBookingHours;

    @NotNull(message = "Allow online booking flag is required")
    private Boolean allowOnlineBooking;

    @NotNull(message = "Requires approval flag is required")
    private Boolean requiresApproval;

    private List<LocalDate> excludedDates; // Optional dates to exclude from recurrence

    // Validation methods

    /**
     * Validate that end time is after start time
     */
    @AssertTrue(message = "End time must be after start time")
    public boolean isValidTimeRange() {
        if (startTime == null || endTime == null) {
            return true; // Let other validations handle null values
        }
        return endTime.isAfter(startTime);
    }

    /**
     * Validate that end date is after start date
     */
    @AssertTrue(message = "End date must be after start date")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true; // End date is optional
        }
        return endDate.isAfter(startDate);
    }

    /**
     * Validate that day of week is provided for weekly recurrence
     */
    @AssertTrue(message = "Day of week is required for weekly recurrence")
    public boolean isValidWeeklyRecurrence() {
        if (recurrenceType == ProviderAvailability.RecurrenceType.WEEKLY) {
            return dayOfWeek != null;
        }
        return true;
    }

    /**
     * Validate that the time slot allows for at least one appointment
     */
    @AssertTrue(message = "Time range must allow for at least one appointment slot")
    public boolean hasValidSlotCapacity() {
        if (startTime == null || endTime == null || slotDurationMinutes == null || bufferTimeMinutes == null) {
            return true;
        }
        
        long totalMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        int totalSlotDuration = slotDurationMinutes + bufferTimeMinutes;
        
        return totalMinutes >= totalSlotDuration;
    }

    /**
     * Validate location details are provided for in-person appointments
     */
    @AssertTrue(message = "Location details are required for in-person appointments")
    public boolean hasValidLocationDetails() {
        if (locationType == ProviderAvailability.LocationType.IN_PERSON) {
            return locationDetails != null && !locationDetails.trim().isEmpty();
        }
        return true;
    }

    /**
     * Validate excluded dates are within the availability date range
     */
    @AssertTrue(message = "Excluded dates must be within the availability date range")
    public boolean hasValidExcludedDates() {
        if (excludedDates == null || excludedDates.isEmpty() || startDate == null) {
            return true;
        }

        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.of(2099, 12, 31);
        
        return excludedDates.stream()
                .allMatch(date -> !date.isBefore(startDate) && !date.isAfter(effectiveEndDate));
    }

    /**
     * Validate that minimum advance booking is reasonable
     */
    @AssertTrue(message = "Minimum advance booking hours should be reasonable for the appointment type")
    public boolean hasReasonableMinAdvanceBooking() {
        if (minAdvanceBookingHours == null || appointmentType == null) {
            return true;
        }

        // Different appointment types may have different minimum advance requirements
        return switch (appointmentType) {
            case EMERGENCY -> minAdvanceBookingHours <= 4; // Emergency appointments should be soon
            case CONSULTATION, FOLLOW_UP -> minAdvanceBookingHours <= 48; // Regular appointments
            case PROCEDURE -> minAdvanceBookingHours <= 168; // Procedures may need more planning
            case THERAPY -> minAdvanceBookingHours <= 72; // Therapy sessions
            case ROUTINE_CHECKUP -> minAdvanceBookingHours <= 168; // Routine checkups
            default -> true;
        };
    }

    // Utility methods

    /**
     * Get the total duration of each appointment slot including buffer time
     */
    public int getTotalSlotDuration() {
        return (slotDurationMinutes != null ? slotDurationMinutes : 0) + 
               (bufferTimeMinutes != null ? bufferTimeMinutes : 0);
    }

    /**
     * Calculate the maximum number of slots for this availability
     */
    public int getMaxSlotsCount() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        
        long totalMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        int totalSlotDuration = getTotalSlotDuration();
        
        return totalSlotDuration > 0 ? (int) (totalMinutes / totalSlotDuration) : 0;
    }

    /**
     * Check if this availability is for a single occurrence
     */
    public boolean isOneTime() {
        return recurrenceType == ProviderAvailability.RecurrenceType.ONE_TIME;
    }

    /**
     * Check if this availability is recurring
     */
    public boolean isRecurring() {
        return recurrenceType != ProviderAvailability.RecurrenceType.ONE_TIME;
    }

    /**
     * Get formatted time range string
     */
    public String getTimeRangeString() {
        if (startTime == null || endTime == null) {
            return "";
        }
        return String.format("%s - %s", startTime, endTime);
    }

    /**
     * Get formatted duration string
     */
    public String getDurationString() {
        if (slotDurationMinutes == null) {
            return "";
        }
        return String.format("%d minutes", slotDurationMinutes);
    }

    /**
     * Get user-friendly recurrence description
     */
    public String getRecurrenceDescription() {
        if (recurrenceType == null) {
            return "";
        }

        return switch (recurrenceType) {
            case ONE_TIME -> "One-time availability on " + (startDate != null ? startDate : "specified date");
            case WEEKLY -> "Weekly on " + getDayOfWeekName();
            case DAILY -> "Daily from " + (startDate != null ? startDate : "start date") + 
                         (endDate != null ? " to " + endDate : " onwards");
            case CUSTOM -> "Custom recurrence pattern";
        };
    }

    /**
     * Get day of week name for weekly recurrence
     */
    private String getDayOfWeekName() {
        if (dayOfWeek == null) {
            return "specified day";
        }
        
        return switch (dayOfWeek) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            case 7 -> "Sunday";
            default -> "Invalid day";
        };
    }

    /**
     * Check if this request represents a valid business hours schedule
     */
    public boolean isBusinessHours() {
        if (startTime == null || endTime == null) {
            return false;
        }
        
        // Business hours typically 6 AM to 10 PM
        LocalTime businessStart = LocalTime.of(6, 0);
        LocalTime businessEnd = LocalTime.of(22, 0);
        
        return !startTime.isBefore(businessStart) && !endTime.isAfter(businessEnd);
    }

    /**
     * Get summary of the availability for display
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (title != null) {
            summary.append(title);
        }
        
        if (appointmentType != null) {
            summary.append(" (").append(appointmentType.getDescription()).append(")");
        }
        
        summary.append(" - ").append(getTimeRangeString());
        
        if (recurrenceType != null) {
            summary.append(" - ").append(getRecurrenceDescription());
        }
        
        return summary.toString();
    }
} 