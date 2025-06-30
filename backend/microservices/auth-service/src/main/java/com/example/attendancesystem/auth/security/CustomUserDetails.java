package com.example.attendancesystem.auth.security;

import com.example.attendancesystem.auth.model.EntityAdmin;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

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
        return true; // You can implement your logic here
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // You can implement your logic here
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // You can implement your logic here
    }

    @Override
    public boolean isEnabled() {
        return true; // You can implement your logic here based on EntityAdmin status
    }

    // Getter for EntityAdmin
    public EntityAdmin getEntityAdmin() {
        return entityAdmin;
    }
}
