package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.LoginRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Ensure the test properties are loaded
@Transactional // Ensure tests are rolled back
public class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityAdminRepository entityAdminRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Organization testOrganization;

    @BeforeEach
    void setUp() {
        // Clear repositories if needed, or rely on @Transactional
        entityAdminRepository.deleteAll();
        organizationRepository.deleteAll();

        testOrganization = new Organization();
        testOrganization.setName("Test Org for Auth");
        testOrganization.setAddress("123 Test St");
        organizationRepository.save(testOrganization);

        EntityAdmin admin = new EntityAdmin();
        admin.setUsername("testuser");
        admin.setPassword(passwordEncoder.encode("password"));
        admin.setOrganization(testOrganization);
        entityAdminRepository.save(admin);
    }

    @Test
    void testAuthenticate_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        MvcResult result = mockMvc.perform(post("/admin/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").exists())
                .andExpect(jsonPath("$.jwt").isNotEmpty())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        // Further validation of JWT structure could be done here if needed
        assertThat(responseBody).contains("jwt");
    }

    @Test
    void testAuthenticate_InvalidCredentials_WrongPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/admin/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()) // Based on GlobalExceptionHandler returning 500 for "Incorrect username or password"
                                                     // This should ideally be a 401. Let's adjust the exception handler or expectation.
                                                     // For now, I expect the AuthenticationController to throw "Incorrect username or password" leading to a 500.
                                                     // A real 401 would be from Spring Security itself if BadCredentialsException is thrown before controller advice.
                                                     // Let's assume Spring Security handles BadCredentialsException directly.
                .andExpect(status().isUnauthorized()); // After spring security 6, BadCredentialsException leads to 401 if not caught by specific controller logic.
                                                       // The current AuthenticationController catches it and re-throws a generic Exception.
                                                       // This needs to be fixed in AuthenticationController to return a proper 401.
    }

    @Test
    void testAuthenticate_InvalidCredentials_UserNotFound() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistentuser");
        loginRequest.setPassword("password");

        mockMvc.perform(post("/admin/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                 .andExpect(status().isUnauthorized()); // Similar to above, depends on how UsernameNotFoundException is handled.
                                                       // Spring Security's DaoAuthenticationProvider throws BadCredentialsException for this too.
    }
}
