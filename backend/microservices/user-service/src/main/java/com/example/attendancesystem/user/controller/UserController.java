package com.example.attendancesystem.user.controller;

import com.example.attendancesystem.user.dto.UserDto;
import com.example.attendancesystem.user.dto.CreateUserRequest;
import com.example.attendancesystem.user.model.Permission;
import com.example.attendancesystem.user.model.UserType;
import com.example.attendancesystem.user.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generic User Controller
 * Handles all user types with role-based access control
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    // ========== SUPER ADMIN ENDPOINTS ==========

    /**
     * Create Super Admin (Only Super Admins can create other Super Admins)
     */
    @PostMapping("/super-admin")
    public ResponseEntity<?> createSuperAdmin(@RequestBody CreateUserRequest request,
                                              @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            logger.info("Creating Super Admin: {} by user: {}", request.getUsername(), currentUserId);

            // Manual validation for required fields
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Username is required"
                ));
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Password is required"
                ));
            }

            // Create UserDto from request
            UserDto userDto = new UserDto();
            userDto.setUsername(request.getUsername());
            userDto.setPassword(request.getPassword());
            userDto.setEmail(request.getEmail());
            userDto.setFirstName(request.getFirstName());
            userDto.setLastName(request.getLastName());
            userDto.setUserType(UserType.SUPER_ADMIN); // Set this programmatically

            UserDto createdUser = userService.createSuperAdmin(userDto, currentUserId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Super Admin created successfully",
                "user", createdUser
            ));
        } catch (Exception e) {
            logger.error("Error creating Super Admin", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get all Super Admins
     */
    @GetMapping("/super-admins")
    public ResponseEntity<?> getAllSuperAdmins(@RequestHeader("X-User-ID") Long currentUserId) {
        try {
            List<UserDto> superAdmins = userService.getAllSuperAdmins();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "users", superAdmins,
                "count", superAdmins.size()
            ));
        } catch (Exception e) {
            logger.error("Error fetching Super Admins", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch Super Admins"
            ));
        }
    }

    // ========== ENTITY ADMIN ENDPOINTS ==========

    /**
     * Create Entity Admin (Super Admins can create Entity Admins)
     */
    @PostMapping("/entity-admin")
    public ResponseEntity<?> createEntityAdmin(@RequestBody UserDto userDto,
                                               @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            logger.info("Creating Entity Admin: {} for organization: {} by user: {}",
                       userDto.getUsername(), userDto.getOrganizationId(), currentUserId);

            // Set user type before validation
            userDto.setUserType(UserType.ENTITY_ADMIN);

            // Manual validation for required fields
            if (userDto.getUsername() == null || userDto.getUsername().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Username is required"
                ));
            }
            if (userDto.getPassword() == null || userDto.getPassword().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Password is required"
                ));
            }
            if (userDto.getOrganizationId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Organization ID is required for Entity Admin"
                ));
            }

            UserDto createdUser = userService.createEntityAdmin(userDto, currentUserId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Entity Admin created successfully",
                "user", createdUser
            ));
        } catch (Exception e) {
            logger.error("Error creating Entity Admin", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get Entity Admins by Organization
     */
    @GetMapping("/entity-admins/organization/{organizationId}")
    public ResponseEntity<?> getEntityAdminsByOrganization(@PathVariable Long organizationId,
                                                           @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            List<UserDto> entityAdmins = userService.getEntityAdminsByOrganization(organizationId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "users", entityAdmins,
                "count", entityAdmins.size(),
                "organizationId", organizationId
            ));
        } catch (Exception e) {
            logger.error("Error fetching Entity Admins for organization: {}", organizationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch Entity Admins"
            ));
        }
    }

    // ========== MEMBER ENDPOINTS ==========

    /**
     * Create Member (Entity Admins can create Members in their organization)
     */
    @PostMapping("/member")
    public ResponseEntity<?> createMember(@RequestBody UserDto userDto,
                                          @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            logger.info("Creating Member: {} for organization: {} by user: {}",
                       userDto.getMobileNumber(), userDto.getOrganizationId(), currentUserId);

            // Set user type before validation
            userDto.setUserType(UserType.MEMBER);

            // Manual validation for required fields
            if (userDto.getMobileNumber() == null || userDto.getMobileNumber().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Mobile number is required for Member"
                ));
            }
            if (userDto.getOrganizationId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Organization ID is required for Member"
                ));
            }

            UserDto createdUser = userService.createMember(userDto, currentUserId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Member created successfully with default PIN: 0000",
                "user", createdUser
            ));
        } catch (Exception e) {
            logger.error("Error creating Member", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get Members by Organization with pagination
     */
    @GetMapping("/members/organization/{organizationId}")
    public ResponseEntity<?> getMembersByOrganization(@PathVariable Long organizationId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      @RequestParam(defaultValue = "firstName") String sortBy,
                                                      @RequestParam(defaultValue = "asc") String sortDir,
                                                      @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                       Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<UserDto> members = userService.getMembersByOrganization(organizationId, pageable);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "users", members.getContent(),
                "totalElements", members.getTotalElements(),
                "totalPages", members.getTotalPages(),
                "currentPage", page,
                "pageSize", size,
                "organizationId", organizationId
            ));
        } catch (Exception e) {
            logger.error("Error fetching Members for organization: {}", organizationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch Members"
            ));
        }
    }

    // ========== GENERIC USER ENDPOINTS ==========

    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId,
                                         @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            Optional<UserDto> user = userService.getUserById(userId);
            if (user.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", user.get()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }
        } catch (Exception e) {
            logger.error("Error fetching user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch user"
            ));
        }
    }

    /**
     * Get user by username
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username,
                                               @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            Optional<UserDto> user = userService.getUserByUsername(username);
            if (user.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", user.get()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }
        } catch (Exception e) {
            logger.error("Error fetching user by username: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch user"
            ));
        }
    }

    /**
     * Get user by mobile number (for member lookup)
     */
    @GetMapping("/mobile/{mobileNumber}")
    public ResponseEntity<?> getUserByMobileNumber(@PathVariable String mobileNumber,
                                                   @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            Optional<UserDto> user = userService.getUserByMobileNumber(mobileNumber);
            if (user.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", user.get()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }
        } catch (Exception e) {
            logger.error("Error fetching user by mobile: {}", mobileNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch user"
            ));
        }
    }

    /**
     * Get user by username for authentication (no auth required)
     *
     * ⚠️  SECURITY WARNING: This endpoint includes password data and should ONLY be called
     * by the Auth Service for authentication purposes. Never expose this to public APIs.
     *
     * @deprecated This endpoint should only be used for internal authentication flows.
     * TODO: Replace with secure gRPC authentication service communication
     */
    @Deprecated
    @GetMapping("/auth/username/{username}")
    public ResponseEntity<?> getUserByUsernameForAuth(@PathVariable String username) {
        try {
            Optional<UserDto> user = userService.getUserByUsernameForAuth(username);
            if (user.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", user.get()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }
        } catch (Exception e) {
            logger.error("Error fetching user by username for auth: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch user"
            ));
        }
    }

    /**
     * Get all users with pagination and filtering
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(defaultValue = "firstName") String sortBy,
                                        @RequestParam(defaultValue = "asc") String sortDir,
                                        @RequestParam(required = false) String userType,
                                        @RequestParam(required = false) Long organizationId,
                                        @RequestParam(required = false) String search,
                                        @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            Pageable pageable = PageRequest.of(page, size,
                sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());

            Page<UserDto> users;
            if (userType != null && !userType.isEmpty()) {
                UserType type = UserType.valueOf(userType.toUpperCase());
                users = userService.getUsersByType(type, pageable);
            } else if (organizationId != null) {
                users = userService.getUsersByOrganization(organizationId, pageable);
            } else if (search != null && !search.isEmpty()) {
                users = userService.searchUsers(search, pageable);
            } else {
                users = userService.getAllUsers(pageable);
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "users", users.getContent(),
                "totalElements", users.getTotalElements(),
                "totalPages", users.getTotalPages(),
                "currentPage", users.getNumber(),
                "size", users.getSize()
            ));
        } catch (Exception e) {
            logger.error("Error fetching users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch users"
            ));
        }
    }

    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getCurrentUserProfile(@RequestHeader("X-User-ID") Long currentUserId) {
        try {
            Optional<UserDto> user = userService.getUserById(currentUserId);
            if (user.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", user.get()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "User profile not found"
                ));
            }
        } catch (Exception e) {
            logger.error("Error fetching user profile: {}", currentUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch user profile"
            ));
        }
    }

    /**
     * Update current user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateCurrentUserProfile(@Valid @RequestBody UserDto userDto,
                                                      @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            UserDto updatedUser = userService.updateUser(currentUserId, userDto, currentUserId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Profile updated successfully",
                "user", updatedUser
            ));
        } catch (Exception e) {
            logger.error("Error updating user profile: {}", currentUserId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Deactivate user
     */
    @PutMapping("/{userId}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable Long userId,
                                           @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            userService.deactivateUser(userId, currentUserId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User deactivated successfully"
            ));
        } catch (Exception e) {
            logger.error("Error deactivating user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Activate user
     */
    @PutMapping("/{userId}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long userId,
                                         @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            userService.activateUser(userId, currentUserId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User activated successfully"
            ));
        } catch (Exception e) {
            logger.error("Error activating user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Update user profile
     */
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable Long userId,
                                        @Valid @RequestBody UserDto userDto,
                                        @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            UserDto updatedUser = userService.updateUser(userId, userDto, currentUserId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User updated successfully",
                "user", updatedUser
            ));
        } catch (Exception e) {
            logger.error("Error updating user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Change user password
     */
    @PutMapping("/{userId}/password")
    public ResponseEntity<?> changePassword(@PathVariable Long userId,
                                            @RequestBody Map<String, String> request,
                                            @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "New password is required"
                ));
            }

            userService.changePassword(userId, newPassword, currentUserId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password changed successfully"
            ));
        } catch (Exception e) {
            logger.error("Error changing password for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Activate/Deactivate user
     */
    @PutMapping("/{userId}/status")
    public ResponseEntity<?> setUserStatus(@PathVariable Long userId,
                                           @RequestBody Map<String, Boolean> request,
                                           @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            Boolean active = request.get("active");
            if (active == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Active status is required"
                ));
            }

            UserDto updatedUser = userService.setUserActive(userId, active, currentUserId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User status updated successfully",
                "user", updatedUser
            ));
        } catch (Exception e) {
            logger.error("Error updating user status: {}", userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Delete user (soft delete)
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId,
                                        @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            userService.deleteUser(userId, currentUserId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User deleted successfully"
            ));
        } catch (Exception e) {
            logger.error("Error deleting user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ========== PERMISSION ENDPOINTS ==========

    /**
     * Get user permissions
     */
    @GetMapping("/{userId}/permissions")
    public ResponseEntity<?> getUserPermissions(@PathVariable Long userId,
                                                @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            List<String> permissions = userService.getUserPermissions(userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "permissions", permissions,
                "count", permissions.size()
            ));
        } catch (Exception e) {
            logger.error("Error fetching permissions for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to fetch permissions"
            ));
        }
    }

    /**
     * Grant permission to user
     */
    @PostMapping("/{userId}/permissions/{permission}")
    public ResponseEntity<?> grantPermission(@PathVariable Long userId,
                                             @PathVariable String permission,
                                             @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            Permission perm = Permission.valueOf(permission.toUpperCase());
            userService.grantPermission(userId, perm, currentUserId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Permission granted successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", "Invalid permission: " + permission
            ));
        } catch (Exception e) {
            logger.error("Error granting permission {} to user: {}", permission, userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Revoke permission from user
     */
    @DeleteMapping("/{userId}/permissions/{permission}")
    public ResponseEntity<?> revokePermission(@PathVariable Long userId,
                                              @PathVariable String permission,
                                              @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            Permission perm = Permission.valueOf(permission.toUpperCase());
            userService.revokePermission(userId, perm, currentUserId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Permission revoked successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", "Invalid permission: " + permission
            ));
        } catch (Exception e) {
            logger.error("Error revoking permission {} from user: {}", permission, userId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Check if user has permission
     */
    @GetMapping("/{userId}/permissions/{permission}/check")
    public ResponseEntity<?> checkPermission(@PathVariable Long userId,
                                             @PathVariable String permission,
                                             @RequestHeader("X-User-ID") Long currentUserId) {
        try {
            Permission perm = Permission.valueOf(permission.toUpperCase());
            boolean hasPermission = userService.hasPermission(userId, perm);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "hasPermission", hasPermission,
                "permission", permission
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", "Invalid permission: " + permission
            ));
        } catch (Exception e) {
            logger.error("Error checking permission {} for user: {}", permission, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to check permission"
            ));
        }
    }
}
