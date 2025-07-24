package com.healthfirst.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.*;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing provider availability schedules
 * Supports recurring and one-time availability patterns with detailed time slot management
 */
@Entity
@Table(name = "provider_availability", indexes = {
    @Index(name = "idx_provider_availability_provider_id", columnList = "provider_id"),
    @Index(name = "idx_provider_availability_date_range", columnList = "start_date, end_date"),
    @Index(name = "idx_provider_availability_day_of_week", columnList = "day_of_week"),
    @Index(name = "idx_provider_availability_is_active", columnList = "is_active"),
    @Index(name = "idx_provider_availability_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @NotNull(message = "Provider ID is required")
    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_.,()]+$", message = "Title contains invalid characters")
    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    @NotNull(message = "Recurrence type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 20)
    private RecurrenceType recurrenceType;

    @Column(name = "day_of_week")
    private Integer dayOfWeek; // 1=Monday, 2=Tuesday, ..., 7=Sunday (for weekly recurrence)

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate; // null for open-ended availability

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Min(value = 5, message = "Slot duration must be at least 5 minutes")
    @Max(value = 480, message = "Slot duration cannot exceed 8 hours")
    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMinutes = 30; // Default 30-minute slots

    @Min(value = 0, message = "Buffer time cannot be negative")
    @Max(value = 120, message = "Buffer time cannot exceed 2 hours")
    @Column(name = "buffer_time_minutes", nullable = false)
    private Integer bufferTimeMinutes = 0; // Time between appointments

    @NotNull(message = "Time zone is required")
    @Column(name = "time_zone", nullable = false, length = 50)
    private String timeZone; // e.g., "America/New_York"

    @NotNull(message = "Location type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 20)
    private LocationType locationType;

    @Size(max = 200, message = "Location details cannot exceed 200 characters")
    @Column(name = "location_details", length = 200)
    private String locationDetails; // Room number, address, or virtual meeting link

    @NotNull(message = "Appointment type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_type", nullable = false, length = 20)
    private AppointmentType appointmentType;

    @Column(name = "max_advance_booking_days")
    private Integer maxAdvanceBookingDays = 90; // How far in advance patients can book

    @Column(name = "min_advance_booking_hours")
    private Integer minAdvanceBookingHours = 2; // Minimum notice required

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "allow_online_booking", nullable = false)
    private Boolean allowOnlineBooking = true;

    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = false; // Whether bookings need provider approval

    @ElementCollection
    @CollectionTable(name = "provider_availability_excluded_dates",
            joinColumns = @JoinColumn(name = "availability_id"))
    @Column(name = "excluded_date")
    private List<LocalDate> excludedDates; // Specific dates to exclude from recurrence

    @OneToMany(mappedBy = "availability", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AvailabilitySlot> slots;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums

    public enum RecurrenceType {
        ONE_TIME("One-time availability"),
        WEEKLY("Weekly recurring"),
        DAILY("Daily recurring"),
        CUSTOM("Custom pattern");

        private final String description;

        RecurrenceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum LocationType {
        IN_PERSON("In-person visit"),
        VIRTUAL("Virtual appointment"),
        HYBRID("Either in-person or virtual");

        private final String description;

        LocationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum AppointmentType {
        CONSULTATION("General consultation"),
        FOLLOW_UP("Follow-up appointment"),
        PROCEDURE("Medical procedure"),
        THERAPY("Therapy session"),
        EMERGENCY("Emergency appointment"),
        ROUTINE_CHECKUP("Routine checkup");

        private final String description;

        AppointmentType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Utility methods

    /**
     * Check if this availability is valid for the given date
     */
    public boolean isValidForDate(LocalDate date) {
        if (!isActive) {
            return false;
        }

        // Check date range
        if (date.isBefore(startDate)) {
            return false;
        }

        if (endDate != null && date.isAfter(endDate)) {
            return false;
        }

        // Check excluded dates
        if (excludedDates != null && excludedDates.contains(date)) {
            return false;
        }

        // Check recurrence pattern
        switch (recurrenceType) {
            case ONE_TIME:
                return date.equals(startDate);
            case WEEKLY:
                return dayOfWeek != null && date.getDayOfWeek().getValue() == dayOfWeek;
            case DAILY:
                return true;
            case CUSTOM:
                return true; // Custom logic would be implemented separately
            default:
                return false;
        }
    }

    /**
     * Get the duration of each appointment slot in minutes
     */
    public int getTotalSlotDuration() {
        return slotDurationMinutes + bufferTimeMinutes;
    }

    /**
     * Calculate the number of available slots for this availability window
     */
    public int getMaxSlotsCount() {
        Duration totalDuration = Duration.between(startTime, endTime);
        long totalMinutes = totalDuration.toMinutes();
        return (int) (totalMinutes / getTotalSlotDuration());
    }

    /**
     * Check if the availability allows advance booking for the given date
     */
    public boolean isBookingAllowed(LocalDate requestedDate) {
        if (!allowOnlineBooking) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of(timeZone));
        LocalDateTime appointmentDateTime = requestedDate.atTime(startTime)
                .atZone(ZoneId.of(timeZone))
                .toLocalDateTime();

        // Check minimum advance booking time
        if (minAdvanceBookingHours != null) {
            LocalDateTime minBookingTime = now.plusHours(minAdvanceBookingHours);
            if (appointmentDateTime.isBefore(minBookingTime)) {
                return false;
            }
        }

        // Check maximum advance booking time
        if (maxAdvanceBookingDays != null) {
            LocalDateTime maxBookingTime = now.plusDays(maxAdvanceBookingDays);
            if (appointmentDateTime.isAfter(maxBookingTime)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get formatted time range string
     */
    public String getTimeRangeString() {
        return String.format("%s - %s (%s)", 
                startTime.toString(), 
                endTime.toString(), 
                timeZone);
    }

    /**
     * Get formatted duration string
     */
    public String getDurationString() {
        return String.format("%d minutes", slotDurationMinutes);
    }

    /**
     * Check if this availability conflicts with another availability
     */
    public boolean conflictsWith(ProviderAvailability other) {
        if (!this.providerId.equals(other.providerId)) {
            return false; // Different providers can't conflict
        }

        // Check if date ranges overlap
        LocalDate thisStart = this.startDate;
        LocalDate thisEnd = this.endDate != null ? this.endDate : LocalDate.of(2099, 12, 31);
        LocalDate otherStart = other.startDate;
        LocalDate otherEnd = other.endDate != null ? other.endDate : LocalDate.of(2099, 12, 31);

        if (thisEnd.isBefore(otherStart) || otherEnd.isBefore(thisStart)) {
            return false; // No date overlap
        }

        // Check if time ranges overlap on the same days
        if (this.endTime.isBefore(other.startTime) || other.endTime.isBefore(this.startTime)) {
            return false; // No time overlap
        }

        // Additional logic for checking day-of-week conflicts for recurring appointments
        if (this.recurrenceType == RecurrenceType.WEEKLY && 
            other.recurrenceType == RecurrenceType.WEEKLY) {
            return this.dayOfWeek != null && this.dayOfWeek.equals(other.dayOfWeek);
        }

        return true; // Potential conflict detected
    }
} 