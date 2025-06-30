package com.example.attendancesystem.attendance.util;

import org.springframework.security.core.Authentication;

/**
 * Utility methods for extracting information from Authentication objects.
 * Simplified version for attendance-service.
 */
public final class AuthUtil {
    private AuthUtil() {}

    /**
     * Extract the entity ID from the authenticated principal.
     * For now, returns a default entity ID. This should be updated
     * to integrate with auth-service via gRPC calls.
     *
     * @param authentication the Authentication object
     * @return the entity ID
     * @throws IllegalArgumentException if the entity ID cannot be determined
     */
    public static String getEntityId(Authentication authentication) {
        // TODO: Implement proper authentication integration with auth-service via gRPC
        // For now, return a default entity ID to allow compilation
        if (authentication != null && authentication.getName() != null) {
            return "MSD00001"; // Default entity ID for development
        }
        throw new IllegalArgumentException("Unable to determine entity ID from authentication");
    }
}
