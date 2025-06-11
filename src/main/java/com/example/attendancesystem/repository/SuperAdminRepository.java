package com.example.attendancesystem.repository;

import com.example.attendancesystem.model.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuperAdminRepository extends JpaRepository<SuperAdmin, Long> {

    /**
     * Find a super admin by username
     */
    Optional<SuperAdmin> findByUsername(String username);

    /**
     * Find a super admin by email
     */
    Optional<SuperAdmin> findByEmail(String email);

    /**
     * Check if a super admin exists with the given username
     */
    boolean existsByUsername(String username);

    /**
     * Check if a super admin exists with the given email
     */
    boolean existsByEmail(String email);

    /**
     * Find all active super admins
     */
    List<SuperAdmin> findByIsActiveTrue();

    /**
     * Find all inactive super admins
     */
    List<SuperAdmin> findByIsActiveFalse();

    /**
     * Find super admins by role name
     */
    @Query("SELECT sa FROM SuperAdmin sa WHERE sa.role.name = :roleName")
    List<SuperAdmin> findByRoleName(String roleName);

    /**
     * Find active super admins by role name
     */
    @Query("SELECT sa FROM SuperAdmin sa WHERE sa.role.name = :roleName AND sa.isActive = true")
    List<SuperAdmin> findActiveByRoleName(String roleName);

    /**
     * Count total super admins
     */
    @Query("SELECT COUNT(sa) FROM SuperAdmin sa")
    long countTotalSuperAdmins();

    /**
     * Count active super admins
     */
    @Query("SELECT COUNT(sa) FROM SuperAdmin sa WHERE sa.isActive = true")
    long countActiveSuperAdmins();
}
