package com.example.attendancesystem.shared.repository;

import com.example.attendancesystem.shared.model.Organization;
import com.example.attendancesystem.shared.model.ScheduledSession;
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
    
    // Find by organization
    List<ScheduledSession> findByOrganization(Organization organization);
    Page<ScheduledSession> findByOrganization(Organization organization, Pageable pageable);

    // Find by organization entity ID (for attendance-service compatibility)
    @Query("SELECT ss FROM ScheduledSession ss WHERE ss.organization.entityId = :entityId")
    List<ScheduledSession> findAllByOrganizationEntityId(@Param("entityId") String entityId);

    @Query("SELECT ss FROM ScheduledSession ss WHERE ss.organization.entityId = :entityId AND ss.active = true")
    List<ScheduledSession> findAllByOrganizationEntityIdAndActiveTrue(@Param("entityId") String entityId);
    
    // Find active scheduled sessions
    List<ScheduledSession> findByOrganizationAndActiveTrue(Organization organization);
    
    // Find by organization and ID
    Optional<ScheduledSession> findByIdAndOrganization(Long id, Organization organization);

    // Find by ID and organization entity ID (for attendance-service compatibility)
    @Query("SELECT ss FROM ScheduledSession ss WHERE ss.id = :id AND ss.organization.entityId = :entityId")
    Optional<ScheduledSession> findByIdAndOrganizationEntityId(@Param("id") Long id, @Param("entityId") String entityId);
    
    // Find sessions that should be active for a specific day and time
    @Query("SELECT ss FROM ScheduledSession ss JOIN ss.daysOfWeek dow " +
           "WHERE ss.active = true AND dow = :dayOfWeek " +
           "AND ss.startTime = :startTime")
    List<ScheduledSession> findActiveSessionsForDayAndTime(
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime);
    
    // Find sessions by name (case-insensitive search)
    @Query("SELECT ss FROM ScheduledSession ss WHERE ss.organization = :organization " +
           "AND LOWER(ss.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<ScheduledSession> findByOrganizationAndNameContaining(
            @Param("organization") Organization organization,
            @Param("name") String name);
    
    // Find sessions that are active on a specific day
    @Query("SELECT ss FROM ScheduledSession ss JOIN ss.daysOfWeek dow " +
           "WHERE ss.organization = :organization AND ss.active = true " +
           "AND dow = :dayOfWeek ORDER BY ss.startTime")
    List<ScheduledSession> findByOrganizationAndDayOfWeek(
            @Param("organization") Organization organization,
            @Param("dayOfWeek") DayOfWeek dayOfWeek);
    
    // Count scheduled sessions for an organization
    long countByOrganization(Organization organization);
    long countByOrganizationAndActiveTrue(Organization organization);
    
    // Delete by organization (for cleanup)
    void deleteByOrganization(Organization organization);
    
    // Find sessions that overlap with a given time range on specific days
    @Query(value = "SELECT ss.* FROM scheduled_sessions ss " +
           "JOIN scheduled_session_days ssd ON ss.id = ssd.scheduled_session_id " +
           "WHERE ss.organization_id = :#{#organization.id} AND ss.active = true " +
           "AND ssd.days_of_week IN :daysOfWeek " +
           "AND ((ss.start_time <= :startTime AND " +
           "      ss.start_time + (ss.duration_minutes * INTERVAL '1 minute') > :startTime) " +
           "OR (ss.start_time < :endTime AND " +
           "    ss.start_time + (ss.duration_minutes * INTERVAL '1 minute') >= :endTime) " +
           "OR (ss.start_time >= :startTime AND " +
           "    ss.start_time + (ss.duration_minutes * INTERVAL '1 minute') <= :endTime))",
           nativeQuery = true)
    List<ScheduledSession> findOverlappingSessions(
            @Param("organization") Organization organization,
            @Param("daysOfWeek") List<DayOfWeek> daysOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);
}
