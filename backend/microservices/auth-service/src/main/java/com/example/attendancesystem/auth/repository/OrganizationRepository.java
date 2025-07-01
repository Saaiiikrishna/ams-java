package com.example.attendancesystem.auth.repository;

import com.example.attendancesystem.auth.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Organization entity in Auth Service
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    /**
     * Find organization by entity ID
     */
    Optional<Organization> findByEntityId(String entityId);
    
    /**
     * Find organization by name
     */
    Optional<Organization> findByName(String name);
    
    /**
     * Check if organization exists by name
     */
    boolean existsByName(String name);
    
    /**
     * Check if organization exists by entity ID
     */
    boolean existsByEntityId(String entityId);
}
