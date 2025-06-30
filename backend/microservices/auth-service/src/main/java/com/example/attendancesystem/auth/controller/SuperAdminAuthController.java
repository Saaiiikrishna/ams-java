package com.example.attendancesystem.auth.controller;

import com.example.attendancesystem.shared.dto.LoginRequest;
import com.example.attendancesystem.shared.dto.LoginResponse;
import com.example.attendancesystem.shared.dto.NewAccessTokenResponse;
import com.example.attendancesystem.shared.dto.RefreshTokenRequest;
import com.example.attendancesystem.auth.model.EntityAdmin;
import com.example.attendancesystem.auth.model.RefreshToken;
import com.example.attendancesystem.auth.model.SuperAdminRefreshToken;
import com.example.attendancesystem.auth.security.SuperAdminJwtUtil;
import com.example.attendancesystem.auth.security.SuperAdminUserDetailsService;
import com.example.attendancesystem.auth.service.RefreshTokenService;
import com.example.attendancesystem.auth.service.SuperAdminRefreshTokenService;
import com.example.attendancesystem.auth.dto.ErrorResponse;
import com.example.attendancesystem.auth.dto.SuccessResponse;
import com.example.attendancesystem.auth.model.SuperAdmin;
import com.example.attendancesystem.auth.repository.SuperAdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.Map;

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

    @Autowired
    private SuperAdminRefreshTokenService superAdminRefreshTokenService;

    @Autowired
    private SuperAdminRepository superAdminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            System.out.println("DEBUG: ===== SUPER ADMIN LOGIN ATTEMPT =====");
            System.out.println("DEBUG: Username: " + loginRequest.getUsername());
            System.out.println("DEBUG: Attempting authentication...");

            superAdminAuthenticationProvider.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword())
            );

            System.out.println("DEBUG: Authentication successful!");
        } catch (BadCredentialsException | UsernameNotFoundException ex) {
            System.err.println("DEBUG: Authentication failed: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("AUTHENTICATION_FAILED", "Incorrect username or password", 401));
        }

        final UserDetails userDetails =
                superAdminUserDetailsService.loadUserByUsername(loginRequest.getUsername());

        // Verify this is actually a Super Admin
        if (!userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("ACCESS_DENIED", "Access denied: Super Admin privileges required", 403));
        }

        final String accessToken = superAdminJwtUtil.generateToken(userDetails);
        final String refreshTokenString = superAdminJwtUtil.generateRefreshToken(userDetails);

        // Save the refresh token to the database using SuperAdmin-specific service
        superAdminRefreshTokenService.createAndSaveRefreshToken(
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
                    .body(new ErrorResponse("INVALID_TOKEN", "Invalid Super Admin refresh token", 401));
        }

        Optional<SuperAdminRefreshToken> refreshTokenOptional = superAdminRefreshTokenService.findByToken(requestRefreshToken);

        if (refreshTokenOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("TOKEN_NOT_FOUND", "Refresh token not found in database", 401));
        }

        SuperAdminRefreshToken storedRefreshToken = refreshTokenOptional.get();

        if (!superAdminRefreshTokenService.verifyExpiration(storedRefreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("TOKEN_EXPIRED", "Refresh token expired", 401));
        }

        SuperAdmin user = storedRefreshToken.getUser();
        UserDetails userDetails = superAdminUserDetailsService.loadUserByUsername(user.getUsername());

        // Verify this is still a Super Admin
        if (!userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("ACCESS_DENIED", "Access denied: Super Admin privileges required", 403));
        }

        String newAccessToken = superAdminJwtUtil.generateToken(userDetails);

        // Refresh Token Rotation
        superAdminRefreshTokenService.deleteToken(requestRefreshToken);
        String newRefreshTokenString = superAdminJwtUtil.generateRefreshToken(userDetails);
        superAdminRefreshTokenService.createAndSaveRefreshToken(userDetails.getUsername(), newRefreshTokenString);

        return ResponseEntity.ok(new NewAccessTokenResponse(newAccessToken, newRefreshTokenString));
    }

    // TEMPORARY ENDPOINT TO RESET SUPERADMIN PASSWORD - REMOVE IN PRODUCTION
    @PostMapping("/reset-superadmin-password")
    public ResponseEntity<?> resetSuperAdminPassword() {
        try {
            System.out.println("DEBUG: ===== RESETTING SUPERADMIN PASSWORD =====");
            SuperAdmin superAdmin = superAdminRepository.findByUsername("superadmin")
                    .orElseThrow(() -> new RuntimeException("SuperAdmin not found"));

            String newPassword = "admin123";
            String encodedPassword = passwordEncoder.encode(newPassword);
            superAdmin.setPassword(encodedPassword);
            superAdminRepository.save(superAdmin);

            System.out.println("DEBUG: SuperAdmin password reset successfully");
            System.out.println("DEBUG: New password: " + newPassword);
            System.out.println("DEBUG: Encoded password: " + encodedPassword);

            return ResponseEntity.ok(new SuccessResponse("SuperAdmin password reset successfully",
                    Map.of("newPassword", newPassword)));
        } catch (Exception e) {
            System.err.println("ERROR: Failed to reset SuperAdmin password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("PASSWORD_RESET_FAILED", "Failed to reset password", 500));
        }
    }
}
