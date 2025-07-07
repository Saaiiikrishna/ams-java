package com.example.attendancesystem.auth.grpc;

import com.example.attendancesystem.grpc.auth.*;
import com.example.attendancesystem.auth.dto.LoginRequest;
import com.example.attendancesystem.auth.dto.SubscriberLoginDto;
import com.example.attendancesystem.auth.model.BlacklistedToken;
import com.example.attendancesystem.auth.repository.BlacklistedTokenRepository;
import com.example.attendancesystem.auth.security.CustomUserDetailsService;
import com.example.attendancesystem.auth.security.JwtUtil;
import com.example.attendancesystem.auth.security.SuperAdminJwtUtil;
import com.example.attendancesystem.auth.security.SuperAdminUserDetailsService;
import com.example.attendancesystem.auth.service.RefreshTokenService;
import com.example.attendancesystem.auth.service.SubscriberAuthService;
import com.example.attendancesystem.auth.service.SuperAdminRefreshTokenService;
import com.example.attendancesystem.auth.model.EntityAdmin;
import com.example.attendancesystem.auth.model.Organization;
import com.example.attendancesystem.auth.model.Role;
import com.example.attendancesystem.auth.repository.EntityAdminRepository;
import com.example.attendancesystem.auth.repository.OrganizationRepository;
import com.example.attendancesystem.auth.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Map;

@GrpcService
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private SuperAdminUserDetailsService superAdminUserDetailsService;

    @Autowired
    private SubscriberAuthService subscriberAuthService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SuperAdminJwtUtil superAdminJwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private SuperAdminRefreshTokenService superAdminRefreshTokenService;

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Autowired
    private EntityAdminRepository entityAdminRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Override
    public void authenticateEntityAdmin(EntityAdminLoginRequest request, StreamObserver<AuthResponse> responseObserver) {
        try {
            logger.info("Entity Admin authentication attempt for username: {}", request.getUsername());

            // Authenticate using Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

            // Verify this is actually an Entity Admin (not a Super Admin)
            if (userDetails.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
                AuthResponse response = AuthResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Access denied: Entity Admin login required")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            final String accessToken = jwtUtil.generateToken(userDetails);
            final String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // Save the refresh token to the database
            refreshTokenService.createAndSaveRefreshToken(userDetails.getUsername(), refreshToken);

            // Build user info
            UserInfo userInfo = UserInfo.newBuilder()
                    .setUsername(userDetails.getUsername())
                    .setUserType("ENTITY_ADMIN")
                    .setOrganizationId(request.getOrganizationId())
                    .addAllRoles(userDetails.getAuthorities().stream()
                            .map(auth -> auth.getAuthority())
                            .toList())
                    .build();

            AuthResponse response = AuthResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Authentication successful")
                    .setAccessToken(accessToken)
                    .setRefreshToken(refreshToken)
                    .setExpiresIn(24 * 60 * 60 * 1000L) // 24 hours
                    .setUserInfo(userInfo)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            logger.info("Entity Admin authentication successful for username: {}", request.getUsername());

        } catch (BadCredentialsException e) {
            logger.warn("Entity Admin authentication failed for username: {}", request.getUsername());
            AuthResponse response = AuthResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Invalid username or password")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Entity Admin authentication error for username: {}", request.getUsername(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Authentication failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void authenticateSuperAdmin(SuperAdminLoginRequest request, StreamObserver<AuthResponse> responseObserver) {
        try {
            logger.info("Super Admin authentication attempt for username: {}", request.getUsername());

            // Authenticate using Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            final UserDetails userDetails = superAdminUserDetailsService.loadUserByUsername(request.getUsername());

            // Verify this is actually a Super Admin
            if (!userDetails.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
                AuthResponse response = AuthResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Access denied: Super Admin privileges required")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            final String accessToken = superAdminJwtUtil.generateToken(userDetails);
            final String refreshToken = superAdminJwtUtil.generateRefreshToken(userDetails);

            // Save the refresh token to the database
            superAdminRefreshTokenService.createAndSaveRefreshToken(userDetails.getUsername(), refreshToken);

            // Build user info
            UserInfo userInfo = UserInfo.newBuilder()
                    .setUsername(userDetails.getUsername())
                    .setUserType("SUPER_ADMIN")
                    .addAllRoles(userDetails.getAuthorities().stream()
                            .map(auth -> auth.getAuthority())
                            .toList())
                    .build();

            AuthResponse response = AuthResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Authentication successful")
                    .setAccessToken(accessToken)
                    .setRefreshToken(refreshToken)
                    .setExpiresIn(24 * 60 * 60 * 1000L) // 24 hours
                    .setUserInfo(userInfo)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            logger.info("Super Admin authentication successful for username: {}", request.getUsername());

        } catch (BadCredentialsException e) {
            logger.warn("Super Admin authentication failed for username: {}", request.getUsername());
            AuthResponse response = AuthResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Invalid username or password")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Super Admin authentication error for username: {}", request.getUsername(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Authentication failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void authenticateSubscriber(SubscriberLoginRequest request, StreamObserver<AuthResponse> responseObserver) {
        try {
            logger.info("Subscriber authentication attempt for username: {}", request.getUsername());

            // Create SubscriberLoginDto for the existing service
            SubscriberLoginDto loginDto = new SubscriberLoginDto();
            loginDto.setMobileNumber(request.getUsername());
            loginDto.setPin(request.getPassword());

            // Use existing subscriber authentication service
            Map<String, Object> authResult = subscriberAuthService.loginWithPinSimple(loginDto);

            if (authResult.containsKey("token")) {
                String token = (String) authResult.get("token");
                Map<String, Object> subscriberInfo = (Map<String, Object>) authResult.get("subscriber");
                Map<String, Object> organizationInfo = (Map<String, Object>) authResult.get("organization");

                // Build user info
                UserInfo userInfo = UserInfo.newBuilder()
                        .setId((Long) subscriberInfo.get("id"))
                        .setUsername(request.getUsername())
                        .setUserType("SUBSCRIBER")
                        .setOrganizationId((Long) organizationInfo.get("id"))
                        .addRoles("SUBSCRIBER")
                        .build();

                AuthResponse response = AuthResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Authentication successful")
                        .setAccessToken(token)
                        .setExpiresIn(24 * 60 * 60 * 1000L) // 24 hours
                        .setUserInfo(userInfo)
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                logger.info("Subscriber authentication successful for username: {}", request.getUsername());
            } else {
                AuthResponse response = AuthResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Authentication failed")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Subscriber authentication failed for username: {}", request.getUsername());
            AuthResponse response = AuthResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Invalid username or password")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Subscriber authentication error for username: {}", request.getUsername(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Authentication failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void validateToken(TokenValidationRequest request, StreamObserver<TokenValidationResponse> responseObserver) {
        try {
            String token = request.getToken();
            String userType = request.getUserType();

            // Check if token is blacklisted (simplified check for now)
            // TODO: Implement proper token blacklisting with hash comparison
            // if (blacklistedTokenRepository.existsByTokenHash(hashToken(token))) {
            //     TokenValidationResponse response = TokenValidationResponse.newBuilder()
            //             .setValid(false)
            //             .setMessage("Token is blacklisted")
            //             .build();
            //     responseObserver.onNext(response);
            //     responseObserver.onCompleted();
            //     return;
            // }

            boolean isValid = false;
            UserInfo userInfo = null;

            switch (userType) {
                case "ENTITY_ADMIN":
                    isValid = jwtUtil.isTokenValid(token) && jwtUtil.isEntityAdminToken(token);
                    if (isValid) {
                        String username = jwtUtil.extractUsername(token);
                        userInfo = UserInfo.newBuilder()
                                .setUsername(username)
                                .setUserType("ENTITY_ADMIN")
                                .build();
                    }
                    break;
                case "SUPER_ADMIN":
                    try {
                        // Check if it's a super admin token and not expired
                        isValid = superAdminJwtUtil.isSuperAdminToken(token) &&
                                 superAdminJwtUtil.extractExpiration(token).after(new java.util.Date());
                    } catch (Exception e) {
                        isValid = false;
                    }
                    if (isValid) {
                        String username = superAdminJwtUtil.extractUsername(token);
                        userInfo = UserInfo.newBuilder()
                                .setUsername(username)
                                .setUserType("SUPER_ADMIN")
                                .build();
                    }
                    break;
                case "SUBSCRIBER":
                    isValid = jwtUtil.isTokenValid(token) && jwtUtil.isSubscriberToken(token);
                    if (isValid) {
                        String username = jwtUtil.extractUsername(token);
                        userInfo = UserInfo.newBuilder()
                                .setUsername(username)
                                .setUserType("SUBSCRIBER")
                                .build();
                    }
                    break;
            }

            TokenValidationResponse.Builder responseBuilder = TokenValidationResponse.newBuilder()
                    .setValid(isValid)
                    .setMessage(isValid ? "Token is valid" : "Token is invalid or expired");

            if (userInfo != null) {
                responseBuilder.setUserInfo(userInfo);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Token validation error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Token validation failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void refreshEntityAdminToken(RefreshTokenRequest request, StreamObserver<AuthResponse> responseObserver) {
        try {
            String refreshToken = request.getRefreshToken();

            // Validate refresh token
            var tokenOpt = refreshTokenService.findByToken(refreshToken);
            if (tokenOpt.isEmpty() || !refreshTokenService.verifyExpiration(tokenOpt.get())) {
                AuthResponse response = AuthResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Invalid or expired refresh token")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Generate new access token
            UserDetails userDetails = userDetailsService.loadUserByUsername(tokenOpt.get().getUsername());
            String newAccessToken = jwtUtil.generateToken(userDetails);

            AuthResponse response = AuthResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Token refreshed successfully")
                    .setAccessToken(newAccessToken)
                    .setRefreshToken(refreshToken)
                    .setExpiresIn(24 * 60 * 60 * 1000L)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Entity Admin token refresh error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Token refresh failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void refreshSuperAdminToken(RefreshTokenRequest request, StreamObserver<AuthResponse> responseObserver) {
        try {
            String refreshToken = request.getRefreshToken();

            // Validate refresh token
            var tokenOpt = superAdminRefreshTokenService.findByToken(refreshToken);
            if (tokenOpt.isEmpty() || !superAdminRefreshTokenService.verifyExpiration(tokenOpt.get())) {
                AuthResponse response = AuthResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Invalid or expired refresh token")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Generate new access token
            UserDetails userDetails = superAdminUserDetailsService.loadUserByUsername(tokenOpt.get().getUser().getUsername());
            String newAccessToken = superAdminJwtUtil.generateToken(userDetails);

            AuthResponse response = AuthResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Token refreshed successfully")
                    .setAccessToken(newAccessToken)
                    .setRefreshToken(refreshToken)
                    .setExpiresIn(24 * 60 * 60 * 1000L)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Super Admin token refresh error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Token refresh failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void refreshSubscriberToken(RefreshTokenRequest request, StreamObserver<AuthResponse> responseObserver) {
        // Subscribers currently don't have refresh tokens in the existing system
        // This is a placeholder for future implementation
        AuthResponse response = AuthResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Subscriber token refresh not implemented")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void logoutEntityAdmin(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        try {
            String accessToken = request.getAccessToken();
            String refreshToken = request.getRefreshToken();

            // TODO: Blacklist access token when blacklisting is properly implemented
            // if (!accessToken.isEmpty()) {
            //     BlacklistedToken blacklistedToken = new BlacklistedToken();
            //     blacklistedToken.setTokenHash(hashToken(accessToken));
            //     blacklistedToken.setUsername(extractUsernameFromToken(accessToken));
            //     blacklistedToken.setBlacklistedAt(Instant.now());
            //     blacklistedTokenRepository.save(blacklistedToken);
            // }

            // Delete refresh token
            if (!refreshToken.isEmpty()) {
                refreshTokenService.deleteToken(refreshToken);
            }

            LogoutResponse response = LogoutResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Logout successful")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Entity Admin logout error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Logout failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void logoutSuperAdmin(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        try {
            String accessToken = request.getAccessToken();
            String refreshToken = request.getRefreshToken();

            // TODO: Blacklist access token when blacklisting is properly implemented
            // if (!accessToken.isEmpty()) {
            //     BlacklistedToken blacklistedToken = new BlacklistedToken();
            //     blacklistedToken.setTokenHash(hashToken(accessToken));
            //     blacklistedToken.setUsername(extractUsernameFromToken(accessToken));
            //     blacklistedToken.setBlacklistedAt(Instant.now());
            //     blacklistedTokenRepository.save(blacklistedToken);
            // }

            // Delete refresh token
            if (!refreshToken.isEmpty()) {
                superAdminRefreshTokenService.deleteToken(refreshToken);
            }

            LogoutResponse response = LogoutResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Logout successful")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Super Admin logout error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Logout failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void logoutSubscriber(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        try {
            String accessToken = request.getAccessToken();

            // TODO: Blacklist access token when blacklisting is properly implemented
            // if (!accessToken.isEmpty()) {
            //     BlacklistedToken blacklistedToken = new BlacklistedToken();
            //     blacklistedToken.setTokenHash(hashToken(accessToken));
            //     blacklistedToken.setUsername(extractUsernameFromToken(accessToken));
            //     blacklistedToken.setBlacklistedAt(Instant.now());
            //     blacklistedTokenRepository.save(blacklistedToken);
            // }

            LogoutResponse response = LogoutResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Logout successful")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Subscriber logout error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Logout failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void blacklistToken(BlacklistTokenRequest request, StreamObserver<BlacklistTokenResponse> responseObserver) {
        try {
            String token = request.getToken();
            String userType = request.getUserType();

            // Create blacklisted token entry (simplified for now)
            // TODO: Implement proper token blacklisting
            // BlacklistedToken blacklistedToken = new BlacklistedToken();
            // blacklistedToken.setTokenHash(hashToken(token));
            // blacklistedToken.setUsername(extractUsernameFromToken(token));
            // blacklistedToken.setBlacklistedAt(Instant.now());

            // blacklistedTokenRepository.save(blacklistedToken);

            BlacklistTokenResponse response = BlacklistTokenResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Token blacklisting not fully implemented yet")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            logger.info("Token blacklisting requested for user type: {}", userType);

        } catch (Exception e) {
            logger.error("Token blacklisting error", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Token blacklisting failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void createEntityAdminForAuth(CreateEntityAdminForAuthRequest request, StreamObserver<CreateEntityAdminForAuthResponse> responseObserver) {
        try {
            logger.info("Creating Entity Admin for authentication: {} for organization: {}",
                       request.getUsername(), request.getOrganizationId());

            // Check if EntityAdmin already exists
            if (entityAdminRepository.findByUsername(request.getUsername()).isPresent()) {
                logger.warn("Entity Admin already exists: {}", request.getUsername());
                CreateEntityAdminForAuthResponse response = CreateEntityAdminForAuthResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Entity Admin with username '" + request.getUsername() + "' already exists")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Find or create organization entity in auth service
            Organization organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseGet(() -> {
                        // Create a minimal organization record for the relationship
                        Organization newOrg = new Organization();
                        newOrg.setId(request.getOrganizationId());
                        newOrg.setName("Organization " + request.getOrganizationId()); // Placeholder name
                        return organizationRepository.save(newOrg);
                    });

            // Get Entity Admin role
            Role entityAdminRole = roleRepository.findByName("ENTITY_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Entity Admin role not found"));

            // Create Entity Admin
            EntityAdmin entityAdmin = new EntityAdmin();
            entityAdmin.setUsername(request.getUsername());
            entityAdmin.setPassword(passwordEncoder.encode(request.getPassword()));
            entityAdmin.setOrganization(organization);
            entityAdmin.setRole(entityAdminRole);
            entityAdmin.setCreatedAt(LocalDateTime.now());

            EntityAdmin savedEntityAdmin = entityAdminRepository.save(entityAdmin);

            logger.info("Entity Admin created successfully in Auth Service: {} (ID: {})",
                       savedEntityAdmin.getUsername(), savedEntityAdmin.getId());

            CreateEntityAdminForAuthResponse response = CreateEntityAdminForAuthResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Entity Admin created successfully for authentication")
                    .setEntityAdminId(savedEntityAdmin.getId())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            logger.error("Error creating Entity Admin for authentication: {}", request.getUsername(), e);
            CreateEntityAdminForAuthResponse response = CreateEntityAdminForAuthResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to create Entity Admin: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void hashPassword(HashPasswordRequest request, StreamObserver<HashPasswordResponse> responseObserver) {
        try {
            logger.info("Hashing password via gRPC");

            String originalPassword = request.getPassword();
            if (originalPassword == null || originalPassword.trim().isEmpty()) {
                HashPasswordResponse response = HashPasswordResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Password cannot be empty")
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Hash the password using the same encoder used for authentication
            String hashedPassword = passwordEncoder.encode(originalPassword);

            HashPasswordResponse response = HashPasswordResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Password hashed successfully")
                    .setHashedPassword(hashedPassword)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("Password hashed successfully via gRPC");

        } catch (Exception e) {
            logger.error("Error hashing password via gRPC: {}", e.getMessage());

            HashPasswordResponse response = HashPasswordResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to hash password: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
