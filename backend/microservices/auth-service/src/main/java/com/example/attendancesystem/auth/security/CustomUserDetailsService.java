package com.example.attendancesystem.auth.security;

import com.example.attendancesystem.auth.model.EntityAdmin;
import com.example.attendancesystem.auth.repository.EntityAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("entityAdminUserDetailsService")
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private EntityAdminRepository entityAdminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        EntityAdmin entityAdmin = entityAdminRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Entity Admin not found with username: " + username));

        // Verify that this user has ENTITY_ADMIN role (or no role defaults to ENTITY_ADMIN)
        if (entityAdmin.getRole() != null && "SUPER_ADMIN".equals(entityAdmin.getRole().getName())) {
            throw new UsernameNotFoundException("User is not an Entity Admin: " + username);
        }

        // CRITICAL SECURITY CHECK: Verify EntityAdmin is assigned to an organization
        if (entityAdmin.getOrganization() == null) {
            throw new UsernameNotFoundException("Entity Admin is not assigned to any organization: " + username);
        }

        // Additional validation: Ensure the EntityAdmin record is valid and active
        // This prevents deleted admins from logging in if their records somehow still exist
        if (entityAdmin.getId() == null) {
            throw new UsernameNotFoundException("Invalid Entity Admin record: " + username);
        }

        return new CustomUserDetails(entityAdmin);
    }
}
