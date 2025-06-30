package com.example.attendancesystem.shared.repository;

import com.example.attendancesystem.shared.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Organization entity in subscriber-service
 * This provides read-only access to organization data for subscriber operations
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    // Find by entity ID
    Optional<Organization> findByEntityId(String entityId);
    
    // Find by name
    Optional<Organization> findByName(String name);
    
    // Existence checks
    boolean existsByEntityId(String entityId);
    boolean existsByName(String name);
}
