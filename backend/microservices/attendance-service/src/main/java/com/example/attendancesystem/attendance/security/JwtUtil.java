package com.example.attendancesystem.attendance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * JWT Utility class for attendance service
 * Handles parsing and validation of JWT tokens from different services
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret:mySecretKey}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            logger.debug("Error extracting claims from token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Check if token is expired
     */
    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Validate token
     */
    public Boolean isValidToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }

            Claims claims = extractAllClaims(token);
            
            // Check expiration
            if (isTokenExpired(token)) {
                logger.debug("Token is expired");
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract user type from token
     * Handles different token formats from different services
     */
    public String extractUserType(String token) {
        try {
            Claims claims = extractAllClaims(token);
            
            // Handle different claim structures from different services
            String userType = (String) claims.get("userType");     // User service format
            String tokenType = (String) claims.get("tokenType");   // Auth service format
            String type = (String) claims.get("type");             // Subscriber service format
            String role = (String) claims.get("role");             // Subscriber service format

            // For subscriber tokens, use role as userType
            if ("ACCESS".equals(type) && "SUBSCRIBER".equals(role)) {
                return "MEMBER"; // Convert SUBSCRIBER to MEMBER for consistency
            }

            // For SuperAdmin tokens from Auth Service
            if ("SUPER_ADMIN_ACCESS".equals(tokenType)) {
                return "SUPER_ADMIN";
            }

            // For EntityAdmin tokens from Auth Service
            if ("ENTITY_ADMIN_ACCESS".equals(tokenType)) {
                return "ENTITY_ADMIN";
            }

            // Return userType if available
            if (userType != null) {
                return userType;
            }

            logger.debug("Unable to determine user type from token");
            return null;
        } catch (Exception e) {
            logger.debug("Error extracting user type from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract subscriber ID from token (for member tokens)
     */
    public Long extractSubscriberId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            
            // Try different claim names
            Object subscriberId = claims.get("subscriberId");
            if (subscriberId != null) {
                if (subscriberId instanceof String) {
                    return Long.parseLong((String) subscriberId);
                } else if (subscriberId instanceof Number) {
                    return ((Number) subscriberId).longValue();
                }
            }
            
            // Try userId as fallback
            Object userId = claims.get("userId");
            if (userId != null) {
                if (userId instanceof String) {
                    return Long.parseLong((String) userId);
                } else if (userId instanceof Number) {
                    return ((Number) userId).longValue();
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.debug("Error extracting subscriber ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract organization ID from token
     */
    public Long extractOrganizationId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            
            // Try different claim names
            Object organizationId = claims.get("organizationId");
            if (organizationId != null) {
                if (organizationId instanceof String) {
                    return Long.parseLong((String) organizationId);
                } else if (organizationId instanceof Number) {
                    return ((Number) organizationId).longValue();
                }
            }
            
            // Try orgId as fallback
            Object orgId = claims.get("orgId");
            if (orgId != null) {
                if (orgId instanceof String) {
                    return Long.parseLong((String) orgId);
                } else if (orgId instanceof Number) {
                    return ((Number) orgId).longValue();
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.debug("Error extracting organization ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract entity ID from token
     */
    public String extractEntityId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return (String) claims.get("entityId");
        } catch (Exception e) {
            logger.debug("Error extracting entity ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract JWT token from Authorization header
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
