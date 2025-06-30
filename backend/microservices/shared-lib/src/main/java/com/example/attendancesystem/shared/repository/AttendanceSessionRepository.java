package com.example.attendancesystem.shared.repository;

import com.example.attendancesystem.shared.model.AttendanceSession;
import com.example.attendancesystem.shared.model.Organization;
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

    // Find all active sessions across all organizations (for SuperAdmin)
    List<AttendanceSession> findByEndTimeIsNullAndStartTimeBefore(LocalDateTime currentTime);
    long countByOrganization(Organization organization);
    void deleteByOrganization(Organization organization);

    // Find sessions within a date range
    @Query("SELECT s FROM AttendanceSession s WHERE s.organization = :organization " +
           "AND s.startTime >= :startDate AND s.startTime <= :endDate " +
           "ORDER BY s.startTime DESC")
    List<AttendanceSession> findByOrganizationAndDateRange(
            @Param("organization") Organization organization,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Find sessions by organization and start time between (for attendance-service compatibility)
    List<AttendanceSession> findByOrganizationAndStartTimeBetween(
            Organization organization, LocalDateTime startTime, LocalDateTime endTime);

    // Find sessions by name (case-insensitive search)
    @Query("SELECT s FROM AttendanceSession s WHERE s.organization = :organization " +
           "AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "ORDER BY s.startTime DESC")
    List<AttendanceSession> findByOrganizationAndNameContaining(
            @Param("organization") Organization organization,
            @Param("name") String name);

    // Find all active sessions (endTime is null)
    List<AttendanceSession> findByEndTimeIsNull();

    // Find active sessions for an organization (endTime is null)
    List<AttendanceSession> findByOrganizationAndEndTimeIsNull(Organization organization);

    // Find active session for an organization (for attendance-service compatibility)
    @Query("SELECT s FROM AttendanceSession s WHERE s.organization = :organization AND s.endTime IS NULL")
    AttendanceSession findActiveSessionByOrganization(@Param("organization") Organization organization);

    // Find by ID and organization entity ID (for attendance-service compatibility)
    @Query("SELECT s FROM AttendanceSession s WHERE s.id = :id AND s.organization.entityId = :entityId")
    Optional<AttendanceSession> findByIdAndOrganizationEntityId(@Param("id") Long id, @Param("entityId") String entityId);

    // Find session by QR code that is still active
    Optional<AttendanceSession> findByQrCodeAndEndTimeIsNull(String qrCode);

    // Pagination support
    Page<AttendanceSession> findByOrganization(Organization organization, Pageable pageable);
    Page<AttendanceSession> findByOrganizationAndNameContainingIgnoreCase(Organization organization, String name, Pageable pageable);

    // Statistics queries
    @Query("SELECT COUNT(s) FROM AttendanceSession s WHERE s.organization = :organization " +
           "AND s.startTime >= :startDate AND s.startTime <= :endDate")
    long countByOrganizationAndDateRange(
            @Param("organization") Organization organization,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Find recent sessions
    @Query("SELECT s FROM AttendanceSession s WHERE s.organization = :organization " +
           "ORDER BY s.startTime DESC")
    List<AttendanceSession> findRecentByOrganization(
            @Param("organization") Organization organization,
            Pageable pageable);
}
