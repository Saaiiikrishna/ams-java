package com.example.attendancesystem.security;

import com.example.attendancesystem.model.EntityAdmin;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections; // Added for Collections.singletonList

public class CustomUserDetails implements UserDetails {

    private final EntityAdmin entityAdmin; // Assuming EntityAdmin is your user model

    public CustomUserDetails(EntityAdmin entityAdmin) {
        this.entityAdmin = entityAdmin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = entityAdmin.getRole() != null ? entityAdmin.getRole().getName() : "ENTITY_ADMIN";
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    @Override
    public String getPassword() {
        return entityAdmin.getPassword();
    }

    @Override
    public String getUsername() {
        return entityAdmin.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Or implement logic
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Or implement logic
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Or implement logic
    }

    @Override
    public boolean isEnabled() {
        return true; // Or implement logic, e.g., based on an 'active' flag in EntityAdmin
    }

    // Helper to get the wrapped EntityAdmin if needed
    public EntityAdmin getEntityAdmin() {
        return entityAdmin;
    }
}
