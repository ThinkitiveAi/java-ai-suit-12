package com.healthfirst.repository;

import com.healthfirst.entity.ProviderAvailability;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ProviderAvailability entity
 * Provides specialized queries for availability management and scheduling
 */
@Repository
public interface ProviderAvailabilityRepository extends JpaRepository<ProviderAvailability, UUID> {

    // Basic queries

    /**
     * Find all availabilities for a specific provider
     */
    @Query("SELECT pa FROM ProviderAvailability pa WHERE pa.providerId = :providerId ORDER BY pa.startDate, pa.startTime")
    List<ProviderAvailability> findByProviderId(@Param("providerId") UUID providerId);

    /**
     * Find all active availabilities for a specific provider
     */
    @Query("SELECT pa FROM ProviderAvailability pa WHERE pa.providerId = :providerId AND pa.isActive = true ORDER BY pa.startDate, pa.startTime")
    List<ProviderAvailability> findActiveByProviderId(@Param("providerId") UUID providerId);

    /**
     * Find availabilities for a provider with pagination
     */
    @Query("SELECT pa FROM ProviderAvailability pa WHERE pa.providerId = :providerId ORDER BY pa.startDate DESC, pa.startTime")
    Page<ProviderAvailability> findByProviderIdPageable(@Param("providerId") UUID providerId, Pageable pageable);

    // Date range queries

    /**
     * Find availabilities for a provider within a date range
     */
    @Query("""
        SELECT pa FROM ProviderAvailability pa 
        WHERE pa.providerId = :providerId 
        AND pa.isActive = true
        AND (
            (pa.startDate <= :endDate AND (pa.endDate IS NULL OR pa.endDate >= :startDate))
            OR (pa.recurrenceType = 'ONE_TIME' AND pa.startDate BETWEEN :startDate AND :endDate)
        )
        ORDER BY pa.startDate, pa.startTime
        """)
    List<ProviderAvailability> findByProviderIdAndDateRange(
            @Param("providerId") UUID providerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find availabilities for multiple providers within a date range
     */
    @Query("""
        SELECT pa FROM ProviderAvailability pa 
        WHERE pa.providerId IN :providerIds 
        AND pa.isActive = true
        AND (
            (pa.startDate <= :endDate AND (pa.endDate IS NULL OR pa.endDate >= :startDate))
            OR (pa.recurrenceType = 'ONE_TIME' AND pa.startDate BETWEEN :startDate AND :endDate)
        )
        ORDER BY pa.providerId, pa.startDate, pa.startTime
        """)
    List<ProviderAvailability> findByProviderIdsAndDateRange(
            @Param("providerIds") List<UUID> providerIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Recurrence and day-specific queries

    /**
     * Find weekly recurring availabilities for a specific day of week
     */
    @Query("""
        SELECT pa FROM ProviderAvailability pa 
        WHERE pa.providerId = :providerId 
        AND pa.recurrenceType = 'WEEKLY' 
        AND pa.dayOfWeek = :dayOfWeek 
        AND pa.isActive = true
        AND (pa.endDate IS NULL OR pa.endDate >= :currentDate)
        ORDER BY pa.startTime
        """)
    List<ProviderAvailability> findWeeklyRecurringByProviderAndDayOfWeek(
            @Param("providerId") UUID providerId,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("currentDate") LocalDate currentDate);

    /**
     * Find daily recurring availabilities for a provider
     */
    @Query("""
        SELECT pa FROM ProviderAvailability pa 
        WHERE pa.providerId = :providerId 
        AND pa.recurrenceType = 'DAILY' 
        AND pa.isActive = true
        AND pa.startDate <= :currentDate
        AND (pa.endDate IS NULL OR pa.endDate >= :currentDate)
        ORDER BY pa.startTime
        """)
    List<ProviderAvailability> findDailyRecurringByProvider(
            @Param("providerId") UUID providerId,
            @Param("currentDate") LocalDate currentDate);

    // Time and booking queries

    /**
     * Find availabilities with online booking enabled
     */
    @Query("""
        SELECT pa FROM ProviderAvailability pa 
        WHERE pa.providerId = :providerId 
        AND pa.isActive = true 
        AND pa.allowOnlineBooking = true
        AND (pa.endDate IS NULL OR pa.endDate >= :currentDate)
        ORDER BY pa.startDate, pa.startTime
        """)
    List<ProviderAvailability> findBookableByProvider(
            @Param("providerId") UUID providerId,
            @Param("currentDate") LocalDate currentDate);

    /**
     * Find availabilities by appointment type
     */
    @Query("""
        SELECT pa FROM ProviderAvailability pa 
        WHERE pa.providerId = :providerId 
        AND pa.appointmentType = :appointmentType 
        AND pa.isActive = true
        ORDER BY pa.startDate, pa.startTime
        """)
    List<ProviderAvailability> findByProviderIdAndAppointmentType(
            @Param("providerId") UUID providerId,
            @Param("appointmentType") ProviderAvailability.AppointmentType appointmentType);

    /**
     * Find availabilities by location type
     */
    @Query("""
        SELECT pa FROM ProviderAvailability pa 
        WHERE pa.providerId = :providerId 
        AND pa.locationType = :locationType 
        AND pa.isActive = true
        ORDER BY pa.startDate, pa.startTime
        """)
    List<ProviderAvailability> findByProviderIdAndLocationType(
            @Param("providerId") UUID providerId,
            @Param("locationType") ProviderAvailability.LocationType locationType);

    // Conflict detection queries

    /**
     * Find conflicting availabilities for a provider within a time range
     */
    @Query("""
        SELECT pa FROM ProviderAvailability pa 
        WHERE pa.providerId = :providerId 
        AND pa.isActive = true
        AND pa.id != :excludeId
        AND (
            (pa.startDate <= :endDate AND (pa.endDate IS NULL OR pa.endDate >= :startDate))
            OR (pa.recurrenceType = 'ONE_TIME' AND pa.startDate BETWEEN :startDate AND :endDate)
        )
        AND NOT (pa.endTime <= :startTime OR pa.startTime >= :endTime)
        """)
    List<ProviderAvailability> findConflictingAvailabilities(
            @Param("providerId") UUID providerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") UUID excludeId);

    /**
     * Check if provider has availability conflict for a specific date and time
     */
    @Query("""
        SELECT COUNT(pa) > 0 FROM ProviderAvailability pa 
        WHERE pa.providerId = :providerId 
        AND pa.isActive = true
        AND pa.id != :excludeId
        AND (
            (pa.recurrenceType = 'ONE_TIME' AND pa.startDate = :date)
            OR (pa.recurrenceType = 'WEEKLY' AND pa.dayOfWeek = :dayOfWeek 
                AND pa.startDate <= :date AND (pa.endDate IS NULL OR pa.endDate >= :date))
            OR (pa.recurrenceType = 'DAILY' 
                AND pa.startDate <= :date AND (pa.endDate IS NULL OR pa.endDate >= :date))
        )
        AND NOT (pa.endTime <= :startTime OR pa.startTime >= :endTime)
        """)
    boolean hasConflictOnDate(
            @Param("providerId") UUID providerId,
            @Param("date") LocalDate date,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") UUID excludeId);

    // Update operations

    /**
     * Deactivate availability
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProviderAvailability pa SET pa.isActive = false WHERE pa.id = :id")
    int deactivateAvailability(@Param("id") UUID id);

    /**
     * Activate availability
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProviderAvailability pa SET pa.isActive = true WHERE pa.id = :id")
    int activateAvailability(@Param("id") UUID id);

    /**
     * Disable online booking for availability
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProviderAvailability pa SET pa.allowOnlineBooking = false WHERE pa.id = :id")
    int disableOnlineBooking(@Param("id") UUID id);

    /**
     * Enable online booking for availability
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProviderAvailability pa SET pa.allowOnlineBooking = true WHERE pa.id = :id")
    int enableOnlineBooking(@Param("id") UUID id);

    /**
     * Update end date for availability
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProviderAvailability pa SET pa.endDate = :endDate WHERE pa.id = :id")
    int updateEndDate(@Param("id") UUID id, @Param("endDate") LocalDate endDate);

    // Statistics and analytics queries

    /**
     * Count active availabilities for a provider
     */
    @Query("SELECT COUNT(pa) FROM ProviderAvailability pa WHERE pa.providerId = :providerId AND pa.isActive = true")
    long countActiveByProviderId(@Param("providerId") UUID providerId);

    /**
     * Count total availabilities for a provider
     */
    @Query("SELECT COUNT(pa) FROM ProviderAvailability pa WHERE pa.providerId = :providerId")
    long countByProviderId(@Param("providerId") UUID providerId);

         /**
      * Get provider availability statistics
      */
     @Query("""
         SELECT COUNT(pa),
                SUM(CASE WHEN pa.isActive = true THEN 1 ELSE 0 END),
                SUM(CASE WHEN pa.allowOnlineBooking = true THEN 1 ELSE 0 END),
                AVG(pa.slotDurationMinutes)
         FROM ProviderAvailability pa 
         WHERE pa.providerId = :providerId
         """)
     Object[] getProviderAvailabilityStatsRaw(@Param("providerId") UUID providerId);

    /**
     * Find availabilities expiring soon (end date within specified days)
     */
    @Query("""
        SELECT pa FROM ProviderAvailability pa 
        WHERE pa.providerId = :providerId 
        AND pa.endDate IS NOT NULL 
        AND pa.endDate BETWEEN :currentDate AND :futureDate
        AND pa.isActive = true
        ORDER BY pa.endDate
        """)
    List<ProviderAvailability> findExpiringSoon(
            @Param("providerId") UUID providerId,
            @Param("currentDate") LocalDate currentDate,
            @Param("futureDate") LocalDate futureDate);

    /**
     * Find unused availabilities (no slots booked)
     */
    @Query("""
        SELECT pa FROM ProviderAvailability pa 
        WHERE pa.providerId = :providerId 
        AND pa.isActive = true
        AND NOT EXISTS (
            SELECT 1 FROM AvailabilitySlot s 
            WHERE s.availability.id = pa.id 
            AND s.status != 'AVAILABLE'
        )
        ORDER BY pa.startDate, pa.startTime
        """)
    List<ProviderAvailability> findUnusedAvailabilities(@Param("providerId") UUID providerId);

    // Cleanup operations

    /**
     * Delete expired availabilities
     */
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM ProviderAvailability pa 
        WHERE pa.endDate IS NOT NULL 
        AND pa.endDate < :cutoffDate
        AND NOT EXISTS (
            SELECT 1 FROM AvailabilitySlot s 
            WHERE s.availability.id = pa.id 
            AND s.status IN ('BOOKED', 'PENDING_CONFIRMATION')
        )
        """)
    int deleteExpiredAvailabilities(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Find availabilities for cleanup (expired and no active bookings)
     */
    @Query("""
        SELECT pa FROM ProviderAvailability pa 
        WHERE pa.endDate IS NOT NULL 
        AND pa.endDate < :cutoffDate
        AND NOT EXISTS (
            SELECT 1 FROM AvailabilitySlot s 
            WHERE s.availability.id = pa.id 
            AND s.status IN ('BOOKED', 'PENDING_CONFIRMATION')
        )
        """)
    List<ProviderAvailability> findAvailabilitiesForCleanup(@Param("cutoffDate") LocalDate cutoffDate);

    // Search and filtering

    /**
     * Search availabilities by title or description
     */
    @Query("""
        SELECT pa FROM ProviderAvailability pa 
        WHERE pa.providerId = :providerId 
        AND (
            LOWER(pa.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(pa.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
        AND pa.isActive = true
        ORDER BY pa.startDate, pa.startTime
        """)
    List<ProviderAvailability> searchByTitleOrDescription(
            @Param("providerId") UUID providerId,
            @Param("searchTerm") String searchTerm);

    /**
     * Find availabilities with specific characteristics
     */
    @Query("""
        SELECT pa FROM ProviderAvailability pa 
        WHERE pa.providerId = :providerId 
        AND pa.isActive = true
        AND (:appointmentType IS NULL OR pa.appointmentType = :appointmentType)
        AND (:locationType IS NULL OR pa.locationType = :locationType)
        AND (:allowOnlineBooking IS NULL OR pa.allowOnlineBooking = :allowOnlineBooking)
        AND (:requiresApproval IS NULL OR pa.requiresApproval = :requiresApproval)
        ORDER BY pa.startDate, pa.startTime
        """)
    List<ProviderAvailability> findWithFilters(
            @Param("providerId") UUID providerId,
            @Param("appointmentType") ProviderAvailability.AppointmentType appointmentType,
            @Param("locationType") ProviderAvailability.LocationType locationType,
            @Param("allowOnlineBooking") Boolean allowOnlineBooking,
            @Param("requiresApproval") Boolean requiresApproval);

    // Note: Statistics are returned as Object[] from getProviderAvailabilityStatsRaw()
    // Array contents: [totalCount, activeCount, bookableCount, avgSlotDuration]
} 