package com.example.attendancesystem.auth.config;

import com.example.attendancesystem.auth.security.JwtRequestFilter;
import com.example.attendancesystem.auth.security.SuperAdminJwtRequestFilter;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    @Qualifier("entityAdminUserDetailsService")
    private UserDetailsService entityAdminUserDetailsService;

    @Autowired
    @Qualifier("superAdminUserDetailsService")
    private UserDetailsService superAdminUserDetailsService;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

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
    @Qualifier("entityAdminAuthenticationProvider")
    public DaoAuthenticationProvider entityAdminAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(entityAdminUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    @Qualifier("superAdminAuthenticationProvider")
    public DaoAuthenticationProvider superAdminAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider() {
            @Override
            protected void additionalAuthenticationChecks(UserDetails userDetails,
                    UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
                System.out.println("DEBUG: ===== PASSWORD VERIFICATION =====");
                System.out.println("DEBUG: Raw password from request: " + authentication.getCredentials());
                System.out.println("DEBUG: Encoded password from DB: " + userDetails.getPassword());
                System.out.println("DEBUG: Password encoder class: " + getPasswordEncoder().getClass().getSimpleName());

                try {
                    super.additionalAuthenticationChecks(userDetails, authentication);
                    System.out.println("DEBUG: Password verification SUCCESSFUL");
                } catch (Exception e) {
                    System.err.println("DEBUG: Password verification FAILED: " + e.getMessage());
                    throw e;
                }
            }
        };
        authProvider.setUserDetailsService(superAdminUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Super Admin Security Configuration (Order 1 - Highest Priority)
    @Bean
    @Order(1)
    public SecurityFilterChain superAdminSecurityFilterChain(HttpSecurity http) throws Exception {
        logger.debug("Configuring SuperAdmin security filter chain");
        return http
            .securityMatcher("/super/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/super/auth/login", "/super/auth/refresh-token", "/super/auth/reset-superadmin-password").permitAll()
                .requestMatchers("/super/**").permitAll()  // Temporarily allow all for debugging
                .anyRequest().denyAll()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(superAdminJwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter((request, response, chain) -> {
                logger.debug("SuperAdmin Security Filter Chain processing request: {}", ((jakarta.servlet.http.HttpServletRequest) request).getRequestURI());
                chain.doFilter(request, response);
            }, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    // Entity Admin Security Configuration (Order 2)
    @Bean
    @Order(2)
    public SecurityFilterChain entityAdminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/refresh-token").permitAll()
                .requestMatchers("/api/v2/auth/login", "/api/v2/auth/refresh", "/api/v2/auth/hash-password").permitAll() // Modern auth endpoints
                .requestMatchers("/api/discovery/**").permitAll() // Allow discovery endpoints for mobile apps
                .requestMatchers("/api/subscriber/auth/**").permitAll() // Allow subscriber authentication
                .requestMatchers("/api/**").hasRole("ENTITY_ADMIN")
                .anyRequest().denyAll()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Default Security Configuration (Order 3 - Lowest Priority)
    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/health/**", "/error").permitAll()
                .anyRequest().denyAll()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterAfter((request, response, chain) -> {
                logger.debug("Default Security Filter Chain processing request: {}", ((jakarta.servlet.http.HttpServletRequest) request).getRequestURI());
                chain.doFilter(request, response);
            }, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
