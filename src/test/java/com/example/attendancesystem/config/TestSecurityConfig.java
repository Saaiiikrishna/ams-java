package com.example.attendancesystem.config;

import com.example.attendancesystem.model.EntityAdmin;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.repository.EntityAdminRepository;
import com.example.attendancesystem.repository.OrganizationRepository;
import com.example.attendancesystem.security.CustomUserDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@TestConfiguration
public class TestSecurityConfig {

    // This map will store pre-configured users for testing with @WithUserDetails
    private final Map<String, CustomUserDetails> userDetailsMap = new HashMap<>();

    // You'll need to populate this map, perhaps in the @BeforeEach of your test class
    // or by pre-creating users here if they are static for all tests using this config.

    @Bean
    @Primary // To override the main UserDetailsService
    public UserDetailsService testUserDetailsService(
            OrganizationRepository organizationRepository,
            EntityAdminRepository entityAdminRepository,
            PasswordEncoder passwordEncoder) {

        // Setup a default organization and admin for testing if needed
        // This is just one way to provide users for @WithUserDetails
        // More dynamic setup can be done in test classes themselves by populating userDetailsMap

        Organization org1 = organizationRepository.findByName("Test Org One (EntityController)")
                .orElseGet(() -> {
                    Organization o = new Organization();
                    o.setName("Test Org One (EntityController)");
                    o.setAddress("1 Org St");
                    return organizationRepository.save(o);
                });

        EntityAdmin admin1 = entityAdminRepository.findByUsername("entityadmin1")
                .orElseGet(() -> {
                    EntityAdmin ea = new EntityAdmin();
                    ea.setUsername("entityadmin1");
                    ea.setPassword(passwordEncoder.encode("password"));
                    ea.setOrganization(org1);
                    return entityAdminRepository.save(ea);
                });
        userDetailsMap.put("entityadmin1", new CustomUserDetails(admin1));

        Organization org2 = organizationRepository.findByName("Test Org Two (EntityController)")
                .orElseGet(() -> {
                    Organization o = new Organization();
                    o.setName("Test Org Two (EntityController)");
                    o.setAddress("2 Org St");
                    return organizationRepository.save(o);
                });

        EntityAdmin admin2 = entityAdminRepository.findByUsername("entityadmin2")
                .orElseGet(() -> {
                    EntityAdmin ea = new EntityAdmin();
                    ea.setUsername("entityadmin2");
                    ea.setPassword(passwordEncoder.encode("password"));
                    ea.setOrganization(org2);
                    return entityAdminRepository.save(ea);
                });
        userDetailsMap.put("entityadmin2", new CustomUserDetails(admin2));

        // Special user for testing access to unassigned entities or general auth
        EntityAdmin unassignedAdmin = entityAdminRepository.findByUsername("unassignedadmin")
                .orElseGet(() -> {
                    EntityAdmin ea = new EntityAdmin();
                    // No organization assigned, or assign to a "limbo" org if your model requires it
                    // For this example, let's assume an admin might not have an org, though current model requires it.
                    // Better: create a 'limbo' org or ensure all test admins have one.
                    Organization limboOrg = organizationRepository.findByName("Limbo Org")
                        .orElseGet(()-> {
                            Organization o = new Organization();
                            o.setName("Limbo Org");
                            o.setAddress("0 Limbo Lane");
                            return organizationRepository.save(o);
                        });
                    ea.setUsername("unassignedadmin");
                    ea.setPassword(passwordEncoder.encode("password"));
                    ea.setOrganization(limboOrg); // Must have an org per model
                    return entityAdminRepository.save(ea);
                });
        userDetailsMap.put("unassignedadmin", new CustomUserDetails(unassignedAdmin));


        return username -> Optional.ofNullable(userDetailsMap.get(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found in TestUserDetailsService: " + username));
    }

    // Helper method for test classes to add or update users if needed, though less common
    public void addUser(String username, CustomUserDetails userDetails) {
        userDetailsMap.put(username, userDetails);
    }
}
