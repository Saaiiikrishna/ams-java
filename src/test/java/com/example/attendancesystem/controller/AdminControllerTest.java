package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.EntityAdminDto;
import com.example.attendancesystem.dto.OrganizationDto;
import com.example.attendancesystem.model.EntityAdmin;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.repository.EntityAdminRepository;
import com.example.attendancesystem.repository.OrganizationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize; // For list size check
import static org.hamcrest.Matchers.is; // For value check
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; // Added for GET
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays; // For creating list of orgs

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EntityAdminRepository entityAdminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        entityAdminRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    // Test creating an organization
    @Test
    @WithMockUser(username = "superadmin", roles = {"SUPER_ADMIN"})
    void testCreateOrganization_Success() throws Exception {
        OrganizationDto organizationDto = new OrganizationDto("New Test Org", "789 Super St");

        mockMvc.perform(post("/super/entities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(organizationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Test Org"))
                .andExpect(jsonPath("$.address").value("789 Super St"));

        assertThat(organizationRepository.existsByName("New Test Org")).isTrue();
    }

    @Test
    @WithMockUser(username = "superadmin", roles = {"SUPER_ADMIN"})
    void testCreateOrganization_Conflict() throws Exception {
        Organization existingOrg = new Organization();
        existingOrg.setName("Existing Org");
        existingOrg.setAddress("111 Main St");
        organizationRepository.save(existingOrg);

        OrganizationDto organizationDto = new OrganizationDto("Existing Org", "789 Super St");

        mockMvc.perform(post("/super/entities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(organizationDto)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Organization name already exists"));
    }

    @Test
    @WithMockUser(username = "entityadmin", roles = {"ENTITY_ADMIN"}) // User with wrong role
    void testCreateOrganization_Forbidden_WrongRole() throws Exception {
        OrganizationDto organizationDto = new OrganizationDto("Forbidden Org", "000 Forbidden Ave");

        mockMvc.perform(post("/super/entities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(organizationDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreateOrganization_Forbidden_NoAuth() throws Exception { // No @WithMockUser
        OrganizationDto organizationDto = new OrganizationDto("No Auth Org", "000 No Auth Ave");

        mockMvc.perform(post("/super/entities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(organizationDto)))
                .andExpect(status().isUnauthorized()); // Or 401, Spring security default for unauthenticated
    }

    // Test creating an entity admin
    @Test
    @WithMockUser(username = "superadmin", roles = {"SUPER_ADMIN"})
    void testCreateEntityAdmin_Success() throws Exception {
        Organization org = new Organization();
        org.setName("Org For Admin");
        org.setAddress("456 Admin Ave");
        Organization savedOrg = organizationRepository.save(org);

        EntityAdminDto entityAdminDto = new EntityAdminDto("newadmin", "securepassword", savedOrg.getId());

        mockMvc.perform(post("/admin/entity-admins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entityAdminDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newadmin"))
                .andExpect(jsonPath("$.organizationId").value(savedOrg.getId()));

        EntityAdmin newAdmin = entityAdminRepository.findByUsername("newadmin").orElse(null);
        assertThat(newAdmin).isNotNull();
        assertThat(newAdmin.getOrganization().getId()).isEqualTo(savedOrg.getId());
        assertThat(passwordEncoder.matches("securepassword", newAdmin.getPassword())).isTrue();
    }

    @Test
    @WithMockUser(username = "superadmin", roles = {"SUPER_ADMIN"})
    void testCreateEntityAdmin_UsernameConflict() throws Exception {
        Organization org = new Organization();
        org.setName("Org For Conflict Admin");
        org.setAddress("789 Conflict St");
        Organization savedOrg = organizationRepository.save(org);

        EntityAdmin existingAdmin = new EntityAdmin();
        existingAdmin.setUsername("existingadmin");
        existingAdmin.setPassword(passwordEncoder.encode("password"));
        existingAdmin.setOrganization(savedOrg);
        entityAdminRepository.save(existingAdmin);

        EntityAdminDto entityAdminDto = new EntityAdminDto("existingadmin", "newpassword", savedOrg.getId());

        mockMvc.perform(post("/admin/entity-admins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entityAdminDto)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Username already exists"));
    }

    @Test
    @WithMockUser(username = "superadmin", roles = {"SUPER_ADMIN"})
    void testCreateEntityAdmin_OrganizationNotFound() throws Exception {
        EntityAdminDto entityAdminDto = new EntityAdminDto("adminfornonexistentorg", "password", 9999L); // Non-existent org ID

        mockMvc.perform(post("/admin/entity-admins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entityAdminDto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Organization not found"));
    }


    @Test
    @WithMockUser(username = "entityadmin", roles = {"ENTITY_ADMIN"})
    void testCreateEntityAdmin_Forbidden_WrongRole() throws Exception {
        Organization org = new Organization();
        org.setName("Org For Forbidden Admin");
        org.setAddress("121 Forbidden Ave");
        Organization savedOrg = organizationRepository.save(org);
        EntityAdminDto entityAdminDto = new EntityAdminDto("forbiddenadmin", "password", savedOrg.getId());

        mockMvc.perform(post("/admin/entity-admins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(entityAdminDto)))
                .andExpect(status().isForbidden());
    }

    // --- Tests for GET /admin/entities ---

    @Test
    @WithMockUser(username = "superadmin", roles = {"SUPER_ADMIN"})
    void testGetAllOrganizations_Success() throws Exception {
        Organization org1 = new Organization();
        org1.setName("Org Alpha");
        org1.setAddress("1 Alpha St");
        organizationRepository.save(org1);

        Organization org2 = new Organization();
        org2.setName("Org Beta");
        org2.setAddress("2 Beta St");
        organizationRepository.save(org2);

        mockMvc.perform(get("/admin/entities")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Org Alpha")))
                .andExpect(jsonPath("$[1].name", is("Org Beta")));
    }

    @Test
    @WithMockUser(username = "superadmin", roles = {"SUPER_ADMIN"})
    void testGetAllOrganizations_Success_EmptyList() throws Exception {
        // No organizations saved yet
        mockMvc.perform(get("/admin/entities")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }


    @Test
    @WithMockUser(username = "entityadmin", roles = {"ENTITY_ADMIN"})
    void testGetAllOrganizations_Forbidden_WrongRole() throws Exception {
        mockMvc.perform(get("/admin/entities")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAllOrganizations_Forbidden_NoAuth() throws Exception {
        mockMvc.perform(get("/admin/entities")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
