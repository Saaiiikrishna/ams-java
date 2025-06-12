package com.example.attendancesystem.repository;

import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.ScheduledSession;
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
    
    // Find all scheduled sessions for an organization
    List<ScheduledSession> findAllByOrganization(Organization organization);
    
    // Find active scheduled sessions for an organization
    List<ScheduledSession> findAllByOrganizationAndActiveTrue(Organization organization);
    
    // Find by ID and organization
    Optional<ScheduledSession> findByIdAndOrganization(Long id, Organization organization);
    
    // Find scheduled sessions that should run on a specific day and time
    @Query("SELECT s FROM ScheduledSession s JOIN s.daysOfWeek d " +
           "WHERE s.active = true AND d = :dayOfWeek AND s.startTime = :startTime")
    List<ScheduledSession> findActiveSessionsForDayAndTime(
        @Param("dayOfWeek") DayOfWeek dayOfWeek, 
        @Param("startTime") LocalTime startTime
    );
    
    // Find scheduled sessions that should run on a specific day within a time range
    @Query("SELECT s FROM ScheduledSession s JOIN s.daysOfWeek d " +
           "WHERE s.active = true AND d = :dayOfWeek AND s.startTime BETWEEN :startTime AND :endTime")
    List<ScheduledSession> findActiveSessionsForDayAndTimeRange(
        @Param("dayOfWeek") DayOfWeek dayOfWeek, 
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );
    
    // Entity ID based methods
    @Query("SELECT s FROM ScheduledSession s WHERE s.organization.entityId = :entityId")
    List<ScheduledSession> findAllByOrganizationEntityId(@Param("entityId") String entityId);
    
    @Query("SELECT s FROM ScheduledSession s WHERE s.organization.entityId = :entityId AND s.active = true")
    List<ScheduledSession> findAllByOrganizationEntityIdAndActiveTrue(@Param("entityId") String entityId);

    @Query("SELECT s FROM ScheduledSession s WHERE s.organization.entityId = :entityId AND s.active = true")
    List<ScheduledSession> findByOrganizationEntityIdAndIsActiveTrue(@Param("entityId") String entityId);
    
    @Query("SELECT s FROM ScheduledSession s WHERE s.id = :id AND s.organization.entityId = :entityId")
    Optional<ScheduledSession> findByIdAndOrganizationEntityId(@Param("id") Long id, @Param("entityId") String entityId);
    
    // Count scheduled sessions for an organization
    long countByOrganization(Organization organization);
    
    @Query("SELECT COUNT(s) FROM ScheduledSession s WHERE s.organization.entityId = :entityId")
    long countByOrganizationEntityId(@Param("entityId") String entityId);
}
