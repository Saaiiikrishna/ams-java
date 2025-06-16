package com.example.attendancesystem.controller;

import com.example.attendancesystem.service.DatabaseCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/super/database")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class DatabaseCleanupController {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseCleanupController.class);

    @Autowired
    private DatabaseCleanupService databaseCleanupService;

    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupDatabase() {
        logger.info("🚨 CRITICAL: Database cleanup requested by Super Admin");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get SuperAdmin count before cleanup
            int superAdminCount = databaseCleanupService.getSuperAdminCount();
            logger.info("👑 SuperAdmin count before cleanup: {}", superAdminCount);
            
            if (superAdminCount == 0) {
                logger.error("❌ ABORT: No SuperAdmins found in database!");
                response.put("success", false);
                response.put("message", "Cannot proceed: No SuperAdmins found in database");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Perform the cleanup
            logger.info("🗑️ Executing database cleanup...");
            databaseCleanupService.deleteAllDataExceptSuperAdmins();
            
            // Verify SuperAdmins are still there
            int superAdminCountAfter = databaseCleanupService.getSuperAdminCount();
            logger.info("👑 SuperAdmin count after cleanup: {}", superAdminCountAfter);
            
            if (superAdminCountAfter != superAdminCount) {
                logger.error("❌ CRITICAL: SuperAdmin count changed during cleanup! Before: {}, After: {}", 
                           superAdminCount, superAdminCountAfter);
            }
            
            response.put("success", true);
            response.put("message", "Database cleanup completed successfully");
            response.put("superAdminCountBefore", superAdminCount);
            response.put("superAdminCountAfter", superAdminCountAfter);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("✅ Database cleanup completed successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("💥 Database cleanup failed: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Database cleanup failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        logger.info("📊 Database status requested");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            int superAdminCount = databaseCleanupService.getSuperAdminCount();
            
            response.put("success", true);
            response.put("superAdminCount", superAdminCount);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("📊 Database status: {} SuperAdmins", superAdminCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Error getting database status: {}", e.getMessage());
            
            response.put("success", false);
            response.put("message", "Error getting database status: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
