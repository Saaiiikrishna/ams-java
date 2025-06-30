package com.example.attendancesystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SuperAdminJwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminJwtRequestFilter.class);

    @Autowired
    private SuperAdminJwtUtil superAdminJwtUtil;

    @Autowired
    @Qualifier("superAdminUserDetailsService")
    private UserDetailsService superAdminUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        logger.debug("Processing Super Admin request: {}", request.getRequestURI());

        final String authorizationHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                // Only process if it's a Super Admin token
                if (superAdminJwtUtil.isSuperAdminToken(jwt)) {
                    username = superAdminJwtUtil.extractUsername(jwt);
                    logger.debug("Super Admin token validated for user: {}", username);
                } else {
                    logger.debug("Token is not a Super Admin token for request: {}", request.getRequestURI());
                }
            } catch (Exception e) {
                logger.warn("Super Admin JWT token processing error for request {}: {}", request.getRequestURI(), e.getMessage());
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.superAdminUserDetailsService.loadUserByUsername(username);

            if (superAdminJwtUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                logger.debug("Super Admin authentication set successfully for user: {}", username);
            } else {
                logger.warn("Super Admin token validation failed for user: {}", username);
            }
        }

        chain.doFilter(request, response);
    }
}
