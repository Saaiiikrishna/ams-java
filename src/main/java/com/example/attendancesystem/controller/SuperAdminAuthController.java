package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.LoginRequest;
import com.example.attendancesystem.dto.LoginResponse;
import com.example.attendancesystem.dto.NewAccessTokenResponse;
import com.example.attendancesystem.dto.RefreshTokenRequest;
import com.example.attendancesystem.model.EntityAdmin;
import com.example.attendancesystem.model.RefreshToken;
import com.example.attendancesystem.security.SuperAdminJwtUtil;
import com.example.attendancesystem.security.SuperAdminUserDetailsService;
import com.example.attendancesystem.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/super/auth")
public class SuperAdminAuthController {

    @Autowired
    @Qualifier("superAdminAuthenticationProvider")
    private DaoAuthenticationProvider superAdminAuthenticationProvider;

    @Autowired
    private SuperAdminJwtUtil superAdminJwtUtil;

    @Autowired
    @Qualifier("superAdminUserDetailsService")
    private UserDetailsService superAdminUserDetailsService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            superAdminAuthenticationProvider.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword())
            );
        } catch (BadCredentialsException | UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Incorrect username or password");
        }

        final UserDetails userDetails =
                superAdminUserDetailsService.loadUserByUsername(loginRequest.getUsername());

        // Verify this is actually a Super Admin
        if (!userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: Super Admin privileges required");
        }

        final String accessToken = superAdminJwtUtil.generateToken(userDetails);
        final String refreshTokenString = superAdminJwtUtil.generateRefreshToken(userDetails);

        // Save the refresh token to the database
        refreshTokenService.createAndSaveRefreshToken(
                userDetails.getUsername(), refreshTokenString);

        return ResponseEntity.ok(new LoginResponse(accessToken, refreshTokenString));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        // Verify it's a Super Admin refresh token
        if (!superAdminJwtUtil.isRefreshToken(requestRefreshToken) || 
            !superAdminJwtUtil.isSuperAdminToken(requestRefreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid Super Admin refresh token");
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
        UserDetails userDetails = superAdminUserDetailsService.loadUserByUsername(user.getUsername());

        // Verify this is still a Super Admin
        if (!userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied: Super Admin privileges required");
        }

        String newAccessToken = superAdminJwtUtil.generateToken(userDetails);

        // Refresh Token Rotation
        refreshTokenService.deleteToken(requestRefreshToken);
        String newRefreshTokenString = superAdminJwtUtil.generateRefreshToken(userDetails);
        refreshTokenService.createAndSaveRefreshToken(userDetails.getUsername(), newRefreshTokenString);

        return ResponseEntity.ok(new NewAccessTokenResponse(newAccessToken, newRefreshTokenString));
    }
}
