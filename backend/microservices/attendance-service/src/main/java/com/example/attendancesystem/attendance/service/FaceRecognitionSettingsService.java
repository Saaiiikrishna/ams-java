package com.example.attendancesystem.attendance.service;

import com.example.attendancesystem.attendance.model.FaceRecognitionSettings;
import com.example.attendancesystem.attendance.repository.FaceRecognitionSettingsRepository;
import com.example.attendancesystem.attendance.client.UserServiceGrpcClient;
import com.example.attendancesystem.attendance.dto.UserDto;
// TODO: Replace with gRPC calls to User Service
// import com.example.attendancesystem.shared.repository.SubscriberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing face recognition settings
 * Provides business logic for settings operations and optimization
 */
@Service
public class FaceRecognitionSettingsService {
    
    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionSettingsService.class);
    
    @Autowired
    private FaceRecognitionSettingsRepository settingsRepository;
    
    @Autowired
    private UserServiceGrpcClient userServiceGrpcClient;
    
    @Autowired
    private FaceRecognitionService faceRecognitionService;
    
    /**
     * Get settings for an entity, creating defaults if not exists
     */
    public FaceRecognitionSettings getOrCreateSettings(String entityId) {
        Optional<FaceRecognitionSettings> settingsOpt = settingsRepository.findByEntityId(entityId);
        
        if (settingsOpt.isPresent()) {
            return settingsOpt.get();
        } else {
            return createAndSaveDefaultSettings(entityId);
        }
    }
    
    /**
     * Update settings with validation
     */
    @Transactional
    public FaceRecognitionSettings updateSettings(String entityId, FaceRecognitionSettings newSettings) {
        FaceRecognitionSettings existingSettings = getOrCreateSettings(entityId);
        
        // Update fields
        existingSettings.setConfidenceThreshold(newSettings.getConfidenceThreshold());
        existingSettings.setMaxRecognitionDistance(newSettings.getMaxRecognitionDistance());
        existingSettings.setEnableAntiSpoofing(newSettings.getEnableAntiSpoofing());
        existingSettings.setEnableMultipleFaceDetection(newSettings.getEnableMultipleFaceDetection());
        existingSettings.setMaxProcessingTimeMs(newSettings.getMaxProcessingTimeMs());
        existingSettings.setPhotoQualityThreshold(newSettings.getPhotoQualityThreshold());
        existingSettings.setUpdatedAt(LocalDateTime.now());
        
        return settingsRepository.save(existingSettings);
    }
    
    /**
     * Generate optimized settings based on entity characteristics
     */
    public FaceRecognitionSettings generateOptimizedSettings(String entityId) {
        try {
            // Get entity statistics
            // TODO: Get counts via gRPC
            long totalSubscribers = 0; // userServiceGrpcClient.countByOrganizationEntityId(entityId);
            long subscribersWithFaces = 0; // userServiceGrpcClient.countByOrganizationEntityIdAndFaceEncodingIsNotNull(entityId);
            
            // Determine entity size category
            String entitySize = categorizeEntitySize(totalSubscribers);
            
            // Determine usage pattern based on face registration ratio
            String usagePattern = determineUsagePattern(totalSubscribers, subscribersWithFaces);
            
            // Generate optimized settings
            FaceRecognitionSettings optimizedSettings = createDefaultSettings(entityId);
            applyOptimizations(optimizedSettings, entitySize, usagePattern, totalSubscribers);
            
            logger.info("Generated optimized settings for entity {} - Size: {}, Pattern: {}, Subscribers: {}", 
                       entityId, entitySize, usagePattern, totalSubscribers);
            
            return optimizedSettings;
            
        } catch (Exception e) {
            logger.error("Failed to generate optimized settings for entity {}", entityId, e);
            return createDefaultSettings(entityId);
        }
    }
    
    /**
     * Analyze current settings performance and suggest improvements
     */
    public Map<String, Object> analyzeSettingsPerformance(String entityId) {
        Map<String, Object> analysis = new HashMap<>();
        
        try {
            FaceRecognitionSettings settings = getOrCreateSettings(entityId);
            
            // Get entity statistics
            // TODO: Get counts via gRPC
            long totalSubscribers = 0; // userServiceGrpcClient.countByOrganizationEntityId(entityId);
            long subscribersWithFaces = 0; // userServiceGrpcClient.countByOrganizationEntityIdAndFaceEncodingIsNotNull(entityId);
            
            // Calculate metrics
            double faceRegistrationRatio = totalSubscribers > 0 ? 
                (double) subscribersWithFaces / totalSubscribers : 0.0;
            
            // Analyze settings
            analysis.put("entityId", entityId);
            analysis.put("totalSubscribers", totalSubscribers);
            analysis.put("subscribersWithFaces", subscribersWithFaces);
            analysis.put("faceRegistrationRatio", faceRegistrationRatio);
            analysis.put("currentSettings", convertSettingsToMap(settings));
            
            // Performance analysis
            Map<String, Object> performanceAnalysis = analyzePerformanceMetrics(settings, totalSubscribers);
            analysis.put("performanceAnalysis", performanceAnalysis);
            
            // Recommendations
            Map<String, Object> recommendations = generatePerformanceRecommendations(settings, totalSubscribers, faceRegistrationRatio);
            analysis.put("recommendations", recommendations);
            
            // Overall score
            int overallScore = calculateOverallScore(settings, totalSubscribers, faceRegistrationRatio);
            analysis.put("overallScore", overallScore);
            analysis.put("scoreDescription", getScoreDescription(overallScore));
            
            return analysis;
            
        } catch (Exception e) {
            logger.error("Failed to analyze settings performance for entity {}", entityId, e);
            analysis.put("error", e.getMessage());
            return analysis;
        }
    }
    
    /**
     * Validate settings and return validation results
     */
    public Map<String, Object> validateSettings(FaceRecognitionSettings settings) {
        Map<String, Object> validation = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        Map<String, String> warnings = new HashMap<>();
        
        // Validate confidence threshold
        if (settings.getConfidenceThreshold().compareTo(new BigDecimal("0.5")) < 0) {
            errors.put("confidenceThreshold", "Confidence threshold too low - may result in false positives");
        } else if (settings.getConfidenceThreshold().compareTo(new BigDecimal("0.95")) > 0) {
            warnings.put("confidenceThreshold", "Confidence threshold very high - may result in false negatives");
        }
        
        // Validate max recognition distance
        if (settings.getMaxRecognitionDistance().compareTo(new BigDecimal("0.8")) > 0) {
            warnings.put("maxRecognitionDistance", "Max distance very high - may reduce accuracy");
        } else if (settings.getMaxRecognitionDistance().compareTo(new BigDecimal("0.3")) < 0) {
            warnings.put("maxRecognitionDistance", "Max distance very low - may be too restrictive");
        }
        
        // Validate processing time
        if (settings.getMaxProcessingTimeMs() < 2000) {
            warnings.put("maxProcessingTimeMs", "Processing time very low - may cause timeouts");
        } else if (settings.getMaxProcessingTimeMs() > 10000) {
            warnings.put("maxProcessingTimeMs", "Processing time very high - may impact user experience");
        }
        
        // Validate quality threshold
        if (settings.getPhotoQualityThreshold().compareTo(new BigDecimal("0.9")) > 0) {
            warnings.put("photoQualityThreshold", "Quality threshold very high - may reject good photos");
        } else if (settings.getPhotoQualityThreshold().compareTo(new BigDecimal("0.5")) < 0) {
            warnings.put("photoQualityThreshold", "Quality threshold very low - may accept poor photos");
        }
        
        validation.put("isValid", errors.isEmpty());
        validation.put("errors", errors);
        validation.put("warnings", warnings);
        validation.put("errorCount", errors.size());
        validation.put("warningCount", warnings.size());
        
        return validation;
    }
    
    /**
     * Export settings for backup or migration
     */
    public Map<String, Object> exportSettings(String entityId) {
        FaceRecognitionSettings settings = getOrCreateSettings(entityId);
        
        Map<String, Object> export = new HashMap<>();
        export.put("entityId", entityId);
        export.put("exportedAt", LocalDateTime.now());
        export.put("version", "1.0");
        export.put("settings", convertSettingsToMap(settings));
        
        return export;
    }
    
    /**
     * Import settings from backup
     */
    @Transactional
    public FaceRecognitionSettings importSettings(String entityId, Map<String, Object> importData) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> settingsData = (Map<String, Object>) importData.get("settings");
            
            FaceRecognitionSettings settings = createDefaultSettings(entityId);
            
            // Import settings values
            if (settingsData.containsKey("confidenceThreshold")) {
                settings.setConfidenceThreshold(new BigDecimal(settingsData.get("confidenceThreshold").toString()));
            }
            if (settingsData.containsKey("maxRecognitionDistance")) {
                settings.setMaxRecognitionDistance(new BigDecimal(settingsData.get("maxRecognitionDistance").toString()));
            }
            if (settingsData.containsKey("enableAntiSpoofing")) {
                settings.setEnableAntiSpoofing((Boolean) settingsData.get("enableAntiSpoofing"));
            }
            if (settingsData.containsKey("enableMultipleFaceDetection")) {
                settings.setEnableMultipleFaceDetection((Boolean) settingsData.get("enableMultipleFaceDetection"));
            }
            if (settingsData.containsKey("maxProcessingTimeMs")) {
                settings.setMaxProcessingTimeMs(((Number) settingsData.get("maxProcessingTimeMs")).intValue());
            }
            if (settingsData.containsKey("photoQualityThreshold")) {
                settings.setPhotoQualityThreshold(new BigDecimal(settingsData.get("photoQualityThreshold").toString()));
            }
            
            settings.setUpdatedAt(LocalDateTime.now());
            
            return settingsRepository.save(settings);
            
        } catch (Exception e) {
            logger.error("Failed to import settings for entity {}", entityId, e);
            throw new RuntimeException("Failed to import settings: " + e.getMessage());
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
     * Create and save default settings
     */
    private FaceRecognitionSettings createAndSaveDefaultSettings(String entityId) {
        FaceRecognitionSettings settings = createDefaultSettings(entityId);
        return settingsRepository.save(settings);
    }

    /**
     * Categorize entity size based on subscriber count
     */
    private String categorizeEntitySize(long subscriberCount) {
        if (subscriberCount < 50) {
            return "small";
        } else if (subscriberCount < 200) {
            return "medium";
        } else {
            return "large";
        }
    }

    /**
     * Determine usage pattern based on statistics
     */
    private String determineUsagePattern(long totalSubscribers, long subscribersWithFaces) {
        if (totalSubscribers == 0) {
            return "standard";
        }

        double faceRegistrationRatio = (double) subscribersWithFaces / totalSubscribers;

        if (faceRegistrationRatio > 0.8) {
            return "high_throughput";
        } else if (faceRegistrationRatio < 0.3) {
            return "high_security";
        } else {
            return "standard";
        }
    }

    /**
     * Apply optimizations to settings based on entity characteristics
     */
    private void applyOptimizations(FaceRecognitionSettings settings, String entitySize,
                                  String usagePattern, long subscriberCount) {

        // Base optimizations by entity size
        switch (entitySize) {
            case "small":
                settings.setConfidenceThreshold(new BigDecimal("0.8500"));
                settings.setMaxRecognitionDistance(new BigDecimal("0.500000"));
                settings.setMaxProcessingTimeMs(7000);
                settings.setPhotoQualityThreshold(new BigDecimal("0.8000"));
                break;

            case "large":
                settings.setConfidenceThreshold(new BigDecimal("0.7500"));
                settings.setMaxRecognitionDistance(new BigDecimal("0.650000"));
                settings.setMaxProcessingTimeMs(3000);
                settings.setPhotoQualityThreshold(new BigDecimal("0.6500"));
                break;

            default: // medium
                // Keep default values
                break;
        }

        // Adjust based on usage pattern
        switch (usagePattern) {
            case "high_security":
                settings.setConfidenceThreshold(settings.getConfidenceThreshold().add(new BigDecimal("0.0500")));
                settings.setEnableAntiSpoofing(true);
                settings.setPhotoQualityThreshold(settings.getPhotoQualityThreshold().add(new BigDecimal("0.1000")));
                break;

            case "high_throughput":
                settings.setConfidenceThreshold(settings.getConfidenceThreshold().subtract(new BigDecimal("0.0500")));
                settings.setMaxProcessingTimeMs(Math.max(2000, settings.getMaxProcessingTimeMs() - 1000));
                settings.setEnableMultipleFaceDetection(true);
                break;
        }

        // Ensure values are within valid ranges
        settings.setConfidenceThreshold(
            settings.getConfidenceThreshold().max(new BigDecimal("0.5000")).min(new BigDecimal("0.9500")));
        settings.setMaxRecognitionDistance(
            settings.getMaxRecognitionDistance().max(new BigDecimal("0.300000")).min(new BigDecimal("0.800000")));
        settings.setPhotoQualityThreshold(
            settings.getPhotoQualityThreshold().max(new BigDecimal("0.5000")).min(new BigDecimal("0.9000")));
    }

    /**
     * Analyze performance metrics
     */
    private Map<String, Object> analyzePerformanceMetrics(FaceRecognitionSettings settings, long subscriberCount) {
        Map<String, Object> analysis = new HashMap<>();

        // Accuracy analysis
        float accuracyScore = calculateAccuracyScore(settings);
        analysis.put("accuracyScore", accuracyScore);
        analysis.put("accuracyLevel", getAccuracyLevel(accuracyScore));

        // Speed analysis
        float speedScore = calculateSpeedScore(settings);
        analysis.put("speedScore", speedScore);
        analysis.put("speedLevel", getSpeedLevel(speedScore));

        // Security analysis
        float securityScore = calculateSecurityScore(settings);
        analysis.put("securityScore", securityScore);
        analysis.put("securityLevel", getSecurityLevel(securityScore));

        // Scalability analysis
        float scalabilityScore = calculateScalabilityScore(settings, subscriberCount);
        analysis.put("scalabilityScore", scalabilityScore);
        analysis.put("scalabilityLevel", getScalabilityLevel(scalabilityScore));

        return analysis;
    }

    /**
     * Generate performance recommendations
     */
    private Map<String, Object> generatePerformanceRecommendations(FaceRecognitionSettings settings,
                                                                 long subscriberCount, double faceRegistrationRatio) {
        Map<String, Object> recommendations = new HashMap<>();

        // Accuracy recommendations
        if (settings.getConfidenceThreshold().compareTo(new BigDecimal("0.7")) < 0) {
            recommendations.put("accuracy", "Consider increasing confidence threshold for better accuracy");
        } else if (settings.getConfidenceThreshold().compareTo(new BigDecimal("0.9")) > 0) {
            recommendations.put("accuracy", "Consider decreasing confidence threshold to reduce false negatives");
        }

        // Speed recommendations
        if (settings.getMaxProcessingTimeMs() > 7000) {
            recommendations.put("speed", "Consider reducing processing time limit for better user experience");
        } else if (settings.getMaxProcessingTimeMs() < 3000) {
            recommendations.put("speed", "Consider increasing processing time to improve accuracy");
        }

        // Security recommendations
        if (!settings.getEnableAntiSpoofing() && subscriberCount > 100) {
            recommendations.put("security", "Consider enabling anti-spoofing for larger organizations");
        }

        // Scalability recommendations
        if (subscriberCount > 500 && settings.getMaxProcessingTimeMs() > 5000) {
            recommendations.put("scalability", "Consider optimizing settings for high-volume usage");
        }

        return recommendations;
    }

    /**
     * Calculate overall score for settings
     */
    private int calculateOverallScore(FaceRecognitionSettings settings, long subscriberCount, double faceRegistrationRatio) {
        float accuracyScore = calculateAccuracyScore(settings);
        float speedScore = calculateSpeedScore(settings);
        float securityScore = calculateSecurityScore(settings);
        float scalabilityScore = calculateScalabilityScore(settings, subscriberCount);

        // Weighted average (accuracy and security are more important)
        float weightedScore = (accuracyScore * 0.3f) + (speedScore * 0.2f) +
                             (securityScore * 0.3f) + (scalabilityScore * 0.2f);

        return Math.round(weightedScore * 100);
    }

    /**
     * Calculate accuracy score based on settings
     */
    private float calculateAccuracyScore(FaceRecognitionSettings settings) {
        float score = 0.5f; // Base score

        // Confidence threshold contribution
        float confidenceScore = settings.getConfidenceThreshold().floatValue();
        if (confidenceScore >= 0.8f && confidenceScore <= 0.9f) {
            score += 0.3f;
        } else if (confidenceScore >= 0.7f && confidenceScore < 0.95f) {
            score += 0.2f;
        } else {
            score += 0.1f;
        }

        // Quality threshold contribution
        float qualityScore = settings.getPhotoQualityThreshold().floatValue();
        if (qualityScore >= 0.7f && qualityScore <= 0.8f) {
            score += 0.2f;
        } else if (qualityScore >= 0.6f && qualityScore < 0.9f) {
            score += 0.1f;
        }

        return Math.min(1.0f, score);
    }

    /**
     * Calculate speed score based on settings
     */
    private float calculateSpeedScore(FaceRecognitionSettings settings) {
        float score = 0.5f; // Base score

        // Processing time contribution
        int processingTime = settings.getMaxProcessingTimeMs();
        if (processingTime <= 3000) {
            score += 0.4f;
        } else if (processingTime <= 5000) {
            score += 0.3f;
        } else if (processingTime <= 7000) {
            score += 0.2f;
        } else {
            score += 0.1f;
        }

        // Multiple face detection can speed up batch operations
        if (settings.getEnableMultipleFaceDetection()) {
            score += 0.1f;
        }

        return Math.min(1.0f, score);
    }

    /**
     * Calculate security score based on settings
     */
    private float calculateSecurityScore(FaceRecognitionSettings settings) {
        float score = 0.3f; // Base score

        // Anti-spoofing contribution
        if (settings.getEnableAntiSpoofing()) {
            score += 0.4f;
        }

        // High confidence threshold improves security
        float confidenceScore = settings.getConfidenceThreshold().floatValue();
        if (confidenceScore >= 0.85f) {
            score += 0.2f;
        } else if (confidenceScore >= 0.8f) {
            score += 0.1f;
        }

        // Low max distance improves security
        float maxDistance = settings.getMaxRecognitionDistance().floatValue();
        if (maxDistance <= 0.5f) {
            score += 0.1f;
        }

        return Math.min(1.0f, score);
    }

    /**
     * Calculate scalability score based on settings and entity size
     */
    private float calculateScalabilityScore(FaceRecognitionSettings settings, long subscriberCount) {
        float score = 0.5f; // Base score

        // Adjust based on entity size and settings appropriateness
        if (subscriberCount < 50) {
            // Small entities can afford higher processing times
            if (settings.getMaxProcessingTimeMs() <= 7000) score += 0.3f;
            if (settings.getConfidenceThreshold().floatValue() >= 0.8f) score += 0.2f;
        } else if (subscriberCount < 200) {
            // Medium entities need balanced settings
            if (settings.getMaxProcessingTimeMs() <= 5000) score += 0.3f;
            if (settings.getConfidenceThreshold().floatValue() >= 0.75f &&
                settings.getConfidenceThreshold().floatValue() <= 0.85f) score += 0.2f;
        } else {
            // Large entities need optimized settings
            if (settings.getMaxProcessingTimeMs() <= 3000) score += 0.3f;
            if (settings.getEnableMultipleFaceDetection()) score += 0.1f;
            if (settings.getConfidenceThreshold().floatValue() >= 0.7f &&
                settings.getConfidenceThreshold().floatValue() <= 0.8f) score += 0.1f;
        }

        return Math.min(1.0f, score);
    }

    /**
     * Convert settings to map for API responses
     */
    private Map<String, Object> convertSettingsToMap(FaceRecognitionSettings settings) {
        Map<String, Object> map = new HashMap<>();
        map.put("confidenceThreshold", settings.getConfidenceThreshold());
        map.put("maxRecognitionDistance", settings.getMaxRecognitionDistance());
        map.put("enableAntiSpoofing", settings.getEnableAntiSpoofing());
        map.put("enableMultipleFaceDetection", settings.getEnableMultipleFaceDetection());
        map.put("maxProcessingTimeMs", settings.getMaxProcessingTimeMs());
        map.put("photoQualityThreshold", settings.getPhotoQualityThreshold());
        return map;
    }

    // Level description methods
    private String getAccuracyLevel(float score) {
        if (score >= 0.8f) return "High";
        if (score >= 0.6f) return "Medium";
        return "Low";
    }

    private String getSpeedLevel(float score) {
        if (score >= 0.8f) return "Fast";
        if (score >= 0.6f) return "Medium";
        return "Slow";
    }

    private String getSecurityLevel(float score) {
        if (score >= 0.8f) return "High";
        if (score >= 0.6f) return "Medium";
        return "Low";
    }

    private String getScalabilityLevel(float score) {
        if (score >= 0.8f) return "Excellent";
        if (score >= 0.6f) return "Good";
        return "Poor";
    }

    private String getScoreDescription(int score) {
        if (score >= 85) return "Excellent - Settings are well optimized";
        if (score >= 70) return "Good - Settings are mostly optimized";
        if (score >= 55) return "Fair - Some optimization needed";
        return "Poor - Significant optimization recommended";
    }
}
