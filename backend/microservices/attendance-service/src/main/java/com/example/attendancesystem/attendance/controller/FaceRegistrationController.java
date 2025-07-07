package com.example.attendancesystem.attendance.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
@RequestMapping("/api/face-registration")
public class FaceRegistrationController {
    
    @PostMapping("/register")
    public ResponseEntity<?> registerFace(@RequestParam Long subscriberId, 
                                        @RequestParam String imageData) {
        return ResponseEntity.ok(Map.of(
            "success", false,
            "message", "Face registration temporarily disabled for microservices independence"
        ));
    }
    
    @PostMapping("/register-base64")
    public ResponseEntity<?> registerFaceBase64(@RequestParam Long subscriberId, 
                                              @RequestParam String imageData) {
        return ResponseEntity.ok(Map.of(
            "success", false,
            "message", "Face registration temporarily disabled for microservices independence"
        ));
    }
    
    @DeleteMapping("/remove/{subscriberId}")
    public ResponseEntity<?> removeFace(@PathVariable Long subscriberId) {
        return ResponseEntity.ok(Map.of(
            "success", false,
            "message", "Face removal temporarily disabled for microservices independence"
        ));
    }
    
    @PostMapping("/extract-encoding")
    public ResponseEntity<?> extractFaceEncoding(@RequestParam String imageData) {
        return ResponseEntity.ok(Map.of(
            "success", false,
            "message", "Face encoding extraction temporarily disabled for microservices independence"
        ));
    }
}
