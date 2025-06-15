package com.example.attendancesystem.service;

import com.example.attendancesystem.model.FeaturePermission;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.OrganizationPermission;
import com.example.attendancesystem.repository.OrganizationPermissionRepository;
import com.example.attendancesystem.repository.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class PermissionService {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionService.class);
    
    @Autowired
    private OrganizationPermissionRepository permissionRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    /**
     * Check if organization has specific permission
     */
    public boolean hasPermission(String entityId, FeaturePermission permission) {
        return permissionRepository.hasActivePermissionByEntityId(entityId, permission, LocalDateTime.now());
    }
    
    /**
     * Check if organization has specific permission
     */
    public boolean hasPermission(Organization organization, FeaturePermission permission) {
        return permissionRepository.hasActivePermission(organization, permission, LocalDateTime.now());
    }
    
    /**
     * Check if organization has any of the specified permissions
     */
    public boolean hasAnyPermission(String entityId, FeaturePermission... permissions) {
        return Arrays.stream(permissions)
                .anyMatch(permission -> hasPermission(entityId, permission));
    }
    
    /**
     * Check if organization has all of the specified permissions
     */
    public boolean hasAllPermissions(String entityId, FeaturePermission... permissions) {
        return Arrays.stream(permissions)
                .allMatch(permission -> hasPermission(entityId, permission));
    }
    
    /**
     * Get all active permissions for an organization
     */
    public List<OrganizationPermission> getActivePermissions(String entityId) {
        return permissionRepository.findActivePermissionsByEntityId(entityId, LocalDateTime.now());
    }
    
    /**
     * Get all permissions (active and inactive) for an organization
     */
    public List<OrganizationPermission> getAllPermissions(String entityId) {
        return permissionRepository.findByOrganizationEntityId(entityId);
    }
    
    /**
     * Grant permission to an organization
     */
    public OrganizationPermission grantPermission(String entityId, FeaturePermission permission, 
                                                 String grantedBy, LocalDateTime expiresAt, String notes) {
        logger.info("Granting permission {} to organization {} by {}", permission, entityId, grantedBy);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + entityId));
        
        // Check if permission already exists
        var existingPermission = permissionRepository.findByOrganizationAndFeaturePermission(organization, permission);
        
        if (existingPermission.isPresent()) {
            // Update existing permission
            OrganizationPermission perm = existingPermission.get();
            perm.setIsEnabled(true);
            perm.setGrantedBy(grantedBy);
            perm.setGrantedAt(LocalDateTime.now());
            perm.setExpiresAt(expiresAt);
            perm.setNotes(notes);
            
            OrganizationPermission saved = permissionRepository.save(perm);
            logger.info("Updated existing permission {} for organization {}", permission, entityId);
            return saved;
        } else {
            // Create new permission
            OrganizationPermission newPermission = new OrganizationPermission(organization, permission, grantedBy);
            newPermission.setExpiresAt(expiresAt);
            newPermission.setNotes(notes);
            
            OrganizationPermission saved = permissionRepository.save(newPermission);
            logger.info("Created new permission {} for organization {}", permission, entityId);
            return saved;
        }
    }
    
    /**
     * Grant permission to an organization (without expiration)
     */
    public OrganizationPermission grantPermission(String entityId, FeaturePermission permission, String grantedBy) {
        return grantPermission(entityId, permission, grantedBy, null, null);
    }
    
    /**
     * Revoke permission from an organization
     */
    public void revokePermission(String entityId, FeaturePermission permission, String revokedBy) {
        logger.info("Revoking permission {} from organization {} by {}", permission, entityId, revokedBy);
        
        Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + entityId));
        
        var existingPermission = permissionRepository.findByOrganizationAndFeaturePermission(organization, permission);
        
        if (existingPermission.isPresent()) {
            OrganizationPermission perm = existingPermission.get();
            perm.setIsEnabled(false);
            perm.setNotes("Revoked by " + revokedBy + " at " + LocalDateTime.now());
            
            permissionRepository.save(perm);
            logger.info("Revoked permission {} from organization {}", permission, entityId);
        } else {
            logger.warn("Attempted to revoke non-existent permission {} from organization {}", permission, entityId);
        }
    }
    
    /**
     * Grant multiple permissions to an organization
     */
    public List<OrganizationPermission> grantPermissions(String entityId, List<FeaturePermission> permissions, 
                                                        String grantedBy, LocalDateTime expiresAt, String notes) {
        return permissions.stream()
                .map(permission -> grantPermission(entityId, permission, grantedBy, expiresAt, notes))
                .collect(Collectors.toList());
    }
    
    /**
     * Revoke multiple permissions from an organization
     */
    public void revokePermissions(String entityId, List<FeaturePermission> permissions, String revokedBy) {
        permissions.forEach(permission -> revokePermission(entityId, permission, revokedBy));
    }
    
    /**
     * Get permission status map for an organization
     */
    public Map<FeaturePermission, Boolean> getPermissionStatus(String entityId) {
        List<OrganizationPermission> activePermissions = getActivePermissions(entityId);
        
        return Arrays.stream(FeaturePermission.values())
                .collect(Collectors.toMap(
                        permission -> permission,
                        permission -> activePermissions.stream()
                                .anyMatch(p -> p.getFeaturePermission() == permission && p.isActive())
                ));
    }
    
    /**
     * Get organizations with specific permission
     */
    public List<Organization> getOrganizationsWithPermission(FeaturePermission permission) {
        return permissionRepository.findOrganizationsWithPermission(permission, LocalDateTime.now());
    }
    
    /**
     * Clean up expired permissions
     */
    public void cleanupExpiredPermissions() {
        List<OrganizationPermission> expiredPermissions = permissionRepository.findExpiredPermissions(LocalDateTime.now());
        
        for (OrganizationPermission permission : expiredPermissions) {
            permission.setIsEnabled(false);
            permission.setNotes("Automatically disabled due to expiration");
            permissionRepository.save(permission);
        }
        
        logger.info("Cleaned up {} expired permissions", expiredPermissions.size());
    }
    
    /**
     * Get permissions expiring soon
     */
    public List<OrganizationPermission> getPermissionsExpiringSoon(int daysAhead) {
        LocalDateTime threshold = LocalDateTime.now().plusDays(daysAhead);
        return permissionRepository.findPermissionsExpiringSoon(LocalDateTime.now(), threshold);
    }
    
    /**
     * Check if organization has menu/ordering permissions
     */
    public boolean hasMenuOrderingAccess(String entityId) {
        return hasAnyPermission(entityId, FeaturePermission.getMenuOrderingPermissions());
    }
    
    /**
     * Check if organization has attendance permissions
     */
    public boolean hasAttendanceAccess(String entityId) {
        return hasAnyPermission(entityId, FeaturePermission.getAttendancePermissions());
    }
    
    /**
     * Get permission summary for an organization
     */
    public Map<String, Object> getPermissionSummary(String entityId) {
        List<OrganizationPermission> activePermissions = getActivePermissions(entityId);
        Map<FeaturePermission, Boolean> permissionStatus = getPermissionStatus(entityId);
        
        return Map.of(
                "entityId", entityId,
                "totalActivePermissions", activePermissions.size(),
                "hasMenuAccess", hasMenuOrderingAccess(entityId),
                "hasAttendanceAccess", hasAttendanceAccess(entityId),
                "permissions", permissionStatus,
                "lastUpdated", LocalDateTime.now()
        );
    }
}
