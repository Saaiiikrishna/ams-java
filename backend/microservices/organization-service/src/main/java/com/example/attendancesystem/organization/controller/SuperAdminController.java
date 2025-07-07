package com.example.attendancesystem.organization.controller;

import com.example.attendancesystem.organization.dto.CreateEntityAdminRequest;
// Removed UserServiceGrpcClient import for microservices independence
import com.example.attendancesystem.organization.model.Organization;
import com.example.attendancesystem.organization.repository.OrganizationRepository;
import com.example.attendancesystem.organization.repository.OrganizationPermissionRepository;
import com.example.attendancesystem.organization.service.EntityIdService;
// Removed UserResponse import for microservices independence
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Super Admin Controller for Organization Service
 * Handles super admin operations related to organizations and cross-service coordination
 */
@RestController
@RequestMapping("/super")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminController.class);

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationPermissionRepository organizationPermissionRepository;

    // Removed UserServiceGrpcClient dependency for microservices independence

    @Autowired
    private EntityIdService entityIdService;

    /**
     * Create new organization
     * POST /organization/super/organizations
     * Also available as POST /organization/super/entities (alias for compatibility)
     */
    @PostMapping({"/organizations", "/entities"})
    public ResponseEntity<?> createOrganization(@Valid @RequestBody CreateOrganizationRequest request) {
        try {
            logger.info("SuperAdmin creating organization: {}", request.getName());

            // Check if organization name already exists
            if (organizationRepository.existsByNameAndIsActiveTrue(request.getName())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Organization with this name already exists");
                errorResponse.put("errorCode", "ORGANIZATION_EXISTS");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            // Create organization
            Organization organization = new Organization();
            organization.setName(request.getName());
            organization.setAddress(request.getAddress());
            organization.setEmail(request.getContactEmail());
            organization.setContactPerson(request.getContactPhone());
            organization.setLatitude(request.getLatitude());
            organization.setLongitude(request.getLongitude());
            organization.setCreatedAt(LocalDateTime.now());
            organization.setUpdatedAt(LocalDateTime.now());
            organization.setIsActive(true);

            // Generate unique entity ID
            String entityId = entityIdService.generateUniqueEntityId();
            organization.setEntityId(entityId);

            Organization savedOrganization = organizationRepository.save(organization);

            logger.info("Organization created successfully: {} with Entity ID: {}",
                       savedOrganization.getName(), savedOrganization.getEntityId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Organization created successfully");
            response.put("organization", convertToResponseDto(savedOrganization));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Error creating organization: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Internal server error");
            errorResponse.put("errorCode", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get all organizations
     * GET /organization/super/organizations
     * Also available as GET /organization/super/entities (alias for compatibility)
     */
    @GetMapping({"/organizations", "/entities"})
    public ResponseEntity<?> getAllOrganizations() {
        try {
            logger.info("SuperAdmin retrieving all organizations");

            List<Organization> organizations = organizationRepository.findByIsActiveTrue();

            List<Map<String, Object>> organizationDtos = organizations.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Organizations retrieved successfully");
            response.put("organizations", organizationDtos);
            response.put("totalCount", organizations.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving organizations: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Create Entity Admin for an organization
     * SuperAdmin can assign Entity Admins to manage organizations
     * POST /organization/super/entity-admins
     */
    @PostMapping("/entity-admins")
    public ResponseEntity<?> createEntityAdmin(@Valid @RequestBody CreateEntityAdminRequest request) {
        try {
            logger.info("SuperAdmin creating Entity Admin: {} for organization ID: {}",
                       request.getUsername(), request.getOrganizationId());

            // Verify organization exists and is active
            Optional<Organization> organizationOpt = organizationRepository.findById(request.getOrganizationId());
            if (organizationOpt.isEmpty() || !organizationOpt.get().getIsActive()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Organization not found or inactive");
                errorResponse.put("errorCode", "ORGANIZATION_NOT_FOUND");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            Organization organization = organizationOpt.get();

            // Get current user ID from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long currentUserId = extractUserIdFromAuthentication(authentication);

            // Validate that the current user is indeed a SuperAdmin
            if (!hasRole(authentication, "SUPER_ADMIN")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Access denied. Only SuperAdmins can create Entity Admins");
                errorResponse.put("errorCode", "ACCESS_DENIED");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            // Note: In microservices architecture, Entity Admin creation should be handled
            // by the User Service independently via its own endpoints.
            // Organization Service focuses on organization management only.
            logger.info("Entity Admin creation request received for organization: {}", request.getOrganizationId());
            logger.info("Note: Entity Admin should be created via User Service endpoints independently");

            // Prepare response - Organization Service only validates organization
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Organization validated successfully. Please create Entity Admin via User Service.");
            response.put("organizationInfo", Map.of(
                    "organizationId", request.getOrganizationId(),
                    "organizationName", organization.getName(),
                    "entityId", organization.getEntityId(),
                    "note", "Entity Admin creation should be done via User Service endpoints"
            ));

            logger.info("Organization validation successful for Entity Admin request: {} for organization: {} (Entity ID: {})",
                       request.getUsername(), organization.getName(), organization.getEntityId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Error creating Entity Admin for organization ID: {}", request.getOrganizationId(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Internal server error: " + e.getMessage());
            errorResponse.put("errorCode", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Hash password via Auth Service
     */
    private String hashPasswordViaAuthService(String plainPassword) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, String> request = new HashMap<>();
            request.put("password", plainPassword);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "http://ams-auth-service:8081/auth/api/v2/auth/hash-password",
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    return (String) responseBody.get("hashedPassword");
                }
            }

            logger.error("Failed to hash password via Auth Service");
            return null;

        } catch (Exception e) {
            logger.error("Error calling Auth Service to hash password", e);
            return null;
        }
    }

    /**
     * Get system metrics (organization-related metrics)
     * TODO: Aggregate metrics from other services via gRPC
     */
    @GetMapping("/system-metrics")
    public ResponseEntity<?> getSystemMetrics() {
        try {
            // Organization-specific metrics
            long totalOrganizations = organizationRepository.count();
            long activeOrganizations = organizationRepository.findByIsActiveTrue().size();
            long totalPermissions = organizationPermissionRepository.count();

            // TODO: Get metrics from other services via gRPC
            // long totalUsers = subscriberServiceClient.getTotalSubscribers();
            // long totalSessions = attendanceServiceClient.getTotalSessions();
            // long totalOrders = orderServiceClient.getTotalOrders();

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("totalOrganizations", totalOrganizations);
            metrics.put("activeOrganizations", activeOrganizations);
            metrics.put("totalPermissions", totalPermissions);
            
            // Placeholder values until gRPC clients are implemented
            metrics.put("totalUsers", 0);
            metrics.put("totalSessions", 0);
            metrics.put("totalOrders", 0);
            metrics.put("systemLoad", getSystemLoad());

            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Failed to fetch system metrics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch system metrics"));
        }
    }

    /**
     * Placeholder endpoint for NFC cards statistics
     * TODO: Implement proper NFC cards service integration
     */
    @GetMapping("/placeholder/nfc-cards")
    public ResponseEntity<?> getNfcCardsPlaceholder() {
        try {
            Map<String, Object> nfcStats = new HashMap<>();
            nfcStats.put("count", 0);
            nfcStats.put("totalCards", 0);
            nfcStats.put("assignedCards", 0);
            nfcStats.put("unassignedCards", 0);
            nfcStats.put("message", "NFC Cards service not yet implemented in microservices");

            return ResponseEntity.ok(nfcStats);
        } catch (Exception e) {
            logger.error("Failed to fetch NFC cards placeholder: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch NFC cards data", "count", 0));
        }
    }

    /**
     * Placeholder endpoint for recent activity
     * TODO: Implement proper activity logging service
     */
    @GetMapping("/placeholder/recent-activity")
    public ResponseEntity<?> getRecentActivityPlaceholder() {
        try {
            // Return empty activity list for now
            return ResponseEntity.ok(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Failed to fetch recent activity placeholder: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ArrayList<>());
        }
    }

    /**
     * Get organization-specific data count
     * TODO: Implement cross-service data counting via gRPC
     */
    @GetMapping("/organization/{entityId}/data-count")
    public ResponseEntity<?> getOrganizationDataCount(@PathVariable String entityId) {
        try {
            Optional<Organization> organizationOpt = organizationRepository.findByEntityIdAndIsActiveTrue(entityId);
            if (organizationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Organization organization = organizationOpt.get();
            
            // Organization-specific data
            long organizationPermissionsCount = organizationPermissionRepository.countByOrganization(organization);

            // TODO: Get data counts from other services via gRPC
            // long subscribersCount = subscriberServiceClient.countByOrganization(organization.getId());
            // long entityAdminsCount = authServiceClient.countEntityAdminsByOrganization(organization.getId());
            // long attendanceSessionsCount = attendanceServiceClient.countByOrganization(organization.getId());
            // long categoriesCount = menuServiceClient.countCategoriesByOrganization(organization.getId());
            // long itemsCount = menuServiceClient.countItemsByOrganization(organization.getId());
            // long ordersCount = orderServiceClient.countByOrganization(organization.getId());
            // long tablesCount = tableServiceClient.countByOrganization(organization.getId());

            Map<String, Object> dataCount = new HashMap<>();
            dataCount.put("organizationPermissions", organizationPermissionsCount);
            
            // Placeholder values until gRPC clients are implemented
            dataCount.put("subscribers", 0);
            dataCount.put("entityAdmins", 0);
            dataCount.put("attendanceSessions", 0);
            dataCount.put("categories", 0);
            dataCount.put("items", 0);
            dataCount.put("orders", 0);
            dataCount.put("tables", 0);

            return ResponseEntity.ok(dataCount);
        } catch (Exception e) {
            logger.error("Error getting organization data count for entity ID: {}", entityId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get organization data count"));
        }
    }

    /**
     * Cleanup organization data
     * TODO: Implement cross-service cleanup via gRPC
     */
    @DeleteMapping("/organization/{entityId}/cleanup")
    public ResponseEntity<?> cleanupOrganizationData(@PathVariable String entityId) {
        try {
            Optional<Organization> organizationOpt = organizationRepository.findByEntityIdAndIsActiveTrue(entityId);
            if (organizationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Organization organization = organizationOpt.get();
            
            logger.warn("CLEANUP: Starting organization data cleanup for: {}", organization.getName());

            // Count data before deletion
            long organizationPermissionsCount = organizationPermissionRepository.countByOrganization(organization);

            // Delete organization-specific data
            organizationPermissionRepository.deleteByOrganization(organization);
            logger.info("CLEANUP: Deleted {} organization permissions", organizationPermissionsCount);

            // TODO: Delete data from other services via gRPC
            // subscriberServiceClient.deleteByOrganization(organization.getId());
            // authServiceClient.deleteEntityAdminsByOrganization(organization.getId());
            // attendanceServiceClient.deleteByOrganization(organization.getId());
            // menuServiceClient.deleteByOrganization(organization.getId());
            // orderServiceClient.deleteByOrganization(organization.getId());
            // tableServiceClient.deleteByOrganization(organization.getId());

            Map<String, Object> deletedRecords = new HashMap<>();
            deletedRecords.put("organizationPermissions", organizationPermissionsCount);
            deletedRecords.put("message", "Organization data cleanup completed");

            logger.info("CLEANUP: Organization data cleanup completed for: {}", organization.getName());
            return ResponseEntity.ok(deletedRecords);

        } catch (Exception e) {
            logger.error("Error during organization cleanup for entity ID: {}", entityId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to cleanup organization data"));
        }
    }

    /**
     * Get system load (simplified implementation)
     */
    private double getSystemLoad() {
        try {
            // Simple system load calculation
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            return (double) usedMemory / totalMemory * 100;
        } catch (Exception e) {
            logger.warn("Failed to calculate system load: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Extract user ID from authentication context
     * In a proper implementation, this should parse the JWT token
     */
    private Long extractUserIdFromAuthentication(Authentication authentication) {
        // TODO: Implement proper JWT token parsing to extract user ID
        // For now, return a default value
        // In production, this should extract the user ID from JWT claims
        if (authentication != null && authentication.getName() != null) {
            logger.debug("Extracting user ID for authenticated user: {}", authentication.getName());
            // This is a placeholder - in real implementation, parse JWT token
            return 1L; // SuperAdmin default ID
        }
        return 1L; // Default SuperAdmin ID
    }

    /**
     * Check if the authenticated user has the specified role
     */
    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        String roleWithPrefix = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(roleWithPrefix));
    }

    /**
     * Convert Organization entity to response DTO
     */
    private Map<String, Object> convertToResponseDto(Organization organization) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", organization.getId());
        dto.put("entityId", organization.getEntityId());
        dto.put("name", organization.getName());
        dto.put("address", organization.getAddress());
        dto.put("email", organization.getEmail());
        dto.put("contactPerson", organization.getContactPerson());
        dto.put("latitude", organization.getLatitude());
        dto.put("longitude", organization.getLongitude());
        dto.put("isActive", organization.getIsActive());
        dto.put("createdAt", organization.getCreatedAt());
        dto.put("updatedAt", organization.getUpdatedAt());
        return dto;
    }

    /**
     * DTO for organization creation request
     */
    public static class CreateOrganizationRequest {
        private String name;
        private String address;
        private String contactEmail;
        private String contactPhone;
        private Double latitude;
        private Double longitude;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getContactEmail() { return contactEmail; }
        public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }
}
