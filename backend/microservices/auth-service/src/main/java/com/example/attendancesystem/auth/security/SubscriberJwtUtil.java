package com.example.attendancesystem.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Utility class for Subscriber authentication
 * Handles token generation and validation for mobile app users
 */
@Component
public class SubscriberJwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(SubscriberJwtUtil.class);

    @Value("${jwt.subscriber.secret:subscriberSecretKey123456789012345678901234567890}")
    private String secret;

    @Value("${jwt.subscriber.expiration:86400}") // 24 hours in seconds
    private Long expiration;

    @Value("${jwt.subscriber.refresh.expiration:604800}") // 7 days in seconds
    private Long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Extract username (mobile number) from token
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
     * Generate access token for subscriber
     */
    public String generateToken(String mobileNumber, Map<String, Object> subscriberInfo) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "ACCESS");
        claims.put("role", "SUBSCRIBER");
        claims.put("mobileNumber", mobileNumber);
        
        // Add subscriber-specific claims
        if (subscriberInfo != null) {
            claims.put("subscriberId", subscriberInfo.get("subscriberId"));
            claims.put("organizationId", subscriberInfo.get("organizationId"));
            claims.put("name", subscriberInfo.get("name"));
            claims.put("status", subscriberInfo.get("status"));
        }
        
        return createToken(claims, mobileNumber, expiration * 1000);
    }

    /**
     * Generate refresh token for subscriber
     */
    public String generateRefreshToken(String mobileNumber) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "REFRESH");
        claims.put("role", "SUBSCRIBER");
        claims.put("mobileNumber", mobileNumber);
        
        return createToken(claims, mobileNumber, refreshExpiration * 1000);
    }

    /**
     * Create JWT token with claims
     */
    private String createToken(Map<String, Object> claims, String subject, Long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Validate access token
     */
    public Boolean isValidToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }

            Claims claims = extractAllClaims(token);
            
            // Check if it's an access token
            if (!"ACCESS".equals(claims.get("type"))) {
                logger.debug("Token is not an access token");
                return false;
            }
            
            // Check if it's a subscriber token
            if (!"SUBSCRIBER".equals(claims.get("role"))) {
                logger.debug("Token is not a subscriber token");
                return false;
            }

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
     * Validate refresh token
     */
    public Boolean isValidRefreshToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }

            Claims claims = extractAllClaims(token);
            
            // Check if it's a refresh token
            if (!"REFRESH".equals(claims.get("type"))) {
                logger.debug("Token is not a refresh token");
                return false;
            }
            
            // Check if it's a subscriber token
            if (!"SUBSCRIBER".equals(claims.get("role"))) {
                logger.debug("Token is not a subscriber token");
                return false;
            }

            // Check expiration
            if (isTokenExpired(token)) {
                logger.debug("Refresh token is expired");
                return false;
            }

            return true;
        } catch (Exception e) {
            logger.debug("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get subscriber ID from token
     */
    public String getSubscriberId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return (String) claims.get("subscriberId");
        } catch (Exception e) {
            logger.debug("Error extracting subscriber ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get organization ID from token
     */
    public String getOrganizationId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return (String) claims.get("organizationId");
        } catch (Exception e) {
            logger.debug("Error extracting organization ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get subscriber name from token
     */
    public String getSubscriberName(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return (String) claims.get("name");
        } catch (Exception e) {
            logger.debug("Error extracting subscriber name from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token belongs to a specific subscriber
     */
    public Boolean validateTokenForSubscriber(String token, String mobileNumber) {
        try {
            if (!isValidToken(token)) {
                return false;
            }
            
            String tokenMobileNumber = extractUsername(token);
            return mobileNumber.equals(tokenMobileNumber);
        } catch (Exception e) {
            logger.debug("Error validating token for subscriber: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get remaining token validity time in seconds
     */
    public Long getTokenRemainingTime(String token) {
        try {
            Date expiration = extractExpiration(token);
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remaining / 1000);
        } catch (Exception e) {
            return 0L;
        }
    }
}
