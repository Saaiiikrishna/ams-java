package com.example.attendancesystem.service;

import com.example.attendancesystem.model.EntityAdmin;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.repository.EntityAdminRepository;
import com.example.attendancesystem.repository.OrganizationRepository;
import com.example.attendancesystem.security.CustomUserDetails;
import com.example.attendancesystem.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
public class CustomUserDetailsServiceTest {

    @Autowired
    private CustomUserDetailsService service;

    @Autowired
    private EntityAdminRepository entityAdminRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        entityAdminRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void loadUserByUsername_returnsUserDetails() {
        Organization org = new Organization();
        org.setName("Service Org");
        org.setAddress("1 Service St");
        organizationRepository.save(org);

        EntityAdmin admin = new EntityAdmin();
        admin.setUsername("serviceuser");
        admin.setPassword(passwordEncoder.encode("pass"));
        admin.setOrganization(org);
        entityAdminRepository.save(admin);

        CustomUserDetails details = (CustomUserDetails) service.loadUserByUsername("serviceuser");
        assertThat(details.getUsername()).isEqualTo("serviceuser");
        assertThat(details.getEntityAdmin().getOrganization().getName()).isEqualTo("Service Org");
        assertThat(details.getAuthorities()).extracting("authority").contains("ROLE_ENTITY_ADMIN");
    }

    @Test
    void loadUserByUsername_notFound() {
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing"));
    }
}
