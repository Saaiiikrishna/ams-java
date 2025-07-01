package com.example.attendancesystem.attendance.repository;

import com.example.attendancesystem.attendance.model.ScheduledSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledSessionRepository extends JpaRepository<ScheduledSession, Long> {
    
    // Find by organization ID (microservices architecture)
    List<ScheduledSession> findByOrganizationId(Long organizationId);

    // Methods using organizationId instead of Organization entity
    List<ScheduledSession> findAllByOrganizationIdAndActiveTrue(Long organizationId);

    // Methods using entityId for compatibility - handled via service layer with gRPC
    Page<ScheduledSession> findByOrganizationId(Long organizationId, Pageable pageable);

    // Find active scheduled sessions
    List<ScheduledSession> findByOrganizationIdAndActiveTrue(Long organizationId);

    // Find by organization and ID
    Optional<ScheduledSession> findByIdAndOrganizationId(Long id, Long organizationId);
    
    // Find sessions that should be active for a specific day and time
    @Query("SELECT ss FROM ScheduledSession ss JOIN ss.daysOfWeek dow " +
           "WHERE ss.active = true AND dow = :dayOfWeek " +
           "AND ss.startTime = :startTime")
    List<ScheduledSession> findActiveSessionsForDayAndTime(
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime);
    
    // Find sessions by name (case-insensitive search)
    @Query("SELECT ss FROM ScheduledSession ss WHERE ss.organizationId = :organizationId " +
           "AND LOWER(ss.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<ScheduledSession> findByOrganizationIdAndNameContaining(
            @Param("organizationId") Long organizationId,
            @Param("name") String name);

    // Find sessions that are active on a specific day
    @Query("SELECT ss FROM ScheduledSession ss JOIN ss.daysOfWeek dow " +
           "WHERE ss.organizationId = :organizationId AND ss.active = true " +
           "AND dow = :dayOfWeek ORDER BY ss.startTime")
    List<ScheduledSession> findByOrganizationIdAndDayOfWeek(
            @Param("organizationId") Long organizationId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek);

    // Count scheduled sessions for an organization
    long countByOrganizationId(Long organizationId);
    long countByOrganizationIdAndActiveTrue(Long organizationId);

    // Delete by organization (for cleanup)
    void deleteByOrganizationId(Long organizationId);
    
    // Find sessions that overlap with a given time range on specific days
    @Query(value = "SELECT ss.* FROM scheduled_sessions ss " +
           "JOIN scheduled_session_days ssd ON ss.id = ssd.scheduled_session_id " +
           "WHERE ss.organization_id = :organizationId AND ss.active = true " +
           "AND ssd.days_of_week IN :daysOfWeek " +
           "AND ((ss.start_time <= :startTime AND " +
           "      ss.start_time + (ss.duration_minutes * INTERVAL '1 minute') > :startTime) " +
           "OR (ss.start_time < :endTime AND " +
           "    ss.start_time + (ss.duration_minutes * INTERVAL '1 minute') >= :endTime) " +
           "OR (ss.start_time >= :startTime AND " +
           "    ss.start_time + (ss.duration_minutes * INTERVAL '1 minute') <= :endTime))",
           nativeQuery = true)
    List<ScheduledSession> findOverlappingSessions(
            @Param("organizationId") Long organizationId,
            @Param("daysOfWeek") List<DayOfWeek> daysOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);
}
