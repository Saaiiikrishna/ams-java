package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.SubscriberLoginDto;
import com.example.attendancesystem.service.SubscriberAuthService;
import com.example.attendancesystem.model.*;
import com.example.attendancesystem.repository.*;
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

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("subscriber", createSubscriberInfo(subscriber));
            dashboard.put("organization", createOrganizationInfo(organization));
            dashboard.put("activeSessions", activeSessions.stream().map(this::createSessionInfo).collect(Collectors.toList()));
            dashboard.put("recentAttendance", recentAttendance.stream().map(this::createAttendanceInfo).collect(Collectors.toList()));
            dashboard.put("upcomingSessions", upcomingSessions.stream().map(this::createScheduledSessionInfo).collect(Collectors.toList()));

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

            logger.info("Session found - ID: {}, Name: {}, Org EntityId: {}",
                    session.getId(), session.getName(), session.getOrganization().getEntityId());

            // Check if session belongs to subscriber's organization
            if (!session.getOrganization().getEntityId().equals(entityId)) {
                logger.error("Entity ID mismatch - Subscriber EntityId: {}, Session EntityId: {}, Request EntityId: {}",
                        subscriber.getOrganization().getEntityId(), session.getOrganization().getEntityId(), entityId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "QR code does not belong to your organization"));
            }

            // Check if already checked in to this session
            AttendanceLog existingLog = attendanceLogRepository
                    .findBySubscriberAndSessionAndCheckOutTimeIsNull(subscriber, session);

            // Also check if there's any attendance record for this subscriber-session combination
            List<AttendanceLog> allLogsForSession = attendanceLogRepository
                    .findBySubscriberAndSession(subscriber, session);

            if (!allLogsForSession.isEmpty() && existingLog == null) {
                // Subscriber already has a completed attendance record for this session
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "You have already completed attendance for this session"));
            }

            if (existingLog != null) {
                // Check out
                existingLog.setCheckOutTime(LocalDateTime.now());
                existingLog.setCheckOutMethod(CheckInMethod.QR);
                attendanceLogRepository.save(existingLog);
                return ResponseEntity.ok(Map.of(
                        "action", "CHECK_OUT",
                        "message", "Successfully checked out",
                        "session", session.getName(),
                        "time", existingLog.getCheckOutTime(),
                        "checkInMethod", existingLog.getCheckInMethod().toString(),
                        "checkOutMethod", "QR"
                ));
            } else {
                // Check in
                AttendanceLog newLog = new AttendanceLog();
                newLog.setSubscriber(subscriber);
                newLog.setSession(session);
                newLog.setCheckInTime(LocalDateTime.now());
                newLog.setCheckInMethod(CheckInMethod.QR);
                attendanceLogRepository.save(newLog);

                return ResponseEntity.ok(Map.of(
                        "action", "CHECK_IN",
                        "message", "Successfully checked in",
                        "session", session.getName(),
                        "time", newLog.getCheckInTime(),
                        "checkInMethod", "QR",
                        "checkOutMethod", (String) null
                ));
            }

        } catch (Exception e) {
            logger.error("QR check-in failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
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
        // First try direct match (for raw QR codes)
        Optional<AttendanceSession> session = attendanceSessionRepository.findByQrCodeAndEndTimeIsNull(qrCode);

        if (session.isPresent()) {
            return session;
        }

        // If not found, try Base64 encoding the input (in case DB stores encoded version)
        try {
            String encodedQrCode = Base64.getEncoder().encodeToString(qrCode.getBytes());
            session = attendanceSessionRepository.findByQrCodeAndEndTimeIsNull(encodedQrCode);

            if (session.isPresent()) {
                return session;
            }
        } catch (Exception e) {
            logger.debug("Failed to encode QR code: {}", e.getMessage());
        }

        // If still not found, try Base64 decoding the input (in case input is encoded)
        try {
            String decodedQrCode = new String(Base64.getDecoder().decode(qrCode));
            session = attendanceSessionRepository.findByQrCodeAndEndTimeIsNull(decodedQrCode);

            if (session.isPresent()) {
                return session;
            }
        } catch (Exception e) {
            logger.debug("Failed to decode QR code: {}", e.getMessage());
        }

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

            if (mobileNumber == null || entityId == null || wifiNetworkName == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Mobile number, entity ID, and WiFi network name are required"));
            }

            Subscriber subscriber = subscriberRepository.findByMobileNumberAndOrganizationEntityId(mobileNumber, entityId)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found"));

            logger.info("WiFi Check-in attempt - Mobile: {}, EntityId: {}, Network: {}",
                       mobileNumber, entityId, wifiNetworkName);
            logger.info("Subscriber found - ID: {}, Name: {} {}, Org EntityId: {}",
                    subscriber.getId(), subscriber.getFirstName(), subscriber.getLastName(),
                    subscriber.getOrganization().getEntityId());

            // Find active session for the organization
            List<AttendanceSession> activeSessions = attendanceSessionRepository
                    .findByOrganizationAndEndTimeIsNull(subscriber.getOrganization());

            if (activeSessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No active session found for WiFi check-in"));
            }

            AttendanceSession session = activeSessions.get(0); // Get the first active session
            logger.info("Active session found - ID: {}, Name: {}, Org EntityId: {}",
                    session.getId(), session.getName(), session.getOrganization().getEntityId());

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

            // Check if already checked in to this session
            AttendanceLog existingLog = attendanceLogRepository
                    .findBySubscriberAndSessionAndCheckOutTimeIsNull(subscriber, session);

            // Also check if there's any attendance record for this subscriber-session combination
            List<AttendanceLog> allLogsForSession = attendanceLogRepository
                    .findBySubscriberAndSession(subscriber, session);

            if (!allLogsForSession.isEmpty() && existingLog == null) {
                // Subscriber already has a completed attendance record for this session
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "You have already completed attendance for this session"));
            }

            if (existingLog != null) {
                // Check out
                existingLog.setCheckOutTime(LocalDateTime.now());
                existingLog.setCheckOutMethod(CheckInMethod.WIFI);
                attendanceLogRepository.save(existingLog);
                return ResponseEntity.ok(Map.of(
                        "action", "CHECK_OUT",
                        "message", "Successfully checked out via WiFi",
                        "session", session.getName(),
                        "time", existingLog.getCheckOutTime(),
                        "checkInMethod", existingLog.getCheckInMethod().toString(),
                        "checkOutMethod", "WiFi",
                        "networkName", wifiNetworkName
                ));
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

                return ResponseEntity.ok(Map.of(
                        "action", "CHECK_IN",
                        "message", "Successfully checked in via WiFi",
                        "session", session.getName(),
                        "time", newLog.getCheckInTime(),
                        "checkInMethod", "WiFi",
                        "checkOutMethod", (String) null,
                        "networkName", wifiNetworkName
                ));
            }

        } catch (Exception e) {
            logger.error("WiFi check-in failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Check-in failed: " + e.getMessage()));
        }
    }

    private boolean isAuthorizedWifiNetwork(String networkName) {
        if (networkName == null || networkName.trim().isEmpty()) {
            return false;
        }

        // List of authorized network patterns for demo
        String[] authorizedPatterns = {
            "office", "company", "work", "corporate", "admin",
            "church", "school", "organization", "entity", "wifi"
        };

        String lowerNetworkName = networkName.toLowerCase();

        for (String pattern : authorizedPatterns) {
            if (lowerNetworkName.contains(pattern)) {
                return true;
            }
        }

        // Also allow networks that are reasonably named (not random)
        return lowerNetworkName.length() > 3 && !lowerNetworkName.matches(".*\\d{4,}.*");
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
}
