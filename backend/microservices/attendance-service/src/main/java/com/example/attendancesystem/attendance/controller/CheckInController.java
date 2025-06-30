package com.example.attendancesystem.attendance.controller;

import com.example.attendancesystem.attendance.dto.CheckInRequestDto;
import com.example.attendancesystem.shared.model.*;
import com.example.attendancesystem.shared.repository.*;
import com.example.attendancesystem.attendance.service.QrCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/subscriber/checkin")
public class CheckInController {

    private static final Logger logger = LoggerFactory.getLogger(CheckInController.class);

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private QrCodeService qrCodeService;

    /**
     * QR Code check-in
     */
    @PostMapping("/qr")
    @Transactional
    public ResponseEntity<?> qrCheckIn(@RequestBody CheckInRequestDto request,
                                      @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("QR check-in request received");

            // Extract subscriber from JWT token (simplified for now)
            Long subscriberId = extractSubscriberIdFromToken(authHeader);
            Subscriber subscriber = subscriberRepository.findById(subscriberId)
                    .orElseThrow(() -> new IllegalArgumentException("Subscriber not found"));

            // Find session by QR code
            AttendanceSession session = findSessionByQrCode(request.getQrCode());
            if (session == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid or expired QR code"));
            }

            // Validate QR code
            if (!qrCodeService.validateQrCode(request.getQrCode(), session)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "QR code validation failed"));
            }

            // Check if QR is allowed for this session
            if (!session.getAllowedCheckInMethods().contains(CheckInMethod.QR)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "QR check-in not allowed for this session"));
            }

            return processCheckIn(subscriber, session, CheckInMethod.QR, request);

        } catch (Exception e) {
            logger.error("QR check-in failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Check-in failed: " + e.getMessage()));
        }
    }

    /**
     * Bluetooth proximity check-in
     */
    @PostMapping("/bluetooth")
    @Transactional
    public ResponseEntity<?> bluetoothCheckIn(@RequestBody CheckInRequestDto request,
                                             @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("Bluetooth check-in request received");

            Long subscriberId = extractSubscriberIdFromToken(authHeader);
            Subscriber subscriber = subscriberRepository.findById(subscriberId)
                    .orElseThrow(() -> new IllegalArgumentException("Subscriber not found"));

            // Find active session for the organization
            AttendanceSession session = findActiveSessionForOrganization(subscriber.getOrganization());
            if (session == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No active session found"));
            }

            // Check if Bluetooth is allowed for this session
            if (!session.getAllowedCheckInMethods().contains(CheckInMethod.BLUETOOTH)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Bluetooth check-in not allowed for this session"));
            }

            // Validate Bluetooth proximity (simplified)
            if (!validateBluetoothProximity(request)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Not in proximity of Bluetooth beacon"));
            }

            return processCheckIn(subscriber, session, CheckInMethod.BLUETOOTH, request);

        } catch (Exception e) {
            logger.error("Bluetooth check-in failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Check-in failed: " + e.getMessage()));
        }
    }

    /**
     * WiFi network check-in
     */
    @PostMapping("/wifi")
    @Transactional
    public ResponseEntity<?> wifiCheckIn(@RequestBody CheckInRequestDto request,
                                        @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("WiFi check-in request received");

            Long subscriberId = extractSubscriberIdFromToken(authHeader);
            Subscriber subscriber = subscriberRepository.findById(subscriberId)
                    .orElseThrow(() -> new IllegalArgumentException("Subscriber not found"));

            // Find active session for the organization
            AttendanceSession session = findActiveSessionForOrganization(subscriber.getOrganization());
            if (session == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No active session found"));
            }

            // Check if WiFi is allowed for this session
            if (!session.getAllowedCheckInMethods().contains(CheckInMethod.WIFI)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "WiFi check-in not allowed for this session"));
            }

            // Validate WiFi network (simplified)
            if (!validateWifiNetwork(request)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Not connected to authorized WiFi network"));
            }

            return processCheckIn(subscriber, session, CheckInMethod.WIFI, request);

        } catch (Exception e) {
            logger.error("WiFi check-in failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Check-in failed: " + e.getMessage()));
        }
    }

    /**
     * Mobile NFC check-in
     */
    @PostMapping("/mobile-nfc")
    @Transactional
    public ResponseEntity<?> mobileNfcCheckIn(@RequestBody CheckInRequestDto request,
                                             @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("Mobile NFC check-in request received");

            Long subscriberId = extractSubscriberIdFromToken(authHeader);
            Subscriber subscriber = subscriberRepository.findById(subscriberId)
                    .orElseThrow(() -> new IllegalArgumentException("Subscriber not found"));

            // Find active session for the organization
            AttendanceSession session = findActiveSessionForOrganization(subscriber.getOrganization());
            if (session == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No active session found"));
            }

            // Check if Mobile NFC is allowed for this session
            if (!session.getAllowedCheckInMethods().contains(CheckInMethod.MOBILE_NFC)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Mobile NFC check-in not allowed for this session"));
            }

            // Validate NFC data (simplified)
            if (!validateMobileNfcData(request)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid NFC data"));
            }

            return processCheckIn(subscriber, session, CheckInMethod.MOBILE_NFC, request);

        } catch (Exception e) {
            logger.error("Mobile NFC check-in failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Check-in failed: " + e.getMessage()));
        }
    }

    /**
     * Get available sessions for check-in
     * Accessible by all user types: SuperAdmin, EntityAdmin, and Members
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getAvailableSessions(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user information from token (supports all user types)
            String userType = extractUserTypeFromToken(authHeader);

            List<AttendanceSession> activeSessions;

            if ("SUPER_ADMIN".equals(userType)) {
                // SuperAdmin can see all active sessions
                activeSessions = attendanceSessionRepository
                        .findByEndTimeIsNullAndStartTimeBefore(LocalDateTime.now());
            } else if ("ENTITY_ADMIN".equals(userType)) {
                // EntityAdmin can see sessions for their organization
                Long organizationId = extractOrganizationIdFromToken(authHeader);
                Organization organization = organizationRepository.findById(organizationId)
                        .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
                activeSessions = attendanceSessionRepository
                        .findByOrganizationAndEndTimeIsNullAndStartTimeBefore(organization, LocalDateTime.now());
            } else {
                // Member/Subscriber can see sessions for their organization
                Long subscriberId = extractSubscriberIdFromToken(authHeader);
                Subscriber subscriber = subscriberRepository.findById(subscriberId)
                        .orElseThrow(() -> new IllegalArgumentException("Subscriber not found"));
                activeSessions = attendanceSessionRepository
                        .findByOrganizationAndEndTimeIsNullAndStartTimeBefore(
                                subscriber.getOrganization(), LocalDateTime.now());
            }

            return ResponseEntity.ok(Map.of(
                    "sessions", activeSessions.stream().map(this::convertSessionToDto).toList(),
                    "count", activeSessions.size(),
                    "userType", userType
            ));

        } catch (Exception e) {
            logger.error("Failed to get available sessions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get sessions: " + e.getMessage()));
        }
    }

    // Helper methods

    private ResponseEntity<?> processCheckIn(Subscriber subscriber, AttendanceSession session, 
                                           CheckInMethod method, CheckInRequestDto request) {
        
        // Check if already checked in
        Optional<AttendanceLog> existingLog = attendanceLogRepository
                .findByUserIdAndSession(subscriber.getId(), session);

        if (existingLog.isPresent()) {
            AttendanceLog log = existingLog.get();
            if (log.getCheckOutTime() == null) {
                // Check out
                log.setCheckOutTime(LocalDateTime.now());
                log.setCheckOutMethod(method); // Set the checkout method
                attendanceLogRepository.save(log);

                return ResponseEntity.ok(Map.of(
                        "action", "CHECK_OUT",
                        "message", "Checked out successfully",
                        "session", session.getName(),
                        "time", log.getCheckOutTime(),
                        "method", method.getDisplayName()
                ));
            }
        }

        // New check-in
        AttendanceLog newLog = new AttendanceLog(
            subscriber.getId(),
            subscriber.getFirstName() + " " + subscriber.getLastName(),
            subscriber.getMobileNumber(),
            session,
            LocalDateTime.now(),
            method
        );
        newLog.setDeviceInfo(request.getDeviceInfo());
        newLog.setLocationInfo(request.getLocationInfo());
        
        AttendanceLog savedLog = attendanceLogRepository.save(newLog);
        
        return ResponseEntity.ok(Map.of(
                "action", "CHECK_IN",
                "message", "Checked in successfully",
                "session", session.getName(),
                "time", savedLog.getCheckInTime(),
                "method", method.getDisplayName()
        ));
    }

    private AttendanceSession findSessionByQrCode(String qrCode) {
        // Find session with matching QR code
        return attendanceSessionRepository.findAll().stream()
                .filter(session -> qrCode.equals(session.getQrCode()) && session.isQrCodeValid())
                .findFirst()
                .orElse(null);
    }

    private AttendanceSession findActiveSessionForOrganization(Organization organization) {
        List<AttendanceSession> activeSessions = attendanceSessionRepository
                .findByOrganizationAndEndTimeIsNullAndStartTimeBefore(organization, LocalDateTime.now());
        
        return activeSessions.isEmpty() ? null : activeSessions.get(0);
    }

    private boolean validateBluetoothProximity(CheckInRequestDto request) {
        // Simplified validation - in real implementation, check beacon proximity
        return request.getLocationInfo() != null && 
               request.getLocationInfo().contains("BEACON");
    }

    private boolean validateWifiNetwork(CheckInRequestDto request) {
        // Enhanced WiFi validation
        if (request.getLocationInfo() == null || request.getLocationInfo().trim().isEmpty()) {
            logger.warn("WiFi validation failed: No network information provided");
            return false;
        }

        String networkInfo = request.getLocationInfo().trim();
        logger.info("Validating WiFi network: {}", networkInfo);

        // Check if the request contains network information
        if (networkInfo.startsWith("WIFI:")) {
            String networkName = networkInfo.substring(5); // Remove "WIFI:" prefix

            // For demo purposes, we'll validate based on network name patterns
            // In production, you would check against a list of authorized networks
            boolean isAuthorized = isAuthorizedWifiNetwork(networkName);

            if (isAuthorized) {
                logger.info("WiFi network '{}' is authorized for check-in", networkName);
                return true;
            } else {
                logger.warn("WiFi network '{}' is not authorized for check-in", networkName);
                return false;
            }
        }

        logger.warn("WiFi validation failed: Invalid network information format");
        return false;
    }

    private boolean isAuthorizedWifiNetwork(String networkName) {
        // List of authorized network patterns for demo
        // In production, this would be stored in database per organization
        String[] authorizedPatterns = {
            "office", "company", "work", "corporate", "admin",
            "church", "school", "organization", "entity"
        };

        String lowerNetworkName = networkName.toLowerCase();

        for (String pattern : authorizedPatterns) {
            if (lowerNetworkName.contains(pattern)) {
                return true;
            }
        }

        // Also allow networks that match the organization name pattern
        // This is a simplified check - in production you'd have proper network registration
        return lowerNetworkName.length() > 3; // Basic validation
    }

    private boolean validateMobileNfcData(CheckInRequestDto request) {
        // Simplified validation - in real implementation, validate NFC tag data
        return request.getNfcData() != null && 
               request.getNfcData().length() > 0;
    }

    private Long extractSubscriberIdFromToken(String authHeader) {
        // Simplified token extraction - in real implementation, use JWT validation
        // For now, return a dummy subscriber ID
        return 1L;
    }

    private String extractUserTypeFromToken(String authHeader) {
        // Simplified token extraction - in real implementation, decode JWT and extract user type
        // For now, return MEMBER as default for testing
        // TODO: Implement proper JWT token parsing to extract tokenType claim
        return "MEMBER";
    }

    private Long extractOrganizationIdFromToken(String authHeader) {
        // Simplified token extraction - in real implementation, decode JWT and extract organization ID
        // For now, return a dummy organization ID
        // TODO: Implement proper JWT token parsing to extract organizationId claim
        return 1L;
    }

    private Map<String, Object> convertSessionToDto(AttendanceSession session) {
        return Map.of(
                "id", session.getId(),
                "name", session.getName(),
                "description", session.getDescription() != null ? session.getDescription() : "",
                "startTime", session.getStartTime(),
                "allowedMethods", session.getAllowedCheckInMethods()
        );
    }
}
