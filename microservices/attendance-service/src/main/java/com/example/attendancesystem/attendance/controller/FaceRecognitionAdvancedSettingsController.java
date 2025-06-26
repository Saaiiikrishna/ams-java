package com.example.attendancesystem.organization.controller;

import com.example.attendancesystem.organization.service.FaceRecognitionSettingsService;
import com.example.attendancesystem.util.AuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * Advanced controller for face recognition settings management
 * Provides optimization, analysis, and bulk operations
 */
@RestController
@RequestMapping("/api/face/settings/advanced")
@CrossOrigin(origins = "*")
public class FaceRecognitionAdvancedSettingsController {
    
    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionAdvancedSettingsController.class);
    
    @Autowired
    private FaceRecognitionSettingsService settingsService;
    
    /**
     * Generate optimized settings based on entity characteristics
     */
    @PostMapping("/optimize")
    public ResponseEntity<Map<String, Object>> optimizeSettings(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Settings optimization request - Entity: {}", entityId);
            
            // Generate optimized settings
            var optimizedSettings = settingsService.generateOptimizedSettings(entityId);
            
            response.put("success", true);
            response.put("entityId", entityId);
            response.put("message", "Optimized settings generated successfully");
            response.put("optimizedSettings", convertSettingsToMap(optimizedSettings));
            response.put("recommendation", "Review the optimized settings and apply if suitable for your organization");
            
            logger.info("Settings optimization completed for entity: {}", entityId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to optimize settings", e);
            response.put("success", false);
            response.put("message", "Error optimizing settings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Analyze current settings performance
     */
    @GetMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeSettings(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Settings analysis request - Entity: {}", entityId);
            
            // Perform comprehensive analysis
            Map<String, Object> analysis = settingsService.analyzeSettingsPerformance(entityId);
            
            response.put("success", true);
            response.put("entityId", entityId);
            response.put("analysis", analysis);
            response.put("message", "Settings analysis completed successfully");
            
            logger.info("Settings analysis completed for entity: {}", entityId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to analyze settings", e);
            response.put("success", false);
            response.put("message", "Error analyzing settings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Validate settings configuration
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateSettings(
            @Valid @RequestBody Map<String, Object> settingsData,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Settings validation request - Entity: {}", entityId);
            
            // Convert request to settings object
            var settings = convertMapToSettings(settingsData, entityId);
            
            // Validate settings
            Map<String, Object> validation = settingsService.validateSettings(settings);
            
            response.put("success", true);
            response.put("entityId", entityId);
            response.put("validation", validation);
            response.put("message", "Settings validation completed");
            
            logger.info("Settings validation completed for entity: {} - Valid: {}", 
                       entityId, validation.get("isValid"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to validate settings", e);
            response.put("success", false);
            response.put("message", "Error validating settings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Export settings for backup
     */
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportSettings(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Settings export request - Entity: {}", entityId);
            
            // Export settings
            Map<String, Object> exportData = settingsService.exportSettings(entityId);
            
            response.put("success", true);
            response.put("entityId", entityId);
            response.put("export", exportData);
            response.put("message", "Settings exported successfully");
            
            logger.info("Settings exported for entity: {}", entityId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to export settings", e);
            response.put("success", false);
            response.put("message", "Error exporting settings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Import settings from backup
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importSettings(
            @Valid @RequestBody Map<String, Object> importData,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Settings import request - Entity: {}", entityId);
            
            // Import settings
            var importedSettings = settingsService.importSettings(entityId, importData);
            
            response.put("success", true);
            response.put("entityId", entityId);
            response.put("importedSettings", convertSettingsToMap(importedSettings));
            response.put("message", "Settings imported successfully");
            
            logger.info("Settings imported for entity: {}", entityId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to import settings", e);
            response.put("success", false);
            response.put("message", "Error importing settings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get performance benchmarks and comparisons
     */
    @GetMapping("/benchmark")
    public ResponseEntity<Map<String, Object>> getBenchmarks(
            @RequestParam(required = false, defaultValue = "medium") String entitySize,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Settings benchmark request - Entity: {}, Size: {}", entityId, entitySize);
            
            // Get current settings analysis
            Map<String, Object> currentAnalysis = settingsService.analyzeSettingsPerformance(entityId);
            
            // Generate benchmark data
            Map<String, Object> benchmarks = generateBenchmarks(entitySize);
            
            response.put("success", true);
            response.put("entityId", entityId);
            response.put("entitySize", entitySize);
            response.put("currentPerformance", currentAnalysis);
            response.put("benchmarks", benchmarks);
            response.put("message", "Benchmark data generated successfully");
            
            logger.info("Benchmark data generated for entity: {}", entityId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to generate benchmarks", e);
            response.put("success", false);
            response.put("message", "Error generating benchmarks: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get settings presets for different scenarios
     */
    @GetMapping("/presets")
    public ResponseEntity<Map<String, Object>> getPresets(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String entityId = getEntityIdFromAuthentication(authentication);
            logger.info("Settings presets request - Entity: {}", entityId);
            
            Map<String, Object> presets = generatePresets();
            
            response.put("success", true);
            response.put("entityId", entityId);
            response.put("presets", presets);
            response.put("message", "Settings presets retrieved successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get presets", e);
            response.put("success", false);
            response.put("message", "Error retrieving presets: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Generate benchmark data for comparison
     */
    private Map<String, Object> generateBenchmarks(String entitySize) {
        Map<String, Object> benchmarks = new HashMap<>();

        // Industry standard benchmarks
        Map<String, Object> industryStandards = new HashMap<>();
        industryStandards.put("accuracyScore", 0.85);
        industryStandards.put("speedScore", 0.75);
        industryStandards.put("securityScore", 0.80);
        industryStandards.put("overallScore", 80);

        // Size-specific benchmarks
        Map<String, Object> sizeSpecific = new HashMap<>();
        switch (entitySize.toLowerCase()) {
            case "small":
                sizeSpecific.put("recommendedConfidenceThreshold", 0.85);
                sizeSpecific.put("recommendedProcessingTime", 7000);
                sizeSpecific.put("expectedAccuracy", 0.90);
                sizeSpecific.put("expectedSpeed", 0.70);
                break;
            case "medium":
                sizeSpecific.put("recommendedConfidenceThreshold", 0.80);
                sizeSpecific.put("recommendedProcessingTime", 5000);
                sizeSpecific.put("expectedAccuracy", 0.85);
                sizeSpecific.put("expectedSpeed", 0.80);
                break;
            case "large":
                sizeSpecific.put("recommendedConfidenceThreshold", 0.75);
                sizeSpecific.put("recommendedProcessingTime", 3000);
                sizeSpecific.put("expectedAccuracy", 0.80);
                sizeSpecific.put("expectedSpeed", 0.90);
                break;
        }

        benchmarks.put("industryStandards", industryStandards);
        benchmarks.put("sizeSpecific", sizeSpecific);
        benchmarks.put("generatedAt", java.time.LocalDateTime.now());

        return benchmarks;
    }

    /**
     * Generate settings presets for different scenarios
     */
    private Map<String, Object> generatePresets() {
        Map<String, Object> presets = new HashMap<>();

        // High Security Preset
        Map<String, Object> highSecurity = new HashMap<>();
        highSecurity.put("name", "High Security");
        highSecurity.put("description", "Maximum security with anti-spoofing and high confidence");
        highSecurity.put("confidenceThreshold", 0.90);
        highSecurity.put("maxRecognitionDistance", 0.45);
        highSecurity.put("enableAntiSpoofing", true);
        highSecurity.put("enableMultipleFaceDetection", false);
        highSecurity.put("maxProcessingTimeMs", 8000);
        highSecurity.put("photoQualityThreshold", 0.85);
        highSecurity.put("useCase", "Banks, government facilities, high-security areas");

        // High Performance Preset
        Map<String, Object> highPerformance = new HashMap<>();
        highPerformance.put("name", "High Performance");
        highPerformance.put("description", "Optimized for speed and throughput");
        highPerformance.put("confidenceThreshold", 0.70);
        highPerformance.put("maxRecognitionDistance", 0.70);
        highPerformance.put("enableAntiSpoofing", false);
        highPerformance.put("enableMultipleFaceDetection", true);
        highPerformance.put("maxProcessingTimeMs", 2500);
        highPerformance.put("photoQualityThreshold", 0.60);
        highPerformance.put("useCase", "Large events, high-traffic areas, quick check-ins");

        // Balanced Preset
        Map<String, Object> balanced = new HashMap<>();
        balanced.put("name", "Balanced");
        balanced.put("description", "Good balance of security and performance");
        balanced.put("confidenceThreshold", 0.80);
        balanced.put("maxRecognitionDistance", 0.60);
        balanced.put("enableAntiSpoofing", true);
        balanced.put("enableMultipleFaceDetection", false);
        balanced.put("maxProcessingTimeMs", 5000);
        balanced.put("photoQualityThreshold", 0.70);
        balanced.put("useCase", "Offices, schools, general business use");

        // Accuracy Focused Preset
        Map<String, Object> accuracyFocused = new HashMap<>();
        accuracyFocused.put("name", "Accuracy Focused");
        accuracyFocused.put("description", "Maximum accuracy with strict validation");
        accuracyFocused.put("confidenceThreshold", 0.88);
        accuracyFocused.put("maxRecognitionDistance", 0.50);
        accuracyFocused.put("enableAntiSpoofing", true);
        accuracyFocused.put("enableMultipleFaceDetection", false);
        accuracyFocused.put("maxProcessingTimeMs", 7500);
        accuracyFocused.put("photoQualityThreshold", 0.80);
        accuracyFocused.put("useCase", "Research facilities, precision applications");

        presets.put("highSecurity", highSecurity);
        presets.put("highPerformance", highPerformance);
        presets.put("balanced", balanced);
        presets.put("accuracyFocused", accuracyFocused);

        return presets;
    }

    /**
     * Convert settings to map for API responses
     */
    private Map<String, Object> convertSettingsToMap(com.example.attendancesystem.model.FaceRecognitionSettings settings) {
        Map<String, Object> map = new HashMap<>();
        map.put("confidenceThreshold", settings.getConfidenceThreshold());
        map.put("maxRecognitionDistance", settings.getMaxRecognitionDistance());
        map.put("enableAntiSpoofing", settings.getEnableAntiSpoofing());
        map.put("enableMultipleFaceDetection", settings.getEnableMultipleFaceDetection());
        map.put("maxProcessingTimeMs", settings.getMaxProcessingTimeMs());
        map.put("photoQualityThreshold", settings.getPhotoQualityThreshold());
        return map;
    }

    /**
     * Convert map to settings object
     */
    private com.example.attendancesystem.model.FaceRecognitionSettings convertMapToSettings(
            Map<String, Object> map, String entityId) {

        var settings = new com.example.attendancesystem.model.FaceRecognitionSettings();
        settings.setEntityId(entityId);

        if (map.containsKey("confidenceThreshold")) {
            settings.setConfidenceThreshold(new java.math.BigDecimal(map.get("confidenceThreshold").toString()));
        }
        if (map.containsKey("maxRecognitionDistance")) {
            settings.setMaxRecognitionDistance(new java.math.BigDecimal(map.get("maxRecognitionDistance").toString()));
        }
        if (map.containsKey("enableAntiSpoofing")) {
            settings.setEnableAntiSpoofing((Boolean) map.get("enableAntiSpoofing"));
        }
        if (map.containsKey("enableMultipleFaceDetection")) {
            settings.setEnableMultipleFaceDetection((Boolean) map.get("enableMultipleFaceDetection"));
        }
        if (map.containsKey("maxProcessingTimeMs")) {
            settings.setMaxProcessingTimeMs(((Number) map.get("maxProcessingTimeMs")).intValue());
        }
        if (map.containsKey("photoQualityThreshold")) {
            settings.setPhotoQualityThreshold(new java.math.BigDecimal(map.get("photoQualityThreshold").toString()));
        }

        return settings;
    }

    /**
     * Helper method to extract entity ID from authentication
     */
    private String getEntityIdFromAuthentication(Authentication authentication) {
        return com.example.attendancesystem.util.AuthUtil.getEntityId(authentication);
    }
}
