package com.example.attendancesystem.config;

import com.example.attendancesystem.model.EntityAdmin;
import com.example.attendancesystem.model.SuperAdmin;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.Role;
import com.example.attendancesystem.repository.EntityAdminRepository;
import com.example.attendancesystem.repository.SuperAdminRepository;
import com.example.attendancesystem.repository.OrganizationRepository;
import com.example.attendancesystem.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(RoleRepository roleRepo,
                               OrganizationRepository orgRepo,
                               EntityAdminRepository adminRepo,
                               SuperAdminRepository superAdminRepo,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            Role superRole = roleRepo.findByName("SUPER_ADMIN")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName("SUPER_ADMIN");
                        return roleRepo.save(r);
                    });
            Role entityRole = roleRepo.findByName("ENTITY_ADMIN")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName("ENTITY_ADMIN");
                        return roleRepo.save(r);
                    });

            // Organizations should only be created by Super Admins, not by system initialization

            // Check if super admin exists
            boolean superAdminExists = superAdminRepo.findByUsername("superadmin").isPresent();

            if (!superAdminExists) {
                // Create SuperAdmin directly (don't try to migrate from EntityAdmin)
                SuperAdmin superAdmin = new SuperAdmin();
                superAdmin.setUsername("superadmin");
                superAdmin.setPassword(passwordEncoder.encode("admin123"));
                superAdmin.setEmail("superadmin@example.com");
                superAdmin.setFirstName("Super");
                superAdmin.setLastName("Admin");
                superAdmin.setRole(superRole);
                superAdminRepo.save(superAdmin);
                System.out.println("Created default Super Admin:");
                System.out.println("Username: superadmin");
                System.out.println("Password: admin123");
                System.out.println("*** PLEASE CHANGE THE DEFAULT PASSWORD IN PRODUCTION ***");
            }
        };
    }
}
