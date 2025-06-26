package com.example.attendancesystem.organization.controller;

import com.example.attendancesystem.dto.SubscriberLoginDto;
import com.example.attendancesystem.organization.service.SubscriberAuthService;
import com.example.attendancesystem.organization.service.QrCodeService;
import com.example.attendancesystem.organization.service.MDnsService;
import com.example.attendancesystem.organization.model.*;
import com.example.attendancesystem.organization.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/subscriber")
public class SubscriberController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriberController.class);

    @Autowired
    private SubscriberAuthService subscriberAuthService;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    @Autowired
    private ScheduledSessionRepository scheduledSessionRepository;

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private MDnsService mdnsService;



    /**
     * Login with PIN (simplified - no organization ID required)
     */
    @PostMapping("/login-pin")
    public ResponseEntity<?> loginWithPin(@RequestBody SubscriberLoginDto loginDto) {
        try {
            if (loginDto.getMobileNumber() == null || loginDto.getPin() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Mobile number and PIN are required"));
            }

            Map<String, Object> response = subscriberAuthService.loginWithPinSimple(loginDto);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("PIN login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }






    /**
     * Update subscriber PIN
     */
    @PutMapping("/update-pin")
    public ResponseEntity<?> updatePin(@RequestBody Map<String, String> request) {
        try {
            String mobileNumber = request.get("mobileNumber");
            String currentPin = request.get("currentPin");
            String newPin = request.get("newPin");

            if (mobileNumber == null || currentPin == null || newPin == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Mobile number, current PIN, and new PIN are required"));
            }

            if (newPin.length() != 4 || !newPin.matches("\\d{4}")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "New PIN must be exactly 4 digits"));
            }

            subscriberAuthService.updatePin(mobileNumber, currentPin, newPin);
            
            return ResponseEntity.ok(Map.of(
                    "message", "PIN updated successfully",
                    "success", true
            ));

        } catch (Exception e) {
            logger.error("PIN update failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    /**
     * Get subscriber profile information
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            // This would need JWT token validation for subscriber
            // For now, return a placeholder response
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(Map.of("error", "Profile endpoint not yet implemented"));

        } catch (Exception e) {
            logger.error("Failed to get profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get subscriber dashboard data for mobile app
     */
    @GetMapping("/mobile/dashboard")
    public ResponseEntity<?> getDashboard(@RequestParam String mobileNumber, @RequestParam String entityId) {
        try {
            // Find subscriber by mobile number and entity ID
            Subscriber subscriber = subscriberRepository.findByMobileNumberAndOrganizationEntityId(mobileNumber, entityId)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found"));

            Organization organization = subscriber.getOrganization();

            // Get active sessions
            List<AttendanceSession> activeSessions = attendanceSessionRepository
                    .findByOrganizationAndEndTimeIsNull(organization);

            // Get recent attendance logs for this subscriber
            List<AttendanceLog> recentAttendance = attendanceLogRepository
                    .findTop10BySubscriberOrderByCheckInTimeDesc(subscriber);

            // Get upcoming scheduled sessions
            List<ScheduledSession> upcomingSessions = scheduledSessionRepository
                    .findByOrganizationEntityIdAndIsActiveTrue(entityId);

            // Get current check-in status
            Map<String, Object> currentCheckIn = getCurrentCheckInStatus(subscriber);

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("subscriber", createSubscriberInfo(subscriber));
            dashboard.put("organization", createOrganizationInfo(organization));
            dashboard.put("activeSessions", activeSessions.stream().map(this::createSessionInfo).collect(Collectors.toList()));
            dashboard.put("recentAttendance", recentAttendance.stream().map(this::createAttendanceInfo).collect(Collectors.toList()));
            dashboard.put("upcomingSessions", upcomingSessions.stream().map(this::createScheduledSessionInfo).collect(Collectors.toList()));
            dashboard.put("currentCheckIn", currentCheckIn);

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            logger.error("Failed to get dashboard: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get available sessions for check-in for mobile app
     */
    @GetMapping("/mobile/sessions")
    public ResponseEntity<?> getAvailableSessions(@RequestParam String mobileNumber, @RequestParam String entityId) {
        try {
            Subscriber subscriber = subscriberRepository.findByMobileNumberAndOrganizationEntityId(mobileNumber, entityId)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found"));

            List<AttendanceSession> activeSessions = attendanceSessionRepository
                    .findByOrganizationAndEndTimeIsNull(subscriber.getOrganization());

            List<Map<String, Object>> sessions = activeSessions.stream()
                    .map(this::createSessionInfo)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("sessions", sessions, "count", sessions.size()));

        } catch (Exception e) {
            logger.error("Failed to get sessions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * QR Code check-in for mobile app
     */
    @PostMapping("/mobile/checkin/qr")
    public ResponseEntity<?> qrCheckIn(@RequestBody Map<String, Object> request) {
        try {
            String mobileNumber = (String) request.get("mobileNumber");
            String entityId = (String) request.get("entityId");
            String qrCode = (String) request.get("qrCode");

            if (mobileNumber == null || entityId == null || qrCode == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Mobile number, entity ID, and QR code are required"));
            }

            Subscriber subscriber = subscriberRepository.findByMobileNumberAndOrganizationEntityId(mobileNumber, entityId)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found"));

            logger.info("QR Check-in attempt - Mobile: {}, EntityId: {}, QR: {}", mobileNumber, entityId, qrCode);
            logger.info("Subscriber found - ID: {}, Name: {} {}, Org EntityId: {}",
                    subscriber.getId(), subscriber.getFirstName(), subscriber.getLastName(),
                    subscriber.getOrganization().getEntityId());

            // Find session by QR code using flexible matching
            AttendanceSession session = findSessionByQrCode(qrCode)
                    .orElseThrow(() -> new RuntimeException("Invalid or expired QR code"));

            logger.info("Session found - ID: {}, Name: {}, Org EntityId: {}, Stored QR: {}",
                    session.getId(), session.getName(), session.getOrganization().getEntityId(), session.getQrCode());

            // Extract the actual QR code for validation (remove URL prefix if present)
            String qrCodeForValidation = qrCode;
            if (qrCode.startsWith("ams://checkin?qr=")) {
                qrCodeForValidation = qrCode.substring("ams://checkin?qr=".length());
            }

            // Validate QR code using QrCodeService
            if (!qrCodeService.validateQrCode(qrCodeForValidation, session)) {
                logger.warn("QR code validation failed for session {} with QR: {}", session.getId(), qrCodeForValidation);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "QR code validation failed"));
            }

            // Check if session belongs to subscriber's organization
            if (!session.getOrganization().getEntityId().equals(entityId)) {
                logger.error("Entity ID mismatch - Subscriber EntityId: {}, Session EntityId: {}, Request EntityId: {}",
                        subscriber.getOrganization().getEntityId(), session.getOrganization().getEntityId(), entityId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "QR code does not belong to your organization"));
            }

            // Check if subscriber is already checked in to ANY session (not just this one)
            Optional<AttendanceLog> anyActiveCheckIn = attendanceLogRepository
                    .findFirstBySubscriberAndCheckOutTimeIsNullOrderByCheckInTimeDesc(subscriber);

            // Check if already checked in to this specific session
            AttendanceLog existingLog = attendanceLogRepository
                    .findBySubscriberAndSessionAndCheckOutTimeIsNull(subscriber, session);

            // Also check if there's any attendance record for this subscriber-session combination
            List<AttendanceLog> allLogsForSession = attendanceLogRepository
                    .findBySessionAndSubscriber(session, subscriber);

            if (!allLogsForSession.isEmpty() && existingLog == null) {
                // Subscriber already has a completed attendance record for this session
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "You have already completed attendance for this session"));
            }

            // If subscriber is checked in to a different session, prevent new check-in
            if (anyActiveCheckIn.isPresent() && existingLog == null) {
                AttendanceLog activeLog = anyActiveCheckIn.get();
                if (!activeLog.getSession().getId().equals(session.getId())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of(
                                "error", "You are already checked in to session: " + activeLog.getSession().getName() +
                                        ". Please check out first before checking in to a new session.",
                                "currentSession", activeLog.getSession().getName(),
                                "currentSessionId", activeLog.getSession().getId()
                            ));
                }
            }

            if (existingLog != null) {
                // Check out
                existingLog.setCheckOutTime(LocalDateTime.now());
                existingLog.setCheckOutMethod(CheckInMethod.QR);
                attendanceLogRepository.save(existingLog);
                Map<String, Object> checkOutResponse = new HashMap<>();
                checkOutResponse.put("action", "CHECK_OUT");
                checkOutResponse.put("message", "Successfully checked out");
                checkOutResponse.put("session", session.getName());
                checkOutResponse.put("time", existingLog.getCheckOutTime());
                checkOutResponse.put("checkInMethod", existingLog.getCheckInMethod().toString());
                checkOutResponse.put("checkOutMethod", "QR");
                return ResponseEntity.ok(checkOutResponse);
            } else {
                // Check in
                logger.info("Creating new attendance log for check-in");
                AttendanceLog newLog = new AttendanceLog();
                newLog.setSubscriber(subscriber);
                newLog.setSession(session);
                newLog.setCheckInTime(LocalDateTime.now());
                newLog.setCheckInMethod(CheckInMethod.QR);

                logger.info("Saving attendance log to database");
                AttendanceLog savedLog = attendanceLogRepository.save(newLog);
                logger.info("Attendance log saved successfully with ID: {}", savedLog.getId());

                logger.info("Creating success response");
                Map<String, Object> response = new HashMap<>();
                response.put("action", "CHECK_IN");
                response.put("message", "Successfully checked in");
                response.put("session", session.getName());
                response.put("time", savedLog.getCheckInTime());
                response.put("checkInMethod", "QR");
                response.put("checkOutMethod", null);

                logger.info("Returning success response");
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            logger.error("QR check-in failed: {}", e.getMessage(), e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error occurred";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", errorMessage));
        }
    }

    /**
     * Get subscriber's attendance history for mobile app
     */
    @GetMapping("/mobile/attendance/history")
    public ResponseEntity<?> getAttendanceHistory(@RequestParam String mobileNumber, @RequestParam String entityId) {
        try {
            Subscriber subscriber = subscriberRepository.findByMobileNumberAndOrganizationEntityId(mobileNumber, entityId)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found"));

            List<AttendanceLog> attendanceHistory = attendanceLogRepository
                    .findBySubscriberOrderByCheckInTimeDesc(subscriber);

            List<Map<String, Object>> history = attendanceHistory.stream()
                    .map(this::createAttendanceInfo)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "history", history,
                    "totalSessions", history.size()
            ));

        } catch (Exception e) {
            logger.error("Failed to get attendance history: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Helper methods
    private Map<String, Object> createSubscriberInfo(Subscriber subscriber) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", subscriber.getId());
        info.put("firstName", subscriber.getFirstName());
        info.put("lastName", subscriber.getLastName());
        info.put("mobileNumber", subscriber.getMobileNumber());
        info.put("email", subscriber.getEmail());
        info.put("hasNfcCard", subscriber.getNfcCard() != null);
        return info;
    }

    private Map<String, Object> createOrganizationInfo(Organization organization) {
        Map<String, Object> info = new HashMap<>();
        info.put("entityId", organization.getEntityId());
        info.put("name", organization.getName());
        info.put("address", organization.getAddress());
        return info;
    }

    private Map<String, Object> createSessionInfo(AttendanceSession session) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", session.getId());
        info.put("name", session.getName());
        info.put("description", session.getDescription());
        info.put("startTime", session.getStartTime());
        info.put("allowedMethods", session.getAllowedCheckInMethods());
        info.put("qrCode", session.getQrCode());
        return info;
    }

    private Map<String, Object> createAttendanceInfo(AttendanceLog log) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", log.getId());
        info.put("sessionName", log.getSession().getName());
        info.put("checkInTime", log.getCheckInTime());
        info.put("checkOutTime", log.getCheckOutTime());
        info.put("checkInMethod", log.getCheckInMethod() != null ? log.getCheckInMethod().toString() : "NFC");
        info.put("checkOutMethod", log.getCheckOutMethod() != null ? log.getCheckOutMethod().toString() : null);
        info.put("status", log.getCheckOutTime() != null ? "completed" : "active");
        return info;
    }

    private Map<String, Object> createScheduledSessionInfo(ScheduledSession session) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", session.getId());
        info.put("name", session.getName());
        info.put("description", session.getDescription());
        info.put("startTime", session.getStartTime());
        info.put("durationMinutes", session.getDurationMinutes());
        info.put("daysOfWeek", session.getDaysOfWeek());
        info.put("allowedMethods", session.getAllowedCheckInMethods());
        info.put("isActive", session.getActive());
        return info;
    }

    /**
     * Helper method to find session by QR code with flexible matching
     * Handles both Base64 encoded QR codes and raw decoded strings
     */
    private Optional<AttendanceSession> findSessionByQrCode(String qrCode) {
        logger.info("Finding session by QR code: '{}'", qrCode);

        // First try direct match (for raw QR codes)
        Optional<AttendanceSession> session = attendanceSessionRepository.findByQrCodeAndEndTimeIsNull(qrCode);
        logger.info("Direct match result: {}", session.isPresent() ? "FOUND" : "NOT_FOUND");

        if (session.isPresent()) {
            logger.info("Found session via direct match - ID: {}, Name: {}",
                    session.get().getId(), session.get().getName());
            return session;
        }

        // If the input looks like a URL (ams://checkin?qr=...), extract the QR parameter
        if (qrCode.startsWith("ams://checkin?qr=")) {
            String extractedQrCode = qrCode.substring("ams://checkin?qr=".length());
            logger.info("Extracted QR code from URL: '{}'", extractedQrCode);

            session = attendanceSessionRepository.findByQrCodeAndEndTimeIsNull(extractedQrCode);
            logger.info("URL extracted match result: {}", session.isPresent() ? "FOUND" : "NOT_FOUND");

            if (session.isPresent()) {
                logger.info("Found session via URL extraction - ID: {}, Name: {}",
                        session.get().getId(), session.get().getName());
                return session;
            }
        }

        // If not found, try Base64 encoding the input (in case DB stores encoded version)
        try {
            String encodedQrCode = Base64.getEncoder().encodeToString(qrCode.getBytes());
            logger.info("Trying Base64 encoded version: '{}'", encodedQrCode);
            session = attendanceSessionRepository.findByQrCodeAndEndTimeIsNull(encodedQrCode);
            logger.info("Encoded match result: {}", session.isPresent() ? "FOUND" : "NOT_FOUND");

            if (session.isPresent()) {
                logger.info("Found session via encoded match - ID: {}, Name: {}",
                        session.get().getId(), session.get().getName());
                return session;
            }
        } catch (Exception e) {
            logger.debug("Failed to encode QR code: {}", e.getMessage());
        }

        // If still not found, try Base64 decoding the input (in case input is encoded)
        try {
            String decodedQrCode = new String(Base64.getDecoder().decode(qrCode));
            logger.info("Trying Base64 decoded version: '{}'", decodedQrCode);
            session = attendanceSessionRepository.findByQrCodeAndEndTimeIsNull(decodedQrCode);
            logger.info("Decoded match result: {}", session.isPresent() ? "FOUND" : "NOT_FOUND");

            if (session.isPresent()) {
                logger.info("Found session via decoded match - ID: {}, Name: {}",
                        session.get().getId(), session.get().getName());
                return session;
            }
        } catch (Exception e) {
            logger.debug("Failed to decode QR code: {}", e.getMessage());
        }

        logger.warn("No session found for QR code: '{}'", qrCode);
        return Optional.empty();
    }

    /**
     * WiFi check-in for mobile app
     */
    @PostMapping("/mobile/checkin/wifi")
    public ResponseEntity<?> wifiCheckIn(@RequestBody Map<String, Object> request) {
        try {
            String mobileNumber = (String) request.get("mobileNumber");
            String entityId = (String) request.get("entityId");
            String wifiNetworkName = (String) request.get("wifiNetworkName");
            String deviceInfo = (String) request.get("deviceInfo");
            String sessionIdStr = (String) request.get("sessionId");

            if (mobileNumber == null || entityId == null || wifiNetworkName == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Mobile number, entity ID, and WiFi network name are required"));
            }

            Subscriber subscriber = subscriberRepository.findByMobileNumberAndOrganizationEntityId(mobileNumber, entityId)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found"));

            logger.info("WiFi Check-in attempt - Mobile: {}, EntityId: {}, Network: {}, SessionId: {}",
                       mobileNumber, entityId, wifiNetworkName, sessionIdStr);
            logger.info("Subscriber found - ID: {}, Name: {} {}, Org EntityId: {}",
                    subscriber.getId(), subscriber.getFirstName(), subscriber.getLastName(),
                    subscriber.getOrganization().getEntityId());

            AttendanceSession session;

            // If sessionId is provided, use that specific session
            if (sessionIdStr != null && !sessionIdStr.trim().isEmpty()) {
                try {
                    Long sessionId = Long.parseLong(sessionIdStr);
                    session = attendanceSessionRepository.findById(sessionId)
                            .orElseThrow(() -> new RuntimeException("Session not found with ID: " + sessionId));

                    // Verify the session belongs to the subscriber's organization
                    if (!session.getOrganization().getEntityId().equals(entityId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "Session does not belong to your organization"));
                    }

                    // Verify the session is active
                    if (session.getEndTime() != null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("error", "Session has already ended"));
                    }

                    logger.info("Using specific session - ID: {}, Name: {}", session.getId(), session.getName());
                } catch (NumberFormatException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Invalid session ID format"));
                }
            } else {
                // Find active session for the organization (original behavior)
                List<AttendanceSession> activeSessions = attendanceSessionRepository
                        .findByOrganizationAndEndTimeIsNull(subscriber.getOrganization());

                if (activeSessions.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "No active session found for WiFi check-in"));
                }

                session = activeSessions.get(0); // Get the first active session
                logger.info("Using first active session - ID: {}, Name: {}, Org EntityId: {}",
                        session.getId(), session.getName(), session.getOrganization().getEntityId());
            }

            // Check if WiFi is allowed for this session
            if (!session.getAllowedCheckInMethods().contains(CheckInMethod.WIFI)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "WiFi check-in not allowed for this session"));
            }

            // Validate WiFi network
            if (!isAuthorizedWifiNetwork(wifiNetworkName)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "error", "Not connected to authorized WiFi network",
                            "message", "Please connect to an authorized WiFi network to check in",
                            "networkName", wifiNetworkName
                        ));
            }

            // Check if subscriber is already checked in to ANY session (not just this one)
            Optional<AttendanceLog> anyActiveCheckIn = attendanceLogRepository
                    .findFirstBySubscriberAndCheckOutTimeIsNullOrderByCheckInTimeDesc(subscriber);

            // Check if already checked in to this specific session
            AttendanceLog existingLog = attendanceLogRepository
                    .findBySubscriberAndSessionAndCheckOutTimeIsNull(subscriber, session);

            // Also check if there's any attendance record for this subscriber-session combination
            List<AttendanceLog> allLogsForSession = attendanceLogRepository
                    .findBySessionAndSubscriber(session, subscriber);

            if (!allLogsForSession.isEmpty() && existingLog == null) {
                // Subscriber already has a completed attendance record for this session
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "You have already completed attendance for this session"));
            }

            // If subscriber is checked in to a different session, prevent new check-in
            if (anyActiveCheckIn.isPresent() && existingLog == null) {
                AttendanceLog activeLog = anyActiveCheckIn.get();
                if (!activeLog.getSession().getId().equals(session.getId())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of(
                                "error", "You are already checked in to session: " + activeLog.getSession().getName() +
                                        ". Please check out first before checking in to a new session.",
                                "currentSession", activeLog.getSession().getName(),
                                "currentSessionId", activeLog.getSession().getId()
                            ));
                }
            }

            if (existingLog != null) {
                // Check out
                existingLog.setCheckOutTime(LocalDateTime.now());
                existingLog.setCheckOutMethod(CheckInMethod.WIFI);
                attendanceLogRepository.save(existingLog);
                Map<String, Object> response = new HashMap<>();
                response.put("action", "CHECK_OUT");
                response.put("message", "Successfully checked out via WiFi");
                response.put("session", session.getName());
                response.put("time", existingLog.getCheckOutTime());
                response.put("checkInMethod", existingLog.getCheckInMethod().toString());
                response.put("checkOutMethod", "WiFi");
                response.put("networkName", wifiNetworkName);

                return ResponseEntity.ok(response);
            } else {
                // Check in
                AttendanceLog newLog = new AttendanceLog();
                newLog.setSubscriber(subscriber);
                newLog.setSession(session);
                newLog.setCheckInTime(LocalDateTime.now());
                newLog.setCheckInMethod(CheckInMethod.WIFI);
                newLog.setDeviceInfo(deviceInfo);
                newLog.setLocationInfo("WIFI:" + wifiNetworkName);
                attendanceLogRepository.save(newLog);

                Map<String, Object> response = new HashMap<>();
                response.put("action", "CHECK_IN");
                response.put("message", "Successfully checked in via WiFi");
                response.put("session", session.getName());
                response.put("time", newLog.getCheckInTime());
                response.put("checkInMethod", "WiFi");
                response.put("checkOutMethod", null);
                response.put("networkName", wifiNetworkName);

                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            logger.error("WiFi check-in failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Check-in failed: " + e.getMessage()));
        }
    }

    private Map<String, Object> getCurrentCheckInStatus(Subscriber subscriber) {
        try {
            // Find current active attendance (checked in but not checked out) - get the most recent one
            Optional<AttendanceLog> currentAttendance = attendanceLogRepository
                    .findFirstBySubscriberAndCheckOutTimeIsNullOrderByCheckInTimeDesc(subscriber);

            if (currentAttendance.isPresent()) {
                AttendanceLog log = currentAttendance.get();
                Map<String, Object> checkInStatus = new HashMap<>();
                checkInStatus.put("isCheckedIn", true);
                checkInStatus.put("sessionName", log.getSession().getName());
                checkInStatus.put("sessionId", log.getSession().getId());
                checkInStatus.put("checkInTime", log.getCheckInTime().toString());
                checkInStatus.put("checkInMethod", log.getCheckInMethod());

                logger.info("Found active check-in for subscriber {}: session {} ({})",
                    subscriber.getId(), log.getSession().getId(), log.getSession().getName());
                return checkInStatus;
            } else {
                logger.info("No active check-in found for subscriber {}", subscriber.getId());
                Map<String, Object> checkInStatus = new HashMap<>();
                checkInStatus.put("isCheckedIn", false);
                checkInStatus.put("sessionName", null);
                checkInStatus.put("sessionId", null);
                checkInStatus.put("checkInTime", null);
                checkInStatus.put("checkInMethod", null);
                return checkInStatus;
            }
        } catch (Exception e) {
            logger.error("Error getting current check-in status for subscriber {}: {}", subscriber.getId(), e.getMessage());
            // Return default "not checked in" status on error
            Map<String, Object> checkInStatus = new HashMap<>();
            checkInStatus.put("isCheckedIn", false);
            checkInStatus.put("sessionName", null);
            checkInStatus.put("sessionId", null);
            checkInStatus.put("checkInTime", null);
            checkInStatus.put("checkInMethod", null);
            return checkInStatus;
        }
    }

    private boolean isAuthorizedWifiNetwork(String networkName) {
        if (networkName == null || networkName.trim().isEmpty()) {
            return false;
        }

        // List of authorized network patterns for demo
        String[] authorizedPatterns = {
            "office", "company", "work", "corporate", "admin",
            "church", "school", "organization", "entity", "wifi",
            "test", "demo", "guest", "public", "home", "android"
        };

        String lowerNetworkName = networkName.toLowerCase();

        for (String pattern : authorizedPatterns) {
            if (lowerNetworkName.contains(pattern)) {
                return true;
            }
        }

        // For testing purposes, allow most reasonable network names
        // In production, you would have a strict whitelist per organization
        if (lowerNetworkName.length() > 2 && !lowerNetworkName.matches(".*[<>\"'&].*")) {
            logger.info("Allowing WiFi network '{}' for testing purposes", networkName);
            return true;
        }

        logger.warn("WiFi network '{}' not authorized", networkName);
        return false;
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "subscriber-service",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * mDNS service discovery information endpoint
     */
    @GetMapping("/discovery")
    public ResponseEntity<?> discoveryInfo() {
        try {
            Map<String, Object> discoveryInfo = mdnsService.getServiceInfo();
            discoveryInfo.put("timestamp", System.currentTimeMillis());
            discoveryInfo.put("serviceType", "_attendanceapi._tcp.local.");

            return ResponseEntity.ok(discoveryInfo);
        } catch (Exception e) {
            logger.error("Failed to get discovery info: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Test hostname resolution endpoint
     */
    @GetMapping("/test-hostname")
    public ResponseEntity<?> testHostname() {
        try {
            Map<String, Object> testResult = mdnsService.testHostnameResolution();
            testResult.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(testResult);
        } catch (Exception e) {
            logger.error("Failed to test hostname: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get mDNS troubleshooting information
     */
    @GetMapping("/mdns-troubleshoot")
    public ResponseEntity<?> mdnsTroubleshoot() {
        try {
            Map<String, Object> troubleshootInfo = mdnsService.getTroubleshootingInfo();
            troubleshootInfo.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(troubleshootInfo);
        } catch (Exception e) {
            logger.error("Failed to get troubleshooting info: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get manual hosts file configuration
     */
    @GetMapping("/hosts-config")
    public ResponseEntity<?> hostsConfig() {
        try {
            Map<String, Object> hostsInfo = mdnsService.addToHostsFile();
            hostsInfo.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(hostsInfo);
        } catch (Exception e) {
            logger.error("Failed to get hosts config: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Restart mDNS services (useful after network changes)
     */
    @PostMapping("/restart-mdns")
    public ResponseEntity<?> restartMdns() {
        try {
            logger.info("Manual mDNS restart requested");
            Map<String, Object> result = mdnsService.restartService();
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to restart mDNS: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get current network status and mDNS information
     */
    @GetMapping("/network-status")
    public ResponseEntity<?> networkStatus() {
        try {
            Map<String, Object> status = mdnsService.getNetworkStatus();
            status.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Failed to get network status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get comprehensive Windows mDNS diagnostics
     */
    @GetMapping("/windows-diagnostics")
    public ResponseEntity<?> windowsDiagnostics() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            if (!osName.contains("windows")) {
                return ResponseEntity.ok(Map.of(
                    "message", "This endpoint is for Windows diagnostics only",
                    "currentOS", osName,
                    "timestamp", System.currentTimeMillis()
                ));
            }

            Map<String, Object> diagnostics = mdnsService.getWindowsDiagnostics();
            diagnostics.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(diagnostics);
        } catch (Exception e) {
            logger.error("Failed to get Windows diagnostics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
