package com.example.attendancesystem.auth.security;

import com.example.attendancesystem.auth.model.SuperAdmin;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class SuperAdminUserDetails implements UserDetails {

    private final SuperAdmin superAdmin;

    public SuperAdminUserDetails(SuperAdmin superAdmin) {
        this.superAdmin = superAdmin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = superAdmin.getRole() != null ? superAdmin.getRole().getName() : "SUPER_ADMIN";
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    @Override
    public String getPassword() {
        String password = superAdmin.getPassword();
        System.out.println("DEBUG: SuperAdminUserDetails.getPassword() returning: " +
                          (password != null ? password.substring(0, Math.min(10, password.length())) + "..." : "null"));
        System.out.println("DEBUG: Password length: " + (password != null ? password.length() : 0));
        System.out.println("DEBUG: Password starts with $2a: " + (password != null && password.startsWith("$2a")));
        return password;
    }

    @Override
    public String getUsername() {
        return superAdmin.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return superAdmin.getIsActive() != null ? superAdmin.getIsActive() : true;
    }

    // Getter for SuperAdmin
    public SuperAdmin getSuperAdmin() {
        return superAdmin;
    }
}
