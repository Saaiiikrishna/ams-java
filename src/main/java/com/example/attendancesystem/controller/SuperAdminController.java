package com.example.attendancesystem.subscriber.controller;

import com.example.attendancesystem.dto.ChangePasswordRequest;
import com.example.attendancesystem.dto.SuperAdminDto;
import com.example.attendancesystem.model.*;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    @Autowired
    private OrganizationPermissionRepository organizationPermissionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private RestaurantTableRepository restaurantTableRepository;

    @Autowired
    private SubscriberAuthRepository subscriberAuthRepository;

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
            long subscriberAuthCount = subscriberAuthRepository.count();
            long nfcCardsCount = nfcCardRepository.count();
            long subscribersCount = subscriberRepository.count();
            long attendanceSessionsCount = attendanceSessionRepository.count();
            long scheduledSessionsCount = scheduledSessionRepository.count();
            long ordersCount = orderRepository.count();
            long itemsCount = itemRepository.count();
            long categoriesCount = categoryRepository.count();
            long restaurantTablesCount = restaurantTableRepository.count();
            long organizationPermissionsCount = organizationPermissionRepository.count();
            long entityAdminsCount = entityAdminRepository.count();
            long organizationsCount = organizationRepository.count();
            long refreshTokensCount = refreshTokenRepository.count();

            // Delete in order to respect foreign key constraints
            // 1. Delete attendance logs first
            attendanceLogRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} attendance logs", attendanceLogsCount);

            // 2. Delete subscriber auth records
            subscriberAuthRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} subscriber auth records", subscriberAuthCount);

            // 3. Delete orders
            orderRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} orders", ordersCount);

            // 4. Delete items
            itemRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} items", itemsCount);

            // 5. Delete categories
            categoryRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} categories", categoriesCount);

            // 6. Delete restaurant tables
            restaurantTableRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} restaurant tables", restaurantTablesCount);

            // 7. Delete NFC cards
            nfcCardRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} NFC cards", nfcCardsCount);

            // 8. Delete subscribers
            subscriberRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} subscribers", subscribersCount);

            // 9. Delete attendance sessions
            attendanceSessionRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} attendance sessions", attendanceSessionsCount);

            // 10. Delete scheduled sessions
            scheduledSessionRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} scheduled sessions", scheduledSessionsCount);

            // 11. Delete organization permissions
            organizationPermissionRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} organization permissions", organizationPermissionsCount);

            // 12. Delete entity admins
            entityAdminRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} entity admins", entityAdminsCount);

            // 13. Delete organizations
            organizationRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} organizations", organizationsCount);

            // 14. Delete refresh tokens
            refreshTokenRepository.deleteAll();
            logger.info("CLEANUP: Deleted {} refresh tokens", refreshTokensCount);

            long totalDeleted = attendanceLogsCount + subscriberAuthCount + nfcCardsCount + subscribersCount +
                              attendanceSessionsCount + scheduledSessionsCount + ordersCount +
                              itemsCount + categoriesCount + restaurantTablesCount +
                              organizationPermissionsCount + entityAdminsCount + organizationsCount + refreshTokensCount;

            logger.warn("CLEANUP: Complete database cleanup finished. Total records deleted: {}", totalDeleted);

            Map<String, Object> deletedRecordsMap = new HashMap<>();
            deletedRecordsMap.put("attendanceLogs", attendanceLogsCount);
            deletedRecordsMap.put("subscriberAuth", subscriberAuthCount);
            deletedRecordsMap.put("orders", ordersCount);
            deletedRecordsMap.put("items", itemsCount);
            deletedRecordsMap.put("categories", categoriesCount);
            deletedRecordsMap.put("restaurantTables", restaurantTablesCount);
            deletedRecordsMap.put("nfcCards", nfcCardsCount);
            deletedRecordsMap.put("subscribers", subscribersCount);
            deletedRecordsMap.put("attendanceSessions", attendanceSessionsCount);
            deletedRecordsMap.put("scheduledSessions", scheduledSessionsCount);
            deletedRecordsMap.put("organizationPermissions", organizationPermissionsCount);
            deletedRecordsMap.put("entityAdmins", entityAdminsCount);
            deletedRecordsMap.put("organizations", organizationsCount);
            deletedRecordsMap.put("refreshTokens", refreshTokensCount);
            deletedRecordsMap.put("total", totalDeleted);

            return ResponseEntity.ok(Map.of(
                "message", "Database cleanup completed successfully",
                "deletedRecords", deletedRecordsMap
            ));

        } catch (Exception e) {
            logger.error("CLEANUP: Failed to cleanup database", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to cleanup database: " + e.getMessage()));
        }
    }

    /**
     * Get entity deletion preview - shows what will be deleted
     */
    @GetMapping("/organizations/{id}/deletion-preview")
    public ResponseEntity<?> getEntityDeletionPreview(@PathVariable Long id) {
        try {
            var organization = organizationRepository.findById(id);
            if (organization.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Organization not found"));
            }

            var org = organization.get();

            // Count all related data
            long subscribersCount = subscriberRepository.countByOrganization(org);
            long subscriberAuthCount = subscriberAuthRepository.countByOrganization(org);
            long nfcCardsCount = nfcCardRepository.countByOrganization(org);
            long attendanceSessionsCount = attendanceSessionRepository.countByOrganization(org);
            long scheduledSessionsCount = scheduledSessionRepository.countByOrganization(org);
            long attendanceLogsCount = attendanceLogRepository.countByOrganization(org);
            long entityAdminsCount = entityAdminRepository.countByOrganization(org);
            long organizationPermissionsCount = organizationPermissionRepository.countByOrganization(org);

            // Count menu/ordering data
            long categoriesCount = categoryRepository.countByOrganization(org);
            long itemsCount = itemRepository.countByOrganization(org);
            long restaurantTablesCount = restaurantTableRepository.countByOrganization(org);
            long ordersCount = orderRepository.countByOrganization(org);

            long totalRelatedRecords = subscribersCount + subscriberAuthCount + nfcCardsCount + attendanceSessionsCount +
                                     scheduledSessionsCount + attendanceLogsCount + entityAdminsCount +
                                     organizationPermissionsCount + categoriesCount + itemsCount +
                                     restaurantTablesCount + ordersCount;

            Map<String, Object> organizationMap = new HashMap<>();
            organizationMap.put("id", org.getId());
            organizationMap.put("name", org.getName());
            organizationMap.put("entityId", org.getEntityId());
            // Note: Organization doesn't have createdAt field, so we'll skip it

            Map<String, Object> relatedDataMap = new HashMap<>();
            relatedDataMap.put("subscribers", subscribersCount);
            relatedDataMap.put("subscriberAuth", subscriberAuthCount);
            relatedDataMap.put("nfcCards", nfcCardsCount);
            relatedDataMap.put("attendanceSessions", attendanceSessionsCount);
            relatedDataMap.put("scheduledSessions", scheduledSessionsCount);
            relatedDataMap.put("attendanceLogs", attendanceLogsCount);
            relatedDataMap.put("entityAdmins", entityAdminsCount);
            relatedDataMap.put("organizationPermissions", organizationPermissionsCount);
            relatedDataMap.put("categories", categoriesCount);
            relatedDataMap.put("items", itemsCount);
            relatedDataMap.put("restaurantTables", restaurantTablesCount);
            relatedDataMap.put("orders", ordersCount);
            relatedDataMap.put("totalRelatedRecords", totalRelatedRecords);

            return ResponseEntity.ok(Map.of(
                "organization", organizationMap,
                "relatedData", relatedDataMap,
                "warning", "This action will permanently delete the organization and ALL related data. This cannot be undone."
            ));

        } catch (Exception e) {
            logger.error("Failed to get deletion preview for organization {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get deletion preview: " + e.getMessage()));
        }
    }

    /**
     * Force delete organization and all related data
     */
    @DeleteMapping("/organizations/{id}/force-delete")
    @Transactional
    public ResponseEntity<?> forceDeleteOrganization(@PathVariable Long id) {
        try {
            var organization = organizationRepository.findById(id);
            if (organization.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Organization not found"));
            }

            var org = organization.get();
            logger.warn("FORCE DELETE: Starting force deletion of organization {} ({})", org.getName(), org.getEntityId());

            // Count records before deletion
            long subscribersCount = subscriberRepository.countByOrganization(org);
            long subscriberAuthCount = subscriberAuthRepository.countByOrganization(org);
            long nfcCardsCount = nfcCardRepository.countByOrganization(org);
            long attendanceSessionsCount = attendanceSessionRepository.countByOrganization(org);
            long scheduledSessionsCount = scheduledSessionRepository.countByOrganization(org);
            long attendanceLogsCount = attendanceLogRepository.countByOrganization(org);
            long entityAdminsCount = entityAdminRepository.countByOrganization(org);
            long organizationPermissionsCount = organizationPermissionRepository.countByOrganization(org);
            long categoriesCount = categoryRepository.countByOrganization(org);
            long itemsCount = itemRepository.countByOrganization(org);
            long restaurantTablesCount = restaurantTableRepository.countByOrganization(org);
            long ordersCount = orderRepository.countByOrganization(org);

            // Delete in proper order to avoid foreign key constraints
            // This order has been tested and works correctly

            // 1. Delete orders first
            orderRepository.deleteByOrganization(org);
            logger.info("FORCE DELETE: Deleted {} orders", ordersCount);

            // 2. Delete items
            itemRepository.deleteByOrganization(org);
            logger.info("FORCE DELETE: Deleted {} items", itemsCount);

            // 3. Delete categories
            categoryRepository.deleteByOrganization(org);
            logger.info("FORCE DELETE: Deleted {} categories", categoriesCount);

            // 4. Delete restaurant tables
            restaurantTableRepository.deleteByOrganization(org);
            logger.info("FORCE DELETE: Deleted {} restaurant tables", restaurantTablesCount);

            // 5. Delete attendance records (they reference attendance sessions)
            attendanceLogRepository.deleteByOrganization(org);
            logger.info("FORCE DELETE: Deleted {} attendance records", attendanceLogsCount);

            // 6. Delete attendance sessions (they reference organization)
            attendanceSessionRepository.deleteByOrganization(org);
            logger.info("FORCE DELETE: Deleted {} attendance sessions", attendanceSessionsCount);

            // 7. Delete scheduled sessions
            scheduledSessionRepository.deleteByOrganization(org);
            logger.info("FORCE DELETE: Deleted {} scheduled sessions", scheduledSessionsCount);

            // 8. Delete subscriber auth first (they reference subscribers)
            subscriberAuthRepository.deleteByOrganization(org);
            logger.info("FORCE DELETE: Deleted {} subscriber auth records", subscriberAuthCount);

            // 9. Delete NFC cards (they reference subscribers)
            nfcCardRepository.deleteByOrganization(org);
            logger.info("FORCE DELETE: Deleted {} NFC cards", nfcCardsCount);

            // 10. Delete subscribers
            subscriberRepository.deleteByOrganization(org);
            logger.info("FORCE DELETE: Deleted {} subscribers", subscribersCount);

            // 11. Delete refresh tokens for entity admins
            List<EntityAdmin> entityAdmins = entityAdminRepository.findAllByOrganization(org);
            for (EntityAdmin admin : entityAdmins) {
                refreshTokenRepository.deleteByUser(admin);
            }
            logger.info("FORCE DELETE: Deleted refresh tokens for {} entity admins", entityAdmins.size());

            // 12. Delete entity admins
            entityAdminRepository.deleteByOrganization(org);
            logger.info("FORCE DELETE: Deleted {} entity admins", entityAdminsCount);

            // 13. Delete organization permissions
            var permissions = organizationPermissionRepository.findByOrganization(org);
            organizationPermissionRepository.deleteAll(permissions);
            logger.info("FORCE DELETE: Deleted {} organization permissions", organizationPermissionsCount);

            // 14. Finally delete the organization itself
            organizationRepository.delete(org);
            logger.warn("FORCE DELETE: Deleted organization {} ({})", org.getName(), org.getEntityId());

            long totalDeleted = subscribersCount + subscriberAuthCount + nfcCardsCount + attendanceSessionsCount +
                              scheduledSessionsCount + attendanceLogsCount + entityAdminsCount +
                              organizationPermissionsCount + categoriesCount + itemsCount +
                              restaurantTablesCount + ordersCount + 1; // +1 for organization

            Map<String, Object> deletedRecordsMap = new HashMap<>();
            deletedRecordsMap.put("subscribers", subscribersCount);
            deletedRecordsMap.put("subscriberAuth", subscriberAuthCount);
            deletedRecordsMap.put("nfcCards", nfcCardsCount);
            deletedRecordsMap.put("attendanceSessions", attendanceSessionsCount);
            deletedRecordsMap.put("scheduledSessions", scheduledSessionsCount);
            deletedRecordsMap.put("attendanceLogs", attendanceLogsCount);
            deletedRecordsMap.put("entityAdmins", entityAdminsCount);
            deletedRecordsMap.put("organizationPermissions", organizationPermissionsCount);
            deletedRecordsMap.put("categories", categoriesCount);
            deletedRecordsMap.put("items", itemsCount);
            deletedRecordsMap.put("restaurantTables", restaurantTablesCount);
            deletedRecordsMap.put("orders", ordersCount);
            deletedRecordsMap.put("organization", 1);
            deletedRecordsMap.put("total", totalDeleted);

            return ResponseEntity.ok(Map.of(
                "message", "Organization and all related data deleted successfully",
                "organizationName", org.getName(),
                "entityId", org.getEntityId(),
                "deletedRecords", deletedRecordsMap
            ));

        } catch (Exception e) {
            logger.error("FORCE DELETE: Failed to force delete organization {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to force delete organization: " + e.getMessage()));
        }
    }

    /**
     * Force delete organization by name (for easier testing and admin use)
     */
    @DeleteMapping("/organizations/force-delete-by-name/{organizationName}")
    @Transactional
    public ResponseEntity<?> forceDeleteOrganizationByName(@PathVariable String organizationName) {
        try {
            logger.info("FORCE DELETE: Searching for organization with name: {}", organizationName);

            // Find the organization by name
            var organizations = organizationRepository.findAll();
            Organization org = organizations.stream()
                    .filter(o -> o.getName().equalsIgnoreCase(organizationName))
                    .findFirst()
                    .orElse(null);

            if (org == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Organization '" + organizationName + "' not found"));
            }

            logger.warn("FORCE DELETE: Found organization {} with ID {} - proceeding with deletion",
                    org.getName(), org.getEntityId());

            // Use the existing force delete logic by calling the ID-based method
            return forceDeleteOrganization(org.getId());

        } catch (Exception e) {
            logger.error("FORCE DELETE: Failed to force delete organization by name {}", organizationName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to force delete organization: " + e.getMessage()));
        }
    }

    @GetMapping("/nfc-cards")
    public ResponseEntity<?> getAllNfcCards() {
        try {
            logger.info("🔍 [NFC CARDS] Fetching NFC cards count...");

            // Get count of NFC cards from the system
            long nfcCardCount = nfcCardRepository.count();

            logger.info("📊 [NFC CARDS] Current NFC cards count: {}", nfcCardCount);
            logger.info("📊 [NFC CARDS] Returning response with count: {}", nfcCardCount);

            Map<String, Object> response = Map.of(
                "count", nfcCardCount,
                "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ [NFC CARDS] Failed to fetch NFC cards: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch NFC cards"));
        }
    }

    @GetMapping("/system-metrics")
    public ResponseEntity<?> getSystemMetrics() {
        try {
            // Calculate real system metrics
            long totalUsers = subscriberRepository.count();
            long totalSessions = attendanceSessionRepository.count();
            long totalOrders = orderRepository != null ? orderRepository.count() : 0;
            double systemLoad = getSystemLoad();

            Map<String, Object> metrics = Map.of(
                    "totalUsers", totalUsers,
                    "totalSessions", totalSessions,
                    "totalOrders", totalOrders,
                    "systemLoad", systemLoad
            );

            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Failed to fetch system metrics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch system metrics"));
        }
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<?> getRecentActivity() {
        try {
            java.util.List<Map<String, Object>> activities = new java.util.ArrayList<>();

            // Add some sample activities for now
            activities.add(Map.of(
                    "id", 1,
                    "type", "organization",
                    "message", "System activity logged",
                    "timestamp", new java.util.Date().toString(),
                    "severity", "info"
            ));

            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            logger.error("Failed to fetch recent activity: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch recent activity"));
        }
    }

    private double getSystemLoad() {
        // Simple system load calculation
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        return Math.round((double) usedMemory / totalMemory * 100.0 * 100.0) / 100.0;
    }

    // ===== PERMISSION MANAGEMENT ENDPOINTS =====

    /**
     * Get all available feature permissions for super admin to manage
     */
    @GetMapping("/permissions/features")
    public ResponseEntity<?> getAvailableFeatures() {
        try {
            List<Map<String, Object>> features = java.util.Arrays.stream(com.example.attendancesystem.model.FeaturePermission.values())
                    .map(permission -> Map.<String, Object>of(
                            "permission", permission.name(),
                            "name", permission.getDisplayName(),
                            "description", permission.getDescription()
                    ))
                    .collect(Collectors.toList());

            // Group permissions by category for better organization
            Map<String, Object> groups = Map.of(
                "menuOrdering", java.util.Arrays.stream(com.example.attendancesystem.model.FeaturePermission.getMenuOrderingPermissions())
                        .map(com.example.attendancesystem.model.FeaturePermission::name)
                        .collect(Collectors.toList()),
                "attendance", java.util.Arrays.stream(com.example.attendancesystem.model.FeaturePermission.getAttendancePermissions())
                        .map(com.example.attendancesystem.model.FeaturePermission::name)
                        .collect(Collectors.toList()),
                "reports", java.util.Arrays.stream(com.example.attendancesystem.model.FeaturePermission.getReportPermissions())
                        .map(com.example.attendancesystem.model.FeaturePermission::name)
                        .collect(Collectors.toList()),
                "advanced", java.util.Arrays.stream(com.example.attendancesystem.model.FeaturePermission.getAdvancedPermissions())
                        .map(com.example.attendancesystem.model.FeaturePermission::name)
                        .collect(Collectors.toList())
            );

            return ResponseEntity.ok(Map.of(
                "features", features,
                "groups", groups
            ));

        } catch (Exception e) {
            logger.error("Failed to get available features: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get available features: " + e.getMessage()));
        }
    }

    /**
     * Get permissions for a specific organization
     */
    @GetMapping("/permissions/organization/{entityId}")
    public ResponseEntity<?> getOrganizationPermissions(@PathVariable String entityId) {
        try {
            Optional<Organization> orgOpt = organizationRepository.findByEntityId(entityId);
            if (orgOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Organization not found"));
            }

            Organization org = orgOpt.get();
            List<com.example.attendancesystem.model.OrganizationPermission> grantedPermissions =
                organizationPermissionRepository.findByOrganization(org);

            // Get all available permissions and mark which ones are granted
            List<Map<String, Object>> allPermissions = new ArrayList<>();

            for (com.example.attendancesystem.model.FeaturePermission permission :
                 com.example.attendancesystem.model.FeaturePermission.values()) {

                // Find if this permission is granted to the organization
                Optional<com.example.attendancesystem.model.OrganizationPermission> grantedPermission =
                    grantedPermissions.stream()
                        .filter(p -> p.getFeaturePermission() == permission)
                        .findFirst();

                Map<String, Object> permissionData = new HashMap<>();
                permissionData.put("featurePermission", permission.name());
                permissionData.put("permissionName", permission.getDisplayName());
                permissionData.put("permissionDescription", permission.getDescription());

                if (grantedPermission.isPresent()) {
                    com.example.attendancesystem.model.OrganizationPermission granted = grantedPermission.get();
                    permissionData.put("id", granted.getId());
                    permissionData.put("isEnabled", granted.getIsEnabled());
                    permissionData.put("isActive", granted.isActive());
                    permissionData.put("grantedBy", granted.getGrantedBy() != null ? granted.getGrantedBy() : "system");
                    permissionData.put("grantedAt", granted.getGrantedAt());
                    permissionData.put("notes", granted.getNotes());
                } else {
                    permissionData.put("id", null);
                    permissionData.put("isEnabled", false);
                    permissionData.put("isActive", false);
                    permissionData.put("grantedBy", null);
                    permissionData.put("grantedAt", null);
                    permissionData.put("notes", null);
                }

                allPermissions.add(permissionData);
            }

            return ResponseEntity.ok(Map.of(
                "entityId", entityId,
                "organizationName", org.getName(),
                "permissions", allPermissions
            ));

        } catch (Exception e) {
            logger.error("Failed to get organization permissions for entityId {}: {}", entityId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get organization permissions: " + e.getMessage()));
        }
    }

    /**
     * Grant permission to an organization
     */
    @PostMapping("/permissions/grant")
    public ResponseEntity<?> grantPermission(@RequestBody Map<String, String> request) {
        try {
            String entityId = request.get("entityId");
            String permissionName = request.get("permission");

            if (entityId == null || permissionName == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "entityId and permission are required"));
            }

            Optional<Organization> orgOpt = organizationRepository.findByEntityId(entityId);
            if (orgOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Organization not found"));
            }

            com.example.attendancesystem.model.FeaturePermission permission;
            try {
                permission = com.example.attendancesystem.model.FeaturePermission.valueOf(permissionName);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid permission: " + permissionName));
            }

            Organization org = orgOpt.get();

            // Check if permission already exists
            boolean exists = organizationPermissionRepository.existsByOrganizationAndFeaturePermission(org, permission);
            if (exists) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Permission already granted"));
            }

            // Grant the permission
            com.example.attendancesystem.model.OrganizationPermission orgPermission =
                new com.example.attendancesystem.model.OrganizationPermission();
            orgPermission.setOrganization(org);
            orgPermission.setFeaturePermission(permission);
            organizationPermissionRepository.save(orgPermission);

            logger.info("Granted permission {} to organization {}", permissionName, entityId);

            return ResponseEntity.ok(Map.of(
                "message", "Permission granted successfully",
                "entityId", entityId,
                "permission", permissionName
            ));

        } catch (Exception e) {
            logger.error("Failed to grant permission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to grant permission: " + e.getMessage()));
        }
    }

    /**
     * Revoke permission from an organization
     */
    @PostMapping("/permissions/revoke")
    public ResponseEntity<?> revokePermission(@RequestBody Map<String, String> request) {
        try {
            String entityId = request.get("entityId");
            String permissionName = request.get("permission");

            if (entityId == null || permissionName == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "entityId and permission are required"));
            }

            Optional<Organization> orgOpt = organizationRepository.findByEntityId(entityId);
            if (orgOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Organization not found"));
            }

            com.example.attendancesystem.model.FeaturePermission permission;
            try {
                permission = com.example.attendancesystem.model.FeaturePermission.valueOf(permissionName);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid permission: " + permissionName));
            }

            Organization org = orgOpt.get();

            // Find and delete the permission
            Optional<com.example.attendancesystem.model.OrganizationPermission> orgPermissionOpt =
                organizationPermissionRepository.findByOrganizationAndFeaturePermission(org, permission);

            if (orgPermissionOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Permission not found"));
            }

            organizationPermissionRepository.delete(orgPermissionOpt.get());

            logger.info("Revoked permission {} from organization {}", permissionName, entityId);

            return ResponseEntity.ok(Map.of(
                "message", "Permission revoked successfully",
                "entityId", entityId,
                "permission", permissionName
            ));

        } catch (Exception e) {
            logger.error("Failed to revoke permission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to revoke permission: " + e.getMessage()));
        }
    }

    /**
     * Update all permissions for an organization (bulk update)
     */
    @PostMapping("/permissions/organization/{entityId}/update")
    @Transactional
    public ResponseEntity<?> updateOrganizationPermissions(
            @PathVariable String entityId,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> permissionNames = (List<String>) request.get("permissions");
            String notes = (String) request.get("notes");

            if (permissionNames == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "permissions list is required"));
            }

            Optional<Organization> orgOpt = organizationRepository.findByEntityId(entityId);
            if (orgOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Organization not found"));
            }

            Organization org = orgOpt.get();

            // Get existing permissions
            List<com.example.attendancesystem.model.OrganizationPermission> existingPermissions =
                organizationPermissionRepository.findByOrganization(org);

            // Convert permission names to enum set
            Set<com.example.attendancesystem.model.FeaturePermission> newPermissions = new HashSet<>();
            for (String permissionName : permissionNames) {
                try {
                    com.example.attendancesystem.model.FeaturePermission permission =
                        com.example.attendancesystem.model.FeaturePermission.valueOf(permissionName);
                    newPermissions.add(permission);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid permission name: {}", permissionName);
                }
            }

            // Remove permissions that are no longer needed
            for (com.example.attendancesystem.model.OrganizationPermission existing : existingPermissions) {
                if (!newPermissions.contains(existing.getFeaturePermission())) {
                    organizationPermissionRepository.delete(existing);
                }
            }

            // Add new permissions that don't exist
            Set<com.example.attendancesystem.model.FeaturePermission> existingPermissionSet =
                existingPermissions.stream()
                    .map(com.example.attendancesystem.model.OrganizationPermission::getFeaturePermission)
                    .collect(Collectors.toSet());

            for (com.example.attendancesystem.model.FeaturePermission permission : newPermissions) {
                if (!existingPermissionSet.contains(permission)) {
                    com.example.attendancesystem.model.OrganizationPermission orgPermission =
                        new com.example.attendancesystem.model.OrganizationPermission();
                    orgPermission.setOrganization(org);
                    orgPermission.setFeaturePermission(permission);
                    orgPermission.setGrantedBy("superadmin");
                    orgPermission.setGrantedAt(LocalDateTime.now());
                    orgPermission.setIsEnabled(true);
                    orgPermission.setNotes(notes);
                    organizationPermissionRepository.save(orgPermission);
                }
            }

            logger.info("Updated permissions for organization {} with {} permissions. Notes: {}",
                    entityId, permissionNames.size(), notes);

            return ResponseEntity.ok(Map.of(
                "message", "Permissions updated successfully",
                "entityId", entityId,
                "permissions", permissionNames,
                "notes", notes
            ));

        } catch (Exception e) {
            logger.error("Failed to update organization permissions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update permissions: " + e.getMessage()));
        }
    }
}
