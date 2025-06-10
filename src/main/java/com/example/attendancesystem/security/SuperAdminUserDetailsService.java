package com.example.attendancesystem.security;

import com.example.attendancesystem.model.EntityAdmin;
import com.example.attendancesystem.repository.EntityAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("superAdminUserDetailsService")
public class SuperAdminUserDetailsService implements UserDetailsService {

    @Autowired
    private EntityAdminRepository entityAdminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        EntityAdmin entityAdmin = entityAdminRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Super Admin not found with username: " + username));
        
        // Verify that this user has SUPER_ADMIN role
        if (entityAdmin.getRole() == null || !"SUPER_ADMIN".equals(entityAdmin.getRole().getName())) {
            throw new UsernameNotFoundException("User is not a Super Admin: " + username);
        }

        // Super Admins should not have an organization (or it should be null)
        if (entityAdmin.getOrganization() != null) {
            System.out.println("Warning: Super Admin " + username + " has an organization assigned. This will be ignored.");
        }
        
        return new CustomUserDetails(entityAdmin);
    }
}
