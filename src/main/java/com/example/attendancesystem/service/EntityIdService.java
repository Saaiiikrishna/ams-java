package com.example.attendancesystem.subscriber.service;

import com.example.attendancesystem.repository.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class EntityIdService {

    private static final Logger logger = LoggerFactory.getLogger(EntityIdService.class);

    @Autowired
    private OrganizationRepository organizationRepository;

    private static final String PREFIX = "MSD";
    private static final int NUMERIC_LENGTH = 5;
    private static final Random random = new Random();

    /**
     * Generates a unique 8-character Entity ID with format: MSD + 5 digits
     * Example: MSD12345, MSD67890
     */
    public String generateUniqueEntityId() {
        String entityId;
        int attempts = 0;
        final int maxAttempts = 100; // Reduced attempts since we're using random generation

        logger.info("Starting entity ID generation. Current organization count: {}", organizationRepository.count());

        // Log existing entity IDs for debugging
        organizationRepository.findAll().forEach(org ->
            logger.info("Existing entity ID: {}", org.getEntityId()));

        do {
            entityId = generateEntityId();
            attempts++;

            logger.info("Generated entity ID attempt {}: {}", attempts, entityId);
            boolean exists = organizationRepository.existsByEntityId(entityId);
            logger.info("Entity ID {} exists: {}", entityId, exists);

            if (attempts > maxAttempts) {
                logger.error("Failed to generate unique Entity ID after {} attempts. Current organization count: {}",
                           maxAttempts, organizationRepository.count());
                throw new RuntimeException("Unable to generate unique Entity ID after " + maxAttempts + " attempts");
            }
        } while (organizationRepository.existsByEntityId(entityId));

        logger.info("Successfully generated unique entity ID: {} after {} attempts", entityId, attempts);
        return entityId;
    }

    /**
     * Generates an Entity ID with MSD prefix and 5 sequential/random digits
     */
    private String generateEntityId() {
        // Use a completely different approach: random generation with collision detection
        Random random = new Random();

        // Generate a random 5-digit number between 10000 and 99999
        int randomNumber = 10000 + random.nextInt(90000);

        return PREFIX + String.format("%05d", randomNumber);
    }

    /**
     * Validates if an Entity ID follows the correct format
     */
    public boolean isValidEntityIdFormat(String entityId) {
        if (entityId == null || entityId.length() != 8) {
            return false;
        }
        
        if (!entityId.startsWith(PREFIX)) {
            return false;
        }
        
        String numericPart = entityId.substring(3);
        try {
            Integer.parseInt(numericPart);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Extracts the numeric part from an Entity ID
     */
    public int getNumericPart(String entityId) {
        if (!isValidEntityIdFormat(entityId)) {
            throw new IllegalArgumentException("Invalid Entity ID format: " + entityId);
        }
        
        return Integer.parseInt(entityId.substring(3));
    }
}
