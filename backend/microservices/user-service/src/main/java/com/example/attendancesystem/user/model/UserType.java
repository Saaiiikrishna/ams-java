package com.example.attendancesystem.user.model;

/**
 * User Type Enumeration
 * Defines the different types of users in the system
 */
public enum UserType {
    /**
     * Super Administrator - Has access to all system features
     * Can create entities, manage entity admins, system-wide settings
     */
    SUPER_ADMIN("Super Admin"),

    /**
     * Entity Administrator - Manages a specific organization/entity
     * Can manage members, sessions, NFC cards within their organization
     */
    ENTITY_ADMIN("Entity Admin"),

    /**
     * Member/Subscriber - End users who participate in sessions
     * Can check-in/out, view their attendance, use mobile app
     */
    MEMBER("Member");

    private final String displayName;

    UserType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get user type from string (case-insensitive)
     */
    public static UserType fromString(String type) {
        if (type == null) {
            return null;
        }
        
        for (UserType userType : UserType.values()) {
            if (userType.name().equalsIgnoreCase(type) || 
                userType.displayName.equalsIgnoreCase(type)) {
                return userType;
            }
        }
        
        // Handle legacy names
        if ("SUBSCRIBER".equalsIgnoreCase(type)) {
            return MEMBER;
        }
        
        throw new IllegalArgumentException("Unknown user type: " + type);
    }

    /**
     * Check if this user type has admin privileges
     */
    public boolean isAdmin() {
        return this == SUPER_ADMIN || this == ENTITY_ADMIN;
    }

    /**
     * Check if this user type can manage other users
     */
    public boolean canManageUsers() {
        return this == SUPER_ADMIN || this == ENTITY_ADMIN;
    }

    /**
     * Check if this user type requires organization association
     */
    public boolean requiresOrganization() {
        return this == ENTITY_ADMIN || this == MEMBER;
    }

    /**
     * Get the role name for Spring Security
     */
    public String getRoleName() {
        return "ROLE_" + this.name();
    }
}
