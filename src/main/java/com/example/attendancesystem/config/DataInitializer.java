package com.example.attendancesystem.config;

import com.example.attendancesystem.model.EntityAdmin;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.Role;
import com.example.attendancesystem.repository.EntityAdminRepository;
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

            Organization defaultOrg = orgRepo.findByName("Default Org").orElseGet(() -> {
                Organization o = new Organization();
                o.setName("My Org");
                // must supply a non-null address
                o.setAddress("123 Default Avenue, Metropolis");
                return orgRepo.save(o);
            });

            // Check if super admin exists with the correct role
            boolean superAdminExists = adminRepo.findByUsername("superadmin")
                    .map(admin -> admin.getRole() != null && "SUPER_ADMIN".equals(admin.getRole().getName()))
                    .orElse(false);

            if (!superAdminExists) {
                // Remove any existing superadmin with wrong role
                adminRepo.findByUsername("superadmin").ifPresent(adminRepo::delete);

                EntityAdmin admin = new EntityAdmin();
                admin.setUsername("superadmin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setOrganization(null); // Super admin doesn't need an organization
                admin.setRole(superRole);
                adminRepo.save(admin);
                System.out.println("Created default Super Admin:");
                System.out.println("Username: superadmin");
                System.out.println("Password: admin123");
                System.out.println("*** PLEASE CHANGE THE DEFAULT PASSWORD IN PRODUCTION ***");
            }
        };
    }
}
