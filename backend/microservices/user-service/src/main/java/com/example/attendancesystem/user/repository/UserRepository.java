package com.example.attendancesystem.user.repository;

import com.example.attendancesystem.user.model.User;
import com.example.attendancesystem.user.model.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic User Repository
 * Handles all user types: SUPER_ADMIN, ENTITY_ADMIN, MEMBER
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ========== AUTHENTICATION QUERIES ==========
    
    /**
     * Find user by username (for authentication)
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by username and user type
     */
    Optional<User> findByUsernameAndUserType(String username, UserType userType);

    /**
     * Find user by mobile number (for member authentication)
     */
    Optional<User> findByMobileNumber(String mobileNumber);

    /**
     * Find user by mobile number and user type
     */
    Optional<User> findByMobileNumberAndUserType(String mobileNumber, UserType userType);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    // ========== USER TYPE QUERIES ==========
    
    /**
     * Find all users by type
     */
    List<User> findByUserType(UserType userType);

    /**
     * Find all users by type with pagination
     */
    Page<User> findByUserType(UserType userType, Pageable pageable);

    /**
     * Find all active users by type
     */
    List<User> findByUserTypeAndIsActive(UserType userType, Boolean isActive);

    /**
     * Find all active users by type with pagination
     */
    Page<User> findByUserTypeAndIsActive(UserType userType, Boolean isActive, Pageable pageable);

    // ========== ORGANIZATION QUERIES ==========
    
    /**
     * Find all users in an organization
     */
    @Query("SELECT u FROM User u WHERE u.organizationId = :organizationId")
    List<User> findByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * Find all users in an organization with pagination
     */
    @Query("SELECT u FROM User u WHERE u.organizationId = :organizationId")
    Page<User> findByOrganizationId(@Param("organizationId") Long organizationId, Pageable pageable);

    /**
     * Find users by organization and type
     */
    @Query("SELECT u FROM User u WHERE u.organizationId = :organizationId AND u.userType = :userType")
    List<User> findByOrganizationIdAndUserType(@Param("organizationId") Long organizationId,
                                               @Param("userType") UserType userType);

    /**
     * Find active users by organization and type
     */
    @Query("SELECT u FROM User u WHERE u.organizationId = :organizationId AND u.userType = :userType AND u.isActive = :isActive")
    List<User> findByOrganizationIdAndUserTypeAndIsActive(@Param("organizationId") Long organizationId,
                                                          @Param("userType") UserType userType,
                                                          @Param("isActive") Boolean isActive);

    // ========== SUPER ADMIN QUERIES ==========
    
    /**
     * Find all super admins
     */
    @Query("SELECT u FROM User u WHERE u.userType = 'SUPER_ADMIN'")
    List<User> findAllSuperAdmins();

    /**
     * Find active super admins
     */
    @Query("SELECT u FROM User u WHERE u.userType = 'SUPER_ADMIN' AND u.isActive = true")
    List<User> findActiveSuperAdmins();

    /**
     * Check if any super admin exists
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.userType = 'SUPER_ADMIN'")
    boolean existsSuperAdmin();

    // ========== ENTITY ADMIN QUERIES ==========
    
    /**
     * Find entity admins by organization
     */
    @Query("SELECT u FROM User u WHERE u.userType = 'ENTITY_ADMIN' AND u.organizationId = :organizationId")
    List<User> findEntityAdminsByOrganization(@Param("organizationId") Long organizationId);

    /**
     * Find active entity admins by organization
     */
    @Query("SELECT u FROM User u WHERE u.userType = 'ENTITY_ADMIN' AND u.organizationId = :organizationId AND u.isActive = true")
    List<User> findActiveEntityAdminsByOrganization(@Param("organizationId") Long organizationId);

    // ========== MEMBER QUERIES ==========
    
    /**
     * Find members by organization
     */
    @Query("SELECT u FROM User u WHERE u.userType = 'MEMBER' AND u.organizationId = :organizationId")
    List<User> findMembersByOrganization(@Param("organizationId") Long organizationId);

    /**
     * Find members by organization with pagination
     */
    @Query("SELECT u FROM User u WHERE u.userType = 'MEMBER' AND u.organizationId = :organizationId")
    Page<User> findMembersByOrganization(@Param("organizationId") Long organizationId, Pageable pageable);

    /**
     * Find active members by organization
     */
    @Query("SELECT u FROM User u WHERE u.userType = 'MEMBER' AND u.organizationId = :organizationId AND u.isActive = true")
    List<User> findActiveMembersByOrganization(@Param("organizationId") Long organizationId);

    /**
     * Find members by mobile number across organizations (for login)
     */
    @Query("SELECT u FROM User u WHERE u.userType = 'MEMBER' AND u.mobileNumber = :mobileNumber")
    List<User> findMembersByMobileNumber(@Param("mobileNumber") String mobileNumber);

    // ========== SEARCH QUERIES ==========
    
    /**
     * Search users by name or username
     */
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND u.userType = :userType")
    Page<User> searchUsersByNameAndType(@Param("searchTerm") String searchTerm, 
                                        @Param("userType") UserType userType, 
                                        Pageable pageable);

    /**
     * Search users in organization by name or username
     */
    @Query("SELECT u FROM User u WHERE " +
           "u.organizationId = :organizationId AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND u.userType = :userType")
    Page<User> searchUsersInOrganizationByNameAndType(@Param("organizationId") Long organizationId,
                                                      @Param("searchTerm") String searchTerm,
                                                      @Param("userType") UserType userType,
                                                      Pageable pageable);

    /**
     * Search all users by name, username, email, or mobile number
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "u.mobileNumber LIKE CONCAT('%', :searchTerm, '%')")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);

    // ========== VALIDATION QUERIES ==========
    
    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if mobile number exists
     */
    boolean existsByMobileNumber(String mobileNumber);

    /**
     * Check if username exists for different user
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username AND u.id != :userId")
    boolean existsByUsernameAndIdNot(@Param("username") String username, @Param("userId") Long userId);

    /**
     * Check if email exists for different user
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.id != :userId")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("userId") Long userId);

    /**
     * Check if mobile number exists for different user
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.mobileNumber = :mobileNumber AND u.id != :userId")
    boolean existsByMobileNumberAndIdNot(@Param("mobileNumber") String mobileNumber, @Param("userId") Long userId);

    // ========== STATISTICS QUERIES ==========
    
    /**
     * Count users by type
     */
    long countByUserType(UserType userType);

    /**
     * Count active users by type
     */
    long countByUserTypeAndIsActive(UserType userType, Boolean isActive);

    /**
     * Count users in organization by type
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.organizationId = :organizationId AND u.userType = :userType")
    long countByOrganizationIdAndUserType(@Param("organizationId") Long organizationId,
                                          @Param("userType") UserType userType);
}
