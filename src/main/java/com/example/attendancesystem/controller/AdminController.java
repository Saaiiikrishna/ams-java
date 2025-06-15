package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.EntityAdminDto;
import com.example.attendancesystem.dto.OrganizationDto;
import com.example.attendancesystem.model.EntityAdmin;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.Role;
import com.example.attendancesystem.repository.EntityAdminRepository;
import com.example.attendancesystem.repository.OrganizationRepository;
import com.example.attendancesystem.repository.RefreshTokenRepository;
import com.example.attendancesystem.repository.RoleRepository;
import com.example.attendancesystem.repository.SubscriberRepository;
import com.example.attendancesystem.repository.AttendanceSessionRepository;
import com.example.attendancesystem.service.EntityIdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/super")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EntityAdminRepository entityAdminRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EntityIdService entityIdService;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/entities")
    public ResponseEntity<?> createOrganization(@RequestBody OrganizationDto organizationDto) {
        try {
            logger.info("Creating new organization: {}", organizationDto.getName());

            if (organizationRepository.existsByName(organizationDto.getName())) {
                logger.warn("Organization creation failed - name already exists: {}", organizationDto.getName());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Organization name already exists");
            }

            String entityId = entityIdService.generateUniqueEntityId();
            logger.info("Generated Entity ID: {} for organization: {}", entityId, organizationDto.getName());

            Organization organization = new Organization();
            organization.setEntityId(entityId);
            organization.setName(organizationDto.getName());
            organization.setAddress(organizationDto.getAddress());
            organization.setLatitude(organizationDto.getLatitude());
            organization.setLongitude(organizationDto.getLongitude());
            organization.setContactPerson(organizationDto.getContactPerson());
            organization.setEmail(organizationDto.getEmail());

            Organization savedOrganization = organizationRepository.save(organization);
            logger.info("Organization created successfully - ID: {}, Entity ID: {}, Name: {}",
                       savedOrganization.getId(), savedOrganization.getEntityId(), savedOrganization.getName());

            OrganizationDto responseDto = convertToDto(savedOrganization);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);

        } catch (Exception e) {
            logger.error("Failed to create organization: {}", organizationDto.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create organization: " + e.getMessage());
        }
    }

    @GetMapping("/entities")
    public ResponseEntity<List<OrganizationDto>> getAllOrganizations() {
        List<Organization> organizations = organizationRepository.findAll();
        logger.info("Current entities in database:");
        organizations.forEach(org -> logger.info("Entity ID: {}, Name: {}", org.getEntityId(), org.getName()));
        List<OrganizationDto> organizationDtos = organizations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(organizationDtos);
    }

    @GetMapping("/entities/without-admin")
    public ResponseEntity<List<OrganizationDto>> getOrganizationsWithoutAdmin() {
        List<Organization> allOrganizations = organizationRepository.findAll();
        List<OrganizationDto> organizationsWithoutAdmin = allOrganizations.stream()
                .filter(org -> !entityAdminRepository.existsByOrganization(org))
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(organizationsWithoutAdmin);
    }

    @PutMapping("/entities/{entityId}")
    public ResponseEntity<?> updateOrganization(@PathVariable String entityId, @RequestBody OrganizationDto organizationDto) {
        try {
            logger.info("Updating organization with Entity ID: {}", entityId);

            Organization organization = organizationRepository.findByEntityId(entityId)
                    .orElse(null);
            if (organization == null) {
                logger.warn("Organization not found with Entity ID: {}", entityId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found");
            }

            // Check if name is being changed and if new name already exists
            if (!organization.getName().equals(organizationDto.getName()) &&
                organizationRepository.existsByName(organizationDto.getName())) {
                logger.warn("Organization update failed - name already exists: {}", organizationDto.getName());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Organization name already exists");
            }

            String oldName = organization.getName();
            organization.setName(organizationDto.getName());
            organization.setAddress(organizationDto.getAddress());
            organization.setLatitude(organizationDto.getLatitude());
            organization.setLongitude(organizationDto.getLongitude());
            organization.setContactPerson(organizationDto.getContactPerson());
            organization.setEmail(organizationDto.getEmail());

            Organization updatedOrganization = organizationRepository.save(organization);
            logger.info("Organization updated successfully - Entity ID: {}, Name: {} -> {}",
                       entityId, oldName, updatedOrganization.getName());

            return ResponseEntity.ok(convertToDto(updatedOrganization));
        } catch (Exception e) {
            logger.error("Failed to update organization with Entity ID: {}", entityId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update organization: " + e.getMessage());
        }
    }

    @DeleteMapping("/entities/{entityId}")
    @Transactional
    public ResponseEntity<?> deleteOrganization(@PathVariable String entityId) {
        try {
            logger.info("Attempting to delete organization with Entity ID: {}", entityId);

            Organization organization = organizationRepository.findByEntityId(entityId)
                    .orElse(null);
            if (organization == null) {
                logger.warn("Organization not found with Entity ID: {}", entityId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found");
            }

            logger.info("Found organization: {} (Entity ID: {})", organization.getName(), organization.getEntityId());

            // Check for related data and provide detailed feedback
            boolean hasEntityAdmins = entityAdminRepository.existsByOrganization(organization);
            long subscriberCount = subscriberRepository.countByOrganization(organization);
            long sessionCount = attendanceSessionRepository.countByOrganization(organization);

            logger.info("Related data check - Entity ID: {}, Admins: {}, Subscribers: {}, Sessions: {}",
                       entityId, hasEntityAdmins, subscriberCount, sessionCount);

            if (hasEntityAdmins || subscriberCount > 0 || sessionCount > 0) {
                StringBuilder message = new StringBuilder("Cannot delete organization '");
                message.append(organization.getName()).append("' because it has:");

                if (hasEntityAdmins) {
                    message.append("\n• Entity admins assigned");
                }
                if (subscriberCount > 0) {
                    message.append("\n• ").append(subscriberCount).append(" subscriber(s)");
                }
                if (sessionCount > 0) {
                    message.append("\n• ").append(sessionCount).append(" attendance session(s)");
                }

                message.append("\n\nPlease remove all related data before deleting the organization.");
                logger.warn("Organization deletion blocked due to related data - Entity ID: {}", entityId);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(message.toString());
            }

            // Safe to delete - no related data
            String organizationName = organization.getName();
            organizationRepository.delete(organization);
            logger.info("Organization deleted successfully - Entity ID: {}, Name: {}", entityId, organizationName);

            return ResponseEntity.ok("Organization '" + organizationName + "' deleted successfully");

        } catch (Exception e) {
            logger.error("Failed to delete organization with Entity ID: {}", entityId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete organization: " + e.getMessage());
        }
    }

    @DeleteMapping("/entities/by-id/{id}")
    @Transactional
    public ResponseEntity<?> deleteOrganizationById(@PathVariable Long id) {
        try {
            logger.info("Attempting to delete organization with ID: {}", id);

            Organization organization = organizationRepository.findById(id)
                    .orElse(null);
            if (organization == null) {
                logger.warn("Organization not found with ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found");
            }

            // Check for related data
            boolean hasEntityAdmins = entityAdminRepository.existsByOrganization(organization);
            long subscriberCount = subscriberRepository.countByOrganization(organization);
            long sessionCount = attendanceSessionRepository.countByOrganization(organization);

            if (hasEntityAdmins || subscriberCount > 0 || sessionCount > 0) {
                StringBuilder message = new StringBuilder("Cannot delete organization '");
                message.append(organization.getName()).append("' because it has:");

                if (hasEntityAdmins) {
                    message.append("\n• Entity admins assigned");
                }
                if (subscriberCount > 0) {
                    message.append("\n• ").append(subscriberCount).append(" subscriber(s)");
                }
                if (sessionCount > 0) {
                    message.append("\n• ").append(sessionCount).append(" attendance session(s)");
                }

                message.append("\n\nPlease remove all related data before deleting the organization.");
                logger.warn("Organization deletion blocked due to related data - ID: {}", id);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(message.toString());
            }

            // Safe to delete - no related data
            String organizationName = organization.getName();
            organizationRepository.delete(organization);
            logger.info("Organization deleted successfully - ID: {}, Name: {}", id, organizationName);

            return ResponseEntity.ok("Organization '" + organizationName + "' deleted successfully");

        } catch (Exception e) {
            logger.error("Failed to delete organization with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete organization: " + e.getMessage());
        }
    }

    @DeleteMapping("/entities/by-id/{id}/force")
    @Transactional
    public ResponseEntity<?> forceDeleteOrganizationById(@PathVariable Long id) {
        try {
            logger.warn("Force deleting organization with ID: {}", id);

            Organization organization = organizationRepository.findById(id)
                    .orElse(null);
            if (organization == null) {
                logger.warn("Organization not found with ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found");
            }

            String organizationName = organization.getName();
            logger.warn("Force deleting organization: {} (ID: {})", organizationName, id);

            // Count related data for reporting
            long entityAdminCount = entityAdminRepository.countByOrganization(organization);
            long subscriberCount = subscriberRepository.countByOrganization(organization);
            long sessionCount = attendanceSessionRepository.countByOrganization(organization);

            logger.info("Deleting related data - ID: {}, Admins: {}, Subscribers: {}, Sessions: {}",
                       id, entityAdminCount, subscriberCount, sessionCount);

            // Delete all related data in correct order (to avoid foreign key constraints)

            // 1. Delete entity admins and their refresh tokens
            List<EntityAdmin> entityAdmins = entityAdminRepository.findAllByOrganization(organization);
            for (EntityAdmin admin : entityAdmins) {
                refreshTokenRepository.deleteByUser(admin);
                entityAdminRepository.delete(admin);
            }

            // 2. Delete subscribers (this will cascade to attendance logs)
            subscriberRepository.deleteAll(subscriberRepository.findAllByOrganization(organization));

            // 3. Delete attendance sessions
            attendanceSessionRepository.deleteAll(attendanceSessionRepository.findAllByOrganization(organization));

            // 4. Finally delete the organization
            organizationRepository.delete(organization);

            logger.warn("Force deletion completed successfully - ID: {}, Name: {}", id, organizationName);

            return ResponseEntity.ok(Map.of(
                "message", "Organization '" + organizationName + "' and all related data deleted successfully",
                "deletedEntityAdmins", entityAdminCount,
                "deletedSubscribers", subscriberCount,
                "deletedSessions", sessionCount
            ));

        } catch (Exception e) {
            logger.error("Failed to force delete organization with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to force delete organization: " + e.getMessage());
        }
    }

    @DeleteMapping("/entities/{entityId}/force")
    @Transactional
    public ResponseEntity<?> forceDeleteOrganization(@PathVariable String entityId) {
        try {
            logger.warn("Force deleting organization with Entity ID: {}", entityId);

            Organization organization = organizationRepository.findByEntityId(entityId)
                    .orElse(null);
            if (organization == null) {
                logger.warn("Organization not found with Entity ID: {}", entityId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found");
            }

            String organizationName = organization.getName();
            logger.warn("Force deleting organization: {} (Entity ID: {})", organizationName, entityId);

            // Count related data for reporting
            long entityAdminCount = entityAdminRepository.countByOrganization(organization);
            long subscriberCount = subscriberRepository.countByOrganization(organization);
            long sessionCount = attendanceSessionRepository.countByOrganization(organization);

            logger.info("Deleting related data - Entity ID: {}, Admins: {}, Subscribers: {}, Sessions: {}",
                       entityId, entityAdminCount, subscriberCount, sessionCount);

            // Delete all related data in correct order (to avoid foreign key constraints)

            // 1. Delete entity admins and their refresh tokens
            List<EntityAdmin> entityAdmins = entityAdminRepository.findAllByOrganization(organization);
            for (EntityAdmin admin : entityAdmins) {
                refreshTokenRepository.deleteByUser(admin);
                entityAdminRepository.delete(admin);
            }

            // 2. Delete subscribers (this will cascade to attendance logs)
            subscriberRepository.deleteAll(subscriberRepository.findAllByOrganization(organization));

            // 3. Delete attendance sessions
            attendanceSessionRepository.deleteAll(attendanceSessionRepository.findAllByOrganization(organization));

            // 4. Finally delete the organization
            organizationRepository.delete(organization);

            logger.warn("Force deletion completed successfully - Entity ID: {}, Name: {}", entityId, organizationName);

            return ResponseEntity.ok(Map.of(
                "message", "Organization '" + organizationName + "' and all related data deleted successfully",
                "deletedEntityAdmins", entityAdminCount,
                "deletedSubscribers", subscriberCount,
                "deletedSessions", sessionCount
            ));

        } catch (Exception e) {
            logger.error("Failed to force delete organization with Entity ID: {}", entityId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to force delete organization: " + e.getMessage());
        }
    }

    @PostMapping("/entities/{entityId}/assign-admin")
    public ResponseEntity<?> assignAdminToEntity(@PathVariable String entityId, @RequestBody Map<String, String> request) {
        try {
            logger.info("Assigning admin to organization with Entity ID: {}", entityId);

            Organization organization = organizationRepository.findByEntityId(entityId)
                    .orElse(null);
            if (organization == null) {
                logger.warn("Organization not found with Entity ID: {}", entityId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found");
            }

            // Check if organization already has an admin
            if (entityAdminRepository.existsByOrganization(organization)) {
                logger.warn("Organization already has admin assigned - Entity ID: {}", entityId);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Organization already has an entity admin assigned");
            }

            String username = request.get("username");
            String password = request.get("password");

            if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                logger.warn("Invalid admin assignment request - missing username or password for Entity ID: {}", entityId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Username and password are required");
            }

            // Check if username already exists
            if (entityAdminRepository.findByUsername(username).isPresent()) {
                logger.warn("Username already exists: {} for Entity ID: {}", username, entityId);
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
            }

            // Create new entity admin
            EntityAdmin entityAdmin = new EntityAdmin();
            entityAdmin.setUsername(username);
            entityAdmin.setPassword(passwordEncoder.encode(password));
            entityAdmin.setOrganization(organization);
            entityAdmin.setCreatedAt(java.time.LocalDateTime.now());

            // Set role
            Role entityAdminRole = roleRepository.findByName("ENTITY_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Entity Admin role not found"));
            entityAdmin.setRole(entityAdminRole);

            EntityAdmin savedAdmin = entityAdminRepository.save(entityAdmin);
            logger.info("Entity admin assigned successfully - Username: {}, Entity ID: {}, Organization: {}",
                       username, entityId, organization.getName());

            return ResponseEntity.ok(Map.of(
                    "message", "Entity admin assigned successfully",
                    "adminId", savedAdmin.getId(),
                    "username", savedAdmin.getUsername(),
                    "entityId", organization.getEntityId(),
                    "organizationName", organization.getName()
            ));
        } catch (Exception e) {
            logger.error("Failed to assign admin to organization with Entity ID: {}", entityId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to assign admin: " + e.getMessage());
        }
    }

    @DeleteMapping("/entities/{entityId}/remove-admin")
    @Transactional
    public ResponseEntity<?> removeAdminFromEntity(@PathVariable String entityId) {
        try {
            logger.info("Removing admin from organization with Entity ID: {}", entityId);

            Organization organization = organizationRepository.findByEntityId(entityId)
                    .orElse(null);
            if (organization == null) {
                logger.warn("Organization not found with Entity ID: {}", entityId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found");
            }

            // Find and remove the entity admin for this organization
            Optional<EntityAdmin> existingAdmin = entityAdminRepository.findByOrganization(organization);
            if (!existingAdmin.isPresent()) {
                logger.warn("No admin found for organization with Entity ID: {}", entityId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No admin found for this organization");
            }

            String adminUsername = existingAdmin.get().getUsername();

            // SECURITY: Delete all related refresh tokens first to prevent foreign key constraint violation
            // This ensures the removed admin cannot access the system with existing tokens
            refreshTokenRepository.deleteByUser(existingAdmin.get());
            logger.info("Deleted all refresh tokens for user: {}", adminUsername);

            entityAdminRepository.delete(existingAdmin.get());
            logger.info("Admin removed successfully - Username: {}, Entity ID: {}, Organization: {}",
                       adminUsername, entityId, organization.getName());

            return ResponseEntity.ok(Map.of(
                    "message", "Admin removed successfully",
                    "entityId", organization.getEntityId(),
                    "organizationName", organization.getName()
            ));
        } catch (Exception e) {
            logger.error("Failed to remove admin from organization with Entity ID: {}", entityId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to remove admin: " + e.getMessage());
        }
    }

    @GetMapping("/entity-admins")
    public ResponseEntity<List<Map<String, Object>>> getAllEntityAdmins() {
        try {
            logger.info("Fetching all entity admins");
            List<EntityAdmin> entityAdmins = entityAdminRepository.findAll();
            logger.debug("Found {} total entity admin records", entityAdmins.size());

            // Filter out SuperAdmins that might be incorrectly stored in entity_admins table
            List<Map<String, Object>> adminDtos = entityAdmins.stream()
                    .filter(admin -> {
                        // Exclude SuperAdmins by username or role
                        boolean isSuperAdmin = "superadmin".equals(admin.getUsername()) ||
                                             (admin.getRole() != null && "SUPER_ADMIN".equals(admin.getRole().getName()));
                        if (isSuperAdmin) {
                            logger.warn("Filtering out SuperAdmin record from entity admins: {}", admin.getUsername());
                        }
                        return !isSuperAdmin;
                    })
                    .map(admin -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", admin.getId());
                        dto.put("username", admin.getUsername());
                        dto.put("entityId", admin.getOrganization() != null ? admin.getOrganization().getEntityId() : null);
                        dto.put("organizationName", admin.getOrganization() != null ? admin.getOrganization().getName() : "No Organization");
                        dto.put("createdAt", admin.getCreatedAt());
                        dto.put("role", admin.getRole() != null ? admin.getRole().getName() : "ENTITY_ADMIN");
                        return dto;
                    })
                    .collect(Collectors.toList());

            logger.info("Returning {} filtered entity admin DTOs", adminDtos.size());
            return ResponseEntity.ok(adminDtos);
        } catch (Exception e) {
            logger.error("Failed to fetch entity admins", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    @PostMapping("/entity-admins")
    public ResponseEntity<?> createEntityAdmin(@RequestBody EntityAdminDto entityAdminDto) {
        if (entityAdminRepository.findByUsername(entityAdminDto.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }

        Organization organization = organizationRepository.findById(entityAdminDto.getOrganizationId())
                .orElse(null);
        if (organization == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found");
        }

        // Check if organization already has an admin
        if (entityAdminRepository.existsByOrganization(organization)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Organization already has an entity admin assigned. Remove the existing admin first.");
        }

        // Get the ENTITY_ADMIN role
        Role entityAdminRole = roleRepository.findByName("ENTITY_ADMIN")
                .orElseThrow(() -> new RuntimeException("ENTITY_ADMIN role not found"));

        EntityAdmin entityAdmin = new EntityAdmin();
        entityAdmin.setUsername(entityAdminDto.getUsername());
        entityAdmin.setPassword(passwordEncoder.encode(entityAdminDto.getPassword()));
        entityAdmin.setOrganization(organization);
        entityAdmin.setRole(entityAdminRole); // Set the role
        entityAdmin.setCreatedAt(java.time.LocalDateTime.now()); // Explicitly set creation time

        EntityAdmin savedEntityAdmin = entityAdminRepository.save(entityAdmin);
        // Avoid sending password back in response
        EntityAdminDto responseDto = new EntityAdminDto(savedEntityAdmin.getUsername(), null, savedEntityAdmin.getOrganization().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @DeleteMapping("/entity-admins/{id}")
    @Transactional
    public ResponseEntity<?> removeEntityAdmin(@PathVariable Long id) {
        try {
            Optional<EntityAdmin> entityAdminOpt = entityAdminRepository.findById(id);
            if (entityAdminOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Entity Admin not found"));
            }

            EntityAdmin entityAdmin = entityAdminOpt.get();
            String username = entityAdmin.getUsername();
            String organizationName = entityAdmin.getOrganization() != null ?
                    entityAdmin.getOrganization().getName() : "Unknown";

            // Check if this is actually a SuperAdmin record in the wrong table
            if ("superadmin".equals(username) || (entityAdmin.getRole() != null &&
                "SUPER_ADMIN".equals(entityAdmin.getRole().getName()))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Cannot delete SuperAdmin from Entity Admins. This record should not exist here."));
            }

            // SECURITY: Delete all related refresh tokens to immediately invalidate all sessions
            // This ensures the removed admin cannot access the system with existing tokens
            refreshTokenRepository.deleteByUser(entityAdmin);
            logger.info("Deleted all refresh tokens for user: {}", username);

            // Delete the entity admin record
            entityAdminRepository.delete(entityAdmin);
            logger.info("Deleted EntityAdmin record for user: {}", username);

            return ResponseEntity.ok(Map.of(
                    "message", "Entity Admin removed successfully",
                    "username", username,
                    "organizationName", organizationName
            ));
        } catch (Exception e) {
            logger.error("Failed to remove entity admin with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to remove entity admin: " + e.getMessage()));
        }
    }



    // Helper method to convert Organization to OrganizationDto
    private OrganizationDto convertToDto(Organization organization) {
        OrganizationDto dto = new OrganizationDto();
        dto.setId(organization.getId());
        dto.setEntityId(organization.getEntityId()); // Include Entity ID
        dto.setName(organization.getName());
        dto.setAddress(organization.getAddress());
        dto.setLatitude(organization.getLatitude());
        dto.setLongitude(organization.getLongitude());
        dto.setContactPerson(organization.getContactPerson());
        dto.setEmail(organization.getEmail());
        return dto;
    }
}
