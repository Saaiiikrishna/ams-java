package com.example.attendancesystem.controller;

import com.example.attendancesystem.model.FeaturePermission;
import com.example.attendancesystem.security.CustomUserDetails;
import com.example.attendancesystem.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/entity/permissions")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ENTITY_ADMIN')")
public class EntityPermissionController {
    
    private static final Logger logger = LoggerFactory.getLogger(EntityPermissionController.class);
    
    @Autowired
    private PermissionService permissionService;
    
    /**
     * Get current entity's permission status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getPermissionStatus() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            String entityId = userDetails.getEntityAdmin().getOrganization().getEntityId();

            Map<String, Object> permissionSummary = permissionService.getPermissionSummary(entityId);

            return ResponseEntity.ok(permissionSummary);

        } catch (Exception e) {
            logger.error("Failed to get permission status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Check if current entity has specific permission
     */
    @GetMapping("/check/{permission}")
    public ResponseEntity<?> checkPermission(@PathVariable FeaturePermission permission) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            String entityId = userDetails.getEntityAdmin().getOrganization().getEntityId();

            boolean hasPermission = permissionService.hasPermission(entityId, permission);

            return ResponseEntity.ok(Map.of(
                    "entityId", entityId,
                    "permission", permission,
                    "permissionName", permission.getDisplayName(),
                    "hasPermission", hasPermission,
                    "checkedAt", LocalDateTime.now()
            ));

        } catch (Exception e) {
            logger.error("Failed to check permission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Check if current entity has menu/ordering access
     */
    @GetMapping("/check/menu-access")
    public ResponseEntity<?> checkMenuAccess() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            String entityId = userDetails.getEntityAdmin().getOrganization().getEntityId();

            boolean hasMenuAccess = permissionService.hasMenuOrderingAccess(entityId);

            Map<FeaturePermission, Boolean> menuPermissions = Map.of(
                    FeaturePermission.MENU_MANAGEMENT, permissionService.hasPermission(entityId, FeaturePermission.MENU_MANAGEMENT),
                    FeaturePermission.ORDER_MANAGEMENT, permissionService.hasPermission(entityId, FeaturePermission.ORDER_MANAGEMENT),
                    FeaturePermission.TABLE_MANAGEMENT, permissionService.hasPermission(entityId, FeaturePermission.TABLE_MANAGEMENT)
            );

            return ResponseEntity.ok(Map.of(
                    "entityId", entityId,
                    "hasMenuAccess", hasMenuAccess,
                    "menuPermissions", menuPermissions,
                    "checkedAt", LocalDateTime.now()
            ));

        } catch (Exception e) {
            logger.error("Failed to check menu access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Check if current entity has attendance access
     */
    @GetMapping("/check/attendance-access")
    public ResponseEntity<?> checkAttendanceAccess() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            String entityId = userDetails.getEntityAdmin().getOrganization().getEntityId();

            boolean hasAttendanceAccess = permissionService.hasAttendanceAccess(entityId);

            Map<FeaturePermission, Boolean> attendancePermissions = Map.of(
                    FeaturePermission.ATTENDANCE_TRACKING, permissionService.hasPermission(entityId, FeaturePermission.ATTENDANCE_TRACKING),
                    FeaturePermission.MEMBER_MANAGEMENT, permissionService.hasPermission(entityId, FeaturePermission.MEMBER_MANAGEMENT)
            );

            return ResponseEntity.ok(Map.of(
                    "entityId", entityId,
                    "hasAttendanceAccess", hasAttendanceAccess,
                    "attendancePermissions", attendancePermissions,
                    "checkedAt", LocalDateTime.now()
            ));

        } catch (Exception e) {
            logger.error("Failed to check attendance access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
