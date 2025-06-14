package com.example.attendancesystem.controller;

import com.example.attendancesystem.model.FaceRecognitionSettings;
import com.example.attendancesystem.repository.FaceRecognitionSettingsRepository;
import com.example.attendancesystem.service.FaceRecognitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for face recognition settings management
 * Allows entity admins to configure face recognition parameters
 */
@RestController
@RequestMapping("/api/face/settings")
@CrossOrigin(origins = "*")
public class FaceRecognitionSettingsController {
    
    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionSettingsController.class);
    
    @Autowired
    private FaceRecognitionSettingsRepository settingsRepository;
    
    @Autowired
    private FaceRecognitionService faceRecognitionService;
    
    /**
     * Get face recognition settings for the authenticated entity
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Face recognition settings request - Entity: {}", entityId);
            
            Optional<FaceRecognitionSettings> settingsOpt = settingsRepository.findByEntityId(entityId);
            FaceRecognitionSettings settings;
            
            if (settingsOpt.isPresent()) {
                settings = settingsOpt.get();
                logger.info("Retrieved existing settings for entity: {}", entityId);
            } else {
                // Create default settings
                settings = createDefaultSettings(entityId);
                settings = settingsRepository.save(settings);
                logger.info("Created default settings for entity: {}", entityId);
            }
            
            response.put("success", true);
            response.put("entityId", entityId);
            response.put("settings", convertToResponseMap(settings));
            response.put("engineStatus", faceRecognitionService.getEngineStatus());
            response.put("isAvailable", faceRecognitionService.isFaceRecognitionAvailable());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get face recognition settings", e);
            response.put("success", false);
            response.put("message", "Error retrieving settings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Update face recognition settings for the authenticated entity
     */
    @PutMapping
    public ResponseEntity<Map<String, Object>> updateSettings(
            @Valid @RequestBody Map<String, Object> settingsRequest,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Face recognition settings update request - Entity: {}", entityId);
            
            // Get existing settings or create new ones
            FaceRecognitionSettings settings = settingsRepository.findByEntityId(entityId)
                .orElse(createDefaultSettings(entityId));
            
            // Update settings from request
            updateSettingsFromRequest(settings, settingsRequest);
            
            // Validate settings
            String validationError = validateSettings(settings);
            if (validationError != null) {
                response.put("success", false);
                response.put("message", "Invalid settings: " + validationError);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Save updated settings
            settings.setUpdatedAt(LocalDateTime.now());
            settings = settingsRepository.save(settings);
            
            response.put("success", true);
            response.put("message", "Settings updated successfully");
            response.put("entityId", entityId);
            response.put("settings", convertToResponseMap(settings));
            response.put("updatedAt", settings.getUpdatedAt());
            
            logger.info("Face recognition settings updated successfully for entity: {}", entityId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to update face recognition settings", e);
            response.put("success", false);
            response.put("message", "Error updating settings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Reset settings to default values
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetSettings(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Face recognition settings reset request - Entity: {}", entityId);
            
            // Create new default settings
            FaceRecognitionSettings defaultSettings = createDefaultSettings(entityId);
            defaultSettings.setUpdatedAt(LocalDateTime.now());
            
            // Save default settings (will update existing or create new)
            FaceRecognitionSettings settings = settingsRepository.save(defaultSettings);
            
            response.put("success", true);
            response.put("message", "Settings reset to default values");
            response.put("entityId", entityId);
            response.put("settings", convertToResponseMap(settings));
            response.put("resetAt", settings.getUpdatedAt());
            
            logger.info("Face recognition settings reset to defaults for entity: {}", entityId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to reset face recognition settings", e);
            response.put("success", false);
            response.put("message", "Error resetting settings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get recommended settings based on entity size and usage patterns
     */
    @GetMapping("/recommendations")
    public ResponseEntity<Map<String, Object>> getRecommendations(
            @RequestParam(required = false, defaultValue = "medium") String entitySize,
            @RequestParam(required = false, defaultValue = "standard") String usagePattern,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Face recognition recommendations request - Entity: {}, Size: {}, Pattern: {}", 
                       entityId, entitySize, usagePattern);
            
            Map<String, Object> recommendations = generateRecommendations(entitySize, usagePattern);
            
            response.put("success", true);
            response.put("entityId", entityId);
            response.put("entitySize", entitySize);
            response.put("usagePattern", usagePattern);
            response.put("recommendations", recommendations);
            response.put("description", getRecommendationDescription(entitySize, usagePattern));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get face recognition recommendations", e);
            response.put("success", false);
            response.put("message", "Error getting recommendations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Test current settings with a sample operation
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testSettings(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Face recognition settings test request - Entity: {}", entityId);
            
            // Get current settings
            FaceRecognitionSettings settings = settingsRepository.findByEntityId(entityId)
                .orElse(createDefaultSettings(entityId));
            
            // Perform basic engine test
            boolean engineAvailable = faceRecognitionService.isFaceRecognitionAvailable();
            String engineStatus = faceRecognitionService.getEngineStatus();
            
            // Get performance metrics if available
            Map<String, Object> performanceMetrics = new HashMap<>();
            performanceMetrics.put("engineAvailable", engineAvailable);
            performanceMetrics.put("engineStatus", engineStatus);
            performanceMetrics.put("maxProcessingTime", settings.getMaxProcessingTimeMs());
            performanceMetrics.put("confidenceThreshold", settings.getConfidenceThreshold());
            
            response.put("success", true);
            response.put("entityId", entityId);
            response.put("settings", convertToResponseMap(settings));
            response.put("performanceMetrics", performanceMetrics);
            response.put("testResult", engineAvailable ? "PASS" : "FALLBACK");
            response.put("message", engineAvailable ? 
                "Face recognition engine is working correctly" : 
                "Using fallback implementation - native engine not available");
            
            logger.info("Face recognition settings test completed for entity: {} - Result: {}", 
                       entityId, engineAvailable ? "PASS" : "FALLBACK");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to test face recognition settings", e);
            response.put("success", false);
            response.put("message", "Error testing settings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Create default settings for an entity
     */
    private FaceRecognitionSettings createDefaultSettings(String entityId) {
        FaceRecognitionSettings settings = new FaceRecognitionSettings();
        settings.setEntityId(entityId);
        settings.setConfidenceThreshold(new BigDecimal("0.8000"));
        settings.setMaxRecognitionDistance(new BigDecimal("0.600000"));
        settings.setEnableAntiSpoofing(true);
        settings.setEnableMultipleFaceDetection(false);
        settings.setMaxProcessingTimeMs(5000);
        settings.setPhotoQualityThreshold(new BigDecimal("0.7000"));
        settings.setCreatedAt(LocalDateTime.now());
        settings.setUpdatedAt(LocalDateTime.now());
        return settings;
    }

    /**
     * Update settings from request map
     */
    private void updateSettingsFromRequest(FaceRecognitionSettings settings, Map<String, Object> request) {
        if (request.containsKey("confidenceThreshold")) {
            settings.setConfidenceThreshold(new BigDecimal(request.get("confidenceThreshold").toString()));
        }
        if (request.containsKey("maxRecognitionDistance")) {
            settings.setMaxRecognitionDistance(new BigDecimal(request.get("maxRecognitionDistance").toString()));
        }
        if (request.containsKey("enableAntiSpoofing")) {
            settings.setEnableAntiSpoofing((Boolean) request.get("enableAntiSpoofing"));
        }
        if (request.containsKey("enableMultipleFaceDetection")) {
            settings.setEnableMultipleFaceDetection((Boolean) request.get("enableMultipleFaceDetection"));
        }
        if (request.containsKey("maxProcessingTimeMs")) {
            settings.setMaxProcessingTimeMs((Integer) request.get("maxProcessingTimeMs"));
        }
        if (request.containsKey("photoQualityThreshold")) {
            settings.setPhotoQualityThreshold(new BigDecimal(request.get("photoQualityThreshold").toString()));
        }
    }

    /**
     * Validate settings values
     */
    private String validateSettings(FaceRecognitionSettings settings) {
        if (settings.getConfidenceThreshold().compareTo(BigDecimal.ZERO) < 0 ||
            settings.getConfidenceThreshold().compareTo(BigDecimal.ONE) > 0) {
            return "Confidence threshold must be between 0.0 and 1.0";
        }

        if (settings.getMaxRecognitionDistance().compareTo(BigDecimal.ZERO) < 0 ||
            settings.getMaxRecognitionDistance().compareTo(BigDecimal.ONE) > 0) {
            return "Max recognition distance must be between 0.0 and 1.0";
        }

        if (settings.getMaxProcessingTimeMs() < 1000 || settings.getMaxProcessingTimeMs() > 30000) {
            return "Max processing time must be between 1000ms and 30000ms";
        }

        if (settings.getPhotoQualityThreshold().compareTo(BigDecimal.ZERO) < 0 ||
            settings.getPhotoQualityThreshold().compareTo(BigDecimal.ONE) > 0) {
            return "Photo quality threshold must be between 0.0 and 1.0";
        }

        return null; // Valid
    }

    /**
     * Convert settings to response map
     */
    private Map<String, Object> convertToResponseMap(FaceRecognitionSettings settings) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", settings.getId());
        map.put("entityId", settings.getEntityId());
        map.put("confidenceThreshold", settings.getConfidenceThreshold());
        map.put("maxRecognitionDistance", settings.getMaxRecognitionDistance());
        map.put("enableAntiSpoofing", settings.getEnableAntiSpoofing());
        map.put("enableMultipleFaceDetection", settings.getEnableMultipleFaceDetection());
        map.put("maxProcessingTimeMs", settings.getMaxProcessingTimeMs());
        map.put("photoQualityThreshold", settings.getPhotoQualityThreshold());
        map.put("createdAt", settings.getCreatedAt());
        map.put("updatedAt", settings.getUpdatedAt());
        return map;
    }

    /**
     * Generate recommendations based on entity size and usage pattern
     */
    private Map<String, Object> generateRecommendations(String entitySize, String usagePattern) {
        Map<String, Object> recommendations = new HashMap<>();

        // Base recommendations
        BigDecimal confidenceThreshold = new BigDecimal("0.8000");
        BigDecimal maxDistance = new BigDecimal("0.600000");
        boolean antiSpoofing = true;
        boolean multipleFaces = false;
        int maxProcessingTime = 5000;
        BigDecimal qualityThreshold = new BigDecimal("0.7000");

        // Adjust based on entity size
        switch (entitySize.toLowerCase()) {
            case "small":
                // Small entities (< 50 people) - Higher accuracy, slower processing OK
                confidenceThreshold = new BigDecimal("0.8500");
                maxDistance = new BigDecimal("0.500000");
                maxProcessingTime = 7000;
                qualityThreshold = new BigDecimal("0.8000");
                break;

            case "medium":
                // Medium entities (50-200 people) - Balanced approach
                confidenceThreshold = new BigDecimal("0.8000");
                maxDistance = new BigDecimal("0.600000");
                maxProcessingTime = 5000;
                qualityThreshold = new BigDecimal("0.7000");
                break;

            case "large":
                // Large entities (200+ people) - Faster processing, slightly lower accuracy
                confidenceThreshold = new BigDecimal("0.7500");
                maxDistance = new BigDecimal("0.650000");
                maxProcessingTime = 3000;
                qualityThreshold = new BigDecimal("0.6500");
                break;
        }

        // Adjust based on usage pattern
        switch (usagePattern.toLowerCase()) {
            case "high_security":
                // High security environments
                confidenceThreshold = confidenceThreshold.add(new BigDecimal("0.0500"));
                maxDistance = maxDistance.subtract(new BigDecimal("0.050000"));
                antiSpoofing = true;
                qualityThreshold = qualityThreshold.add(new BigDecimal("0.1000"));
                break;

            case "high_throughput":
                // High throughput environments
                confidenceThreshold = confidenceThreshold.subtract(new BigDecimal("0.0500"));
                maxDistance = maxDistance.add(new BigDecimal("0.050000"));
                maxProcessingTime = Math.max(2000, maxProcessingTime - 1000);
                multipleFaces = true;
                break;

            case "standard":
            default:
                // Keep default values
                break;
        }

        // Ensure values are within valid ranges
        confidenceThreshold = confidenceThreshold.max(new BigDecimal("0.5000")).min(new BigDecimal("0.9500"));
        maxDistance = maxDistance.max(new BigDecimal("0.300000")).min(new BigDecimal("0.800000"));
        qualityThreshold = qualityThreshold.max(new BigDecimal("0.5000")).min(new BigDecimal("0.9000"));

        recommendations.put("confidenceThreshold", confidenceThreshold);
        recommendations.put("maxRecognitionDistance", maxDistance);
        recommendations.put("enableAntiSpoofing", antiSpoofing);
        recommendations.put("enableMultipleFaceDetection", multipleFaces);
        recommendations.put("maxProcessingTimeMs", maxProcessingTime);
        recommendations.put("photoQualityThreshold", qualityThreshold);

        return recommendations;
    }

    /**
     * Get description for recommendation settings
     */
    private String getRecommendationDescription(String entitySize, String usagePattern) {
        StringBuilder description = new StringBuilder();

        description.append("Recommended settings for ");

        switch (entitySize.toLowerCase()) {
            case "small":
                description.append("small organizations (< 50 people)");
                break;
            case "medium":
                description.append("medium organizations (50-200 people)");
                break;
            case "large":
                description.append("large organizations (200+ people)");
                break;
            default:
                description.append("medium-sized organizations");
        }

        description.append(" with ");

        switch (usagePattern.toLowerCase()) {
            case "high_security":
                description.append("high security requirements. These settings prioritize accuracy and security over speed.");
                break;
            case "high_throughput":
                description.append("high throughput requirements. These settings prioritize speed and efficiency.");
                break;
            case "standard":
            default:
                description.append("standard usage patterns. These settings provide a balanced approach.");
        }

        return description.toString();
    }

    /**
     * Helper method to extract entity ID from authentication
     */
    private String getEntityIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            String username = authentication.getName();
            return extractEntityIdFromUsername(username);
        }
        throw new IllegalArgumentException("Unable to determine entity ID from authentication");
    }

    /**
     * Helper method to extract entity ID from username
     */
    private String extractEntityIdFromUsername(String username) {
        if (username != null && username.contains("@")) {
            return username.split("@")[1];
        }
        return "MSD00001"; // Default fallback
    }
}
