package com.example.attendancesystem.subscriber.controller;

import com.example.attendancesystem.facerecognition.SeetaFaceJNI;
import com.example.attendancesystem.service.FaceRecognitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for face recognition system
 */
@RestController
@RequestMapping("/api/face/health")
public class FaceRecognitionHealthController {
    
    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionHealthController.class);
    
    @Autowired
    private FaceRecognitionService faceRecognitionService;
    
    /**
     * Get face recognition system health status
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Check if native library is loaded
            boolean nativeLibraryLoaded = SeetaFaceJNI.isLibraryLoaded();
            status.put("nativeLibraryLoaded", nativeLibraryLoaded);
            
            // Check if face recognition is available
            boolean faceRecognitionAvailable = faceRecognitionService.isFaceRecognitionAvailable();
            status.put("faceRecognitionAvailable", faceRecognitionAvailable);
            
            // Get engine status
            String engineStatus = faceRecognitionService.getEngineStatus();
            status.put("engineStatus", engineStatus);
            
            // Overall health
            boolean healthy = faceRecognitionAvailable;
            status.put("healthy", healthy);
            status.put("status", healthy ? "UP" : "DOWN");
            
            // Additional info
            status.put("implementation", nativeLibraryLoaded ? "SeetaFace2" : "Fallback");
            status.put("timestamp", System.currentTimeMillis());
            
            logger.info("Face recognition health check - Healthy: {}, Native: {}, Available: {}", 
                       healthy, nativeLibraryLoaded, faceRecognitionAvailable);
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Face recognition health check failed", e);
            
            status.put("healthy", false);
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(503).body(status);
        }
    }
    
    /**
     * Get detailed system information
     */
    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getDetailedStatus() {
        Map<String, Object> details = new HashMap<>();
        
        try {
            // System information
            details.put("javaVersion", System.getProperty("java.version"));
            details.put("osName", System.getProperty("os.name"));
            details.put("osArch", System.getProperty("os.arch"));
            details.put("osVersion", System.getProperty("os.version"));
            
            // Memory information
            Runtime runtime = Runtime.getRuntime();
            details.put("maxMemory", runtime.maxMemory());
            details.put("totalMemory", runtime.totalMemory());
            details.put("freeMemory", runtime.freeMemory());
            details.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            
            // Face recognition specific
            details.put("nativeLibraryLoaded", SeetaFaceJNI.isLibraryLoaded());
            details.put("faceRecognitionAvailable", faceRecognitionService.isFaceRecognitionAvailable());
            details.put("engineStatus", faceRecognitionService.getEngineStatus());
            
            // Library paths
            details.put("javaLibraryPath", System.getProperty("java.library.path"));
            details.put("classPath", System.getProperty("java.class.path"));
            
            return ResponseEntity.ok(details);
            
        } catch (Exception e) {
            logger.error("Failed to get detailed face recognition status", e);
            
            details.put("error", e.getMessage());
            details.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(details);
        }
    }
}
