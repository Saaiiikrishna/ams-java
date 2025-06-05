package com.example.attendancesystem.repository;

import com.example.attendancesystem.model.AttendanceSession;
import com.example.attendancesystem.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
