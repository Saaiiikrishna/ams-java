package com.example.attendancesystem.organization.controller;

import com.example.attendancesystem.dto.FaceRecognitionDto;
import com.example.attendancesystem.facerecognition.FaceEncodingResult;
import com.example.attendancesystem.organization.model.Subscriber;
import com.example.attendancesystem.organization.repository.SubscriberRepository;
import com.example.attendancesystem.organization.service.FaceRecognitionService;
import com.example.attendancesystem.organization.service.FileStorageService;
import com.example.attendancesystem.util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for face registration operations
 * Handles face photo upload, registration, and management
 */
@RestController
@RequestMapping("/api/face")
@CrossOrigin(origins = "*")
public class FaceRegistrationController {
    
    private static final Logger logger = LoggerFactory.getLogger(FaceRegistrationController.class);
    
    @Autowired
    private FaceRecognitionService faceRecognitionService;
    
    @Autowired
    private SubscriberRepository subscriberRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    /**
     * Register face for a subscriber using Base64 image data
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerFace(
            @Valid @RequestBody FaceRecognitionDto request,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Face registration request - Subscriber: {}, Entity: {}", 
                       request.getSubscriberId(), entityId);
            
            // Validate request
            if (request.getSubscriberId() == null) {
                response.put("success", false);
                response.put("message", "Subscriber ID is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (request.getBase64Image() == null || request.getBase64Image().isEmpty()) {
                response.put("success", false);
                response.put("message", "Face image is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verify subscriber exists and belongs to entity
            Optional<Subscriber> subscriberOpt = subscriberRepository
                .findByIdAndOrganizationEntityId(request.getSubscriberId(), entityId);
            
            if (subscriberOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Subscriber not found or access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            Subscriber subscriber = subscriberOpt.get();
            
            // Convert Base64 to byte array
            byte[] imageData = decodeBase64Image(request.getBase64Image());
            if (imageData == null) {
                response.put("success", false);
                response.put("message", "Invalid image format");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Register face
            boolean success = faceRecognitionService.registerFaceForSubscriber(
                request.getSubscriberId(), imageData, entityId);
            
            if (success) {
                // Save profile photo if registration successful
                String photoPath = null;
                try {
                    photoPath = fileStorageService.storeProfilePhoto(imageData, 
                        subscriber.getId(), subscriber.getFirstName() + "_" + subscriber.getLastName());
                    
                    // Update subscriber with photo path
                    subscriber.setProfilePhotoPath(photoPath);
                    subscriberRepository.save(subscriber);
                    
                } catch (Exception e) {
                    logger.warn("Failed to save profile photo for subscriber {}: {}", 
                               subscriber.getId(), e.getMessage());
                }
                
                response.put("success", true);
                response.put("message", "Face registered successfully");
                response.put("subscriberId", subscriber.getId());
                response.put("subscriberName", subscriber.getFirstName() + " " + subscriber.getLastName());
                response.put("profilePhotoPath", photoPath);
                response.put("registeredAt", LocalDateTime.now());
                
                logger.info("Face registered successfully for subscriber {} - {}", 
                           subscriber.getId(), subscriber.getFirstName() + " " + subscriber.getLastName());
                
                return ResponseEntity.ok(response);
                
            } else {
                response.put("success", false);
                response.put("message", "Face registration failed. Please try with a clearer image.");
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Face registration failed", e);
            response.put("success", false);
            response.put("message", "Face registration error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Register face using multipart file upload
     */
    @PostMapping("/register/upload")
    public ResponseEntity<Map<String, Object>> registerFaceWithUpload(
            @RequestParam("subscriberId") Long subscriberId,
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(value = "deviceInfo", required = false) String deviceInfo,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Face registration upload - Subscriber: {}, Entity: {}, File: {}", 
                       subscriberId, entityId, imageFile.getOriginalFilename());
            
            // Validate file
            if (imageFile.isEmpty()) {
                response.put("success", false);
                response.put("message", "Image file is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check file size (max 10MB)
            if (imageFile.getSize() > 10 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "Image file too large. Maximum size is 10MB.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check file type
            String contentType = imageFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("message", "Invalid file type. Please upload an image file.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verify subscriber
            Optional<Subscriber> subscriberOpt = subscriberRepository
                .findByIdAndOrganizationEntityId(subscriberId, entityId);
            
            if (subscriberOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Subscriber not found or access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            Subscriber subscriber = subscriberOpt.get();
            byte[] imageData = imageFile.getBytes();
            
            // Register face
            boolean success = faceRecognitionService.registerFaceForSubscriber(
                subscriberId, imageData, entityId);
            
            if (success) {
                // Save profile photo
                String photoPath = null;
                try {
                    photoPath = fileStorageService.storeProfilePhoto(imageData, 
                        subscriber.getId(), subscriber.getFirstName() + "_" + subscriber.getLastName());
                    
                    subscriber.setProfilePhotoPath(photoPath);
                    subscriberRepository.save(subscriber);
                    
                } catch (Exception e) {
                    logger.warn("Failed to save profile photo for subscriber {}: {}", 
                               subscriber.getId(), e.getMessage());
                }
                
                response.put("success", true);
                response.put("message", "Face registered successfully");
                response.put("subscriberId", subscriber.getId());
                response.put("subscriberName", subscriber.getFirstName() + " " + subscriber.getLastName());
                response.put("profilePhotoPath", photoPath);
                response.put("fileSize", imageFile.getSize());
                response.put("registeredAt", LocalDateTime.now());
                
                logger.info("Face registered successfully via upload for subscriber {} - {}", 
                           subscriber.getId(), subscriber.getFirstName() + " " + subscriber.getLastName());
                
                return ResponseEntity.ok(response);
                
            } else {
                response.put("success", false);
                response.put("message", "Face registration failed. Please try with a clearer image.");
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Face registration upload failed", e);
            response.put("success", false);
            response.put("message", "Face registration error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get face registration status for a subscriber
     */
    @GetMapping("/status/{subscriberId}")
    public ResponseEntity<Map<String, Object>> getFaceRegistrationStatus(
            @PathVariable Long subscriberId,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Face status request - Subscriber: {}, Entity: {}", subscriberId, entityId);
            
            Optional<Subscriber> subscriberOpt = subscriberRepository
                .findByIdAndOrganizationEntityId(subscriberId, entityId);
            
            if (subscriberOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Subscriber not found or access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            Subscriber subscriber = subscriberOpt.get();
            boolean hasFaceRecognition = subscriber.hasFaceRecognition();
            
            response.put("success", true);
            response.put("subscriberId", subscriber.getId());
            response.put("subscriberName", subscriber.getFirstName() + " " + subscriber.getLastName());
            response.put("hasFaceRecognition", hasFaceRecognition);
            response.put("profilePhotoPath", subscriber.getProfilePhotoPath());
            response.put("faceEncodingVersion", subscriber.getFaceEncodingVersion());
            
            if (hasFaceRecognition) {
                response.put("faceRegisteredAt", subscriber.getFaceRegisteredAt());
                response.put("faceUpdatedAt", subscriber.getFaceUpdatedAt());
            }
            
            logger.info("Face status retrieved for subscriber {} - Has face: {}", 
                       subscriber.getId(), hasFaceRecognition);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get face registration status", e);
            response.put("success", false);
            response.put("message", "Error retrieving face status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Remove face registration for a subscriber
     */
    @DeleteMapping("/remove/{subscriberId}")
    public ResponseEntity<Map<String, Object>> removeFaceRegistration(
            @PathVariable Long subscriberId,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Face removal request - Subscriber: {}, Entity: {}", subscriberId, entityId);

            Optional<Subscriber> subscriberOpt = subscriberRepository
                .findByIdAndOrganizationEntityId(subscriberId, entityId);

            if (subscriberOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Subscriber not found or access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            Subscriber subscriber = subscriberOpt.get();

            if (!subscriber.hasFaceRecognition()) {
                response.put("success", false);
                response.put("message", "No face registration found for this subscriber");
                return ResponseEntity.badRequest().body(response);
            }

            // Remove face registration
            boolean success = faceRecognitionService.removeFaceForSubscriber(subscriberId);

            if (success) {
                // Remove profile photo file if exists
                if (subscriber.getProfilePhotoPath() != null) {
                    try {
                        fileStorageService.deleteFile(subscriber.getProfilePhotoPath());
                    } catch (Exception e) {
                        logger.warn("Failed to delete profile photo file: {}", e.getMessage());
                    }
                }

                response.put("success", true);
                response.put("message", "Face registration removed successfully");
                response.put("subscriberId", subscriber.getId());
                response.put("subscriberName", subscriber.getFirstName() + " " + subscriber.getLastName());
                response.put("removedAt", LocalDateTime.now());

                logger.info("Face registration removed for subscriber {} - {}",
                           subscriber.getId(), subscriber.getFirstName() + " " + subscriber.getLastName());

                return ResponseEntity.ok(response);

            } else {
                response.put("success", false);
                response.put("message", "Failed to remove face registration");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            logger.error("Face removal failed", e);
            response.put("success", false);
            response.put("message", "Face removal error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get all subscribers with face registration status for an entity
     */
    @GetMapping("/subscribers")
    public ResponseEntity<Map<String, Object>> getSubscribersWithFaceStatus(
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Face subscribers list request - Entity: {}", entityId);

            List<Subscriber> allSubscribers = subscriberRepository.findAllByOrganizationEntityId(entityId);
            List<Subscriber> withFaceRecognition = subscriberRepository
                .findByOrganizationEntityIdAndFaceEncodingIsNotNull(entityId);

            response.put("success", true);
            response.put("entityId", entityId);
            response.put("totalSubscribers", allSubscribers.size());
            response.put("subscribersWithFace", withFaceRecognition.size());
            response.put("subscribersWithoutFace", allSubscribers.size() - withFaceRecognition.size());

            // Create detailed list
            List<Map<String, Object>> subscriberList = allSubscribers.stream()
                .map(subscriber -> {
                    Map<String, Object> subscriberInfo = new HashMap<>();
                    subscriberInfo.put("id", subscriber.getId());
                    subscriberInfo.put("firstName", subscriber.getFirstName());
                    subscriberInfo.put("lastName", subscriber.getLastName());
                    subscriberInfo.put("mobileNumber", subscriber.getMobileNumber());
                    subscriberInfo.put("hasFaceRecognition", subscriber.hasFaceRecognition());
                    subscriberInfo.put("profilePhotoPath", subscriber.getProfilePhotoPath());

                    if (subscriber.hasFaceRecognition()) {
                        subscriberInfo.put("faceRegisteredAt", subscriber.getFaceRegisteredAt());
                        subscriberInfo.put("faceUpdatedAt", subscriber.getFaceUpdatedAt());
                        subscriberInfo.put("faceEncodingVersion", subscriber.getFaceEncodingVersion());
                    }

                    return subscriberInfo;
                })
                .toList();

            response.put("subscribers", subscriberList);

            logger.info("Face subscribers list retrieved - Entity: {}, Total: {}, With Face: {}",
                       entityId, allSubscribers.size(), withFaceRecognition.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get subscribers with face status", e);
            response.put("success", false);
            response.put("message", "Error retrieving subscribers: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Test face detection on an image without registering
     */
    @PostMapping("/test-detection")
    public ResponseEntity<Map<String, Object>> testFaceDetection(
            @Valid @RequestBody FaceRecognitionDto request,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Face detection test request - Entity: {}", entityId);

            if (request.getBase64Image() == null || request.getBase64Image().isEmpty()) {
                response.put("success", false);
                response.put("message", "Image is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Convert Base64 to byte array
            byte[] imageData = decodeBase64Image(request.getBase64Image());
            if (imageData == null) {
                response.put("success", false);
                response.put("message", "Invalid image format");
                return ResponseEntity.badRequest().body(response);
            }

            // Test face encoding extraction
            FaceEncodingResult result = faceRecognitionService.extractFaceEncoding(imageData, entityId);

            response.put("success", result.isSuccess());
            response.put("message", result.isSuccess() ? "Face detected successfully" : result.getErrorMessage());
            response.put("processingTimeMs", result.getProcessingTimeMs());
            response.put("qualityScore", result.getQualityScore());
            response.put("livenessScore", result.getLivenessScore());

            if (result.isSuccess() && result.getFaceRectangle() != null) {
                Map<String, Object> faceInfo = new HashMap<>();
                faceInfo.put("x", result.getFaceRectangle().getX());
                faceInfo.put("y", result.getFaceRectangle().getY());
                faceInfo.put("width", result.getFaceRectangle().getWidth());
                faceInfo.put("height", result.getFaceRectangle().getHeight());
                faceInfo.put("confidence", result.getFaceRectangle().getConfidence());
                response.put("detectedFace", faceInfo);

                response.put("encodingDimensions", result.getEncodingDimensions());
            }

            logger.info("Face detection test completed - Entity: {}, Success: {}",
                       entityId, result.isSuccess());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Face detection test failed", e);
            response.put("success", false);
            response.put("message", "Face detection error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
            // Remove data URL prefix if present (e.g., "data:image/jpeg;base64,")
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
