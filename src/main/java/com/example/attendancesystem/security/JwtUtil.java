package com.example.attendancesystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    // Fixed secret key for JWT tokens
    private static final String SECRET_STRING = "EntityAdminSecretKeyForJWTTokenGenerationAndValidation2024!@#$%^&*()";
    private final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());
    private final long ACCESS_TOKEN_EXPIRATION_TIME = 1000 * 60 * 60 * 1; // 1 hour
    private final long REFRESH_TOKEN_EXPIRATION_TIME = 1000 * 60 * 60 * 24 * 7; // 7 days

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) { // This is now the Access Token
        Map<String, Object> claims = new HashMap<>();
        // Add authorities to claims
        claims.put("authorities", userDetails.getAuthorities());
        claims.put("tokenType", "ENTITY_ADMIN_ACCESS");
        return createToken(claims, userDetails.getUsername(), ACCESS_TOKEN_EXPIRATION_TIME);
    }

    public String generateRefreshToken(UserDetails userDetails) { // New method for Refresh Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "ENTITY_ADMIN_REFRESH");
        return createToken(claims, userDetails.getUsername(), REFRESH_TOKEN_EXPIRATION_TIME);
    }

    // Modified createToken to accept expiration time
    public String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime)) // Use passed expirationTime
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public Boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = (String) claims.get("tokenType");
            return "ENTITY_ADMIN_REFRESH".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean isEntityAdminToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = (String) claims.get("tokenType");
            return tokenType != null && tokenType.startsWith("ENTITY_ADMIN");
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean isSubscriberToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = (String) claims.get("tokenType");
            return "SUBSCRIBER_ACCESS".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
