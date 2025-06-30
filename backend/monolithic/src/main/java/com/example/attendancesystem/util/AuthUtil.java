package com.example.attendancesystem.util;

import com.example.attendancesystem.security.CustomUserDetails;
import com.example.attendancesystem.model.EntityAdmin;
import org.springframework.security.core.Authentication;

/**
 * Utility methods for extracting information from Authentication objects.
 */
public final class AuthUtil {
    private AuthUtil() {}

    /**
     * Extract the entity ID from the authenticated principal.
     *
     * @param authentication the Authentication object
     * @return the entity ID
     * @throws IllegalArgumentException if the entity ID cannot be determined
     */
    public static String getEntityId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            EntityAdmin admin = userDetails.getEntityAdmin();
            if (admin != null && admin.getOrganization() != null) {
                return admin.getOrganization().getEntityId();
            }
        }
        throw new IllegalArgumentException("Unable to determine entity ID from authentication");
    }
}
