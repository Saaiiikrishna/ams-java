package com.example.attendancesystem.service;

import com.example.attendancesystem.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class EntityIdService {

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
        final int maxAttempts = 1000; // Prevent infinite loop

        do {
            entityId = generateEntityId();
            attempts++;
            
            if (attempts > maxAttempts) {
                throw new RuntimeException("Unable to generate unique Entity ID after " + maxAttempts + " attempts");
            }
        } while (organizationRepository.existsByEntityId(entityId));

        return entityId;
    }

    /**
     * Generates an Entity ID with MSD prefix and 5 sequential/random digits
     */
    private String generateEntityId() {
        // Get the count of existing organizations to create sequential IDs
        long count = organizationRepository.count();
        
        // Start from 10001 to ensure 5-digit numbers
        long nextNumber = 10001 + count;
        
        // If we exceed 99999, use random numbers
        if (nextNumber > 99999) {
            nextNumber = 10000 + random.nextInt(90000); // Random 5-digit number
        }
        
        return PREFIX + String.format("%05d", nextNumber);
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
