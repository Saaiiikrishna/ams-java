package com.example.attendancesystem.security;

import com.example.attendancesystem.model.SuperAdmin;
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
        return superAdmin.getPassword();
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

    // Helper to get the wrapped SuperAdmin if needed
    public SuperAdmin getSuperAdmin() {
        return superAdmin;
    }
}
