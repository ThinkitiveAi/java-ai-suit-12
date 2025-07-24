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
import java.util.UUID;

/**
 * Entity representing individual availability time slots for providers
 * Tracks booking status, patient assignments, and slot-specific details
 */
@Entity
@Table(name = "availability_slots", indexes = {
    @Index(name = "idx_availability_slots_availability_id", columnList = "availability_id"),
    @Index(name = "idx_availability_slots_provider_id", columnList = "provider_id"),
    @Index(name = "idx_availability_slots_date_time", columnList = "slot_date, start_time"),
    @Index(name = "idx_availability_slots_status", columnList = "status"),
    @Index(name = "idx_availability_slots_patient_id", columnList = "patient_id"),
    @Index(name = "idx_availability_slots_is_available", columnList = "is_available")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "availability_id", nullable = false)
    private ProviderAvailability availability;

    @NotNull(message = "Provider ID is required")
    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @NotNull(message = "Slot date is required")
    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SlotStatus status = SlotStatus.AVAILABLE;

    @Column(name = "patient_id")
    private UUID patientId; // null if not booked

    @Size(max = 100, message = "Patient name cannot exceed 100 characters")
    @Column(name = "patient_name", length = 100)
    private String patientName; // Cached for quick access

    @Size(max = 50, message = "Patient phone cannot exceed 50 characters")
    @Column(name = "patient_phone", length = 50)
    private String patientPhone; // For appointment reminders

    @Size(max = 100, message = "Patient email cannot exceed 100 characters")
    @Column(name = "patient_email", length = 100)
    private String patientEmail; // For appointment confirmations

    @Size(max = 500, message = "Appointment notes cannot exceed 500 characters")
    @Column(name = "appointment_notes", length = 500)
    private String appointmentNotes; // Notes about the appointment

    @Size(max = 200, message = "Reason for visit cannot exceed 200 characters")
    @Column(name = "reason_for_visit", length = 200)
    private String reasonForVisit; // Brief description of purpose

    @Column(name = "booking_timestamp")
    private LocalDateTime bookingTimestamp; // When the slot was booked

    @Column(name = "cancellation_timestamp")
    private LocalDateTime cancellationTimestamp; // When the slot was cancelled

    @Size(max = 300, message = "Cancellation reason cannot exceed 300 characters")
    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    @Column(name = "cancelled_by_provider", nullable = false)
    private Boolean cancelledByProvider = false;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "requires_confirmation", nullable = false)
    private Boolean requiresConfirmation = false;

    @Column(name = "confirmation_timestamp")
    private LocalDateTime confirmationTimestamp;

    @Column(name = "reminder_sent", nullable = false)
    private Boolean reminderSent = false;

    @Column(name = "reminder_sent_timestamp")
    private LocalDateTime reminderSentTimestamp;

    @Column(name = "no_show", nullable = false)
    private Boolean noShow = false;

    @Column(name = "checked_in", nullable = false)
    private Boolean checkedIn = false;

    @Column(name = "check_in_timestamp")
    private LocalDateTime checkInTimestamp;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "completion_timestamp")
    private LocalDateTime completionTimestamp;

    @Size(max = 1000, message = "Provider notes cannot exceed 1000 characters")
    @Column(name = "provider_notes", length = 1000)
    private String providerNotes; // Post-appointment notes

    @Column(name = "duration_minutes")
    private Integer durationMinutes; // Actual appointment duration

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums

    public enum SlotStatus {
        AVAILABLE("Available for booking"),
        BOOKED("Booked by patient"),
        BLOCKED("Blocked by provider"),
        CANCELLED("Cancelled"),
        COMPLETED("Appointment completed"),
        NO_SHOW("Patient did not show"),
        PENDING_CONFIRMATION("Awaiting confirmation");

        private final String description;

        SlotStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public boolean isBookable() {
            return this == AVAILABLE;
        }

        public boolean isBooked() {
            return this == BOOKED || this == PENDING_CONFIRMATION;
        }

        public boolean isCancellable() {
            return this == BOOKED || this == PENDING_CONFIRMATION;
        }
    }

    // Utility methods

    /**
     * Get the full date and time of this slot
     */
    public LocalDateTime getSlotDateTime() {
        return slotDate.atTime(startTime);
    }

    /**
     * Get the end date and time of this slot
     */
    public LocalDateTime getSlotEndDateTime() {
        return slotDate.atTime(endTime);
    }

    /**
     * Check if this slot is in the past
     */
    public boolean isPast() {
        return getSlotDateTime().isBefore(LocalDateTime.now());
    }

    /**
     * Check if this slot is today
     */
    public boolean isToday() {
        return slotDate.equals(LocalDate.now());
    }

    /**
     * Check if this slot is within the next 24 hours
     */
    public boolean isWithin24Hours() {
        return getSlotDateTime().isBefore(LocalDateTime.now().plusHours(24));
    }

    /**
     * Get the duration of this slot in minutes
     */
    public long getDurationMinutes() {
        return Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Book this slot for a patient
     */
    public void bookForPatient(UUID patientId, String patientName, String patientEmail, 
                              String patientPhone, String reasonForVisit) {
        if (!status.isBookable()) {
            throw new IllegalStateException("Slot is not available for booking");
        }

        this.patientId = patientId;
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.patientPhone = patientPhone;
        this.reasonForVisit = reasonForVisit;
        this.status = requiresConfirmation ? SlotStatus.PENDING_CONFIRMATION : SlotStatus.BOOKED;
        this.bookingTimestamp = LocalDateTime.now();
        this.isAvailable = false;
    }

    /**
     * Cancel this slot
     */
    public void cancel(String reason, boolean cancelledByProvider) {
        if (!status.isCancellable()) {
            throw new IllegalStateException("Slot cannot be cancelled in current status");
        }

        this.status = SlotStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancellationTimestamp = LocalDateTime.now();
        this.cancelledByProvider = cancelledByProvider;
        this.isAvailable = true;

        // Clear patient information
        this.patientId = null;
        this.patientName = null;
        this.patientEmail = null;
        this.patientPhone = null;
        this.reasonForVisit = null;
        this.appointmentNotes = null;
    }

    /**
     * Mark slot as completed
     */
    public void complete(String providerNotes, Integer actualDurationMinutes) {
        if (status != SlotStatus.BOOKED) {
            throw new IllegalStateException("Only booked slots can be completed");
        }

        this.status = SlotStatus.COMPLETED;
        this.completed = true;
        this.completionTimestamp = LocalDateTime.now();
        this.providerNotes = providerNotes;
        this.durationMinutes = actualDurationMinutes;
    }

    /**
     * Mark patient as no-show
     */
    public void markNoShow() {
        if (status != SlotStatus.BOOKED) {
            throw new IllegalStateException("Only booked slots can be marked as no-show");
        }

        this.status = SlotStatus.NO_SHOW;
        this.noShow = true;
        this.isAvailable = true; // Make slot available again
    }

    /**
     * Check in patient
     */
    public void checkIn() {
        if (status != SlotStatus.BOOKED) {
            throw new IllegalStateException("Only booked slots can be checked in");
        }

        this.checkedIn = true;
        this.checkInTimestamp = LocalDateTime.now();
    }

    /**
     * Confirm appointment (for slots requiring confirmation)
     */
    public void confirmAppointment() {
        if (status != SlotStatus.PENDING_CONFIRMATION) {
            throw new IllegalStateException("Only pending slots can be confirmed");
        }

        this.status = SlotStatus.BOOKED;
        this.confirmationTimestamp = LocalDateTime.now();
    }

    /**
     * Send reminder
     */
    public void markReminderSent() {
        this.reminderSent = true;
        this.reminderSentTimestamp = LocalDateTime.now();
    }

    /**
     * Get formatted time range
     */
    public String getTimeRange() {
        return String.format("%s - %s", startTime.toString(), endTime.toString());
    }

    /**
     * Get patient display name (with privacy protection)
     */
    public String getPatientDisplayName() {
        if (patientName == null || patientName.trim().isEmpty()) {
            return "Patient";
        }
        
        // Return masked name for privacy (show first name + last initial)
        String[] parts = patientName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        } else {
            return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
        }
    }

    /**
     * Check if slot needs reminder
     */
    public boolean needsReminder() {
        if (status != SlotStatus.BOOKED || reminderSent) {
            return false;
        }

        // Send reminder 24 hours before appointment
        LocalDateTime reminderTime = getSlotDateTime().minusHours(24);
        return LocalDateTime.now().isAfter(reminderTime);
    }

    /**
     * Get slot summary for display
     */
    public String getSummary() {
        return String.format("%s %s - %s (%s)", 
                slotDate.toString(), 
                getTimeRange(), 
                status.getDescription(),
                patientId != null ? getPatientDisplayName() : "Available");
    }
} 