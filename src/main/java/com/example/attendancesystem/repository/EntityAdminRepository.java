package com.example.attendancesystem.subscriber.repository;

import com.example.attendancesystem.model.EntityAdmin;
import com.example.attendancesystem.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntityAdminRepository extends JpaRepository<EntityAdmin, Long> {
    Optional<EntityAdmin> findByUsername(String username);
    boolean existsByOrganization(Organization organization);
    Optional<EntityAdmin> findByOrganization(Organization organization);
    List<EntityAdmin> findAllByOrganization(Organization organization);
    long countByOrganization(Organization organization);
    void deleteByOrganization(Organization organization);

    // Entity ID based methods
    @Query("SELECT ea FROM EntityAdmin ea WHERE ea.organization.entityId = :entityId")
    List<EntityAdmin> findAllByOrganizationEntityId(@Param("entityId") String entityId);

    @Query("SELECT ea FROM EntityAdmin ea WHERE ea.organization.entityId = :entityId")
    Optional<EntityAdmin> findByOrganizationEntityId(@Param("entityId") String entityId);

    @Query("SELECT CASE WHEN COUNT(ea) > 0 THEN true ELSE false END FROM EntityAdmin ea WHERE ea.organization.entityId = :entityId")
    boolean existsByOrganizationEntityId(@Param("entityId") String entityId);

    @Query("SELECT COUNT(ea) FROM EntityAdmin ea WHERE ea.organization.entityId = :entityId")
    long countByOrganizationEntityId(@Param("entityId") String entityId);

    // Methods for dashboard statistics - removed due to missing createdAt field
}
