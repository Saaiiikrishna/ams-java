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

            Organization defaultOrg = orgRepo.findById(1L).orElseGet(() -> {
                Organization o = new Organization();
                o.setName("Default Org");
                return orgRepo.save(o);
            });

            if (adminRepo.findByUsername("superadmin").isEmpty()) {
                EntityAdmin admin = new EntityAdmin();
                admin.setUsername("superadmin");
                admin.setPassword(passwordEncoder.encode("password"));
                admin.setOrganization(defaultOrg);
                admin.setRole(superRole);
                adminRepo.save(admin);
            }
        };
    }
}
