package com.example.attendancesystem.organization.repository;

import com.example.attendancesystem.organization.model.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    // Find by entity ID
    java.util.Optional<Organization> findByEntityId(String entityId);
    java.util.Optional<Organization> findByEntityIdAndIsActiveTrue(String entityId);

    // Find by ID with active filter
    java.util.Optional<Organization> findByIdAndIsActiveTrue(Long id);

    // Find by name
    java.util.Optional<Organization> findByName(String name);

    // Find all active organizations
    java.util.List<Organization> findByIsActiveTrue();

    // Pagination support
    Page<Organization> findByIsActiveTrue(Pageable pageable);
    Page<Organization> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Organization> findByIsActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);

    // Existence checks
    boolean existsByName(String name);
    boolean existsByEntityId(String entityId);
    boolean existsByEntityIdAndIsActiveTrue(String entityId);
    boolean existsByNameAndIsActiveTrue(String name);

    // Delete operations
    void deleteByEntityId(String entityId);
}
