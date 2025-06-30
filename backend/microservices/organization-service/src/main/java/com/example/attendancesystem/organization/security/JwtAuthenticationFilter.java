package com.example.attendancesystem.organization.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Generic JWT Authentication Filter for Organization Service
 * Standard implementation that validates JWT tokens from Auth Service
 * Automatically detects and handles SuperAdmin, EntityAdmin, and Subscriber tokens
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    // JWT Secret Keys Configuration - Must match Auth Service EXACTLY
    private static final String SUPER_ADMIN_SECRET = "SuperAdminSecretKeyForJWTTokenGenerationAndValidation2024!@#$%^&*()SUPER";
    private static final String ENTITY_ADMIN_SECRET = "EntityAdminSecretKeyForJWTTokenGenerationAndValidation2024!@#$%^&*()";
    private static final String SUBSCRIBER_SECRET = "subscriberSecretKey123456789012345678901234567890SubscriberSecretExtension";

    // Pre-computed secret keys for performance
    private final List<TokenValidator> tokenValidators;

    public JwtAuthenticationFilter() {
        logger.info("JwtAuthenticationFilter initialized - Generic JWT validation enabled");

        // Initialize token validators in order of priority
        // SUPER_ADMIN first since Auth Service uses SuperAdmin secret for SuperAdmin tokens
        this.tokenValidators = List.of(
            new TokenValidator("SUPER_ADMIN", Keys.hmacShaKeyFor(SUPER_ADMIN_SECRET.getBytes())),
            new TokenValidator("ENTITY_ADMIN", Keys.hmacShaKeyFor(ENTITY_ADMIN_SECRET.getBytes())),
            new TokenValidator("SUBSCRIBER", Keys.hmacShaKeyFor(SUBSCRIBER_SECRET.getBytes()))
        );

        // Debug: Log validator initialization
        logger.info("Initialized {} token validators:", tokenValidators.size());
        for (TokenValidator validator : tokenValidators) {
            logger.info("  - {} validator initialized", validator.userType);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        logger.debug("JwtAuthenticationFilter processing request: {}", request.getRequestURI());

        // Skip authentication if already authenticated
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwt = authorizationHeader.substring(7);

            logger.debug("JWT token found, attempting validation...");

            // Try to validate token with all available validators
            AuthenticationResult authResult = validateJwtToken(jwt);

            if (authResult.isValid()) {
                // Create Spring Security authentication
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        authResult.getUsername(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(authResult.getRole())));

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                logger.info("Authentication successful - User: {}, Role: {}, Type: {}",
                           authResult.getUsername(), authResult.getRole(), authResult.getUserType());
            } else {
                logger.warn("JWT token validation failed for request: {}", request.getRequestURI());
            }
        } else {
            logger.debug("No Authorization header or Bearer token found for request: {}", request.getRequestURI());
        }

        chain.doFilter(request, response);
    }

    /**
     * Validate JWT token using all available validators
     */
    private AuthenticationResult validateJwtToken(String token) {
        logger.debug("Testing token with {} validators", tokenValidators.size());
        for (TokenValidator validator : tokenValidators) {
            try {
                logger.debug("Testing token with {} validator", validator.getUserType());
                AuthenticationResult result = validator.validateToken(token);
                if (result.isValid()) {
                    logger.debug("Token validated successfully with {} validator", validator.getUserType());
                    return result;
                }
            } catch (Exception e) {
                logger.debug("Token validation failed with {} validator: {}", validator.getUserType(), e.getMessage());
            }
        }

        logger.debug("Token validation failed with all validators");
        return new AuthenticationResult(false, null, null, null);
    }

    /**
     * Token Validator class for handling different JWT types
     */
    private static class TokenValidator {
        private final String userType;
        private final SecretKey secretKey;

        public TokenValidator(String userType, SecretKey secretKey) {
            this.userType = userType;
            this.secretKey = secretKey;
        }

        public String getUserType() {
            return userType;
        }

        public AuthenticationResult validateToken(String token) {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Check token expiration
            if (claims.getExpiration().before(new Date())) {
                return new AuthenticationResult(false, null, null, null);
            }

            String username = claims.getSubject();

            // Handle different claim structures from different services
            String claimedUserType = (String) claims.get("userType"); // User service format
            String tokenType = (String) claims.get("tokenType");      // Auth service format
            String type = (String) claims.get("type");                // Subscriber service format
            String role = (String) claims.get("role");                // Subscriber service format

            // For subscriber tokens, use different claim names
            if ("ACCESS".equals(type) && "SUBSCRIBER".equals(role)) {
                claimedUserType = role; // Use role as userType for subscribers
                tokenType = type;       // Use type as tokenType for subscribers
            }

            // Special handling for SuperAdmin tokens from Auth Service
            // These tokens have tokenType="SUPER_ADMIN_ACCESS" but no userType field
            if ("SUPER_ADMIN_ACCESS".equals(tokenType) && claimedUserType == null) {
                claimedUserType = "SUPER_ADMIN";
            }

            // Special handling for EntityAdmin tokens from Auth Service
            // These tokens have tokenType="ENTITY_ADMIN_ACCESS" but no userType field
            if ("ENTITY_ADMIN_ACCESS".equals(tokenType) && claimedUserType == null) {
                claimedUserType = "ENTITY_ADMIN";
            }

            // Validate user type and token type
            if (isValidTokenForUserType(claimedUserType, tokenType)) {
                String springRole = "ROLE_" + claimedUserType;  // Use token claims, not validator type
                return new AuthenticationResult(true, username, springRole, claimedUserType);
            }

            return new AuthenticationResult(false, null, null, null);
        }

        private boolean isValidTokenForUserType(String claimedUserType, String tokenType) {
            // Check if the token type and claimed user type match expected patterns
            if (claimedUserType == null) {
                return false;
            }

            return switch (claimedUserType) {
                case "SUPER_ADMIN" ->
                    // Auth service format: tokenType="SUPER_ADMIN_ACCESS", no userType
                    "SUPER_ADMIN_ACCESS".equals(tokenType) ||
                    // User service format: userType="SUPER_ADMIN", tokenType="ACCESS"
                    "ACCESS".equals(tokenType);
                case "ENTITY_ADMIN" ->
                    // Auth service format: tokenType="ENTITY_ADMIN_ACCESS", no userType
                    "ENTITY_ADMIN_ACCESS".equals(tokenType) ||
                    // User service format: userType="ENTITY_ADMIN", tokenType="ACCESS"
                    "ACCESS".equals(tokenType);
                case "SUBSCRIBER" ->
                    // User service format: userType="SUBSCRIBER", tokenType="ACCESS"
                    "ACCESS".equals(tokenType);
                default -> false;
            };
        }
    }

    /**
     * Authentication result holder
     */
    private static class AuthenticationResult {
        private final boolean valid;
        private final String username;
        private final String role;
        private final String userType;

        public AuthenticationResult(boolean valid, String username, String role, String userType) {
            this.valid = valid;
            this.username = username;
            this.role = role;
            this.userType = userType;
        }

        public boolean isValid() { return valid; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public String getUserType() { return userType; }
    }
}
