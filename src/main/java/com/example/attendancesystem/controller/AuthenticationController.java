package com.example.attendancesystem.controller;

import com.example.attendancesystem.dto.LoginRequest;
import com.example.attendancesystem.dto.LoginResponse;
import com.example.attendancesystem.dto.NewAccessTokenResponse; // Added
import com.example.attendancesystem.dto.RefreshTokenRequest; // Added
import com.example.attendancesystem.model.EntityAdmin; // Added (assuming this is the user model from RefreshToken)
import com.example.attendancesystem.model.RefreshToken; // Added
import com.example.attendancesystem.security.JwtUtil;
import com.example.attendancesystem.security.CustomUserDetailsService;
import com.example.attendancesystem.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // Added
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid; // Added
import java.util.Optional; // Added
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin") // Or a more generic path like /auth if preferred
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired // Added
    private RefreshTokenService refreshTokenService; // Added

    @PostMapping("/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
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

        Optional<RefreshToken> refreshTokenOptional = refreshTokenService.findByToken(requestRefreshToken);

        if (refreshTokenOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token not found in database.");
        }

        RefreshToken storedRefreshToken = refreshTokenOptional.get();

        if (!refreshTokenService.verifyExpiration(storedRefreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired.");
        }

        EntityAdmin user = storedRefreshToken.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        String newAccessToken = jwtUtil.generateToken(userDetails);

        // Refresh Token Rotation
        refreshTokenService.deleteToken(requestRefreshToken);
        String newRefreshTokenString = jwtUtil.generateRefreshToken(userDetails);
        refreshTokenService.createAndSaveRefreshToken(userDetails.getUsername(), newRefreshTokenString);

        return ResponseEntity.ok(new NewAccessTokenResponse(newAccessToken, newRefreshTokenString));
    }
}
