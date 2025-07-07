package com.example.attendancesystem.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security Configuration for User Service
 * Provides password encoding functionality
 */
@Configuration
public class SecurityConfig {

    /**
     * Password encoder bean for hashing passwords
     * Uses BCrypt with default strength (10 rounds)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
