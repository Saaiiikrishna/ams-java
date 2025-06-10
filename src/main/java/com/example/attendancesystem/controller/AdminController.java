package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.EntityAdminDto;
import com.example.attendancesystem.dto.OrganizationDto;
import com.example.attendancesystem.model.EntityAdmin;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.Role;
import com.example.attendancesystem.repository.EntityAdminRepository;
import com.example.attendancesystem.repository.OrganizationRepository;
import com.example.attendancesystem.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping; // Added for GET
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import java.util.List; // Added for List
import java.util.stream.Collectors; // Added for Collectors

@RestController
@RequestMapping("/super")
@PreAuthorize("hasRole('SUPER_ADMIN')") // Requires SUPER_ADMIN for all methods in this controller
public class AdminController {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EntityAdminRepository entityAdminRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/entities")
    public ResponseEntity<?> createOrganization(@RequestBody OrganizationDto organizationDto) {
        if (organizationRepository.existsByName(organizationDto.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Organization name already exists");
        }
        Organization organization = new Organization();
        organization.setName(organizationDto.getName());
        organization.setAddress(organizationDto.getAddress());
        organization.setLatitude(organizationDto.getLatitude());
        organization.setLongitude(organizationDto.getLongitude());
        organization.setContactPerson(organizationDto.getContactPerson());
        organization.setEmail(organizationDto.getEmail());
        Organization savedOrganization = organizationRepository.save(organization);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(savedOrganization)); // Use DTO
    }

    @GetMapping("/entities")
    public ResponseEntity<List<OrganizationDto>> getAllOrganizations() {
        List<Organization> organizations = organizationRepository.findAll();
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

    @PutMapping("/entities/{id}")
    public ResponseEntity<?> updateOrganization(@PathVariable Long id, @RequestBody OrganizationDto organizationDto) {
        Organization organization = organizationRepository.findById(id)
                .orElse(null);
        if (organization == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found");
        }

        // Check if name is being changed and if new name already exists
        if (!organization.getName().equals(organizationDto.getName()) &&
            organizationRepository.existsByName(organizationDto.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Organization name already exists");
        }

        organization.setName(organizationDto.getName());
        organization.setAddress(organizationDto.getAddress());
        organization.setLatitude(organizationDto.getLatitude());
        organization.setLongitude(organizationDto.getLongitude());
        organization.setContactPerson(organizationDto.getContactPerson());
        organization.setEmail(organizationDto.getEmail());

        Organization updatedOrganization = organizationRepository.save(organization);
        return ResponseEntity.ok(convertToDto(updatedOrganization));
    }

    @DeleteMapping("/entities/{id}")
    public ResponseEntity<?> deleteOrganization(@PathVariable Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElse(null);
        if (organization == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found");
        }

        // Check if organization has any entity admins
        if (entityAdminRepository.existsByOrganization(organization)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Cannot delete organization with existing entity admins. Please remove all entity admins first.");
        }

        organizationRepository.delete(organization);
        return ResponseEntity.ok("Organization deleted successfully");
    }

    @PostMapping("/entities/{id}/assign-admin")
    public ResponseEntity<?> assignAdminToEntity(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Organization organization = organizationRepository.findById(id)
                .orElse(null);
        if (organization == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found");
        }

        // Check if organization already has an admin
        if (entityAdminRepository.existsByOrganization(organization)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Organization already has an entity admin assigned");
        }

        String username = request.get("username");
        String password = request.get("password");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Username and password are required");
        }

        // Check if username already exists
        if (entityAdminRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }

        // Create new entity admin
        EntityAdmin entityAdmin = new EntityAdmin();
        entityAdmin.setUsername(username);
        entityAdmin.setPassword(passwordEncoder.encode(password));
        entityAdmin.setOrganization(organization);

        // Set role
        Role entityAdminRole = roleRepository.findByName("ROLE_ENTITY_ADMIN")
                .orElseThrow(() -> new RuntimeException("Entity Admin role not found"));
        entityAdmin.setRole(entityAdminRole);

        EntityAdmin savedAdmin = entityAdminRepository.save(entityAdmin);

        return ResponseEntity.ok(Map.of(
                "message", "Entity admin assigned successfully",
                "adminId", savedAdmin.getId(),
                "username", savedAdmin.getUsername(),
                "organizationId", organization.getId(),
                "organizationName", organization.getName()
        ));
    }

    @DeleteMapping("/entities/{id}/remove-admin")
    public ResponseEntity<?> removeAdminFromEntity(@PathVariable Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElse(null);
        if (organization == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Organization not found");
        }

        // Find and remove the entity admin for this organization
        Optional<EntityAdmin> existingAdmin = entityAdminRepository.findByOrganization(organization);
        if (!existingAdmin.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No admin found for this organization");
        }

        entityAdminRepository.delete(existingAdmin.get());

        return ResponseEntity.ok(Map.of(
                "message", "Admin removed successfully",
                "organizationId", organization.getId(),
                "organizationName", organization.getName()
        ));
    }

    @GetMapping("/entity-admins")
    public ResponseEntity<List<Map<String, Object>>> getAllEntityAdmins() {
        List<EntityAdmin> entityAdmins = entityAdminRepository.findAll();
        List<Map<String, Object>> adminDtos = entityAdmins.stream()
                .map(admin -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", admin.getId());
                    dto.put("username", admin.getUsername());
                    dto.put("organizationId", admin.getOrganization().getId());
                    dto.put("organizationName", admin.getOrganization().getName());
                    dto.put("createdAt", admin.getCreatedAt());
                    dto.put("role", admin.getRole().getName());
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(adminDtos);
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

        // Get the ENTITY_ADMIN role
        Role entityAdminRole = roleRepository.findByName("ENTITY_ADMIN")
                .orElseThrow(() -> new RuntimeException("ENTITY_ADMIN role not found"));

        EntityAdmin entityAdmin = new EntityAdmin();
        entityAdmin.setUsername(entityAdminDto.getUsername());
        entityAdmin.setPassword(passwordEncoder.encode(entityAdminDto.getPassword()));
        entityAdmin.setOrganization(organization);
        entityAdmin.setRole(entityAdminRole); // Set the role

        EntityAdmin savedEntityAdmin = entityAdminRepository.save(entityAdmin);
        // Avoid sending password back in response
        EntityAdminDto responseDto = new EntityAdminDto(savedEntityAdmin.getUsername(), null, savedEntityAdmin.getOrganization().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    // Helper method to convert Organization to OrganizationDto
    private OrganizationDto convertToDto(Organization organization) {
        OrganizationDto dto = new OrganizationDto();
        dto.setId(organization.getId());
        dto.setName(organization.getName());
        dto.setAddress(organization.getAddress());
        dto.setLatitude(organization.getLatitude());
        dto.setLongitude(organization.getLongitude());
        dto.setContactPerson(organization.getContactPerson());
        dto.setEmail(organization.getEmail());
        return dto;
    }
}
