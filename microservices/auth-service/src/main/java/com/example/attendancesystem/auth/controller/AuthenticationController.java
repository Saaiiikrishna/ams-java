package com.example.attendancesystem.auth.controller;

import com.example.attendancesystem.dto.LoginRequest;
import com.example.attendancesystem.dto.LoginResponse;
import com.example.attendancesystem.dto.NewAccessTokenResponse; // Added
import com.example.attendancesystem.dto.RefreshTokenRequest; // Added
import com.example.attendancesystem.auth.model.EntityAdmin; // Added (assuming this is the user model from RefreshToken)
import com.example.attendancesystem.auth.model.RefreshToken; // Added
import com.example.attendancesystem.auth.security.JwtUtil;
import com.example.attendancesystem.auth.security.CustomUserDetailsService;
import com.example.attendancesystem.auth.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus; // Added
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid; // Added
import java.util.Optional; // Added
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @Autowired
    @Qualifier("entityAdminAuthenticationProvider")
    private DaoAuthenticationProvider entityAdminAuthenticationProvider;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired // Added
    private RefreshTokenService refreshTokenService; // Added

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            entityAdminAuthenticationProvider.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword())
            );
        } catch (BadCredentialsException | UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Incorrect username or password");
        }

        final UserDetails userDetails =
                userDetailsService.loadUserByUsername(loginRequest.getUsername());

        // Verify this is actually an Entity Admin (not a Super Admin)
        if (userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: Entity Admin login required");
        }

        final String accessToken = jwtUtil.generateToken(userDetails);
        final String refreshTokenString = jwtUtil.generateRefreshToken(userDetails);

        // Save the refresh token to the database
        refreshTokenService.createAndSaveRefreshToken(
                userDetails.getUsername(), refreshTokenString);

        return ResponseEntity.ok(new LoginResponse(accessToken, refreshTokenString));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        // Verify it's an Entity Admin refresh token
        if (!jwtUtil.isRefreshToken(requestRefreshToken) ||
            !jwtUtil.isEntityAdminToken(requestRefreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid Entity Admin refresh token");
        }

        Optional<RefreshToken> refreshTokenOptional = refreshTokenService.findByToken(requestRefreshToken);

        if (refreshTokenOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Refresh token not found in database");
        }

        RefreshToken storedRefreshToken = refreshTokenOptional.get();

        if (!refreshTokenService.verifyExpiration(storedRefreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Refresh token expired");
        }

        EntityAdmin user = storedRefreshToken.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        // Verify this is still an Entity Admin (not a Super Admin)
        if (userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: Entity Admin privileges required");
        }

        String newAccessToken = jwtUtil.generateToken(userDetails);

        // Refresh Token Rotation
        refreshTokenService.deleteToken(requestRefreshToken);
        String newRefreshTokenString = jwtUtil.generateRefreshToken(userDetails);
        refreshTokenService.createAndSaveRefreshToken(userDetails.getUsername(), newRefreshTokenString);

        return ResponseEntity.ok(new NewAccessTokenResponse(newAccessToken, newRefreshTokenString));
    }
}
