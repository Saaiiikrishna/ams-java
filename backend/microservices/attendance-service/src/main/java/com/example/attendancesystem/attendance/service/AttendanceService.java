package com.example.attendancesystem.attendance.service;

import com.example.attendancesystem.shared.model.AttendanceLog;
import com.example.attendancesystem.shared.model.AttendanceSession;
import com.example.attendancesystem.shared.model.CheckInMethod;
import com.example.attendancesystem.shared.repository.AttendanceLogRepository;
import com.example.attendancesystem.shared.repository.AttendanceSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Attendance Service for managing check-ins and check-outs
 * Works with the new User Service architecture
 */
@Service
@Transactional
public class AttendanceService {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceService.class);

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    // ========== CHECK-IN METHODS ==========

    /**
     * Process check-in for a user
     */
    public AttendanceCheckResult processCheckIn(Long userId, String userName, String userMobile, 
                                               Long sessionId, CheckInMethod method, 
                                               String deviceInfo, String locationInfo) {
        logger.info("Processing check-in for user {} using method {}", userId, method);

        // Find the session
        AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        // Check if session is active
        if (!isSessionActive(session)) {
            logger.warn("Check-in failed - session not active: {}", sessionId);
            return new AttendanceCheckResult(false, "Session is not active", null);
        }

        // Check if user already checked in for this session
        Optional<AttendanceLog> existingLog = attendanceLogRepository.findByUserIdAndSession(userId, session);
        
        if (existingLog.isPresent()) {
            AttendanceLog log = existingLog.get();
            if (log.getCheckOutTime() == null) {
                // User is already checked in, process check-out instead
                return processCheckOut(userId, userName, userMobile, sessionId, method, deviceInfo, locationInfo);
            } else {
                // User already completed attendance for this session
                logger.warn("Check-in failed - user already completed attendance: user={}, session={}", userId, sessionId);
                return new AttendanceCheckResult(false, "Already completed attendance for this session", log);
            }
        }

        // Create new check-in log
        AttendanceLog newLog = new AttendanceLog(userId, userName, userMobile, session, LocalDateTime.now(), method);
        newLog.setDeviceInfo(deviceInfo);
        newLog.setLocationInfo(locationInfo);
        
        AttendanceLog savedLog = attendanceLogRepository.save(newLog);
        
        logger.info("Check-in successful: user={}, session={}, method={}", userId, sessionId, method);
        return new AttendanceCheckResult(true, "Checked in successfully", savedLog);
    }

    /**
     * Process check-out for a user
     */
    public AttendanceCheckResult processCheckOut(Long userId, String userName, String userMobile, 
                                                Long sessionId, CheckInMethod method, 
                                                String deviceInfo, String locationInfo) {
        logger.info("Processing check-out for user {} using method {}", userId, method);

        // Find the session
        AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        // Find existing check-in log
        Optional<AttendanceLog> existingLogOpt = attendanceLogRepository.findByUserIdAndSession(userId, session);
        
        if (!existingLogOpt.isPresent()) {
            logger.warn("Check-out failed - no check-in found: user={}, session={}", userId, sessionId);
            return new AttendanceCheckResult(false, "No check-in found for this session", null);
        }

        AttendanceLog log = existingLogOpt.get();
        
        if (log.getCheckOutTime() != null) {
            logger.warn("Check-out failed - already checked out: user={}, session={}", userId, sessionId);
            return new AttendanceCheckResult(false, "Already checked out", log);
        }

        // Update log with check-out information
        log.setCheckOutTime(LocalDateTime.now());
        log.setCheckOutMethod(method);
        
        AttendanceLog savedLog = attendanceLogRepository.save(log);
        
        logger.info("Check-out successful: user={}, session={}, method={}", userId, sessionId, method);
        return new AttendanceCheckResult(true, "Checked out successfully", savedLog);
    }

    /**
     * Process NFC card check-in/out
     */
    public AttendanceCheckResult processNfcCardAttendance(String cardUid, Long sessionId, 
                                                         String deviceInfo, String locationInfo) {
        logger.info("Processing NFC card attendance: card={}, session={}", cardUid, sessionId);

        // This method would need to call user-service via gRPC to get user info from card UID
        // For now, returning a placeholder response
        // TODO: Implement gRPC call to user-service to get user by NFC card UID
        
        logger.warn("NFC card attendance not yet implemented - requires gRPC integration with user-service");
        return new AttendanceCheckResult(false, "NFC card attendance not yet implemented", null);
    }

    // ========== ATTENDANCE QUERIES ==========

    /**
     * Get attendance log for a user in a session
     */
    public Optional<AttendanceLog> getAttendanceLog(Long userId, Long sessionId) {
        AttendanceSession session = attendanceSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return Optional.empty();
        }
        return attendanceLogRepository.findByUserIdAndSession(userId, session);
    }

    /**
     * Get all attendance logs for a session
     */
    public List<AttendanceLog> getSessionAttendance(Long sessionId) {
        AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        return attendanceLogRepository.findBySession(session);
    }

    /**
     * Get attendance logs for a user
     */
    public List<AttendanceLog> getUserAttendance(Long userId) {
        return attendanceLogRepository.findByUserId(userId);
    }

    /**
     * Get attendance logs for a user within date range
     */
    public List<AttendanceLog> getUserAttendanceInDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return attendanceLogRepository.findByUserIdAndCheckInTimeBetween(userId, startDate, endDate);
    }

    // ========== SESSION MANAGEMENT ==========

    /**
     * Check if a session is currently active
     */
    public boolean isSessionActive(AttendanceSession session) {
        LocalDateTime now = LocalDateTime.now();
        return session.getStartTime() != null && 
               session.getStartTime().isBefore(now) && 
               (session.getEndTime() == null || session.getEndTime().isAfter(now));
    }

    /**
     * Get active sessions for an organization
     */
    public List<AttendanceSession> getActiveSessionsForOrganization(Long organizationId) {
        // This would need organization lookup - simplified for now
        return attendanceSessionRepository.findByEndTimeIsNull();
    }

    // ========== STATISTICS ==========

    /**
     * Get attendance statistics for a session
     */
    public AttendanceStats getSessionStats(Long sessionId) {
        AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        
        List<AttendanceLog> logs = attendanceLogRepository.findBySession(session);
        
        long totalAttendees = logs.size();
        long checkedIn = logs.stream().mapToLong(log -> log.getCheckOutTime() == null ? 1 : 0).sum();
        long checkedOut = logs.stream().mapToLong(log -> log.getCheckOutTime() != null ? 1 : 0).sum();
        
        return new AttendanceStats(totalAttendees, checkedIn, checkedOut);
    }

    // ========== INNER CLASSES ==========

    public static class AttendanceCheckResult {
        private boolean success;
        private String message;
        private AttendanceLog attendanceLog;

        public AttendanceCheckResult(boolean success, String message, AttendanceLog attendanceLog) {
            this.success = success;
            this.message = message;
            this.attendanceLog = attendanceLog;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public AttendanceLog getAttendanceLog() { return attendanceLog; }
    }

    public static class AttendanceStats {
        private long totalAttendees;
        private long checkedIn;
        private long checkedOut;

        public AttendanceStats(long totalAttendees, long checkedIn, long checkedOut) {
            this.totalAttendees = totalAttendees;
            this.checkedIn = checkedIn;
            this.checkedOut = checkedOut;
        }

        // Getters
        public long getTotalAttendees() { return totalAttendees; }
        public long getCheckedIn() { return checkedIn; }
        public long getCheckedOut() { return checkedOut; }
    }
}
