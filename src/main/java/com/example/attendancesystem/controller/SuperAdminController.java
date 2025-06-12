package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.ChangePasswordRequest;
import com.example.attendancesystem.dto.SuperAdminDto;
import com.example.attendancesystem.model.Role;
import com.example.attendancesystem.model.SuperAdmin;
import com.example.attendancesystem.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/super")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminController.class);

    @Autowired
    private SuperAdminRepository superAdminRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Repositories for cleanup functionality
    @Autowired
    private AttendanceLogRepository attendanceLogRepository;

    @Autowired
    private NfcCardRepository nfcCardRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private ScheduledSessionRepository scheduledSessionRepository;

    @Autowired
    private EntityAdminRepository entityAdminRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @GetMapping("/super-admins")
    public ResponseEntity<List<Map<String, Object>>> getAllSuperAdmins() {
        try {
            System.out.println("DEBUG: ===== SUPER ADMINS ENDPOINT CALLED =====");
            List<SuperAdmin> superAdmins = superAdminRepository.findByIsActiveTrue();
            System.out.println("DEBUG: Found " + superAdmins.size() + " active super admins");
            List<Map<String, Object>> adminDtos = superAdmins.stream()
                    .map(admin -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", admin.getId());
                        dto.put("username", admin.getUsername());
                        dto.put("email", admin.getEmail());
                        dto.put("firstName", admin.getFirstName());
                        dto.put("lastName", admin.getLastName());
                        dto.put("fullName", admin.getFullName());
                        dto.put("createdAt", admin.getCreatedAt());
                        dto.put("isActive", admin.getIsActive());
                        return dto;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(adminDtos);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to fetch super admins: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/super-admins")
    public ResponseEntity<?> createSuperAdmin(@RequestBody SuperAdminDto superAdminDto) {
        try {
            // Check if username already exists
            if (superAdminRepository.existsByUsername(superAdminDto.getUsername())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Username already exists"));
            }

            // Check if email already exists
            if (superAdminDto.getEmail() != null && superAdminRepository.existsByEmail(superAdminDto.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Email already exists"));
            }

            // Get the SUPER_ADMIN role
            Role superAdminRole = roleRepository.findByName("SUPER_ADMIN")
                    .orElseThrow(() -> new RuntimeException("SUPER_ADMIN role not found"));

            SuperAdmin superAdmin = new SuperAdmin();
            superAdmin.setUsername(superAdminDto.getUsername());
            superAdmin.setPassword(passwordEncoder.encode(superAdminDto.getPassword()));
            superAdmin.setEmail(superAdminDto.getEmail());
            superAdmin.setFirstName(superAdminDto.getFirstName());
            superAdmin.setLastName(superAdminDto.getLastName());
            superAdmin.setRole(superAdminRole);
            superAdmin.setIsActive(true);

            SuperAdmin savedSuperAdmin = superAdminRepository.save(superAdmin);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedSuperAdmin.getId());
            response.put("username", savedSuperAdmin.getUsername());
            response.put("email", savedSuperAdmin.getEmail());
            response.put("fullName", savedSuperAdmin.getFullName());
            response.put("message", "Super Admin created successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to create super admin: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create super admin"));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Authentication authentication) {
        try {
            String currentUsername = authentication.getName();
            Optional<SuperAdmin> superAdminOpt = superAdminRepository.findByUsername(currentUsername);

            if (superAdminOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Super Admin not found"));
            }

            SuperAdmin superAdmin = superAdminOpt.get();

            // Verify old password
            if (!passwordEncoder.matches(request.getOldPassword(), superAdmin.getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Current password is incorrect"));
            }

            // Verify new password confirmation
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "New password and confirmation do not match"));
            }

            // Update password
            superAdmin.setPassword(passwordEncoder.encode(request.getNewPassword()));
            superAdminRepository.save(superAdmin);

            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            System.err.println("ERROR: Failed to change password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to change password"));
        }
    }

    @PutMapping("/super-admins/{id}/deactivate")
    public ResponseEntity<?> deactivateSuperAdmin(@PathVariable Long id, Authentication authentication) {
        try {
            String currentUsername = authentication.getName();
            Optional<SuperAdmin> targetAdminOpt = superAdminRepository.findById(id);

            if (targetAdminOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Super Admin not found"));
            }

            SuperAdmin targetAdmin = targetAdminOpt.get();

            // Prevent self-deactivation
            if (targetAdmin.getUsername().equals(currentUsername)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Cannot deactivate your own account"));
            }

            targetAdmin.setIsActive(false);
            superAdminRepository.save(targetAdmin);

            return ResponseEntity.ok(Map.of("message", "Super Admin deactivated successfully"));
        } catch (Exception e) {
            System.err.println("ERROR: Failed to deactivate super admin: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to deactivate super admin"));
        }
    }

    /**
     * Delete all data except super admins (for development/testing purposes)
     */
    @DeleteMapping("/cleanup-all-data")
    @Transactional
    public ResponseEntity<?> cleanupAllData() {
        try {
            logger.warn("CLEANUP: Starting complete database cleanup (keeping only super admins)");

            // Count records before deletion for logging
            long attendanceLogsCount = attendanceLogRepository.count();
            long nfcCardsCount = nfcCardRepository.count();
            long subscribersCount = subscriberRepository.count();
            long attendanceSessionsCount = attendanceSessionRepository.count();
            long scheduledSessionsCount = scheduledSessionRepository.count();
            long entityAdminsCount = entityAdminRepository.count();
            long organizationsCount = organizationRepository.count();
            long refreshTokensCount = refreshTokenRepository.count();

            // Delete in order to respect foreign key constraints
            // 1. Delete attendance logs first
            attendanceLogRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} attendance logs", attendanceLogsCount);

            // 2. Delete NFC cards
            nfcCardRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} NFC cards", nfcCardsCount);

            // 3. Delete subscribers
            subscriberRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} subscribers", subscribersCount);

            // 4. Delete attendance sessions
            attendanceSessionRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} attendance sessions", attendanceSessionsCount);

            // 5. Delete scheduled sessions
            scheduledSessionRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} scheduled sessions", scheduledSessionsCount);

            // 6. Delete entity admins
            entityAdminRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} entity admins", entityAdminsCount);

            // 7. Delete organizations
            organizationRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} organizations", organizationsCount);

            // 8. Delete refresh tokens (except super admin tokens)
            refreshTokenRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} refresh tokens", refreshTokensCount);

            long totalDeleted = attendanceLogsCount + nfcCardsCount + subscribersCount +
                              attendanceSessionsCount + scheduledSessionsCount +
                              entityAdminsCount + organizationsCount + refreshTokensCount;

            logger.warn("CLEANUP: Complete database cleanup finished. Total records deleted: {}", totalDeleted);

            return ResponseEntity.ok(Map.of(
                "message", "Database cleanup completed successfully",
                "deletedRecords", Map.of(
                    "attendanceLogs", attendanceLogsCount,
                    "nfcCards", nfcCardsCount,
                    "subscribers", subscribersCount,
                    "attendanceSessions", attendanceSessionsCount,
                    "scheduledSessions", scheduledSessionsCount,
                    "entityAdmins", entityAdminsCount,
                    "organizations", organizationsCount,
                    "refreshTokens", refreshTokensCount,
                    "total", totalDeleted
                )
            ));

        } catch (Exception e) {
            logger.error("CLEANUP: Failed to cleanup database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to cleanup database: " + e.getMessage()));
        }
    }
}
