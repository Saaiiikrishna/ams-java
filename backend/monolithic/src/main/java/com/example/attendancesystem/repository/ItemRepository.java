package com.example.attendancesystem.subscriber.repository;

import com.example.attendancesystem.model.Category;
import com.example.attendancesystem.model.Item;
import com.example.attendancesystem.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    
    /**
     * Find all items by organization
     */
    List<Item> findByOrganizationOrderByDisplayOrderAscNameAsc(Organization organization);
    
    /**
     * Find all active and available items by organization
     */
    List<Item> findByOrganizationAndIsActiveTrueAndIsAvailableTrueOrderByDisplayOrderAscNameAsc(Organization organization);
    
    /**
     * Find items by category
     */
    List<Item> findByCategoryOrderByDisplayOrderAscNameAsc(Category category);
    
    /**
     * Find active and available items by category
     */
    List<Item> findByCategoryAndIsActiveTrueAndIsAvailableTrueOrderByDisplayOrderAscNameAsc(Category category);
    
    /**
     * Find item by ID and organization
     */
    Optional<Item> findByIdAndOrganization(Long id, Organization organization);
    
    /**
     * Find item by name and category
     */
    Optional<Item> findByNameAndCategory(String name, Category category);
    
    /**
     * Check if item name exists in category (excluding specific ID)
     */
    boolean existsByNameAndCategoryAndIdNot(String name, Category category, Long id);
    
    /**
     * Check if item name exists in category
     */
    boolean existsByNameAndCategory(String name, Category category);
    
    /**
     * Count items by organization
     */
    long countByOrganization(Organization organization);

    /**
     * Delete all items by organization
     */
    void deleteByOrganization(Organization organization);
    
    /**
     * Count active items by organization
     */
    long countByOrganizationAndIsActiveTrue(Organization organization);
    
    /**
     * Count available items by organization
     */
    long countByOrganizationAndIsActiveTrueAndIsAvailableTrue(Organization organization);
    
    /**
     * Count items by category
     */
    long countByCategory(Category category);
    
    /**
     * Count active items by category
     */
    long countByCategoryAndIsActiveTrue(Category category);
    
    /**
     * Find items by organization entity ID
     */
    @Query("SELECT i FROM Item i WHERE i.organization.entityId = :entityId ORDER BY i.displayOrder ASC, i.name ASC")
    List<Item> findByOrganizationEntityId(@Param("entityId") String entityId);
    
    /**
     * Find active and available items by organization entity ID
     */
    @Query("SELECT i FROM Item i WHERE i.organization.entityId = :entityId AND i.isActive = true AND i.isAvailable = true ORDER BY i.displayOrder ASC, i.name ASC")
    List<Item> findAvailableByOrganizationEntityId(@Param("entityId") String entityId);
    
    /**
     * Search items by name or description
     */
    @Query("SELECT i FROM Item i WHERE i.organization = :organization AND (LOWER(i.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(i.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) ORDER BY i.displayOrder ASC, i.name ASC")
    List<Item> searchByNameOrDescription(@Param("organization") Organization organization, @Param("searchTerm") String searchTerm);
}
