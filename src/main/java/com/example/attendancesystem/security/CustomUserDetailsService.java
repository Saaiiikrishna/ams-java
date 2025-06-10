package com.example.attendancesystem.security;

import com.example.attendancesystem.model.EntityAdmin;
import com.example.attendancesystem.repository.EntityAdminRepository;
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

        return new CustomUserDetails(entityAdmin);
    }
}
