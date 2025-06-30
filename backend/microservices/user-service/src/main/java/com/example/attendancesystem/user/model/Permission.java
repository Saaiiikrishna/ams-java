package com.example.attendancesystem.user.model;

/**
 * Permission Enumeration
 * Defines specific permissions that can be granted to users
 */
public enum Permission {
    
    // ========== SUPER ADMIN PERMISSIONS ==========
    
    // System Management
    SYSTEM_ADMIN("System Administration"),
    CREATE_SUPER_ADMIN("Create Super Admins"),
    MANAGE_SUPER_ADMINS("Manage Super Admins"),
    SYSTEM_SETTINGS("System Settings"),
    SYSTEM_MONITORING("System Monitoring"),
    
    // Entity Management
    CREATE_ENTITY("Create Entities"),
    MANAGE_ENTITIES("Manage Entities"),
    DELETE_ENTITIES("Delete Entities"),
    
    // Entity Admin Management
    CREATE_ENTITY_ADMIN("Create Entity Admins"),
    MANAGE_ENTITY_ADMINS("Manage Entity Admins"),
    DELETE_ENTITY_ADMIN("Delete Entity Admins"),
    ASSIGN_ENTITY_ADMIN("Assign Entity Admins"),
    
    // Global Access
    ACCESS_ALL_ENTITIES("Access All Entities"),
    GLOBAL_REPORTS("Global Reports"),
    GLOBAL_ANALYTICS("Global Analytics"),
    
    // NFC Simulation
    NFC_SIMULATION("NFC Card Simulation"),
    
    // ========== ENTITY ADMIN PERMISSIONS ==========
    
    // Organization Management
    MANAGE_ORGANIZATION("Manage Organization"),
    ORGANIZATION_SETTINGS("Organization Settings"),
    
    // Member Management
    CREATE_MEMBER("Create Members"),
    MANAGE_MEMBERS("Manage Members"),
    DELETE_MEMBER("Delete Members"),
    VIEW_MEMBERS("View Members"),
    
    // NFC Card Management
    REGISTER_NFC_CARD("Register NFC Cards"),
    MANAGE_NFC_CARDS("Manage NFC Cards"),
    ASSIGN_NFC_CARD("Assign NFC Cards"),
    UNASSIGN_NFC_CARD("Unassign NFC Cards"),
    DELETE_NFC_CARD("Delete NFC Cards"),
    
    // Session Management
    CREATE_SESSION("Create Sessions"),
    MANAGE_SESSIONS("Manage Sessions"),
    DELETE_SESSION("Delete Sessions"),
    SCHEDULE_SESSION("Schedule Sessions"),
    VIEW_SESSIONS("View Sessions"),
    
    // Attendance Management
    VIEW_ATTENDANCE("View Attendance"),
    MANAGE_ATTENDANCE("Manage Attendance"),
    ATTENDANCE_REPORTS("Attendance Reports"),
    
    // Reports and Analytics
    GENERATE_REPORTS("Generate Reports"),
    DOWNLOAD_REPORTS("Download Reports"),
    VIEW_ANALYTICS("View Analytics"),
    
    // Profile Management
    CHANGE_PASSWORD("Change Password"),
    UPDATE_PROFILE("Update Profile"),
    
    // ========== MEMBER/SUBSCRIBER PERMISSIONS ==========
    
    // Attendance Actions
    CHECK_IN("Check In"),
    CHECK_OUT("Check Out"),
    VIEW_OWN_ATTENDANCE("View Own Attendance"),
    
    // Profile Actions
    VIEW_OWN_PROFILE("View Own Profile"),
    UPDATE_OWN_PROFILE("Update Own Profile"),
    CHANGE_OWN_PASSWORD("Change Own Password"),
    
    // Session Access
    VIEW_AVAILABLE_SESSIONS("View Available Sessions"),
    JOIN_SESSION("Join Sessions"),
    
    // Mobile App Features
    MOBILE_APP_ACCESS("Mobile App Access"),
    NFC_ACCESS("NFC Access"),
    QR_CODE_ACCESS("QR Code Access"),
    WIFI_ACCESS("WiFi Access"),
    BLUETOOTH_ACCESS("Bluetooth Access");

    private final String displayName;

    Permission(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get default permissions for a user type
     */
    public static Permission[] getDefaultPermissions(UserType userType) {
        switch (userType) {
            case SUPER_ADMIN:
                return new Permission[] {
                    SYSTEM_ADMIN, CREATE_SUPER_ADMIN, MANAGE_SUPER_ADMINS, SYSTEM_SETTINGS, SYSTEM_MONITORING,
                    CREATE_ENTITY, MANAGE_ENTITIES, DELETE_ENTITIES,
                    CREATE_ENTITY_ADMIN, MANAGE_ENTITY_ADMINS, DELETE_ENTITY_ADMIN, ASSIGN_ENTITY_ADMIN,
                    ACCESS_ALL_ENTITIES, GLOBAL_REPORTS, GLOBAL_ANALYTICS, NFC_SIMULATION,
                    CHANGE_PASSWORD, UPDATE_PROFILE
                };
                
            case ENTITY_ADMIN:
                return new Permission[] {
                    MANAGE_ORGANIZATION, ORGANIZATION_SETTINGS,
                    CREATE_MEMBER, MANAGE_MEMBERS, DELETE_MEMBER, VIEW_MEMBERS,
                    REGISTER_NFC_CARD, MANAGE_NFC_CARDS, ASSIGN_NFC_CARD, UNASSIGN_NFC_CARD, DELETE_NFC_CARD,
                    CREATE_SESSION, MANAGE_SESSIONS, DELETE_SESSION, SCHEDULE_SESSION, VIEW_SESSIONS,
                    VIEW_ATTENDANCE, MANAGE_ATTENDANCE, ATTENDANCE_REPORTS,
                    GENERATE_REPORTS, DOWNLOAD_REPORTS, VIEW_ANALYTICS,
                    CHANGE_PASSWORD, UPDATE_PROFILE
                };
                
            case MEMBER:
                return new Permission[] {
                    CHECK_IN, CHECK_OUT, VIEW_OWN_ATTENDANCE,
                    VIEW_OWN_PROFILE, UPDATE_OWN_PROFILE, CHANGE_OWN_PASSWORD,
                    VIEW_AVAILABLE_SESSIONS, JOIN_SESSION,
                    MOBILE_APP_ACCESS, NFC_ACCESS, QR_CODE_ACCESS, WIFI_ACCESS, BLUETOOTH_ACCESS
                };
                
            default:
                return new Permission[0];
        }
    }

    /**
     * Check if this permission is administrative
     */
    public boolean isAdminPermission() {
        return this.name().contains("ADMIN") || 
               this.name().contains("SYSTEM") || 
               this.name().contains("CREATE") || 
               this.name().contains("DELETE") || 
               this.name().contains("MANAGE");
    }

    /**
     * Check if this permission requires organization context
     */
    public boolean requiresOrganization() {
        return !this.name().contains("SYSTEM") && 
               !this.name().contains("GLOBAL") && 
               !this.name().contains("SUPER_ADMIN");
    }
}
