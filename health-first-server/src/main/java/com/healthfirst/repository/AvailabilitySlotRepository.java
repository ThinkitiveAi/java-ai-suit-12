package com.healthfirst.repository;

import com.healthfirst.entity.AvailabilitySlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AvailabilitySlot entity
 * Provides specialized queries for appointment slot management and booking
 */
@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, UUID> {

    // Basic queries

    /**
     * Find all slots for a specific availability
     */
    @Query("SELECT s FROM AvailabilitySlot s WHERE s.availability.id = :availabilityId ORDER BY s.slotDate, s.startTime")
    List<AvailabilitySlot> findByAvailabilityId(@Param("availabilityId") UUID availabilityId);

    /**
     * Find all slots for a specific provider
     */
    @Query("SELECT s FROM AvailabilitySlot s WHERE s.providerId = :providerId ORDER BY s.slotDate, s.startTime")
    List<AvailabilitySlot> findByProviderId(@Param("providerId") UUID providerId);

    /**
     * Find all slots for a specific patient
     */
    @Query("SELECT s FROM AvailabilitySlot s WHERE s.patientId = :patientId ORDER BY s.slotDate, s.startTime")
    List<AvailabilitySlot> findByPatientId(@Param("patientId") UUID patientId);

    // Date and time queries

    /**
     * Find slots for a provider on a specific date
     */
    @Query("SELECT s FROM AvailabilitySlot s WHERE s.providerId = :providerId AND s.slotDate = :date ORDER BY s.startTime")
    List<AvailabilitySlot> findByProviderIdAndDate(@Param("providerId") UUID providerId, @Param("date") LocalDate date);

    /**
     * Find slots for a provider within a date range
     */
    @Query("""
        SELECT s FROM AvailabilitySlot s 
        WHERE s.providerId = :providerId 
        AND s.slotDate BETWEEN :startDate AND :endDate 
        ORDER BY s.slotDate, s.startTime
        """)
    List<AvailabilitySlot> findByProviderIdAndDateRange(
            @Param("providerId") UUID providerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find available slots for a provider within a date range
     */
    @Query("""
        SELECT s FROM AvailabilitySlot s 
        WHERE s.providerId = :providerId 
        AND s.slotDate BETWEEN :startDate AND :endDate 
        AND s.status = 'AVAILABLE'
        AND s.isAvailable = true
        ORDER BY s.slotDate, s.startTime
        """)
    List<AvailabilitySlot> findAvailableSlotsByProviderAndDateRange(
            @Param("providerId") UUID providerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find slots for a patient within a date range
     */
    @Query("""
        SELECT s FROM AvailabilitySlot s 
        WHERE s.patientId = :patientId 
        AND s.slotDate BETWEEN :startDate AND :endDate 
        ORDER BY s.slotDate, s.startTime
        """)
    List<AvailabilitySlot> findByPatientIdAndDateRange(
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Status-based queries

    /**
     * Find slots by status
     */
    @Query("SELECT s FROM AvailabilitySlot s WHERE s.status = :status ORDER BY s.slotDate, s.startTime")
    List<AvailabilitySlot> findByStatus(@Param("status") AvailabilitySlot.SlotStatus status);

    /**
     * Find slots by provider and status
     */
    @Query("""
        SELECT s FROM AvailabilitySlot s 
        WHERE s.providerId = :providerId 
        AND s.status = :status 
        ORDER BY s.slotDate, s.startTime
        """)
    List<AvailabilitySlot> findByProviderIdAndStatus(
            @Param("providerId") UUID providerId,
            @Param("status") AvailabilitySlot.SlotStatus status);

    /**
     * Find booked slots for a provider
     */
    @Query("""
        SELECT s FROM AvailabilitySlot s 
        WHERE s.providerId = :providerId 
        AND s.status IN ('BOOKED', 'PENDING_CONFIRMATION')
        ORDER BY s.slotDate, s.startTime
        """)
    List<AvailabilitySlot> findBookedSlotsByProvider(@Param("providerId") UUID providerId);

    /**
     * Find upcoming booked slots for a provider
     */
    @Query("""
        SELECT s FROM AvailabilitySlot s 
        WHERE s.providerId = :providerId 
        AND s.status IN ('BOOKED', 'PENDING_CONFIRMATION')
        AND s.slotDate >= :currentDate
        ORDER BY s.slotDate, s.startTime
        """)
    List<AvailabilitySlot> findUpcomingBookedSlotsByProvider(
            @Param("providerId") UUID providerId,
            @Param("currentDate") LocalDate currentDate);

    /**
     * Find upcoming appointments for a patient
     */
    @Query("""
        SELECT s FROM AvailabilitySlot s 
        WHERE s.patientId = :patientId 
        AND s.status IN ('BOOKED', 'PENDING_CONFIRMATION')
        AND s.slotDate >= :currentDate
        ORDER BY s.slotDate, s.startTime
        """)
    List<AvailabilitySlot> findUpcomingAppointmentsByPatient(
            @Param("patientId") UUID patientId,
            @Param("currentDate") LocalDate currentDate);

    // Time-sensitive queries

    /**
     * Find slots for today
     */
    @Query("""
        SELECT s FROM AvailabilitySlot s 
        WHERE s.providerId = :providerId 
        AND s.slotDate = :today 
        ORDER BY s.startTime
        """)
    List<AvailabilitySlot> findTodaysSlotsByProvider(
            @Param("providerId") UUID providerId,
            @Param("today") LocalDate today);

    /**
     * Find upcoming slots within next hours
     */
    @Query("""
        SELECT s FROM AvailabilitySlot s 
        WHERE s.providerId = :providerId 
        AND s.status = 'BOOKED'
        AND s.slotDate = :today
        AND s.startTime BETWEEN :currentTime AND :futureTime
        ORDER BY s.startTime
        """)
    List<AvailabilitySlot> findUpcomingSlotsWithinHours(
            @Param("providerId") UUID providerId,
            @Param("today") LocalDate today,
            @Param("currentTime") LocalTime currentTime,
            @Param("futureTime") LocalTime futureTime);

    /**
     * Find slots needing reminders
     */
    @Query("""
        SELECT s FROM AvailabilitySlot s 
        WHERE s.status = 'BOOKED'
        AND s.reminderSent = false
        AND s.slotDate = :tomorrowDate
        ORDER BY s.providerId, s.startTime
        """)
    List<AvailabilitySlot> findSlotsNeedingReminders(@Param("tomorrowDate") LocalDate tomorrowDate);

    /**
     * Find overdue check-ins
     */
    @Query("""
        SELECT s FROM AvailabilitySlot s 
        WHERE s.status = 'BOOKED'
        AND s.checkedIn = false
        AND s.slotDate = :today
        AND s.startTime < :currentTime
        ORDER BY s.startTime
        """)
    List<AvailabilitySlot> findOverdueCheckIns(
            @Param("today") LocalDate today,
            @Param("currentTime") LocalTime currentTime);

    // Conflict and overlap queries

    /**
     * Find overlapping slots for a provider
     */
    @Query("""
        SELECT s FROM AvailabilitySlot s 
        WHERE s.providerId = :providerId 
        AND s.slotDate = :date
        AND s.id != :excludeId
        AND NOT (s.endTime <= :startTime OR s.startTime >= :endTime)
        """)
    List<AvailabilitySlot> findOverlappingSlots(
            @Param("providerId") UUID providerId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") UUID excludeId);

    /**
     * Check if slot time is available
     */
    @Query("""
        SELECT COUNT(s) = 0 FROM AvailabilitySlot s 
        WHERE s.providerId = :providerId 
        AND s.slotDate = :date
        AND s.startTime = :startTime
        AND s.status != 'CANCELLED'
        AND (:excludeId IS NULL OR s.id != :excludeId)
        """)
    boolean isSlotTimeAvailable(
            @Param("providerId") UUID providerId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("excludeId") UUID excludeId);

    // Update operations

    /**
     * Cancel slot and make it available
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE AvailabilitySlot s 
        SET s.status = 'CANCELLED', 
            s.cancellationTimestamp = :cancellationTime,
            s.cancellationReason = :reason,
            s.cancelledByProvider = :cancelledByProvider,
            s.isAvailable = true,
            s.patientId = null,
            s.patientName = null,
            s.patientEmail = null,
            s.patientPhone = null
        WHERE s.id = :slotId
        """)
    int cancelSlot(
            @Param("slotId") UUID slotId,
            @Param("cancellationTime") LocalDateTime cancellationTime,
            @Param("reason") String reason,
            @Param("cancelledByProvider") boolean cancelledByProvider);

    /**
     * Mark slot as completed
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE AvailabilitySlot s 
        SET s.status = 'COMPLETED',
            s.completed = true,
            s.completionTimestamp = :completionTime,
            s.providerNotes = :notes,
            s.durationMinutes = :duration
        WHERE s.id = :slotId
        """)
    int completeSlot(
            @Param("slotId") UUID slotId,
            @Param("completionTime") LocalDateTime completionTime,
            @Param("notes") String notes,
            @Param("duration") Integer duration);

    /**
     * Mark slot as no-show
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE AvailabilitySlot s 
        SET s.status = 'NO_SHOW',
            s.noShow = true,
            s.isAvailable = true
        WHERE s.id = :slotId
        """)
    int markNoShow(@Param("slotId") UUID slotId);

    /**
     * Check in patient
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE AvailabilitySlot s 
        SET s.checkedIn = true,
            s.checkInTimestamp = :checkInTime
        WHERE s.id = :slotId
        """)
    int checkInPatient(@Param("slotId") UUID slotId, @Param("checkInTime") LocalDateTime checkInTime);

    /**
     * Confirm appointment
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE AvailabilitySlot s 
        SET s.status = 'BOOKED',
            s.confirmationTimestamp = :confirmationTime
        WHERE s.id = :slotId AND s.status = 'PENDING_CONFIRMATION'
        """)
    int confirmAppointment(@Param("slotId") UUID slotId, @Param("confirmationTime") LocalDateTime confirmationTime);

    /**
     * Mark reminder as sent
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE AvailabilitySlot s 
        SET s.reminderSent = true,
            s.reminderSentTimestamp = :sentTime
        WHERE s.id = :slotId
        """)
    int markReminderSent(@Param("slotId") UUID slotId, @Param("sentTime") LocalDateTime sentTime);

    // Statistics and counts

    /**
     * Count slots by status for a provider
     */
    @Query("""
        SELECT s.status, COUNT(s) 
        FROM AvailabilitySlot s 
        WHERE s.providerId = :providerId 
        GROUP BY s.status
        """)
    List<Object[]> countSlotsByStatusForProvider(@Param("providerId") UUID providerId);

    /**
     * Count available slots for a provider within date range
     */
    @Query("""
        SELECT COUNT(s) FROM AvailabilitySlot s 
        WHERE s.providerId = :providerId 
        AND s.slotDate BETWEEN :startDate AND :endDate
        AND s.status = 'AVAILABLE'
        AND s.isAvailable = true
        """)
    long countAvailableSlots(
            @Param("providerId") UUID providerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count booked slots for a provider within date range
     */
    @Query("""
        SELECT COUNT(s) FROM AvailabilitySlot s 
        WHERE s.providerId = :providerId 
        AND s.slotDate BETWEEN :startDate AND :endDate
        AND s.status IN ('BOOKED', 'PENDING_CONFIRMATION', 'COMPLETED')
        """)
    long countBookedSlots(
            @Param("providerId") UUID providerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

         /**
      * Get provider slot statistics
      */
     @Query("""
         SELECT COUNT(s),
                SUM(CASE WHEN s.status = 'AVAILABLE' THEN 1 ELSE 0 END),
                SUM(CASE WHEN s.status IN ('BOOKED', 'PENDING_CONFIRMATION') THEN 1 ELSE 0 END),
                SUM(CASE WHEN s.status = 'COMPLETED' THEN 1 ELSE 0 END),
                SUM(CASE WHEN s.status = 'CANCELLED' THEN 1 ELSE 0 END),
                SUM(CASE WHEN s.status = 'NO_SHOW' THEN 1 ELSE 0 END)
         FROM AvailabilitySlot s 
         WHERE s.providerId = :providerId
         AND s.slotDate BETWEEN :startDate AND :endDate
         """)
     Object[] getProviderSlotStatsRaw(
             @Param("providerId") UUID providerId,
             @Param("startDate") LocalDate startDate,
             @Param("endDate") LocalDate endDate);

    // Cleanup operations

    /**
     * Delete old cancelled slots
     */
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM AvailabilitySlot s 
        WHERE s.status = 'CANCELLED'
        AND s.cancellationTimestamp < :cutoffDate
        """)
    int deleteOldCancelledSlots(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find expired available slots
     */
    @Query("""
        SELECT s FROM AvailabilitySlot s 
        WHERE s.status = 'AVAILABLE'
        AND s.slotDate < :currentDate
        """)
    List<AvailabilitySlot> findExpiredAvailableSlots(@Param("currentDate") LocalDate currentDate);

    // Search and pagination

    /**
     * Find slots with pagination
     */
    @Query("SELECT s FROM AvailabilitySlot s WHERE s.providerId = :providerId ORDER BY s.slotDate DESC, s.startTime")
    Page<AvailabilitySlot> findByProviderIdPageable(@Param("providerId") UUID providerId, Pageable pageable);

    /**
     * Search slots by patient name or email
     */
    @Query("""
        SELECT s FROM AvailabilitySlot s 
        WHERE s.providerId = :providerId 
        AND (
            LOWER(s.patientName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(s.patientEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
        ORDER BY s.slotDate, s.startTime
        """)
    List<AvailabilitySlot> searchByPatientInfo(
            @Param("providerId") UUID providerId,
            @Param("searchTerm") String searchTerm);

    // Note: Statistics are returned as Object[] from getProviderSlotStatsRaw()
    // Array contents: [totalSlots, availableSlots, bookedSlots, completedSlots, cancelledSlots, noShowSlots]
} 