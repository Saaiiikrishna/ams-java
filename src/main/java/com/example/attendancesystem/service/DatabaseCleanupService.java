package com.example.attendancesystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
public class DatabaseCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseCleanupService.class);

    @Autowired
    private DataSource dataSource;

    @Transactional
    public void deleteAllDataExceptSuperAdmins() {
        logger.info("🗑️ Starting database cleanup - deleting all data except superadmins table");
        
        try (Connection connection = dataSource.getConnection()) {
            // Get all table names
            List<String> tableNames = getAllTableNames(connection);
            logger.info("📋 Found {} tables in database", tableNames.size());
            
            // Remove super_admins table from the list
            tableNames.removeIf(tableName ->
                tableName.equalsIgnoreCase("super_admins") ||
                tableName.equalsIgnoreCase("superadmins")
            );
            
            logger.info("🎯 Tables to clean: {}", tableNames);
            
            try (Statement statement = connection.createStatement()) {
                // PostgreSQL approach: Delete in correct order to handle foreign keys
                logger.info("🔓 Starting PostgreSQL-compatible cleanup");

                int totalDeletedRows = 0;

                // Define deletion order to handle foreign key constraints
                // Delete child tables first, then parent tables
                String[] deletionOrder = {
                    "attendance_records", "session_subscribers", "sessions", "orders", "order_items",
                    "nfc_cards", "subscribers", "entity_admins", "organizations"
                };

                // First, delete from tables in the defined order
                for (String tableName : deletionOrder) {
                    if (tableNames.contains(tableName)) {
                        try {
                            logger.info("🗑️ Cleaning table: {}", tableName);

                            // Get row count before deletion
                            ResultSet countResult = statement.executeQuery("SELECT COUNT(*) FROM " + tableName);
                            int rowCount = 0;
                            if (countResult.next()) {
                                rowCount = countResult.getInt(1);
                            }
                            countResult.close();

                            if (rowCount > 0) {
                                // Delete all data from table
                                int deletedRows = statement.executeUpdate("DELETE FROM " + tableName);
                                totalDeletedRows += deletedRows;
                                logger.info("✅ Deleted {} rows from table: {}", deletedRows, tableName);
                            } else {
                                logger.info("⚪ Table {} was already empty", tableName);
                            }

                            // Remove from remaining tables list
                            tableNames.remove(tableName);

                        } catch (Exception e) {
                            logger.error("❌ Error cleaning table {}: {}", tableName, e.getMessage());
                            // Continue with other tables even if one fails
                        }
                    }
                }

                // Then delete from any remaining tables
                for (String tableName : tableNames) {
                    try {
                        logger.info("🗑️ Cleaning remaining table: {}", tableName);

                        // Get row count before deletion
                        ResultSet countResult = statement.executeQuery("SELECT COUNT(*) FROM " + tableName);
                        int rowCount = 0;
                        if (countResult.next()) {
                            rowCount = countResult.getInt(1);
                        }
                        countResult.close();

                        if (rowCount > 0) {
                            // Delete all data from table
                            int deletedRows = statement.executeUpdate("DELETE FROM " + tableName);
                            totalDeletedRows += deletedRows;
                            logger.info("✅ Deleted {} rows from remaining table: {}", deletedRows, tableName);
                        } else {
                            logger.info("⚪ Remaining table {} was already empty", tableName);
                        }

                    } catch (Exception e) {
                        logger.error("❌ Error cleaning remaining table {}: {}", tableName, e.getMessage());
                        // Continue with other tables even if one fails
                    }
                }

                logger.info("🎉 Database cleanup completed successfully!");
                logger.info("📊 Total rows deleted: {}", totalDeletedRows);
                logger.info("🛡️ super_admins table preserved");

            }
            
        } catch (Exception e) {
            logger.error("💥 Fatal error during database cleanup: {}", e.getMessage(), e);
            throw new RuntimeException("Database cleanup failed: " + e.getMessage(), e);
        }
    }
    
    private List<String> getAllTableNames(Connection connection) throws Exception {
        List<String> tableNames = new ArrayList<>();
        
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
        
        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            // Skip system tables and schema-related tables
            if (!isSystemTable(tableName)) {
                tableNames.add(tableName);
            }
        }
        tables.close();
        
        logger.info("📋 Discovered tables: {}", tableNames);
        return tableNames;
    }
    
    private boolean isSystemTable(String tableName) {
        String lowerTableName = tableName.toLowerCase();
        return lowerTableName.startsWith("information_schema") ||
               lowerTableName.startsWith("performance_schema") ||
               lowerTableName.startsWith("mysql") ||
               lowerTableName.startsWith("sys") ||
               lowerTableName.startsWith("pg_") ||
               lowerTableName.equals("dual");
    }
    
    public int getSuperAdminCount() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM super_admins");
            if (result.next()) {
                int count = result.getInt(1);
                logger.info("👑 Current SuperAdmin count: {}", count);
                return count;
            }
            result.close();
            
        } catch (Exception e) {
            logger.error("❌ Error getting SuperAdmin count: {}", e.getMessage());
        }
        return 0;
    }
}
