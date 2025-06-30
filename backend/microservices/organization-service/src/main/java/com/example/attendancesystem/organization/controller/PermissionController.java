package com.example.attendancesystem.organization.controller;

import com.example.attendancesystem.organization.dto.PermissionDto;
import com.example.attendancesystem.organization.dto.PermissionUpdateRequest;
import com.example.attendancesystem.organization.model.FeaturePermission;
import com.example.attendancesystem.organization.model.Organization;
import com.example.attendancesystem.organization.model.OrganizationPermission;
import com.example.attendancesystem.organization.repository.OrganizationRepository;
import com.example.attendancesystem.organization.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/permissions")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PermissionController {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionController.class);
    
    @Autowired
    private PermissionService permissionService;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    /**
     * Get all available feature permissions
     */
    @GetMapping("/features")
    public ResponseEntity<?> getAvailableFeatures() {
        try {
            List<Map<String, Object>> features = Arrays.stream(FeaturePermission.values())
                    .map(permission -> Map.<String, Object>of(
                            "permission", permission.name(),
                            "name", permission.getDisplayName(),
                            "description", permission.getDescription()
                    ))
                    .collect(Collectors.toList());
            
            Map<String, Object> response = Map.of(
                    "features", features,
                    "groups", Map.of(
                            "menuOrdering", Arrays.stream(FeaturePermission.getMenuOrderingPermissions())
                                    .map(FeaturePermission::name)
                                    .collect(Collectors.toList()),
                            "attendance", Arrays.stream(FeaturePermission.getAttendancePermissions())
                                    .map(FeaturePermission::name)
                                    .collect(Collectors.toList()),
                            "reports", Arrays.stream(FeaturePermission.getReportPermissions())
                                    .map(FeaturePermission::name)
                                    .collect(Collectors.toList()),
                            "advanced", Arrays.stream(FeaturePermission.getAdvancedPermissions())
                                    .map(FeaturePermission::name)
                                    .collect(Collectors.toList())
                    )
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get available features: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get permissions for a specific organization
     */
    @GetMapping("/organization/{entityId}")
    public ResponseEntity<?> getOrganizationPermissions(@PathVariable String entityId) {
        try {
            Organization organization = organizationRepository.findByEntityId(entityId)
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            
            List<OrganizationPermission> permissions = permissionService.getAllPermissions(entityId);
            Map<FeaturePermission, Boolean> permissionStatus = permissionService.getPermissionStatus(entityId);
            
            List<PermissionDto> permissionDtos = Arrays.stream(FeaturePermission.values())
                    .map(feature -> {
                        PermissionDto dto = new PermissionDto(feature, permissionStatus.get(feature));
                        dto.setEntityId(entityId);
                        dto.setOrganizationName(organization.getName());
                        
                        // Find existing permission details
                        permissions.stream()
                                .filter(p -> p.getFeaturePermission() == feature)
                                .findFirst()
                                .ifPresent(p -> {
                                    dto.setId(p.getId());
                                    dto.setIsEnabled(p.getIsEnabled());
                                    dto.setIsActive(p.isActive());
                                    dto.setGrantedBy(p.getGrantedBy());
                                    dto.setGrantedAt(p.getGrantedAt());
                                    dto.setExpiresAt(p.getExpiresAt());
                                    dto.setNotes(p.getNotes());
                                    dto.setCreatedAt(p.getCreatedAt());
                                    dto.setUpdatedAt(p.getUpdatedAt());
                                });
                        
                        return dto;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = Map.of(
                    "entityId", entityId,
                    "organizationName", organization.getName(),
                    "permissions", permissionDtos,
                    "summary", permissionService.getPermissionSummary(entityId)
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get organization permissions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Update permissions for an organization
     */
    @PostMapping("/organization/{entityId}/update")
    public ResponseEntity<?> updateOrganizationPermissions(
            @PathVariable String entityId,
            @Valid @RequestBody PermissionUpdateRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String superAdminId = auth.getName();
            
            logger.info("Super admin {} updating permissions for organization {}", superAdminId, entityId);
            
            // Grant new permissions
            if (request.getPermissions() != null && !request.getPermissions().isEmpty()) {
                List<OrganizationPermission> grantedPermissions = permissionService.grantPermissions(
                        entityId, 
                        request.getPermissions(), 
                        superAdminId, 
                        request.getExpiresAt(), 
                        request.getNotes()
                );
                
                logger.info("Granted {} permissions to organization {}", grantedPermissions.size(), entityId);
            }
            
            // Get updated permissions
            Map<String, Object> updatedPermissions = permissionService.getPermissionSummary(entityId);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Permissions updated successfully",
                    "entityId", entityId,
                    "updatedBy", superAdminId,
                    "updatedAt", LocalDateTime.now(),
                    "permissions", updatedPermissions
            ));
            
        } catch (Exception e) {
            logger.error("Failed to update organization permissions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Grant specific permission to an organization
     */
    @PostMapping("/organization/{entityId}/grant/{permission}")
    public ResponseEntity<?> grantPermission(
            @PathVariable String entityId,
            @PathVariable FeaturePermission permission,
            @RequestBody(required = false) Map<String, Object> requestBody) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String superAdminId = auth.getName();
            
            LocalDateTime expiresAt = null;
            String notes = null;
            
            if (requestBody != null) {
                if (requestBody.containsKey("expiresAt")) {
                    expiresAt = LocalDateTime.parse((String) requestBody.get("expiresAt"));
                }
                if (requestBody.containsKey("notes")) {
                    notes = (String) requestBody.get("notes");
                }
            }
            
            OrganizationPermission grantedPermission = permissionService.grantPermission(
                    entityId, permission, superAdminId, expiresAt, notes);
            
            logger.info("Granted permission {} to organization {} by {}", permission, entityId, superAdminId);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Permission granted successfully",
                    "permission", permission.getDisplayName(),
                    "entityId", entityId,
                    "grantedBy", superAdminId,
                    "grantedAt", grantedPermission.getGrantedAt()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to grant permission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Revoke specific permission from an organization
     */
    @DeleteMapping("/organization/{entityId}/revoke/{permission}")
    public ResponseEntity<?> revokePermission(
            @PathVariable String entityId,
            @PathVariable FeaturePermission permission) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String superAdminId = auth.getName();
            
            permissionService.revokePermission(entityId, permission, superAdminId);
            
            logger.info("Revoked permission {} from organization {} by {}", permission, entityId, superAdminId);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Permission revoked successfully",
                    "permission", permission.getDisplayName(),
                    "entityId", entityId,
                    "revokedBy", superAdminId,
                    "revokedAt", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to revoke permission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get all organizations with their permission status
     */
    @GetMapping("/organizations/summary")
    public ResponseEntity<?> getOrganizationsPermissionSummary() {
        try {
            List<Organization> organizations = organizationRepository.findAll();
            
            List<Map<String, Object>> summary = organizations.stream()
                    .map(org -> {
                        Map<String, Object> orgSummary = permissionService.getPermissionSummary(org.getEntityId());
                        orgSummary.put("organizationName", org.getName());
                        orgSummary.put("organizationId", org.getId());
                        return orgSummary;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                    "organizations", summary,
                    "totalOrganizations", organizations.size(),
                    "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get organizations permission summary: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
