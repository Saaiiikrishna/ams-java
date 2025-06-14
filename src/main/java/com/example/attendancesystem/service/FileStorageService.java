package com.example.attendancesystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for handling file storage operations
 * Manages profile photos and face recognition related files
 */
@Service
public class FileStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    
    @Value("${face.recognition.storage.profile-photos:uploads/profiles/}")
    private String profilePhotosPath;
    
    @Value("${face.recognition.storage.path:uploads/faces/}")
    private String faceStoragePath;
    
    private final Path profilePhotosLocation;
    private final Path faceStorageLocation;
    
    public FileStorageService(@Value("${face.recognition.storage.profile-photos:uploads/profiles/}") String profilePhotosPath,
                             @Value("${face.recognition.storage.path:uploads/faces/}") String faceStoragePath) {
        this.profilePhotosPath = profilePhotosPath;
        this.faceStoragePath = faceStoragePath;
        
        this.profilePhotosLocation = Paths.get(profilePhotosPath).toAbsolutePath().normalize();
        this.faceStorageLocation = Paths.get(faceStoragePath).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.profilePhotosLocation);
            Files.createDirectories(this.faceStorageLocation);
            logger.info("File storage directories initialized - Profiles: {}, Faces: {}", 
                       this.profilePhotosLocation, this.faceStorageLocation);
        } catch (Exception e) {
            logger.error("Could not create file storage directories", e);
            throw new RuntimeException("Could not create file storage directories", e);
        }
    }
    
    /**
     * Store profile photo for a subscriber
     */
    public String storeProfilePhoto(byte[] imageData, Long subscriberId, String subscriberName) throws IOException {
        try {
            // Generate unique filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String cleanName = StringUtils.cleanPath(subscriberName.replaceAll("[^a-zA-Z0-9_-]", "_"));
            String filename = String.format("profile_%d_%s_%s_%s.jpg", 
                subscriberId, cleanName, timestamp, UUID.randomUUID().toString().substring(0, 8));
            
            // Resolve the file location
            Path targetLocation = this.profilePhotosLocation.resolve(filename);
            
            // Ensure the file path is within the upload directory
            if (!targetLocation.getParent().equals(this.profilePhotosLocation)) {
                throw new IOException("Cannot store file outside designated directory");
            }
            
            // Write file
            Files.write(targetLocation, imageData);
            
            logger.info("Profile photo stored successfully - Subscriber: {}, File: {}", subscriberId, filename);
            return filename;
            
        } catch (IOException e) {
            logger.error("Failed to store profile photo for subscriber {}: {}", subscriberId, e.getMessage());
            throw new IOException("Could not store profile photo", e);
        }
    }
    
    /**
     * Store face recognition image for audit purposes
     */
    public String storeFaceRecognitionImage(byte[] imageData, Long subscriberId, String purpose) throws IOException {
        try {
            // Generate unique filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("face_%s_%d_%s_%s.jpg", 
                purpose, subscriberId, timestamp, UUID.randomUUID().toString().substring(0, 8));
            
            // Resolve the file location
            Path targetLocation = this.faceStorageLocation.resolve(filename);
            
            // Ensure the file path is within the upload directory
            if (!targetLocation.getParent().equals(this.faceStorageLocation)) {
                throw new IOException("Cannot store file outside designated directory");
            }
            
            // Write file
            Files.write(targetLocation, imageData);
            
            logger.info("Face recognition image stored - Subscriber: {}, Purpose: {}, File: {}", 
                       subscriberId, purpose, filename);
            return filename;
            
        } catch (IOException e) {
            logger.error("Failed to store face recognition image for subscriber {}: {}", subscriberId, e.getMessage());
            throw new IOException("Could not store face recognition image", e);
        }
    }
    
    /**
     * Load file as byte array
     */
    public byte[] loadFile(String filename, boolean isProfilePhoto) throws IOException {
        try {
            Path filePath = isProfilePhoto ? 
                this.profilePhotosLocation.resolve(filename) : 
                this.faceStorageLocation.resolve(filename);
            
            // Ensure the file path is within the designated directory
            Path expectedParent = isProfilePhoto ? this.profilePhotosLocation : this.faceStorageLocation;
            if (!filePath.getParent().equals(expectedParent)) {
                throw new IOException("Cannot access file outside designated directory");
            }
            
            if (!Files.exists(filePath)) {
                throw new IOException("File not found: " + filename);
            }
            
            return Files.readAllBytes(filePath);
            
        } catch (IOException e) {
            logger.error("Failed to load file {}: {}", filename, e.getMessage());
            throw new IOException("Could not load file", e);
        }
    }
    
    /**
     * Delete a file
     */
    public boolean deleteFile(String filename) {
        try {
            // Try profile photos directory first
            Path profilePath = this.profilePhotosLocation.resolve(filename);
            if (Files.exists(profilePath) && profilePath.getParent().equals(this.profilePhotosLocation)) {
                Files.delete(profilePath);
                logger.info("Profile photo deleted: {}", filename);
                return true;
            }
            
            // Try face storage directory
            Path facePath = this.faceStorageLocation.resolve(filename);
            if (Files.exists(facePath) && facePath.getParent().equals(this.faceStorageLocation)) {
                Files.delete(facePath);
                logger.info("Face recognition image deleted: {}", filename);
                return true;
            }
            
            logger.warn("File not found for deletion: {}", filename);
            return false;
            
        } catch (IOException e) {
            logger.error("Failed to delete file {}: {}", filename, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if file exists
     */
    public boolean fileExists(String filename, boolean isProfilePhoto) {
        try {
            Path filePath = isProfilePhoto ? 
                this.profilePhotosLocation.resolve(filename) : 
                this.faceStorageLocation.resolve(filename);
            
            Path expectedParent = isProfilePhoto ? this.profilePhotosLocation : this.faceStorageLocation;
            return Files.exists(filePath) && filePath.getParent().equals(expectedParent);
            
        } catch (Exception e) {
            logger.error("Error checking file existence {}: {}", filename, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get file path for serving
     */
    public Path getFilePath(String filename, boolean isProfilePhoto) throws IOException {
        Path filePath = isProfilePhoto ? 
            this.profilePhotosLocation.resolve(filename) : 
            this.faceStorageLocation.resolve(filename);
        
        Path expectedParent = isProfilePhoto ? this.profilePhotosLocation : this.faceStorageLocation;
        if (!filePath.getParent().equals(expectedParent)) {
            throw new IOException("Cannot access file outside designated directory");
        }
        
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filename);
        }
        
        return filePath;
    }
    
    /**
     * Get storage statistics
     */
    public StorageStats getStorageStats() {
        try {
            long profilePhotosCount = Files.list(this.profilePhotosLocation).count();
            long faceImagesCount = Files.list(this.faceStorageLocation).count();
            
            long profilePhotosSize = Files.walk(this.profilePhotosLocation)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
            
            long faceImagesSize = Files.walk(this.faceStorageLocation)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
            
            return new StorageStats(profilePhotosCount, faceImagesCount, profilePhotosSize, faceImagesSize);
            
        } catch (IOException e) {
            logger.error("Failed to get storage statistics: {}", e.getMessage());
            return new StorageStats(0, 0, 0, 0);
        }
    }
    
    /**
     * Storage statistics data class
     */
    public static class StorageStats {
        private final long profilePhotosCount;
        private final long faceImagesCount;
        private final long profilePhotosSize;
        private final long faceImagesSize;
        
        public StorageStats(long profilePhotosCount, long faceImagesCount, 
                           long profilePhotosSize, long faceImagesSize) {
            this.profilePhotosCount = profilePhotosCount;
            this.faceImagesCount = faceImagesCount;
            this.profilePhotosSize = profilePhotosSize;
            this.faceImagesSize = faceImagesSize;
        }
        
        // Getters
        public long getProfilePhotosCount() { return profilePhotosCount; }
        public long getFaceImagesCount() { return faceImagesCount; }
        public long getProfilePhotosSize() { return profilePhotosSize; }
        public long getFaceImagesSize() { return faceImagesSize; }
        public long getTotalFiles() { return profilePhotosCount + faceImagesCount; }
        public long getTotalSize() { return profilePhotosSize + faceImagesSize; }
    }
}
