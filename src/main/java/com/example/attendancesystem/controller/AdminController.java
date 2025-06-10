package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.EntityAdminDto;
import com.example.attendancesystem.dto.OrganizationDto;
import com.example.attendancesystem.model.EntityAdmin;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.repository.EntityAdminRepository;
import com.example.attendancesystem.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')") // Requires SUPER_ADMIN for all methods in this controller
public class AdminController {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EntityAdminRepository entityAdminRepository;

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

        EntityAdmin entityAdmin = new EntityAdmin();
        entityAdmin.setUsername(entityAdminDto.getUsername());
        entityAdmin.setPassword(passwordEncoder.encode(entityAdminDto.getPassword()));
        entityAdmin.setOrganization(organization);

        EntityAdmin savedEntityAdmin = entityAdminRepository.save(entityAdmin);
        // Avoid sending password back in response
        EntityAdminDto responseDto = new EntityAdminDto(savedEntityAdmin.getUsername(), null, savedEntityAdmin.getOrganization().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("/entities/without-admin")
    public ResponseEntity<List<OrganizationDto>> getEntitiesWithoutAdmin() {
        List<Organization> allOrganizations = organizationRepository.findAll();
        List<Organization> entitiesWithoutAdmin = allOrganizations.stream()
                .filter(org -> entityAdminRepository.findByOrganization(org).isEmpty())
                .collect(Collectors.toList());

        List<OrganizationDto> organizationDtos = entitiesWithoutAdmin.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(organizationDtos);
    }

    @PostMapping("/entities/{id}/assign-admin")
    public ResponseEntity<?> assignAdmin(@PathVariable Long id, @RequestBody EntityAdminDto entityAdminDto) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found with id: " + id));

        // Check if admin already exists for this organization
        if (entityAdminRepository.findByOrganization(organization).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Admin already exists for this organization"));
        }

        // Check if username already exists
        if (entityAdminRepository.findByUsername(entityAdminDto.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Username already exists"));
        }

        EntityAdmin entityAdmin = new EntityAdmin();
        entityAdmin.setUsername(entityAdminDto.getUsername());
        entityAdmin.setPassword(passwordEncoder.encode(entityAdminDto.getPassword()));
        entityAdmin.setOrganization(organization);

        EntityAdmin savedAdmin = entityAdminRepository.save(entityAdmin);
        return ResponseEntity.ok(Map.of("message", "Admin assigned successfully", "admin", savedAdmin));
    }

    @DeleteMapping("/entities/{id}/remove-admin")
    public ResponseEntity<?> removeAdmin(@PathVariable Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found with id: " + id));

        // Find and delete the entity admin for this organization
        Optional<EntityAdmin> entityAdminOpt = entityAdminRepository.findByOrganization(organization);
        if (entityAdminOpt.isPresent()) {
            EntityAdmin entityAdmin = entityAdminOpt.get();
            entityAdminRepository.delete(entityAdmin);
            return ResponseEntity.ok(Map.of(
                    "message", "Admin removed successfully",
                    "organizationId", id,
                    "organizationName", organization.getName()
            ));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No admin found for this organization"));
        }
    }

    @GetMapping("/entity-admins")
    public ResponseEntity<List<Map<String, Object>>> getAllEntityAdmins() {
        List<EntityAdmin> entityAdmins = entityAdminRepository.findAll();
        List<Map<String, Object>> adminDtos = entityAdmins.stream()
                .map(admin -> {
                    Map<String, Object> adminMap = new HashMap<>();
                    adminMap.put("id", admin.getId());
                    adminMap.put("username", admin.getUsername());
                    adminMap.put("organizationId", admin.getOrganization().getId());
                    adminMap.put("organizationName", admin.getOrganization().getName());
                    adminMap.put("createdAt", admin.getCreatedAt() != null ? admin.getCreatedAt().toString() : "");
                    adminMap.put("role", "ENTITY_ADMIN");
                    return adminMap;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(adminDtos);
    }

    @DeleteMapping("/entities/{id}")
    public ResponseEntity<?> deleteEntity(@PathVariable Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found with id: " + id));

        // First remove any associated entity admin
        Optional<EntityAdmin> entityAdminOpt = entityAdminRepository.findByOrganization(organization);
        if (entityAdminOpt.isPresent()) {
            entityAdminRepository.delete(entityAdminOpt.get());
        }

        // Then delete the organization
        organizationRepository.delete(organization);
        return ResponseEntity.ok(Map.of(
                "message", "Entity deleted successfully",
                "entityId", id,
                "entityName", organization.getName()
        ));
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
