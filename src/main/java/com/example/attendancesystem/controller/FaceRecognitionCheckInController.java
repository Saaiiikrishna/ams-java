package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.FaceRecognitionDto;
import com.example.attendancesystem.facerecognition.FaceRecognitionResult;
import com.example.attendancesystem.model.*;
import com.example.attendancesystem.repository.*;
import com.example.attendancesystem.service.*;
import com.example.attendancesystem.util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for face recognition check-in operations
 * Handles face-based attendance recording with real-time recognition
 */
@RestController
@RequestMapping("/api/checkin")
@CrossOrigin(origins = "*")
public class FaceRecognitionCheckInController {
    
    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionCheckInController.class);
    
    @Autowired
    private FaceRecognitionService faceRecognitionService;
    

    
    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;
    
    @Autowired
    private SubscriberRepository subscriberRepository;
    
    @Autowired
    private AttendanceLogRepository attendanceLogRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    /**
     * Face recognition check-in endpoint
     */
    @PostMapping("/face")
    public ResponseEntity<Map<String, Object>> faceRecognitionCheckIn(
            @Valid @RequestBody FaceRecognitionDto request,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Face recognition check-in request - Entity: {}, Session: {}", 
                       entityId, request.getSessionId());
            
            // Validate request
            if (request.getBase64Image() == null || request.getBase64Image().isEmpty()) {
                response.put("success", false);
                response.put("message", "Face image is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (request.getSessionId() == null) {
                response.put("success", false);
                response.put("message", "Session ID is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verify session exists and is active
            Optional<AttendanceSession> sessionOpt = attendanceSessionRepository
                .findByIdAndOrganizationEntityId(request.getSessionId(), entityId);
            
            if (sessionOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Session not found or access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            AttendanceSession session = sessionOpt.get();
            
            // Check if session is active
            if (!isSessionActive(session)) {
                response.put("success", false);
                response.put("message", "Session is not currently active");
                response.put("sessionStatus", getSessionStatus(session));
                return ResponseEntity.badRequest().body(response);
            }
            
            // Convert Base64 to byte array
            byte[] imageData = decodeBase64Image(request.getBase64Image());
            if (imageData == null) {
                response.put("success", false);
                response.put("message", "Invalid image format");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Perform face recognition
            FaceRecognitionResult recognitionResult = faceRecognitionService
                .recognizeFace(imageData, entityId);
            
            if (!recognitionResult.isSuccess()) {
                // Log failed recognition attempt
                faceRecognitionService.logRecognitionAttempt(
                    recognitionResult, session, null, request.getDeviceInfo());
                
                response.put("success", false);
                response.put("message", "Face recognition failed: " + recognitionResult.getErrorMessage());
                response.put("processingTimeMs", recognitionResult.getProcessingTimeMs());
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            }
            
            if (!recognitionResult.isMatched()) {
                // Log unrecognized face attempt
                faceRecognitionService.logRecognitionAttempt(
                    recognitionResult, session, null, request.getDeviceInfo());
                
                response.put("success", false);
                response.put("message", "Face not recognized. Please ensure you are registered for face recognition.");
                response.put("confidenceScore", recognitionResult.getConfidenceScore());
                response.put("processingTimeMs", recognitionResult.getProcessingTimeMs());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Get recognized subscriber
            Subscriber subscriber = subscriberRepository.findById(recognitionResult.getMatchedSubscriberId())
                .orElseThrow(() -> new IllegalStateException("Recognized subscriber not found"));
            
            // Check if subscriber is already checked in to this session
            AttendanceLog existingLog = attendanceLogRepository
                .findBySubscriberAndSessionAndCheckOutTimeIsNull(subscriber, session);
            boolean alreadyCheckedIn = existingLog != null;
            
            if (alreadyCheckedIn) {
                // Handle check-out
                return handleFaceRecognitionCheckOut(subscriber, session, recognitionResult, 
                                                   request, response, startTime);
            } else {
                // Handle check-in
                return handleFaceRecognitionCheckIn(subscriber, session, recognitionResult, 
                                                  request, response, startTime, imageData);
            }
            
        } catch (Exception e) {
            logger.error("Face recognition check-in failed", e);
            response.put("success", false);
            response.put("message", "Face recognition check-in error: " + e.getMessage());
            response.put("processingTimeMs", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get face recognition check-in statistics for a session
     */
    @GetMapping("/face/stats/{sessionId}")
    public ResponseEntity<Map<String, Object>> getFaceRecognitionStats(
            @PathVariable Long sessionId,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Face recognition stats request - Session: {}, Entity: {}", sessionId, entityId);
            
            // Verify session
            Optional<AttendanceSession> sessionOpt = attendanceSessionRepository
                .findByIdAndOrganizationEntityId(sessionId, entityId);
            
            if (sessionOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Session not found or access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            AttendanceSession session = sessionOpt.get();
            
            // Get face recognition statistics
            List<AttendanceLog> faceCheckIns = attendanceLogRepository
                .findBySessionAndCheckInMethod(session, CheckInMethod.FACE_RECOGNITION);

            List<AttendanceLog> faceCheckOuts = attendanceLogRepository
                .findBySessionAndCheckOutMethod(session, CheckInMethod.FACE_RECOGNITION);
            
            // Get total registered subscribers for this entity
            long totalRegisteredFaces = subscriberRepository
                .countByOrganizationEntityIdAndFaceEncodingIsNotNull(entityId);
            
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("sessionName", session.getName());
            response.put("faceCheckIns", faceCheckIns.size());
            response.put("faceCheckOuts", faceCheckOuts.size());
            response.put("totalRegisteredFaces", totalRegisteredFaces);
            response.put("faceRecognitionEnabled", faceRecognitionService.isFaceRecognitionAvailable());
            
            // Detailed check-in information
            List<Map<String, Object>> checkInDetails = faceCheckIns.stream()
                .map(record -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("subscriberId", record.getSubscriber().getId());
                    detail.put("subscriberName", record.getSubscriber().getFirstName() + " " + 
                                               record.getSubscriber().getLastName());
                    detail.put("checkInTime", record.getCheckInTime());
                    detail.put("checkOutTime", record.getCheckOutTime());
                    detail.put("isCheckedOut", record.getCheckOutTime() != null);
                    return detail;
                })
                .toList();
            
            response.put("checkInDetails", checkInDetails);
            
            logger.info("Face recognition stats retrieved - Session: {}, Face check-ins: {}, Face check-outs: {}", 
                       sessionId, faceCheckIns.size(), faceCheckOuts.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get face recognition stats", e);
            response.put("success", false);
            response.put("message", "Error retrieving face recognition stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get face recognition logs for debugging and audit
     */
    @GetMapping("/face/logs/{sessionId}")
    public ResponseEntity<Map<String, Object>> getFaceRecognitionLogs(
            @PathVariable Long sessionId,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Face recognition logs request - Session: {}, Entity: {}", sessionId, entityId);
            
            // Verify session
            Optional<AttendanceSession> sessionOpt = attendanceSessionRepository
                .findByIdAndOrganizationEntityId(sessionId, entityId);
            
            if (sessionOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Session not found or access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            AttendanceSession session = sessionOpt.get();
            
            // Get face recognition logs for this session
            List<FaceRecognitionLog> logs = faceRecognitionService.getRecognitionLogsForSession(session);
            
            // Convert to response format
            List<Map<String, Object>> logDetails = logs.stream()
                .map(log -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("id", log.getId());
                    detail.put("timestamp", log.getRecognitionTimestamp());
                    detail.put("status", log.getRecognitionStatus());
                    detail.put("processingTimeMs", log.getProcessingTimeMs());
                    detail.put("deviceInfo", log.getDeviceInfo());
                    detail.put("errorMessage", log.getErrorMessage());
                    
                    if (log.getSubscriber() != null) {
                        detail.put("subscriberId", log.getSubscriber().getId());
                        detail.put("subscriberName", log.getSubscriber().getFirstName() + " " + 
                                                   log.getSubscriber().getLastName());
                    }
                    
                    if (log.getConfidenceScore() != null) {
                        detail.put("confidenceScore", log.getConfidenceScore());
                    }
                    
                    return detail;
                })
                .toList();
            
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("sessionName", session.getName());
            response.put("totalLogs", logs.size());
            response.put("logs", logDetails);
            
            logger.info("Face recognition logs retrieved - Session: {}, Total logs: {}", 
                       sessionId, logs.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get face recognition logs", e);
            response.put("success", false);
            response.put("message", "Error retrieving face recognition logs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Handle face recognition check-in
     */
    private ResponseEntity<Map<String, Object>> handleFaceRecognitionCheckIn(
            Subscriber subscriber, AttendanceSession session, FaceRecognitionResult recognitionResult,
            FaceRecognitionDto request, Map<String, Object> response, long startTime, byte[] imageData) {

        try {
            // Create attendance log
            AttendanceLog log = new AttendanceLog();
            log.setSubscriber(subscriber);
            log.setSession(session);
            log.setCheckInTime(LocalDateTime.now());
            log.setCheckInMethod(CheckInMethod.FACE_RECOGNITION);
            log.setDeviceInfo(request.getDeviceInfo());

            // Save attendance log
            attendanceLogRepository.save(log);

            // Log successful recognition
            faceRecognitionService.logRecognitionAttempt(
                recognitionResult, session, subscriber, request.getDeviceInfo());

            // Store face image for audit (optional)
            String auditImagePath = null;
            try {
                auditImagePath = fileStorageService.storeFaceRecognitionImage(
                    imageData, subscriber.getId(), "checkin_" + session.getId());
            } catch (Exception e) {
                logger.warn("Failed to store audit image for check-in: {}", e.getMessage());
            }

            response.put("success", true);
            response.put("action", "CHECK_IN");
            response.put("message", "Check-in successful via face recognition");
            response.put("subscriberId", subscriber.getId());
            response.put("subscriberName", subscriber.getFirstName() + " " + subscriber.getLastName());
            response.put("sessionId", session.getId());
            response.put("sessionName", session.getName());
            response.put("checkInTime", log.getCheckInTime());
            response.put("confidenceScore", recognitionResult.getConfidenceScore());
            response.put("processingTimeMs", recognitionResult.getProcessingTimeMs());
            response.put("totalProcessingTimeMs", System.currentTimeMillis() - startTime);
            response.put("auditImagePath", auditImagePath);

            logger.info("Face recognition check-in successful - Subscriber: {} {}, Session: {}, Confidence: {:.3f}",
                       subscriber.getFirstName(), subscriber.getLastName(), session.getName(),
                       recognitionResult.getConfidenceScore());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to process face recognition check-in", e);
            response.put("success", false);
            response.put("message", "Failed to process check-in: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Handle face recognition check-out
     */
    private ResponseEntity<Map<String, Object>> handleFaceRecognitionCheckOut(
            Subscriber subscriber, AttendanceSession session, FaceRecognitionResult recognitionResult,
            FaceRecognitionDto request, Map<String, Object> response, long startTime) {

        try {
            // Find existing attendance log
            AttendanceLog existingLog = attendanceLogRepository
                .findBySubscriberAndSessionAndCheckOutTimeIsNull(subscriber, session);

            if (existingLog == null) {
                response.put("success", false);
                response.put("message", "No active check-in found for this subscriber");
                return ResponseEntity.badRequest().body(response);
            }

            // Update with check-out information
            existingLog.setCheckOutTime(LocalDateTime.now());
            existingLog.setCheckOutMethod(CheckInMethod.FACE_RECOGNITION);

            // Save updated log
            attendanceLogRepository.save(existingLog);

            // Log successful recognition for check-out
            faceRecognitionService.logRecognitionAttempt(
                recognitionResult, session, subscriber, request.getDeviceInfo());

            response.put("success", true);
            response.put("action", "CHECK_OUT");
            response.put("message", "Check-out successful via face recognition");
            response.put("subscriberId", subscriber.getId());
            response.put("subscriberName", subscriber.getFirstName() + " " + subscriber.getLastName());
            response.put("sessionId", session.getId());
            response.put("sessionName", session.getName());
            response.put("checkInTime", existingLog.getCheckInTime());
            response.put("checkOutTime", existingLog.getCheckOutTime());
            response.put("duration", java.time.Duration.between(existingLog.getCheckInTime(), existingLog.getCheckOutTime()).toMinutes());
            response.put("confidenceScore", recognitionResult.getConfidenceScore());
            response.put("processingTimeMs", recognitionResult.getProcessingTimeMs());
            response.put("totalProcessingTimeMs", System.currentTimeMillis() - startTime);

            logger.info("Face recognition check-out successful - Subscriber: {} {}, Session: {}, Duration: {} minutes",
                       subscriber.getFirstName(), subscriber.getLastName(), session.getName(),
                       java.time.Duration.between(existingLog.getCheckInTime(), existingLog.getCheckOutTime()).toMinutes());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to process face recognition check-out", e);
            response.put("success", false);
            response.put("message", "Failed to process check-out: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Check if session is currently active
     */
    private boolean isSessionActive(AttendanceSession session) {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(session.getStartTime()) && now.isBefore(session.getEndTime());
    }

    /**
     * Get session status description
     */
    private String getSessionStatus(AttendanceSession session) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getStartTime())) {
            return "NOT_STARTED";
        } else if (now.isAfter(session.getEndTime())) {
            return "ENDED";
        } else {
            return "ACTIVE";
        }
    }

    /**
     * Helper method to extract entity ID from authentication
     */
    private String getEntityIdFromAuthentication(Authentication authentication) {
        return com.example.attendancesystem.util.AuthUtil.getEntityId(authentication);
    }

    /**
     * Helper method to decode Base64 image data
     */
    private byte[] decodeBase64Image(String base64Image) {
        try {
            if (base64Image.startsWith("data:image/")) {
                int commaIndex = base64Image.indexOf(',');
                if (commaIndex > 0) {
                    base64Image = base64Image.substring(commaIndex + 1);
                }
            }
            return java.util.Base64.getDecoder().decode(base64Image);
        } catch (Exception e) {
            logger.error("Failed to decode Base64 image: {}", e.getMessage());
            return null;
        }
    }
}
