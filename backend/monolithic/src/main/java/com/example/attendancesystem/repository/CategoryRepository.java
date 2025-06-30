package com.example.attendancesystem.subscriber.repository;

import com.example.attendancesystem.model.Category;
import com.example.attendancesystem.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    /**
     * Find all categories by organization
     */
    List<Category> findByOrganizationOrderByDisplayOrderAscNameAsc(Organization organization);
    
    /**
     * Find all active categories by organization
     */
    List<Category> findByOrganizationAndIsActiveTrueOrderByDisplayOrderAscNameAsc(Organization organization);
    
    /**
     * Find category by ID and organization
     */
    Optional<Category> findByIdAndOrganization(Long id, Organization organization);
    
    /**
     * Find category by name and organization
     */
    Optional<Category> findByNameAndOrganization(String name, Organization organization);
    
    /**
     * Check if category name exists for organization (excluding specific ID)
     */
    boolean existsByNameAndOrganizationAndIdNot(String name, Organization organization, Long id);
    
    /**
     * Check if category name exists for organization
     */
    boolean existsByNameAndOrganization(String name, Organization organization);
    
    /**
     * Count categories by organization
     */
    long countByOrganization(Organization organization);

    /**
     * Delete all categories by organization
     */
    void deleteByOrganization(Organization organization);
    
    /**
     * Count active categories by organization
     */
    long countByOrganizationAndIsActiveTrue(Organization organization);
    
    /**
     * Find categories with items count
     */
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.items WHERE c.organization = :organization AND c.isActive = true ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findCategoriesWithItems(@Param("organization") Organization organization);
    
    /**
     * Find categories by organization entity ID
     */
    @Query("SELECT c FROM Category c WHERE c.organization.entityId = :entityId ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findByOrganizationEntityId(@Param("entityId") String entityId);
    
    /**
     * Find active categories by organization entity ID
     */
    @Query("SELECT c FROM Category c WHERE c.organization.entityId = :entityId AND c.isActive = true ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findActiveByOrganizationEntityId(@Param("entityId") String entityId);
}
