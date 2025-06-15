package com.example.attendancesystem.config;

import com.example.attendancesystem.security.CustomUserDetailsService;
import com.example.attendancesystem.security.JwtRequestFilter;
import com.example.attendancesystem.security.SubscriberJwtRequestFilter;
import com.example.attendancesystem.security.SuperAdminJwtRequestFilter;
import com.example.attendancesystem.security.SuperAdminUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration; // Added
import org.springframework.web.cors.CorsConfigurationSource; // Added
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // Added

import java.util.Arrays; // Added
import java.util.List; // Added

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enable method-level security
public class SecurityConfig {

    @Autowired
    @Qualifier("entityAdminUserDetailsService")
    private UserDetailsService entityAdminUserDetailsService;

    @Autowired
    @Qualifier("superAdminUserDetailsService")
    private UserDetailsService superAdminUserDetailsService;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private SubscriberJwtRequestFilter subscriberJwtRequestFilter;

    @Autowired
    private SuperAdminJwtRequestFilter superAdminJwtRequestFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider entityAdminAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(entityAdminUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public DaoAuthenticationProvider superAdminAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(superAdminUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Static Resources Security Configuration (Order 1 - Highest Priority)
    @Bean
    @Order(1)
    public SecurityFilterChain staticResourcesSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/assets/**", "/*.js", "/*.css", "/*.png", "/*.ico", "/*.html") // Static resources
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // Allow all static resources
            );

        return http.build();
    }

    // Super Admin Security Configuration (Order 2 - High Priority)
    @Bean
    @Order(2)
    public SecurityFilterChain superAdminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/super/**") // Only apply to /super/** paths
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/super/auth/login", "/super/auth/refresh-token", "/super/auth/reset-superadmin-password").permitAll()
                .requestMatchers("/super/**").hasRole("SUPER_ADMIN")
                .anyRequest().denyAll() // Deny all other requests for this matcher
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(superAdminJwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Subscriber Security Configuration (Order 3 - Medium Priority)
    @Bean
    @Order(3)
    public SecurityFilterChain subscriberSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/subscriber/**") // Only apply to /subscriber/** paths
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/subscriber/send-otp", "/subscriber/verify-otp", "/subscriber/login-pin", "/subscriber/update-pin", "/subscriber/health", "/subscriber/discovery", "/subscriber/test-hostname", "/subscriber/mdns-troubleshoot", "/subscriber/hosts-config").permitAll()
                .requestMatchers("/menu/**").permitAll() // Allow public access to menu endpoints
                .requestMatchers("/subscriber/**").hasRole("SUBSCRIBER")
                .anyRequest().denyAll()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(subscriberJwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Public API Security Configuration (Order 4 - Public endpoints)
    @Bean
    @Order(4)
    public SecurityFilterChain publicApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/public/**") // Only apply to /api/public/** paths
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // Allow all public API requests
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    // Entity Admin Security Configuration (Order 5 - Lower Priority)
    @Bean
    @Order(5)
    public SecurityFilterChain entityAdminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**") // Only apply to /api/** paths
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/refresh-token", "/api/test/**").permitAll()
                .requestMatchers("/api/**").hasRole("ENTITY_ADMIN")
                .anyRequest().denyAll() // Deny all other requests for this matcher
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean // Added CORS configuration source bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // Allow all origins for development
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")); // Allow common methods
        configuration.setAllowedHeaders(List.of("*")); // Allow all headers
        // configuration.setAllowCredentials(true); // Only set if you need credentials and origins are specific

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply this configuration to all paths
        return source;
    }
}
