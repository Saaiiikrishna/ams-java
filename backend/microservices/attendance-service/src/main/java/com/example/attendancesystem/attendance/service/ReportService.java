package com.example.attendancesystem.attendance.service;

import com.example.attendancesystem.attendance.model.AttendanceLog;
import com.example.attendancesystem.attendance.model.AttendanceSession;
import com.example.attendancesystem.attendance.repository.AttendanceLogRepository;
import com.example.attendancesystem.attendance.repository.AttendanceSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    public byte[] generateSessionAttendanceReport(Long sessionId) {
        try {
            logger.info("Generating session attendance report for session: {}", sessionId);
            
            // For microservices independence, use sessionId directly
            AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found"));

            // Get attendance logs - simplified for independence
            List<AttendanceLog> attendanceLogs = attendanceLogRepository.findBySessionId(session.getId());
            
            // Return simplified report message as bytes
            String reportContent = String.format(
                "ATTENDANCE REPORT\n" +
                "Session: %s\n" +
                "Date: %s\n" +
                "Total Attendees: %d\n" +
                "Note: Detailed reporting temporarily simplified for microservices independence",
                session.getName(),
                session.getStartTime(),
                attendanceLogs.size()
            );
            
            return reportContent.getBytes();

        } catch (Exception e) {
            logger.error("Failed to generate session attendance report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate report", e);
        }
    }

    public byte[] generateSubscriberAttendanceReport(Long subscriberId, Long organizationId, 
                                                   LocalDateTime startDate, LocalDateTime endDate) {
        try {
            logger.info("Generating subscriber attendance report for subscriber: {}", subscriberId);
            
            // For microservices independence, use IDs directly
            // Note: Cross-service calls removed for independence
            
            // Get attendance logs for the subscriber in the date range
            List<AttendanceLog> attendanceLogs = attendanceLogRepository
                    .findByUserIdAndCheckInTimeBetween(subscriberId, startDate, endDate);

            // Get all sessions in the date range for this organization
            List<AttendanceSession> allSessions = attendanceSessionRepository
                    .findByOrganizationIdAndStartTimeBetween(organizationId, startDate, endDate);

            // Return simplified report message as bytes
            String reportContent = String.format(
                "SUBSCRIBER ATTENDANCE REPORT\n" +
                "Subscriber ID: %d\n" +
                "Organization ID: %d\n" +
                "Period: %s to %s\n" +
                "Sessions Attended: %d\n" +
                "Total Sessions: %d\n" +
                "Note: Detailed reporting temporarily simplified for microservices independence",
                subscriberId,
                organizationId,
                startDate,
                endDate,
                attendanceLogs.size(),
                allSessions.size()
            );
            
            return reportContent.getBytes();

        } catch (Exception e) {
            logger.error("Failed to generate subscriber attendance report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate report", e);
        }
    }
}
