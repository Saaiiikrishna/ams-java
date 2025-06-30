package com.example.attendancesystem.auth.security;

import com.example.attendancesystem.auth.model.SuperAdmin;
import com.example.attendancesystem.auth.repository.SuperAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("superAdminUserDetailsService")
public class SuperAdminUserDetailsService implements UserDetailsService {

    @Autowired
    private SuperAdminRepository superAdminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("DEBUG: SuperAdminUserDetailsService - Loading user: " + username);
        SuperAdmin superAdmin = superAdminRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.err.println("DEBUG: Super Admin not found with username: " + username);
                    return new UsernameNotFoundException("Super Admin not found with username: " + username);
                });
        System.out.println("DEBUG: Found SuperAdmin: " + superAdmin.getUsername() + ", Role: " +
                          (superAdmin.getRole() != null ? superAdmin.getRole().getName() : "null"));
        System.out.println("DEBUG: SuperAdmin password from DB: " +
                          (superAdmin.getPassword() != null ? superAdmin.getPassword().substring(0, Math.min(10, superAdmin.getPassword().length())) + "..." : "null"));
        System.out.println("DEBUG: SuperAdmin password length: " + (superAdmin.getPassword() != null ? superAdmin.getPassword().length() : 0));

        // Verify that this user has SUPER_ADMIN role
        if (superAdmin.getRole() == null || !"SUPER_ADMIN".equals(superAdmin.getRole().getName())) {
            throw new UsernameNotFoundException("User is not a Super Admin: " + username);
        }

        // Verify the account is active
        if (!superAdmin.getIsActive()) {
            throw new UsernameNotFoundException("Super Admin account is inactive: " + username);
        }

        return new SuperAdminUserDetails(superAdmin);
    }
}
