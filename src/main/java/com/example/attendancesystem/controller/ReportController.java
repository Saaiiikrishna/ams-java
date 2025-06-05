package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.AttendanceLogDto;
import com.example.attendancesystem.dto.SubscriberDto;
import com.example.attendancesystem.model.*;
import com.example.attendancesystem.repository.AttendanceLogRepository;
import com.example.attendancesystem.repository.AttendanceSessionRepository;
import com.example.attendancesystem.repository.SubscriberRepository;
import com.example.attendancesystem.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reports")
@PreAuthorize("hasRole('ENTITY_ADMIN')") // Reports are generally for entity admins
public class ReportController {

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    private Organization getCurrentOrganization() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getEntityAdmin().getOrganization();
    }

    @GetMapping("/sessions/{sessionId}/absentees")
    public ResponseEntity<List<SubscriberDto>> getAbsenteesForSession(@PathVariable Long sessionId) {
        Organization organization = getCurrentOrganization();
        AttendanceSession session = attendanceSessionRepository.findByIdAndOrganization(sessionId, organization)
                .orElseThrow(() -> new EntityNotFoundException("Session not found with id: " + sessionId + " in your organization."));

        List<Subscriber> allSubscribersInOrg = subscriberRepository.findAllByOrganization(organization);
        List<AttendanceLog> logsForSession = attendanceLogRepository.findBySession(session);
        Set<Subscriber> presentSubscribers = logsForSession.stream()
                .map(AttendanceLog::getSubscriber)
                .collect(Collectors.toSet());

        List<SubscriberDto> absentees = allSubscribersInOrg.stream()
                .filter(subscriber -> !presentSubscribers.contains(subscriber))
                .map(this::convertToSubscriberDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(absentees);
    }

    @GetMapping("/subscribers/{subscriberId}/attendance")
    public ResponseEntity<List<AttendanceLogDto>> getAttendanceForSubscriber(
            @PathVariable Long subscriberId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Organization organization = getCurrentOrganization();
        Subscriber subscriber = subscriberRepository.findByIdAndOrganization(subscriberId, organization)
                .orElseThrow(() -> new EntityNotFoundException("Subscriber not found with id: " + subscriberId + " in your organization."));

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : LocalDate.now().minusYears(1).atStartOfDay();
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        List<AttendanceLog> attendanceLogs = attendanceLogRepository
                .findBySubscriberAndCheckInTimeBetweenOrderByCheckInTimeDesc(subscriber, startDateTime, endDateTime);

        return ResponseEntity.ok(attendanceLogs.stream().map(this::convertToAttendanceLogDto).collect(Collectors.toList()));
    }


    // DTO Converters
    private SubscriberDto convertToSubscriberDto(Subscriber subscriber) {
        String nfcUid = subscriber.getNfcCard() != null ? subscriber.getNfcCard().getCardUid() : null;
        return new SubscriberDto(
                subscriber.getId(),
                subscriber.getFirstName(),
                subscriber.getLastName(),
                subscriber.getEmail(),
                subscriber.getOrganization().getId(),
                nfcUid
        );
    }

    private AttendanceLogDto convertToAttendanceLogDto(AttendanceLog log) {
        return new AttendanceLogDto(
                log.getId(),
                log.getSubscriber().getId(),
                log.getSubscriber().getFirstName(),
                log.getSubscriber().getLastName(),
                log.getSubscriber().getEmail(),
                log.getSession().getId(),
                log.getSession().getName(),
                log.getCheckInTime(),
                log.getCheckOutTime()
        );
    }
}
