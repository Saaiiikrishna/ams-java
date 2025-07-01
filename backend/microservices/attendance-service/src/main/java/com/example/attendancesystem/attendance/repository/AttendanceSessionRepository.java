package com.example.attendancesystem.attendance.repository;

import com.example.attendancesystem.attendance.model.AttendanceSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.attendancesystem.attendance.dto.OrganizationDto;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    Optional<AttendanceSession> findByIdAndOrganizationId(Long id, Long organizationId);
    List<AttendanceSession> findAllByOrganizationId(Long organizationId);
    // Find active sessions for an organization (ended time is null and current time is after start time)
    List<AttendanceSession> findByOrganizationIdAndEndTimeIsNullAndStartTimeBefore(Long organizationId, LocalDateTime currentTime);

    // Alternative method for finding active sessions
    List<AttendanceSession> findByEndTimeIsNullAndStartTimeBefore(LocalDateTime currentTime);

    // Methods using organizationId instead of Organization entity (remove duplicates)
    Page<AttendanceSession> findByOrganizationIdAndNameContainingIgnoreCase(Long organizationId, String name, Pageable pageable);

    // Methods using organizationId for compatibility

    // Find by ID and organization ID (proper microservices approach) - using Spring Data JPA method naming
    List<AttendanceSession> findByOrganizationIdAndStartTimeBetween(Long organizationId, LocalDateTime start, LocalDateTime end);
    long countByOrganizationId(Long organizationId);
    void deleteByOrganizationId(Long organizationId);

    // Find sessions within a date range
    @Query("SELECT s FROM AttendanceSession s WHERE s.organizationId = :organizationId " +
           "AND s.startTime >= :startDate AND s.startTime <= :endDate " +
           "ORDER BY s.startTime DESC")
    List<AttendanceSession> findByOrganizationIdAndDateRange(
            @Param("organizationId") Long organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);



    // Find sessions by name (case-insensitive search)
    @Query("SELECT s FROM AttendanceSession s WHERE s.organizationId = :organizationId " +
           "AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "ORDER BY s.startTime DESC")
    List<AttendanceSession> findByOrganizationIdAndNameContaining(
            @Param("organizationId") Long organizationId,
            @Param("name") String name);

    // Find all active sessions (endTime is null)
    List<AttendanceSession> findByEndTimeIsNull();

    // Find active sessions for an organization (endTime is null)
    List<AttendanceSession> findByOrganizationIdAndEndTimeIsNull(Long organizationId);

    // Find active session for an organization
    @Query("SELECT s FROM AttendanceSession s WHERE s.organizationId = :organizationId AND s.endTime IS NULL")
    AttendanceSession findActiveSessionByOrganizationId(@Param("organizationId") Long organizationId);

    // Find session by QR code that is still active
    Optional<AttendanceSession> findByQrCodeAndEndTimeIsNull(String qrCode);

    // Pagination support
    Page<AttendanceSession> findByOrganizationId(Long organizationId, Pageable pageable);

    // Statistics queries
    @Query("SELECT COUNT(s) FROM AttendanceSession s WHERE s.organizationId = :organizationId " +
           "AND s.startTime >= :startDate AND s.startTime <= :endDate")
    long countByOrganizationIdAndDateRange(
            @Param("organizationId") Long organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Find recent sessions
    @Query("SELECT s FROM AttendanceSession s WHERE s.organizationId = :organizationId " +
           "ORDER BY s.startTime DESC")
    List<AttendanceSession> findRecentByOrganizationId(
            @Param("organizationId") Long organizationId,
            Pageable pageable);
}
