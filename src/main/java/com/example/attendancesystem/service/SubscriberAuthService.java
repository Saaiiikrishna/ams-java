package com.example.attendancesystem.service;

import com.example.attendancesystem.dto.SubscriberLoginDto;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.Subscriber;
import com.example.attendancesystem.model.SubscriberAuth;
import com.example.attendancesystem.repository.OrganizationRepository;
import com.example.attendancesystem.repository.SubscriberAuthRepository;
import com.example.attendancesystem.repository.SubscriberRepository;
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
     * Login with PIN (simplified - no organization ID required)
     */
    public Map<String, Object> loginWithPinSimple(SubscriberLoginDto loginDto) {
        try {
            // Find subscriber by mobile number only
            SubscriberAuth auth = subscriberAuthRepository
                    .findBySubscriberMobileNumber(loginDto.getMobileNumber())
                    .orElseThrow(() -> new IllegalArgumentException("Subscriber not found"));

            if (!auth.getIsActive()) {
                throw new IllegalArgumentException("Account is inactive");
            }

            if (!passwordEncoder.matches(loginDto.getPin(), auth.getPin())) {
                throw new IllegalArgumentException("Invalid PIN");
            }

            auth.setLastLoginTime(LocalDateTime.now());
            auth.setLastDeviceId(loginDto.getDeviceId());
            auth.setLastDeviceInfo(loginDto.getDeviceInfo());
            subscriberAuthRepository.save(auth);

            // Generate JWT token for subscriber
            String token = generateSubscriberToken(auth.getSubscriber());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("subscriber", createSubscriberInfo(auth.getSubscriber()));
            response.put("organization", createOrganizationInfo(auth.getSubscriber().getOrganization()));
            response.put("message", "Login successful");

            logger.info("Subscriber logged in with PIN (simple): {}", loginDto.getMobileNumber());
            return response;

        } catch (Exception e) {
            logger.error("Simple PIN login failed for {}: {}", loginDto.getMobileNumber(), e.getMessage());
            throw e;
        }
    }

    /**
     * Update subscriber PIN
     */
    @Transactional
    public void updatePin(String mobileNumber, String currentPin, String newPin) {
        try {
            // Find subscriber auth by mobile number
            SubscriberAuth auth = subscriberAuthRepository
                    .findBySubscriberMobileNumber(mobileNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Subscriber not found"));

            if (!auth.getIsActive()) {
                throw new IllegalArgumentException("Account is inactive");
            }

            // Verify current PIN
            if (!passwordEncoder.matches(currentPin, auth.getPin())) {
                throw new IllegalArgumentException("Current PIN is incorrect");
            }

            // Validate new PIN
            if (newPin == null || newPin.length() != 4 || !newPin.matches("\\d{4}")) {
                throw new IllegalArgumentException("New PIN must be exactly 4 digits");
            }

            // Update PIN
            auth.setPin(passwordEncoder.encode(newPin));
            subscriberAuthRepository.save(auth);

            logger.info("PIN updated successfully for subscriber: {}", mobileNumber);

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
