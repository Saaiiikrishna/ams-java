package com.example.attendancesystem.repository;

import com.example.attendancesystem.model.AttendanceLog;
import com.example.attendancesystem.model.AttendanceSession;
import com.example.attendancesystem.model.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
