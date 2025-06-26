package com.example.attendancesystem.organization.repository;

import com.example.attendancesystem.organization.model.AttendanceSession;
import com.example.attendancesystem.organization.model.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    void deleteByOrganization(Organization organization);

    // Entity ID based methods
    @Query("SELECT s FROM AttendanceSession s WHERE s.organization.entityId = :entityId")
    List<AttendanceSession> findAllByOrganizationEntityId(@Param("entityId") String entityId);

    @Query("SELECT s FROM AttendanceSession s WHERE s.id = :id AND s.organization.entityId = :entityId")
    Optional<AttendanceSession> findByIdAndOrganizationEntityId(@Param("id") Long id, @Param("entityId") String entityId);

    @Query("SELECT s FROM AttendanceSession s WHERE s.organization.entityId = :entityId AND s.endTime IS NULL AND s.startTime < :currentTime")
    List<AttendanceSession> findByOrganizationEntityIdAndEndTimeIsNullAndStartTimeBefore(@Param("entityId") String entityId, @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT COUNT(s) FROM AttendanceSession s WHERE s.organization.entityId = :entityId")
    long countByOrganizationEntityId(@Param("entityId") String entityId);

    // Find sessions within a date range for reports
    List<AttendanceSession> findByOrganizationAndStartTimeBetween(
            Organization organization, LocalDateTime startTime, LocalDateTime endTime);

    // Find all active sessions (endTime is null)
    List<AttendanceSession> findByEndTimeIsNull();

    // Find active sessions for an organization (endTime is null)
    List<AttendanceSession> findByOrganizationAndEndTimeIsNull(Organization organization);

    // Find session by QR code that is still active
    Optional<AttendanceSession> findByQrCodeAndEndTimeIsNull(String qrCode);

    // Pagination support
    Page<AttendanceSession> findByOrganization(Organization organization, Pageable pageable);
    Page<AttendanceSession> findByOrganizationAndNameContainingIgnoreCase(Organization organization, String name, Pageable pageable);

    // Find active session for organization
    @Query("SELECT s FROM AttendanceSession s WHERE s.organization = :organization AND s.active = true AND s.endTime > CURRENT_TIMESTAMP")
    Optional<AttendanceSession> findActiveSessionByOrganization(@Param("organization") Organization organization);
}
