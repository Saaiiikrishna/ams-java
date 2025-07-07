package com.example.attendancesystem.attendance.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/face-checkin")
public class FaceRecognitionCheckInController {
    
    @PostMapping("/recognize")
    public ResponseEntity<?> recognizeFace(@RequestParam String imageData,
                                         @RequestParam Long sessionId) {
        return ResponseEntity.ok(Map.of(
            "success", false,
            "message", "Face recognition check-in temporarily disabled for microservices independence"
        ));
    }
    
    @GetMapping("/status")
    public ResponseEntity<?> getFaceRecognitionStatus() {
        return ResponseEntity.ok(Map.of(
            "available", false,
            "message", "Face recognition temporarily disabled for microservices independence"
        ));
    }
    
    @GetMapping("/logs/{sessionId}")
    public ResponseEntity<?> getRecognitionLogs(@PathVariable Long sessionId) {
        return ResponseEntity.ok(Map.of(
            "logs", new ArrayList<>(),
            "message", "Face recognition logs temporarily disabled for microservices independence"
        ));
    }
    
    @PostMapping("/test-recognition")
    public ResponseEntity<?> testFaceRecognition(@RequestParam String imageData) {
        return ResponseEntity.ok(Map.of(
            "success", false,
            "message", "Face recognition testing temporarily disabled for microservices independence"
        ));
    }
}
