package com.example.attendancesystem.security;

import com.example.attendancesystem.model.SuperAdmin;
import com.example.attendancesystem.repository.SuperAdminRepository;
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
        SuperAdmin superAdmin = superAdminRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Super Admin not found with username: " + username));

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
