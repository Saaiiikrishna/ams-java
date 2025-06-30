package com.example.attendancesystem.organization.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Test Controller for debugging security configuration
 */
@RestController
@RequestMapping("/test")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @GetMapping("/public")
    public ResponseEntity<?> testPublic() {
        logger.info("TestController.testPublic() called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Public endpoint working");
        response.put("timestamp", LocalDateTime.now());
        response.put("securityConfig", "If you see this, SecurityConfig is working");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/secured")
    public ResponseEntity<?> testSecured() {
        logger.info("TestController.testSecured() called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Secured endpoint working");
        response.put("timestamp", LocalDateTime.now());
        response.put("authentication", "This should be blocked by security");
        
        return ResponseEntity.ok(response);
    }
}
