package com.example.attendancesystem.subscriber.repository;

import com.example.attendancesystem.model.AttendanceLog;
import com.example.attendancesystem.model.AttendanceSession;
import com.example.attendancesystem.model.CheckInMethod;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {
    // Find if a subscriber has already checked in for a specific session
    Optional<AttendanceLog> findBySubscriberAndSession(Subscriber subscriber, AttendanceSession session);

    // Find logs for a subscriber within a certain time range, ordered by check-in time
    List<AttendanceLog> findBySubscriberAndCheckInTimeBetweenOrderByCheckInTimeDesc(
            Subscriber subscriber, LocalDateTime startTime, LocalDateTime endTime);

    // Find logs for a specific session
    List<AttendanceLog> findBySession(AttendanceSession session);

    // Find logs for a specific session and subscriber
    List<AttendanceLog> findBySessionAndSubscriber(AttendanceSession session, Subscriber subscriber);

    // Find recent attendance logs for an organization (for real-time dashboard)
    @Query("SELECT al FROM AttendanceLog al WHERE al.session.organization = :organization ORDER BY al.checkInTime DESC")
    List<AttendanceLog> findTop10BySessionOrganizationOrderByCheckInTimeDesc(@Param("organization") Organization organization);

    // Find logs for a subscriber within a date range (for reports)
    List<AttendanceLog> findBySubscriberAndCheckInTimeBetween(
            Subscriber subscriber, LocalDateTime startTime, LocalDateTime endTime);

    // Find active check-in (no check-out time) for subscriber and session
    AttendanceLog findBySubscriberAndSessionAndCheckOutTimeIsNull(Subscriber subscriber, AttendanceSession session);

    // Find active check-in (no check-out time) for subscriber across all sessions - get the most recent one
    Optional<AttendanceLog> findFirstBySubscriberAndCheckOutTimeIsNullOrderByCheckInTimeDesc(Subscriber subscriber);

    // Find recent attendance for a specific subscriber
    List<AttendanceLog> findTop10BySubscriberOrderByCheckInTimeDesc(Subscriber subscriber);

    // Find all attendance for a subscriber ordered by check-in time
    List<AttendanceLog> findBySubscriberOrderByCheckInTimeDesc(Subscriber subscriber);

    // Count and delete methods for cleanup
    @Query("SELECT COUNT(al) FROM AttendanceLog al WHERE al.session.organization = :organization")
    long countByOrganization(@Param("organization") Organization organization);

    // Find logs by organization
    @Query("SELECT al FROM AttendanceLog al WHERE al.session.organization = :organization")
    List<AttendanceLog> findBySessionOrganization(@Param("organization") Organization organization);

    @Modifying
    @Query("DELETE FROM AttendanceLog al WHERE al.session.organization = :organization")
    void deleteByOrganization(@Param("organization") Organization organization);

    void deleteBySession(AttendanceSession session);
    void deleteBySubscriber(Subscriber subscriber);

    // Face recognition specific methods

    /**
     * Find attendance logs by session and check-in method
     */
    List<AttendanceLog> findBySessionAndCheckInMethod(AttendanceSession session, CheckInMethod checkInMethod);

    /**
     * Find attendance logs by session and check-out method
     */
    List<AttendanceLog> findBySessionAndCheckOutMethod(AttendanceSession session, CheckInMethod checkOutMethod);
}
