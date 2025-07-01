package com.example.attendancesystem.user.service;


import com.example.attendancesystem.user.dto.UserDto;
import com.example.attendancesystem.user.model.Permission;
import com.example.attendancesystem.user.model.User;
import com.example.attendancesystem.user.model.UserPermission;
import com.example.attendancesystem.user.model.UserType;
import com.example.attendancesystem.user.repository.UserPermissionRepository;
import com.example.attendancesystem.user.repository.UserRepository;
import com.example.attendancesystem.user.grpc.client.AuthServiceGrpcClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Generic User Service
 * Handles all user types: SUPER_ADMIN, ENTITY_ADMIN, MEMBER
 */
@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private AuthServiceGrpcClient authServiceGrpcClient;





    // ========== USER CREATION METHODS ==========

    /**
     * Create a new Super Admin
     * Only callable by existing Super Admins
     * Note: Password should already be hashed by Auth Service
     */
    public UserDto createSuperAdmin(UserDto userDto, Long createdBy) {
        logger.info("Creating Super Admin: {}", userDto.getUsername());
        
        validateSuperAdminCreation(userDto);
        
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setPassword(userDto.getPassword()); // Password should already be hashed by Auth Service
        user.setEmail(userDto.getEmail());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setUserType(UserType.SUPER_ADMIN);
        user.setOrganizationId(null); // Super admins don't belong to organizations
        user.setCreatedBy(createdBy);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        
        // Grant default super admin permissions
        grantDefaultPermissions(savedUser, createdBy);
        
        logger.info("Super Admin created successfully: {}", savedUser.getUsername());
        return convertToDto(savedUser);
    }

    /**
     * Create a new Entity Admin (gRPC version)
     * Callable by Super Admins via gRPC
     * Note: This method also creates the EntityAdmin in Auth Service for authentication
     */
    public User createEntityAdmin(String username, String originalPassword, String email,
                                String firstName, String lastName, String mobileNumber,
                                Long organizationId, Long createdByUserId) {
        logger.info("*** NEW GRPC METHOD *** Creating Entity Admin via gRPC: {} for organization: {}", username, organizationId);

        try {
            // Hash the password using Auth Service gRPC for consistency
            logger.info("Hashing password via Auth Service gRPC for user: {}", username);
            String hashedPassword = authServiceGrpcClient.hashPassword(originalPassword);
            if (hashedPassword == null) {
                logger.error("Failed to hash password via Auth Service gRPC for user: {}", username);
                throw new RuntimeException("Password hashing failed");
            }
            logger.info("Password hashed successfully via Auth Service gRPC for user: {}", username);

            // Create UserDto with hashed password
            UserDto userDto = new UserDto();
            userDto.setUsername(username);
            userDto.setPassword(hashedPassword); // Hashed by Auth Service
            userDto.setEmail(email);
            userDto.setFirstName(firstName);
            userDto.setLastName(lastName);
            userDto.setMobileNumber(mobileNumber);
            userDto.setUserType(UserType.ENTITY_ADMIN);
            userDto.setOrganizationId(organizationId);
            userDto.setIsActive(true);

            // Create via existing method
            logger.info("About to call createEntityAdmin method...");
            UserDto createdUserDto = createEntityAdmin(userDto, createdByUserId);
            logger.info("UserDto created with ID: {}", createdUserDto != null ? createdUserDto.getId() : "null");

            if (createdUserDto == null) {
                logger.error("createEntityAdmin returned null - this should not happen");
                throw new RuntimeException("User creation failed - createEntityAdmin returned null");
            }

            User createdUser = userRepository.findById(createdUserDto.getId()).orElse(null);
            logger.info("User entity retrieved: {}", createdUser != null ? "SUCCESS" : "FAILED");

            if (createdUser != null) {
                logger.info("User created successfully, now calling Auth Service for authentication setup");

                // Also create the EntityAdmin in Auth Service for authentication
                // Use the original password for Auth Service (it will hash it itself)
                logger.info("Calling Auth Service gRPC client to create EntityAdmin for authentication");
                boolean authServiceSuccess = authServiceGrpcClient.createEntityAdminForAuth(
                        username, originalPassword, organizationId);

                if (authServiceSuccess) {
                    logger.info("Successfully created Entity Admin in Auth Service for user: {}", username);
                } else {
                    logger.warn("Failed to create Entity Admin in Auth Service for user: {}", username);
                    // Note: We don't fail the entire operation since the user was created successfully
                    // The EntityAdmin can be manually created in Auth Service later if needed
                }
            } else {
                logger.error("User creation failed, skipping Auth Service call");
            }

            return createdUser;

        } catch (Exception e) {
            logger.error("Error creating Entity Admin: {}", username, e);
            throw e;
        }
    }

    /**
     * Create a new Entity Admin
     * Callable by Super Admins
     */
    public UserDto createEntityAdmin(UserDto userDto, Long createdBy) {
        logger.info("Creating Entity Admin: {} for organization: {}", 
                   userDto.getUsername(), userDto.getOrganizationId());
        
        validateEntityAdminCreation(userDto);

        // Note: Organization validation should be done via gRPC call to organization-service
        // For now, we'll trust the organizationId is valid

        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setPassword(userDto.getPassword()); // Password should already be hashed by Auth Service
        user.setEmail(userDto.getEmail());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setUserType(UserType.ENTITY_ADMIN);
        user.setOrganizationId(userDto.getOrganizationId());
        user.setCreatedBy(createdBy);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        
        // Grant default entity admin permissions
        grantDefaultPermissions(savedUser, createdBy);
        
        logger.info("Entity Admin created successfully: {}", savedUser.getUsername());
        return convertToDto(savedUser);
    }

    /**
     * Create a new Member (gRPC version)
     * Callable by Entity Admins via gRPC
     * Note: Password handling should be done by Auth Service
     */
    public User createMember(String username, String hashedPassword, String email,
                           String firstName, String lastName, String mobileNumber,
                           Long organizationId, Long createdByUserId) {
        logger.info("Creating Member via gRPC: {} for organization: {}", username, organizationId);

        // Create UserDto with pre-hashed password from Auth Service
        UserDto userDto = new UserDto();
        userDto.setUsername(username);
        userDto.setPassword(hashedPassword); // Already hashed by Auth Service
        userDto.setEmail(email);
        userDto.setFirstName(firstName);
        userDto.setLastName(lastName);
        userDto.setMobileNumber(mobileNumber);
        userDto.setUserType(UserType.MEMBER);
        userDto.setOrganizationId(organizationId);
        userDto.setIsActive(true);

        // Create via existing method
        UserDto createdUserDto = createMember(userDto, createdByUserId);

        // Return the entity
        return userRepository.findById(createdUserDto.getId()).orElse(null);
    }

    /**
     * Create a new Member/Subscriber
     * Callable by Entity Admins within their organization
     */
    public UserDto createMember(UserDto userDto, Long createdBy) {
        logger.info("Creating Member: {} for organization: {}", 
                   userDto.getMobileNumber(), userDto.getOrganizationId());
        
        validateMemberCreation(userDto);

        // Note: Organization validation should be done via gRPC call to organization-service
        // For now, we'll trust the organizationId is valid

        User user = new User();
        user.setUsername(userDto.getMobileNumber()); // Mobile number as username for members
        user.setPassword(userDto.getPassword() != null ? userDto.getPassword() : "0000"); // Default PIN or hashed password from Auth Service
        user.setEmail(userDto.getEmail());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setMobileNumber(userDto.getMobileNumber());
        user.setUserType(UserType.MEMBER);
        user.setOrganizationId(userDto.getOrganizationId());
        user.setCreatedBy(createdBy);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        
        // Grant default member permissions
        grantDefaultPermissions(savedUser, createdBy);
        
        logger.info("Member created successfully: {}", savedUser.getMobileNumber());
        return convertToDto(savedUser);
    }

    // ========== USER RETRIEVAL METHODS ==========

    /**
     * Get user by ID
     */
    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id).map(this::convertToDto);
    }

    /**
     * Get user by username
     */
    public Optional<UserDto> getUserByUsername(String username) {
        return userRepository.findByUsername(username).map(this::convertToDto);
    }

    /**
     * Get user by username for authentication (includes password)
     * This method is specifically for the Auth Service
     */
    public Optional<UserDto> getUserByUsernameForAuth(String username) {
        return userRepository.findByUsername(username).map(this::convertToDtoWithPassword);
    }

    /**
     * Get user by mobile number (for member authentication)
     */
    public Optional<UserDto> getUserByMobileNumber(String mobileNumber) {
        return userRepository.findByMobileNumber(mobileNumber).map(this::convertToDto);
    }

    /**
     * Get all users by type
     */
    public Page<UserDto> getUsersByType(UserType userType, Pageable pageable) {
        return userRepository.findByUserType(userType, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get all users in an organization
     */
    public Page<UserDto> getUsersByOrganization(Long organizationId, Pageable pageable) {
        return userRepository.findByOrganizationId(organizationId, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get users by organization and type
     */
    public List<UserDto> getUsersByOrganizationAndType(Long organizationId, UserType userType) {
        return userRepository.findByOrganizationIdAndUserType(organizationId, userType)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all users with pagination
     */
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::convertToDto);
    }

    /**
     * Search users by name, username, email, or mobile number
     */
    public Page<UserDto> searchUsers(String searchTerm, Pageable pageable) {
        return userRepository.searchUsers(searchTerm, pageable)
                .map(this::convertToDto);
    }

    /**
     * Deactivate user
     */
    public void deactivateUser(Long userId, Long deactivatedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsActive(false);
        userRepository.save(user);

        logger.info("User deactivated: {} by user: {}", user.getUsername(), deactivatedBy);
    }

    /**
     * Activate user
     */
    public void activateUser(Long userId, Long activatedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsActive(true);
        userRepository.save(user);

        logger.info("User activated: {} by user: {}", user.getUsername(), activatedBy);
    }

    /**
     * Get all Super Admins
     */
    public List<UserDto> getAllSuperAdmins() {
        return userRepository.findAllSuperAdmins()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get Entity Admins by organization
     */
    public List<UserDto> getEntityAdminsByOrganization(Long organizationId) {
        return userRepository.findEntityAdminsByOrganization(organizationId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get Members by organization
     */
    public Page<UserDto> getMembersByOrganization(Long organizationId, Pageable pageable) {
        return userRepository.findMembersByOrganization(organizationId, pageable)
                .map(this::convertToDto);
    }

    // ========== USER UPDATE METHODS ==========

    /**
     * Update user profile
     */
    public UserDto updateUser(Long userId, UserDto userDto, Long updatedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update allowed fields based on user type
        if (userDto.getFirstName() != null) {
            user.setFirstName(userDto.getFirstName());
        }
        if (userDto.getLastName() != null) {
            user.setLastName(userDto.getLastName());
        }
        if (userDto.getEmail() != null) {
            user.setEmail(userDto.getEmail());
        }
        
        // Only allow username change for non-members
        if (userDto.getUsername() != null && user.getUserType() != UserType.MEMBER) {
            validateUsernameUnique(userDto.getUsername(), userId);
            user.setUsername(userDto.getUsername());
        }
        
        // Only allow mobile number change for members
        if (userDto.getMobileNumber() != null && user.getUserType() == UserType.MEMBER) {
            validateMobileNumberUnique(userDto.getMobileNumber(), userId);
            user.setMobileNumber(userDto.getMobileNumber());
            user.setUsername(userDto.getMobileNumber()); // Keep username in sync
        }

        User savedUser = userRepository.save(user);
        logger.info("User updated successfully: {}", savedUser.getUsername());
        
        return convertToDto(savedUser);
    }

    /**
     * Change user password
     */
    public void changePassword(Long userId, String newPassword, Long changedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPassword(newPassword); // Password should already be hashed by Auth Service
        userRepository.save(user);
        
        logger.info("Password changed for user: {}", user.getUsername());
    }

    /**
     * Activate/Deactivate user
     */
    public UserDto setUserActive(Long userId, boolean active, Long updatedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsActive(active);
        User savedUser = userRepository.save(user);
        
        logger.info("User {} {}: {}", active ? "activated" : "deactivated", 
                   savedUser.getUsername(), savedUser.getId());
        
        return convertToDto(savedUser);
    }

    // ========== USER DELETION METHODS ==========

    /**
     * Delete user (soft delete by deactivating)
     */
    public void deleteUser(Long userId, Long deletedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Soft delete by deactivating
        user.setIsActive(false);
        userRepository.save(user);
        
        logger.info("User soft deleted: {}", user.getUsername());
    }

    /**
     * Hard delete user (removes from database)
     * Use with caution - only for data cleanup
     */
    public void hardDeleteUser(Long userId, Long deletedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Delete all permissions first
        userPermissionRepository.deleteByUser(user);
        
        // Delete user
        userRepository.delete(user);
        
        logger.warn("User hard deleted: {} by user: {}", user.getUsername(), deletedBy);
    }

    // ========== PERMISSION METHODS ==========

    /**
     * Grant default permissions based on user type
     */
    private void grantDefaultPermissions(User user, Long grantedBy) {
        Permission[] defaultPermissions = Permission.getDefaultPermissions(user.getUserType());
        
        for (Permission permission : defaultPermissions) {
            UserPermission userPermission = new UserPermission(user, permission, grantedBy);
            userPermissionRepository.save(userPermission);
        }
        
        logger.info("Granted {} default permissions to user: {}", 
                   defaultPermissions.length, user.getUsername());
    }

    /**
     * Grant specific permission to user
     */
    public void grantPermission(Long userId, Permission permission, Long grantedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<UserPermission> existing = userPermissionRepository
                .findByUserAndPermission(user, permission);

        if (existing.isPresent()) {
            UserPermission userPermission = existing.get();
            userPermission.setGranted(true);
            userPermission.setGrantedBy(grantedBy);
            userPermission.setGrantedAt(LocalDateTime.now());
            userPermissionRepository.save(userPermission);
        } else {
            UserPermission userPermission = new UserPermission(user, permission, grantedBy);
            userPermissionRepository.save(userPermission);
        }
        
        logger.info("Permission {} granted to user: {}", permission, user.getUsername());
    }

    /**
     * Revoke permission from user
     */
    public void revokePermission(Long userId, Permission permission, Long revokedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<UserPermission> existing = userPermissionRepository
                .findByUserAndPermission(user, permission);

        if (existing.isPresent()) {
            UserPermission userPermission = existing.get();
            userPermission.setGranted(false);
            userPermission.setGrantedBy(revokedBy);
            userPermission.setGrantedAt(LocalDateTime.now());
            userPermissionRepository.save(userPermission);
            
            logger.info("Permission {} revoked from user: {}", permission, user.getUsername());
        }
    }

    /**
     * Check if user has permission
     */
    public boolean hasPermission(Long userId, Permission permission) {
        return userPermissionRepository.hasValidPermissionByUserId(userId, permission, LocalDateTime.now());
    }

    /**
     * Get all permissions for user
     */
    public List<String> getUserPermissions(Long userId) {
        return userPermissionRepository.findValidPermissionsByUserId(userId, LocalDateTime.now())
                .stream()
                .map(up -> up.getPermission().name())
                .collect(Collectors.toList());
    }

    // ========== VALIDATION METHODS ==========

    private void validateSuperAdminCreation(UserDto userDto) {
        if (userDto.getUserType() != UserType.SUPER_ADMIN) {
            throw new IllegalArgumentException("Invalid user type for super admin creation");
        }
        if (userDto.getOrganizationId() != null) {
            throw new IllegalArgumentException("Super admins cannot belong to organizations");
        }
        validateUsernameUnique(userDto.getUsername(), null);
        if (userDto.getEmail() != null) {
            validateEmailUnique(userDto.getEmail(), null);
        }
    }

    private void validateEntityAdminCreation(UserDto userDto) {
        if (userDto.getUserType() != UserType.ENTITY_ADMIN) {
            throw new IllegalArgumentException("Invalid user type for entity admin creation");
        }
        if (userDto.getOrganizationId() == null) {
            throw new IllegalArgumentException("Entity admins must belong to an organization");
        }
        validateUsernameUnique(userDto.getUsername(), null);
        if (userDto.getEmail() != null) {
            validateEmailUnique(userDto.getEmail(), null);
        }
    }

    private void validateMemberCreation(UserDto userDto) {
        if (userDto.getUserType() != UserType.MEMBER) {
            throw new IllegalArgumentException("Invalid user type for member creation");
        }
        if (userDto.getOrganizationId() == null) {
            throw new IllegalArgumentException("Members must belong to an organization");
        }
        if (userDto.getMobileNumber() == null || userDto.getMobileNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number is required for members");
        }
        validateMobileNumberUnique(userDto.getMobileNumber(), null);
        if (userDto.getEmail() != null) {
            validateEmailUnique(userDto.getEmail(), null);
        }
    }

    private void validateUsernameUnique(String username, Long excludeUserId) {
        if (excludeUserId == null) {
            if (userRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("Username already exists: " + username);
            }
        } else {
            if (userRepository.existsByUsernameAndIdNot(username, excludeUserId)) {
                throw new IllegalArgumentException("Username already exists: " + username);
            }
        }
    }

    private void validateEmailUnique(String email, Long excludeUserId) {
        if (excludeUserId == null) {
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email already exists: " + email);
            }
        } else {
            if (userRepository.existsByEmailAndIdNot(email, excludeUserId)) {
                throw new IllegalArgumentException("Email already exists: " + email);
            }
        }
    }

    private void validateMobileNumberUnique(String mobileNumber, Long excludeUserId) {
        if (excludeUserId == null) {
            if (userRepository.existsByMobileNumber(mobileNumber)) {
                throw new IllegalArgumentException("Mobile number already exists: " + mobileNumber);
            }
        } else {
            if (userRepository.existsByMobileNumberAndIdNot(mobileNumber, excludeUserId)) {
                throw new IllegalArgumentException("Mobile number already exists: " + mobileNumber);
            }
        }
    }

    // ========== CONVERSION METHODS ==========

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setMobileNumber(user.getMobileNumber());
        dto.setUserType(user.getUserType());
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setCreatedBy(user.getCreatedBy());
        dto.setLastDeviceId(user.getLastDeviceId());
        dto.setLastDeviceInfo(user.getLastDeviceInfo());
        dto.setLastLoginTime(user.getLastLoginTime());

        if (user.getOrganizationId() != null) {
            dto.setOrganizationId(user.getOrganizationId());
            // Note: Organization name should be fetched via gRPC call to organization-service
            // For now, we'll leave it null or fetch it separately
            dto.setOrganizationName(null);
        }

        // Get user permissions
        dto.setPermissions(getUserPermissions(user.getId()));

        return dto;
    }

    /**
     * Find user by username (returns entity)
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    // REMOVED: validateCredentials method
    // Authentication logic should be handled by Auth Service, not User Service

    /**
     * Convert User entity to UserDto including password (for authentication only)
     *
     * ⚠️  SECURITY WARNING: This method includes the password field and should ONLY be used
     * for authentication purposes by the Auth Service. Never expose this in public APIs.
     *
     * @deprecated This method should only be used for internal authentication flows.
     * Consider using gRPC or other secure inter-service communication for production.
     *
     * TODO: Replace with secure gRPC authentication service communication
     */
    @Deprecated
    private UserDto convertToDtoWithPassword(User user) {
        UserDto dto = convertToDto(user); // Get the standard DTO
        dto.setPassword(user.getPassword()); // Add the password for authentication
        return dto;
    }


}
