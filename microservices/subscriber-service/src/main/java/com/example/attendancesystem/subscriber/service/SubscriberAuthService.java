package com.example.attendancesystem.organization.service;

import com.example.attendancesystem.dto.SubscriberLoginDto;
import com.example.attendancesystem.organization.model.Organization;
import com.example.attendancesystem.organization.model.Subscriber;
import com.example.attendancesystem.organization.model.SubscriberAuth;
import com.example.attendancesystem.organization.repository.OrganizationRepository;
import com.example.attendancesystem.organization.repository.SubscriberAuthRepository;
import com.example.attendancesystem.organization.repository.SubscriberRepository;
import com.example.attendancesystem.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SubscriberAuthService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriberAuthService.class);
    private static final SecureRandom random = new SecureRandom();

    @Autowired
    private SubscriberAuthRepository subscriberAuthRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Create authentication record for a subscriber
     */
    public SubscriberAuth createSubscriberAuth(Long subscriberId, String pin) {
        Subscriber subscriber = subscriberRepository.findById(subscriberId)
                .orElseThrow(() -> new IllegalArgumentException("Subscriber not found"));

        // Check if auth already exists
        Optional<SubscriberAuth> existingAuth = subscriberAuthRepository.findBySubscriber(subscriber);
        if (existingAuth.isPresent()) {
            throw new IllegalArgumentException("Authentication already exists for this subscriber");
        }

        SubscriberAuth auth = new SubscriberAuth();
        auth.setSubscriber(subscriber);
        auth.setPin(passwordEncoder.encode(pin));
        auth.setIsActive(true);

        SubscriberAuth saved = subscriberAuthRepository.save(auth);
        logger.info("Created authentication for subscriber: {}", subscriber.getMobileNumber());

        return saved;
    }

    /**
     * Login with PIN (simplified - handles multiple organizations with same mobile number)
     */
    public Map<String, Object> loginWithPinSimple(SubscriberLoginDto loginDto) {
        try {
            // Find all subscribers with this mobile number across all organizations
            List<SubscriberAuth> authList = subscriberAuthRepository
                    .findBySubscriberMobileNumber(loginDto.getMobileNumber());

            if (authList.isEmpty()) {
                throw new IllegalArgumentException("Subscriber not found");
            }

            // Try to authenticate with each organization until one succeeds
            SubscriberAuth validAuth = null;
            for (SubscriberAuth auth : authList) {
                if (auth.getIsActive() && passwordEncoder.matches(loginDto.getPin(), auth.getPin())) {
                    validAuth = auth;
                    break;
                }
            }

            if (validAuth == null) {
                // Check if any account exists but with wrong PIN
                boolean hasActiveAccount = authList.stream().anyMatch(SubscriberAuth::getIsActive);
                if (hasActiveAccount) {
                    throw new IllegalArgumentException("Invalid PIN");
                } else {
                    throw new IllegalArgumentException("Account is inactive");
                }
            }

            // Update login information
            validAuth.setLastLoginTime(LocalDateTime.now());
            validAuth.setLastDeviceId(loginDto.getDeviceId());
            validAuth.setLastDeviceInfo(loginDto.getDeviceInfo());
            subscriberAuthRepository.save(validAuth);

            // Generate JWT token for subscriber
            String token = generateSubscriberToken(validAuth.getSubscriber());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("subscriber", createSubscriberInfo(validAuth.getSubscriber()));
            response.put("organization", createOrganizationInfo(validAuth.getSubscriber().getOrganization()));
            response.put("message", "Login successful");

            logger.info("Subscriber logged in with PIN: {} for organization: {}",
                       loginDto.getMobileNumber(), validAuth.getSubscriber().getOrganization().getEntityId());
            return response;

        } catch (Exception e) {
            logger.error("PIN login failed for {}: {}", loginDto.getMobileNumber(), e.getMessage());
            throw e;
        }
    }

    /**
     * Update subscriber PIN (handles multiple organizations with same mobile number)
     */
    @Transactional
    public void updatePin(String mobileNumber, String currentPin, String newPin) {
        try {
            // Find all subscriber auth records by mobile number
            List<SubscriberAuth> authList = subscriberAuthRepository
                    .findBySubscriberMobileNumber(mobileNumber);

            if (authList.isEmpty()) {
                throw new IllegalArgumentException("Subscriber not found");
            }

            // Find the auth record that matches the current PIN
            SubscriberAuth validAuth = null;
            for (SubscriberAuth auth : authList) {
                if (auth.getIsActive() && passwordEncoder.matches(currentPin, auth.getPin())) {
                    validAuth = auth;
                    break;
                }
            }

            if (validAuth == null) {
                throw new IllegalArgumentException("Current PIN is incorrect or account is inactive");
            }

            // Validate new PIN
            if (newPin == null || newPin.length() != 4 || !newPin.matches("\\d{4}")) {
                throw new IllegalArgumentException("New PIN must be exactly 4 digits");
            }

            // Update PIN
            validAuth.setPin(passwordEncoder.encode(newPin));
            subscriberAuthRepository.save(validAuth);

            logger.info("PIN updated successfully for subscriber: {} in organization: {}",
                       mobileNumber, validAuth.getSubscriber().getOrganization().getEntityId());

        } catch (Exception e) {
            logger.error("PIN update failed for {}: {}", mobileNumber, e.getMessage());
            throw e;
        }
    }

    /**
     * Deactivate subscriber authentication
     */
    @Transactional
    public void deactivateSubscriberAuth(Long subscriberId) {
        SubscriberAuth auth = subscriberAuthRepository.findBySubscriberId(subscriberId)
                .orElseThrow(() -> new IllegalArgumentException("Subscriber auth not found"));

        auth.setIsActive(false);
        subscriberAuthRepository.save(auth);

        logger.info("Deactivated authentication for subscriber ID: {}", subscriberId);
    }

    /**
     * Generate JWT token for subscriber
     */
    private String generateSubscriberToken(Subscriber subscriber) {
        // Create a custom user details for subscriber
        Map<String, Object> claims = new HashMap<>();
        claims.put("subscriberId", subscriber.getId());
        claims.put("mobileNumber", subscriber.getMobileNumber());
        claims.put("entityId", subscriber.getOrganization().getEntityId());
        claims.put("role", "SUBSCRIBER");
        claims.put("tokenType", "SUBSCRIBER_ACCESS");

        return jwtUtil.createToken(claims, subscriber.getMobileNumber(), 24 * 60 * 60 * 1000L); // 24 hours
    }

    /**
     * Create subscriber info for response
     */
    private Map<String, Object> createSubscriberInfo(Subscriber subscriber) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", subscriber.getId());
        info.put("firstName", subscriber.getFirstName());
        info.put("lastName", subscriber.getLastName());
        info.put("mobileNumber", subscriber.getMobileNumber());
        info.put("email", subscriber.getEmail());
        info.put("hasNfcCard", subscriber.getNfcCard() != null);
        return info;
    }

    /**
     * Create organization info for response
     */
    private Map<String, Object> createOrganizationInfo(Organization organization) {
        Map<String, Object> info = new HashMap<>();
        info.put("entityId", organization.getEntityId());
        info.put("name", organization.getName());
        info.put("address", organization.getAddress());
        return info;
    }
}
