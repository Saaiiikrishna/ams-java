package com.example.attendancesystem.user.repository;

import com.example.attendancesystem.user.model.Permission;
import com.example.attendancesystem.user.model.User;
import com.example.attendancesystem.user.model.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User Permission Repository
 * Manages fine-grained permissions for users
 */
@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    /**
     * Find all permissions for a user
     */
    List<UserPermission> findByUser(User user);

    /**
     * Find all permissions for a user by user ID
     */
    @Query("SELECT up FROM UserPermission up WHERE up.user.id = :userId")
    List<UserPermission> findByUserId(@Param("userId") Long userId);

    /**
     * Find specific permission for a user
     */
    Optional<UserPermission> findByUserAndPermission(User user, Permission permission);

    /**
     * Find specific permission for a user by user ID
     */
    @Query("SELECT up FROM UserPermission up WHERE up.user.id = :userId AND up.permission = :permission")
    Optional<UserPermission> findByUserIdAndPermission(@Param("userId") Long userId, 
                                                       @Param("permission") Permission permission);

    /**
     * Find all granted permissions for a user
     */
    @Query("SELECT up FROM UserPermission up WHERE up.user = :user AND up.granted = true")
    List<UserPermission> findGrantedPermissionsByUser(@Param("user") User user);

    /**
     * Find all granted permissions for a user by user ID
     */
    @Query("SELECT up FROM UserPermission up WHERE up.user.id = :userId AND up.granted = true")
    List<UserPermission> findGrantedPermissionsByUserId(@Param("userId") Long userId);

    /**
     * Find all valid (granted and not expired) permissions for a user
     */
    @Query("SELECT up FROM UserPermission up WHERE up.user = :user AND up.granted = true " +
           "AND (up.expiresAt IS NULL OR up.expiresAt > :now)")
    List<UserPermission> findValidPermissionsByUser(@Param("user") User user, 
                                                    @Param("now") LocalDateTime now);

    /**
     * Find all valid permissions for a user by user ID
     */
    @Query("SELECT up FROM UserPermission up WHERE up.user.id = :userId AND up.granted = true " +
           "AND (up.expiresAt IS NULL OR up.expiresAt > :now)")
    List<UserPermission> findValidPermissionsByUserId(@Param("userId") Long userId, 
                                                      @Param("now") LocalDateTime now);

    /**
     * Check if user has a specific permission (granted and not expired)
     */
    @Query("SELECT COUNT(up) > 0 FROM UserPermission up WHERE up.user = :user " +
           "AND up.permission = :permission AND up.granted = true " +
           "AND (up.expiresAt IS NULL OR up.expiresAt > :now)")
    boolean hasValidPermission(@Param("user") User user, 
                              @Param("permission") Permission permission, 
                              @Param("now") LocalDateTime now);

    /**
     * Check if user has a specific permission by user ID
     */
    @Query("SELECT COUNT(up) > 0 FROM UserPermission up WHERE up.user.id = :userId " +
           "AND up.permission = :permission AND up.granted = true " +
           "AND (up.expiresAt IS NULL OR up.expiresAt > :now)")
    boolean hasValidPermissionByUserId(@Param("userId") Long userId, 
                                      @Param("permission") Permission permission, 
                                      @Param("now") LocalDateTime now);

    /**
     * Find all expired permissions
     */
    @Query("SELECT up FROM UserPermission up WHERE up.expiresAt IS NOT NULL AND up.expiresAt <= :now")
    List<UserPermission> findExpiredPermissions(@Param("now") LocalDateTime now);

    /**
     * Find permissions granted by a specific user
     */
    @Query("SELECT up FROM UserPermission up WHERE up.grantedBy = :grantedBy")
    List<UserPermission> findPermissionsGrantedBy(@Param("grantedBy") Long grantedBy);

    /**
     * Find permissions for users in a specific organization
     */
    @Query("SELECT up FROM UserPermission up WHERE up.user.organizationId = :organizationId")
    List<UserPermission> findPermissionsByOrganization(@Param("organizationId") Long organizationId);

    /**
     * Find users with a specific permission
     */
    @Query("SELECT DISTINCT up.user FROM UserPermission up WHERE up.permission = :permission " +
           "AND up.granted = true AND (up.expiresAt IS NULL OR up.expiresAt > :now)")
    List<User> findUsersWithPermission(@Param("permission") Permission permission, 
                                      @Param("now") LocalDateTime now);

    /**
     * Delete all permissions for a user
     */
    void deleteByUser(User user);

    /**
     * Delete all permissions for a user by user ID
     */
    @Query("DELETE FROM UserPermission up WHERE up.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * Delete specific permission for a user
     */
    void deleteByUserAndPermission(User user, Permission permission);

    /**
     * Count permissions for a user
     */
    @Query("SELECT COUNT(up) FROM UserPermission up WHERE up.user = :user AND up.granted = true")
    long countGrantedPermissionsByUser(@Param("user") User user);

    /**
     * Count valid permissions for a user
     */
    @Query("SELECT COUNT(up) FROM UserPermission up WHERE up.user = :user AND up.granted = true " +
           "AND (up.expiresAt IS NULL OR up.expiresAt > :now)")
    long countValidPermissionsByUser(@Param("user") User user, @Param("now") LocalDateTime now);
}
