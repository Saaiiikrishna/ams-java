package com.example.attendancesystem.shared.repository;

import com.example.attendancesystem.shared.model.AttendanceLog;
import com.example.attendancesystem.shared.model.AttendanceSession;
import com.example.attendancesystem.shared.model.CheckInMethod;
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
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    // ========== NEW USER-BASED METHODS ==========

    // Find if a user has already checked in for a specific session
    Optional<AttendanceLog> findByUserIdAndSession(Long userId, AttendanceSession session);

    // Find active check-in (not checked out) for a user in a session
    AttendanceLog findByUserIdAndSessionAndCheckOutTimeIsNull(Long userId, AttendanceSession session);

    // Find logs for a user within a certain time range, ordered by check-in time
    List<AttendanceLog> findByUserIdAndCheckInTimeBetweenOrderByCheckInTimeDesc(
            Long userId, LocalDateTime startTime, LocalDateTime endTime);

    // Find logs for a user within a certain time range
    List<AttendanceLog> findByUserIdAndCheckInTimeBetween(
            Long userId, LocalDateTime startTime, LocalDateTime endTime);

    // Find all logs for a specific user
    List<AttendanceLog> findByUserId(Long userId);

    // Find all logs for a specific user with pagination
    Page<AttendanceLog> findByUserId(Long userId, Pageable pageable);

    // ========== LEGACY METHODS REMOVED - USE USER-BASED METHODS INSTEAD ==========
    // Note: All subscriber-based methods have been removed to support microservices architecture
    // Use userId-based methods instead and get user information via gRPC from user-service

    // Find all logs for a specific session
    List<AttendanceLog> findBySession(AttendanceSession session);

    // Find logs for a specific session with pagination
    Page<AttendanceLog> findBySession(AttendanceSession session, Pageable pageable);

    // Find logs for an organization within a date range
    @Query("SELECT al FROM AttendanceLog al WHERE al.session.organization = :organization " +
           "AND al.checkInTime >= :startDate AND al.checkInTime <= :endDate " +
           "ORDER BY al.checkInTime DESC")
    List<AttendanceLog> findByOrganizationAndDateRange(
            @Param("organization") Organization organization,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Find recent attendance logs for an organization
    @Query("SELECT al FROM AttendanceLog al WHERE al.session.organization = :organization " +
           "ORDER BY al.checkInTime DESC")
    List<AttendanceLog> findTop10BySessionOrganizationOrderByCheckInTimeDesc(Organization organization);

    // Find all logs for an organization (for attendance-service compatibility)
    @Query("SELECT al FROM AttendanceLog al WHERE al.session.organization = :organization")
    List<AttendanceLog> findBySessionOrganization(@Param("organization") Organization organization);

    // Find logs by session and check-in method
    List<AttendanceLog> findBySessionAndCheckInMethod(AttendanceSession session, CheckInMethod checkInMethod);

    // Find logs by session and check-out method
    List<AttendanceLog> findBySessionAndCheckOutMethod(AttendanceSession session, CheckInMethod checkOutMethod);

    // Count attendance for a session
    long countBySession(AttendanceSession session);

    // Count attendance for an organization within a date range
    @Query("SELECT COUNT(al) FROM AttendanceLog al WHERE al.session.organization = :organization " +
           "AND al.checkInTime >= :startDate AND al.checkInTime <= :endDate")
    long countByOrganizationAndDateRange(
            @Param("organization") Organization organization,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Statistics queries (updated for user-based model)
    @Query("SELECT COUNT(DISTINCT al.userId) FROM AttendanceLog al WHERE al.session = :session")
    long countUniqueAttendeesBySession(@Param("session") AttendanceSession session);

    @Query("SELECT al.checkInMethod, COUNT(al) FROM AttendanceLog al WHERE al.session = :session " +
           "GROUP BY al.checkInMethod")
    List<Object[]> countByCheckInMethodForSession(@Param("session") AttendanceSession session);

    // Find logs that haven't been checked out yet
    @Query("SELECT al FROM AttendanceLog al WHERE al.session = :session AND al.checkOutTime IS NULL")
    List<AttendanceLog> findActiveCheckInsForSession(@Param("session") AttendanceSession session);

    // Delete logs for an organization (for cleanup)
    @Query("DELETE FROM AttendanceLog al WHERE al.session.organization = :organization")
    void deleteByOrganization(@Param("organization") Organization organization);
}
