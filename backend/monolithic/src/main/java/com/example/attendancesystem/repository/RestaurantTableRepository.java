package com.example.attendancesystem.subscriber.repository;

import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    
    /**
     * Find all tables by organization
     */
    List<RestaurantTable> findByOrganizationOrderByTableNumberAsc(Organization organization);
    
    /**
     * Find all active tables by organization
     */
    List<RestaurantTable> findByOrganizationAndIsActiveTrueOrderByTableNumberAsc(Organization organization);
    
    /**
     * Find table by ID and organization
     */
    Optional<RestaurantTable> findByIdAndOrganization(Long id, Organization organization);
    
    /**
     * Find table by table number and organization
     */
    Optional<RestaurantTable> findByTableNumberAndOrganization(Integer tableNumber, Organization organization);
    
    /**
     * Find table by QR code
     */
    Optional<RestaurantTable> findByQrCode(String qrCode);
    
    /**
     * Find table by QR code and organization
     */
    Optional<RestaurantTable> findByQrCodeAndOrganization(String qrCode, Organization organization);
    
    /**
     * Check if table number exists for organization (excluding specific ID)
     */
    boolean existsByTableNumberAndOrganizationAndIdNot(Integer tableNumber, Organization organization, Long id);
    
    /**
     * Check if table number exists for organization
     */
    boolean existsByTableNumberAndOrganization(Integer tableNumber, Organization organization);
    
    /**
     * Count tables by organization
     */
    long countByOrganization(Organization organization);
    
    /**
     * Count active tables by organization
     */
    long countByOrganizationAndIsActiveTrue(Organization organization);
    
    /**
     * Find tables by organization entity ID
     */
    @Query("SELECT t FROM RestaurantTable t WHERE t.organization.entityId = :entityId ORDER BY t.tableNumber ASC")
    List<RestaurantTable> findByOrganizationEntityId(@Param("entityId") String entityId);
    
    /**
     * Find active tables by organization entity ID (excluding soft deleted)
     */
    @Query("SELECT t FROM RestaurantTable t WHERE t.organization.entityId = :entityId AND t.isActive = true AND t.deletedAt IS NULL ORDER BY t.tableNumber ASC")
    List<RestaurantTable> findActiveByOrganizationEntityId(@Param("entityId") String entityId);

    /**
     * Find active table by QR code (excluding soft deleted)
     */
    @Query("SELECT t FROM RestaurantTable t WHERE t.qrCode = :qrCode AND t.isActive = true AND t.deletedAt IS NULL")
    Optional<RestaurantTable> findActiveByQrCode(@Param("qrCode") String qrCode);
    
    /**
     * Delete all tables by organization
     */
    void deleteByOrganization(Organization organization);
    
    /**
     * Find tables by organization with table numbers greater than specified number
     */
    List<RestaurantTable> findByOrganizationAndTableNumberGreaterThanOrderByTableNumberAsc(Organization organization, Integer tableNumber);
}
