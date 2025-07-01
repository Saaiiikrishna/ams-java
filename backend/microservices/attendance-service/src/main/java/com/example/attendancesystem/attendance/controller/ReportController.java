package com.example.attendancesystem.attendance.controller;

import com.example.attendancesystem.attendance.dto.AttendanceLogDto;
import com.example.attendancesystem.attendance.dto.SubscriberDto;
import com.example.attendancesystem.attendance.model.*;
import com.example.attendancesystem.attendance.repository.AttendanceLogRepository;
import com.example.attendancesystem.attendance.repository.AttendanceSessionRepository;
// TODO: Replace with gRPC calls to User Service
// import com.example.attendancesystem.shared.repository.SubscriberRepository;
// import com.example.attendancesystem.shared.security.CustomUserDetails; // TODO: Integrate with auth-service via gRPC
import com.example.attendancesystem.attendance.service.ReportService;
import com.example.attendancesystem.attendance.client.UserServiceGrpcClient;
import com.example.attendancesystem.attendance.client.OrganizationServiceGrpcClient;
import com.example.attendancesystem.attendance.dto.UserDto;
import com.example.attendancesystem.attendance.dto.OrganizationDto;
import com.example.attendancesystem.attendance.util.AuthUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ENTITY_ADMIN')") // Reports are generally for entity admins
public class ReportController {

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private UserServiceGrpcClient userServiceGrpcClient;

    @Autowired
    private OrganizationServiceGrpcClient organizationServiceGrpcClient;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    @Autowired
    private ReportService reportService;

    private OrganizationDto getCurrentOrganization() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // TODO: Replace with proper authentication integration via gRPC calls to auth-service
        String entityId = AuthUtil.getEntityId(authentication);
        // For now, return a default organization - this should be replaced with proper gRPC call
        OrganizationDto org = organizationServiceGrpcClient.getOrganizationByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        return org;
    }

    @GetMapping("/sessions/{sessionId}/absentees")
    public ResponseEntity<List<SubscriberDto>> getAbsenteesForSession(@PathVariable Long sessionId) {
        OrganizationDto organization = getCurrentOrganization();
        AttendanceSession session = attendanceSessionRepository.findByIdAndOrganizationId(sessionId, organization.getId())
                .orElseThrow(() -> new EntityNotFoundException("Session not found with id: " + sessionId + " in your organization."));

        List<UserDto> allSubscribersInOrg = userServiceGrpcClient.getUsersByOrganizationId(organization.getId());
        List<AttendanceLog> logsForSession = attendanceLogRepository.findBySessionId(session.getId());
        Set<Long> presentSubscriberIds = logsForSession.stream()
                .map(AttendanceLog::getUserId)
                .collect(Collectors.toSet());

        List<SubscriberDto> absentees = allSubscribersInOrg.stream()
                .filter(subscriber -> !presentSubscriberIds.contains(subscriber.getId()))
                .map(this::convertToSubscriberDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(absentees);
    }

    @GetMapping("/subscribers/{subscriberId}/attendance")
    public ResponseEntity<List<AttendanceLogDto>> getAttendanceForSubscriber(
            @PathVariable Long subscriberId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        OrganizationDto organization = getCurrentOrganization();
        UserDto subscriber = userServiceGrpcClient.getUserById(subscriberId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + subscriberId));

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : LocalDate.now().minusYears(1).atStartOfDay();
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        List<AttendanceLog> attendanceLogs = attendanceLogRepository
                .findByUserIdAndCheckInTimeBetweenOrderByCheckInTimeDesc(subscriber.getId(), startDateTime, endDateTime);

        return ResponseEntity.ok(attendanceLogs.stream().map(this::convertToAttendanceLogDto).collect(Collectors.toList()));
    }

    /**
     * Generate and download session attendance report as PDF
     */
    @GetMapping("/sessions/{sessionId}/attendance-pdf")
    public ResponseEntity<?> downloadSessionAttendanceReport(@PathVariable Long sessionId) {
        try {
            OrganizationDto organization = getCurrentOrganization();

            byte[] pdfBytes = reportService.generateSessionAttendanceReport(sessionId, organization.getEntityId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                "session_" + sessionId + "_attendance_report_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate report: " + e.getMessage()));
        }
    }

    /**
     * Generate and download subscriber activity report as PDF
     */
    @GetMapping("/subscribers/{subscriberId}/activity-pdf")
    public ResponseEntity<?> downloadSubscriberActivityReport(
            @PathVariable Long subscriberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        try {
            OrganizationDto organization = getCurrentOrganization();

            byte[] pdfBytes = reportService.generateSubscriberActivityReport(
                subscriberId, organization.getEntityId(), startDate, endDate);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                "subscriber_" + subscriberId + "_activity_report_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate report: " + e.getMessage()));
        }
    }

    // DTO Converters
    private SubscriberDto convertToSubscriberDto(UserDto subscriber) {
        String nfcUid = null; // NFC card info not available in UserDto - would need separate service call
        return new SubscriberDto(
                subscriber.getId(),
                subscriber.getFirstName(),
                subscriber.getLastName(),
                subscriber.getEmail(),
                subscriber.getMobileNumber(),
                subscriber.getOrganizationId(),
                nfcUid
        );
    }

    private AttendanceLogDto convertToAttendanceLogDto(AttendanceLog log) {
        return new AttendanceLogDto(
                log.getId(),
                log.getUserId(),
                log.getUserName(),
                "", // Last name - need to get from user service
                "", // Email - need to get from user service
                log.getSession().getId(),
                log.getSession().getName(),
                log.getCheckInTime(),
                log.getCheckOutTime(),
                log.getCheckInMethod() != null ? log.getCheckInMethod().toString() : null,
                log.getCheckOutMethod() != null ? log.getCheckOutMethod().toString() : null
        );
    }
}
