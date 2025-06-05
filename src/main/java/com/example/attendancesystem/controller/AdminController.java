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
import org.springframework.web.bind.annotation.GetMapping; // Added for GET
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List; // Added for List
import java.util.stream.Collectors; // Added for Collectors

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

    // Helper method to convert Organization to OrganizationDto
    private OrganizationDto convertToDto(Organization organization) {
        OrganizationDto dto = new OrganizationDto();
        dto.setId(organization.getId());
        dto.setName(organization.getName());
        dto.setAddress(organization.getAddress());
        // Add other fields if necessary from your full Organization entity to DTO
        return dto;
    }
}
