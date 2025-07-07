package com.example.attendancesystem.attendance.controller;

import com.example.attendancesystem.attendance.dto.CheckInRequestDto;
import com.example.attendancesystem.attendance.model.*;
import com.example.attendancesystem.attendance.repository.*;
import com.example.attendancesystem.attendance.service.QrCodeService;
// Removed cross-service dependencies for microservices independence
// import com.example.attendancesystem.attendance.client.UserServiceGrpcClient;
// import com.example.attendancesystem.attendance.client.OrganizationServiceGrpcClient;
// import com.example.attendancesystem.attendance.dto.UserDto;
// import com.example.attendancesystem.attendance.dto.OrganizationDto;
import com.example.attendancesystem.attendance.security.JwtUtil;
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

    // Removed cross-service dependencies for microservices independence
    // @Autowired
    // private UserServiceGrpcClient userServiceGrpcClient;

    // @Autowired
    // private OrganizationServiceGrpcClient organizationServiceGrpcClient;

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private JwtUtil jwtUtil;

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
            // For microservices independence, use subscriberId directly without cross-service validation
            if (subscriberId == null) {
                throw new IllegalArgumentException("User ID not found in token");
            }

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

            return processCheckIn(subscriberId, session, CheckInMethod.QR, request);

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
            // For microservices independence, use subscriberId directly
            if (subscriberId == null) {
                throw new IllegalArgumentException("User ID not found in token");
            }

            // Extract organization ID from token or use a default approach
            Long organizationId = extractOrganizationIdFromToken(authHeader);
            if (organizationId == null) {
                throw new IllegalArgumentException("Organization ID not found in token");
            }
            AttendanceSession session = findActiveSessionForOrganizationId(organizationId);
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

            return processCheckIn(subscriberId, session, CheckInMethod.BLUETOOTH, request);

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
            // For microservices independence, use subscriberId directly
            if (subscriberId == null) {
                throw new IllegalArgumentException("User ID not found in token");
            }

            // Extract organization ID from token
            Long organizationId = extractOrganizationIdFromToken(authHeader);
            if (organizationId == null) {
                throw new IllegalArgumentException("Organization ID not found in token");
            }
            AttendanceSession session = findActiveSessionForOrganizationId(organizationId);
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

            return processCheckIn(subscriberId, session, CheckInMethod.WIFI, request);

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
            // For microservices independence, use subscriberId directly
            if (subscriberId == null) {
                throw new IllegalArgumentException("User ID not found in token");
            }

            // Extract organization ID from token
            Long organizationId = extractOrganizationIdFromToken(authHeader);
            if (organizationId == null) {
                throw new IllegalArgumentException("Organization ID not found in token");
            }
            AttendanceSession session = findActiveSessionForOrganizationId(organizationId);
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

            return processCheckIn(subscriberId, session, CheckInMethod.MOBILE_NFC, request);

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
                if (organizationId == null) {
                    throw new IllegalArgumentException("Organization ID not found in token");
                }
                activeSessions = attendanceSessionRepository
                        .findByOrganizationIdAndEndTimeIsNullAndStartTimeBefore(organizationId, LocalDateTime.now());
            } else {
                // Member/Subscriber can see sessions for their organization
                Long subscriberId = extractSubscriberIdFromToken(authHeader);
                if (subscriberId == null) {
                    throw new IllegalArgumentException("User ID not found in token");
                }
                // Extract organization ID from token for members
                Long organizationId = extractOrganizationIdFromToken(authHeader);
                if (organizationId == null) {
                    throw new IllegalArgumentException("Organization ID not found in token");
                }
                activeSessions = attendanceSessionRepository
                        .findByOrganizationIdAndEndTimeIsNullAndStartTimeBefore(organizationId, LocalDateTime.now());
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

    private ResponseEntity<?> processCheckIn(Long subscriberId, AttendanceSession session,
                                           CheckInMethod method, CheckInRequestDto request) {

        // Check if already checked in
        Optional<AttendanceLog> existingLog = attendanceLogRepository
                .findByUserIdAndSessionId(subscriberId, session.getId());

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

        // New check-in - simplified for microservices independence
        AttendanceLog newLog = new AttendanceLog(
            subscriberId,
            "User-" + subscriberId, // Simplified name for independence
            "N/A", // Mobile number not available for independence
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

    // Updated for microservices independence - uses organizationId directly
    private AttendanceSession findActiveSessionForOrganizationId(Long organizationId) {
        List<AttendanceSession> activeSessions = attendanceSessionRepository
                .findByOrganizationIdAndEndTimeIsNullAndStartTimeBefore(organizationId, LocalDateTime.now());

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
        try {
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            if (token != null && jwtUtil.isValidToken(token)) {
                return jwtUtil.extractSubscriberId(token);
            }
            logger.warn("Invalid or missing JWT token for subscriber ID extraction");
            return null;
        } catch (Exception e) {
            logger.error("Error extracting subscriber ID from token: {}", e.getMessage());
            return null;
        }
    }

    private String extractUserTypeFromToken(String authHeader) {
        try {
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            if (token != null && jwtUtil.isValidToken(token)) {
                String userType = jwtUtil.extractUserType(token);
                return userType != null ? userType : "MEMBER"; // Default to MEMBER if not found
            }
            logger.warn("Invalid or missing JWT token for user type extraction");
            return "MEMBER"; // Default fallback
        } catch (Exception e) {
            logger.error("Error extracting user type from token: {}", e.getMessage());
            return "MEMBER"; // Default fallback
        }
    }

    private Long extractOrganizationIdFromToken(String authHeader) {
        try {
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            if (token != null && jwtUtil.isValidToken(token)) {
                return jwtUtil.extractOrganizationId(token);
            }
            logger.warn("Invalid or missing JWT token for organization ID extraction");
            return null;
        } catch (Exception e) {
            logger.error("Error extracting organization ID from token: {}", e.getMessage());
            return null;
        }
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

