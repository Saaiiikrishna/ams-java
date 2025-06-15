package com.example.attendancesystem.repository;

import com.example.attendancesystem.model.FeaturePermission;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.OrganizationPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationPermissionRepository extends JpaRepository<OrganizationPermission, Long> {
    
    /**
     * Find all permissions for an organization
     */
    List<OrganizationPermission> findByOrganization(Organization organization);
    long countByOrganization(Organization organization);
    
    /**
     * Find all active permissions for an organization
     */
    @Query("SELECT op FROM OrganizationPermission op WHERE op.organization = :organization " +
           "AND op.isEnabled = true AND (op.expiresAt IS NULL OR op.expiresAt > :now)")
    List<OrganizationPermission> findActivePermissionsByOrganization(
            @Param("organization") Organization organization, 
            @Param("now") LocalDateTime now);
    
    /**
     * Find specific permission for an organization
     */
    Optional<OrganizationPermission> findByOrganizationAndFeaturePermission(
            Organization organization, FeaturePermission featurePermission);

    /**
     * Check if organization has specific permission (exists)
     */
    boolean existsByOrganizationAndFeaturePermission(
            Organization organization, FeaturePermission featurePermission);
    
    /**
     * Check if organization has specific permission
     */
    @Query("SELECT COUNT(op) > 0 FROM OrganizationPermission op WHERE op.organization = :organization " +
           "AND op.featurePermission = :permission AND op.isEnabled = true " +
           "AND (op.expiresAt IS NULL OR op.expiresAt > :now)")
    boolean hasActivePermission(@Param("organization") Organization organization, 
                               @Param("permission") FeaturePermission permission,
                               @Param("now") LocalDateTime now);
    
    /**
     * Find permissions by organization entity ID
     */
    @Query("SELECT op FROM OrganizationPermission op WHERE op.organization.entityId = :entityId")
    List<OrganizationPermission> findByOrganizationEntityId(@Param("entityId") String entityId);
    
    /**
     * Find active permissions by organization entity ID
     */
    @Query("SELECT op FROM OrganizationPermission op WHERE op.organization.entityId = :entityId " +
           "AND op.isEnabled = true AND (op.expiresAt IS NULL OR op.expiresAt > :now)")
    List<OrganizationPermission> findActivePermissionsByEntityId(
            @Param("entityId") String entityId, 
            @Param("now") LocalDateTime now);
    
    /**
     * Check if organization has specific permission by entity ID
     */
    @Query("SELECT COUNT(op) > 0 FROM OrganizationPermission op WHERE op.organization.entityId = :entityId " +
           "AND op.featurePermission = :permission AND op.isEnabled = true " +
           "AND (op.expiresAt IS NULL OR op.expiresAt > :now)")
    boolean hasActivePermissionByEntityId(@Param("entityId") String entityId, 
                                         @Param("permission") FeaturePermission permission,
                                         @Param("now") LocalDateTime now);
    
    /**
     * Find permissions granted by specific super admin
     */
    List<OrganizationPermission> findByGrantedBy(String grantedBy);
    
    /**
     * Find permissions by feature permission
     */
    List<OrganizationPermission> findByFeaturePermission(FeaturePermission featurePermission);
    
    /**
     * Find permissions expiring soon
     */
    @Query("SELECT op FROM OrganizationPermission op WHERE op.expiresAt IS NOT NULL " +
           "AND op.expiresAt BETWEEN :now AND :expiryThreshold AND op.isEnabled = true")
    List<OrganizationPermission> findPermissionsExpiringSoon(
            @Param("now") LocalDateTime now, 
            @Param("expiryThreshold") LocalDateTime expiryThreshold);
    
    /**
     * Find expired permissions
     */
    @Query("SELECT op FROM OrganizationPermission op WHERE op.expiresAt IS NOT NULL " +
           "AND op.expiresAt < :now AND op.isEnabled = true")
    List<OrganizationPermission> findExpiredPermissions(@Param("now") LocalDateTime now);
    
    /**
     * Count active permissions for an organization
     */
    @Query("SELECT COUNT(op) FROM OrganizationPermission op WHERE op.organization = :organization " +
           "AND op.isEnabled = true AND (op.expiresAt IS NULL OR op.expiresAt > :now)")
    long countActivePermissions(@Param("organization") Organization organization, 
                               @Param("now") LocalDateTime now);
    
    /**
     * Find organizations with specific permission
     */
    @Query("SELECT DISTINCT op.organization FROM OrganizationPermission op " +
           "WHERE op.featurePermission = :permission AND op.isEnabled = true " +
           "AND (op.expiresAt IS NULL OR op.expiresAt > :now)")
    List<Organization> findOrganizationsWithPermission(@Param("permission") FeaturePermission permission,
                                                      @Param("now") LocalDateTime now);
    
    /**
     * Delete all permissions for an organization
     */
    void deleteByOrganization(Organization organization);
    
    /**
     * Delete specific permission for an organization
     */
    void deleteByOrganizationAndFeaturePermission(Organization organization, FeaturePermission featurePermission);
}
