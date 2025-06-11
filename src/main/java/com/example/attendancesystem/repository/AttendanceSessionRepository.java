package com.example.attendancesystem.repository;

import com.example.attendancesystem.model.AttendanceSession;
import com.example.attendancesystem.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    Optional<AttendanceSession> findByIdAndOrganization(Long id, Organization organization);
    List<AttendanceSession> findAllByOrganization(Organization organization);
    // Find active sessions for an organization (ended time is null and current time is after start time)
    List<AttendanceSession> findByOrganizationAndEndTimeIsNullAndStartTimeBefore(Organization organization, LocalDateTime currentTime);
    long countByOrganization(Organization organization);

    // Entity ID based methods
    @Query("SELECT s FROM AttendanceSession s WHERE s.organization.entityId = :entityId")
    List<AttendanceSession> findAllByOrganizationEntityId(@Param("entityId") String entityId);

    @Query("SELECT s FROM AttendanceSession s WHERE s.id = :id AND s.organization.entityId = :entityId")
    Optional<AttendanceSession> findByIdAndOrganizationEntityId(@Param("id") Long id, @Param("entityId") String entityId);

    @Query("SELECT s FROM AttendanceSession s WHERE s.organization.entityId = :entityId AND s.endTime IS NULL AND s.startTime < :currentTime")
    List<AttendanceSession> findByOrganizationEntityIdAndEndTimeIsNullAndStartTimeBefore(@Param("entityId") String entityId, @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT COUNT(s) FROM AttendanceSession s WHERE s.organization.entityId = :entityId")
    long countByOrganizationEntityId(@Param("entityId") String entityId);
}
