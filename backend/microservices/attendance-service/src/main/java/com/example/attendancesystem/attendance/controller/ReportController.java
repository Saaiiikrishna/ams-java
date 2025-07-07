package com.example.attendancesystem.attendance.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> generateSessionReport(@PathVariable Long sessionId) {
        return ResponseEntity.ok(Map.of("message", "Report generation temporarily disabled for microservices independence"));
    }
    
    @GetMapping("/subscriber/{subscriberId}")
    public ResponseEntity<?> generateSubscriberReport(@PathVariable Long subscriberId) {
        return ResponseEntity.ok(Map.of("message", "Report generation temporarily disabled for microservices independence"));
    }
}
