package com.example.attendancesystem.repository;

import com.example.attendancesystem.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    boolean existsByName(String name);
    java.util.Optional<Organization> findByName(String name);
    java.util.Optional<Organization> findByEntityId(String entityId);
    boolean existsByEntityId(String entityId);

    // Additional methods for entity ID based operations
    void deleteByEntityId(String entityId);

    // Methods for dashboard statistics - removed due to missing createdAt field
}
