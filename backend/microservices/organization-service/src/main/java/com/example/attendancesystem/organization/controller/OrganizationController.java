package com.example.attendancesystem.organization.controller;

import com.example.attendancesystem.organization.model.Organization;
import com.example.attendancesystem.organization.repository.OrganizationRepository;
import com.example.attendancesystem.organization.service.EntityIdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for Organization management
 * Handles CRUD operations for organizations
 */
@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationController.class);

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EntityIdService entityIdService;

    /**
     * Get all organizations
     */
    @GetMapping
    public ResponseEntity<List<Organization>> getAllOrganizations() {
        try {
            List<Organization> organizations = organizationRepository.findByIsActiveTrue();
            return ResponseEntity.ok(organizations);
        } catch (Exception e) {
            logger.error("Error fetching organizations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get organization by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganizationById(@PathVariable Long id) {
        try {
            Optional<Organization> organization = organizationRepository.findByIdAndIsActiveTrue(id);
            return organization.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error fetching organization with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get organization by entity ID
     */
    @GetMapping("/entity/{entityId}")
    public ResponseEntity<Organization> getOrganizationByEntityId(@PathVariable String entityId) {
        try {
            Optional<Organization> organization = organizationRepository.findByEntityIdAndIsActiveTrue(entityId);
            return organization.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error fetching organization with entity ID: {}", entityId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new organization
     */
    @PostMapping
    public ResponseEntity<Organization> createOrganization(@RequestBody Organization organization) {
        try {
            // Generate entity ID if not provided
            if (organization.getEntityId() == null || organization.getEntityId().isEmpty()) {
                String entityId = entityIdService.generateUniqueEntityId();
                organization.setEntityId(entityId);
            }

            // Validate entity ID uniqueness
            if (organizationRepository.existsByEntityId(organization.getEntityId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(null); // Entity ID already exists
            }

            // Validate name uniqueness
            if (organizationRepository.existsByNameAndIsActiveTrue(organization.getName())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(null); // Name already exists
            }

            Organization savedOrganization = organizationRepository.save(organization);
            logger.info("Created organization: {} with entity ID: {}", 
                    savedOrganization.getName(), savedOrganization.getEntityId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedOrganization);
        } catch (Exception e) {
            logger.error("Error creating organization", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update organization
     */
    @PutMapping("/{id}")
    public ResponseEntity<Organization> updateOrganization(@PathVariable Long id, 
                                                         @RequestBody Organization organizationDetails) {
        try {
            Optional<Organization> optionalOrganization = organizationRepository.findByIdAndIsActiveTrue(id);
            
            if (optionalOrganization.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Organization organization = optionalOrganization.get();
            
            // Update fields (entity ID should not be changed)
            organization.setName(organizationDetails.getName());
            organization.setAddress(organizationDetails.getAddress());
            organization.setLatitude(organizationDetails.getLatitude());
            organization.setLongitude(organizationDetails.getLongitude());
            organization.setContactPerson(organizationDetails.getContactPerson());
            organization.setEmail(organizationDetails.getEmail());

            Organization updatedOrganization = organizationRepository.save(organization);
            logger.info("Updated organization: {}", updatedOrganization.getName());
            
            return ResponseEntity.ok(updatedOrganization);
        } catch (Exception e) {
            logger.error("Error updating organization with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Soft delete organization
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteOrganization(@PathVariable Long id) {
        try {
            Optional<Organization> optionalOrganization = organizationRepository.findByIdAndIsActiveTrue(id);
            
            if (optionalOrganization.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Organization organization = optionalOrganization.get();
            
            // Soft delete
            organization.setIsActive(false);
            organizationRepository.save(organization);
            
            logger.info("Soft deleted organization: {}", organization.getName());
            return ResponseEntity.ok("Organization deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting organization with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if organization exists by entity ID
     */
    @GetMapping("/exists/{entityId}")
    public ResponseEntity<Boolean> organizationExists(@PathVariable String entityId) {
        try {
            boolean exists = organizationRepository.existsByEntityIdAndIsActiveTrue(entityId);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            logger.error("Error checking organization existence for entity ID: {}", entityId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
