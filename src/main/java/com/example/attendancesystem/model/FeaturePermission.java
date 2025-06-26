package com.example.attendancesystem.subscriber.model;

public enum FeaturePermission {
    // Menu and Ordering System
    MENU_MANAGEMENT("Menu Management", "Manage categories and menu items"),
    ORDER_MANAGEMENT("Order Management", "View and manage customer orders"),
    TABLE_MANAGEMENT("Table Management", "Manage restaurant tables and QR codes"),
    
    // Attendance System (existing)
    ATTENDANCE_TRACKING("Attendance Tracking", "Track member attendance"),
    MEMBER_MANAGEMENT("Member Management", "Manage organization members"),
    
    // Reports and Analytics
    SALES_REPORTS("Sales Reports", "View sales and revenue reports"),
    ATTENDANCE_REPORTS("Attendance Reports", "View attendance reports"),
    
    // Advanced Features
    BULK_OPERATIONS("Bulk Operations", "Perform bulk operations"),
    DATA_EXPORT("Data Export", "Export data to various formats"),
    ADVANCED_SETTINGS("Advanced Settings", "Access advanced configuration settings");
    
    private final String displayName;
    private final String description;
    
    FeaturePermission(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    // Helper methods to group permissions
    public static FeaturePermission[] getMenuOrderingPermissions() {
        return new FeaturePermission[]{
            MENU_MANAGEMENT,
            ORDER_MANAGEMENT,
            TABLE_MANAGEMENT
        };
    }
    
    public static FeaturePermission[] getAttendancePermissions() {
        return new FeaturePermission[]{
            ATTENDANCE_TRACKING,
            MEMBER_MANAGEMENT
        };
    }
    
    public static FeaturePermission[] getReportPermissions() {
        return new FeaturePermission[]{
            SALES_REPORTS,
            ATTENDANCE_REPORTS
        };
    }
    
    public static FeaturePermission[] getAdvancedPermissions() {
        return new FeaturePermission[]{
            BULK_OPERATIONS,
            DATA_EXPORT,
            ADVANCED_SETTINGS
        };
    }
}
