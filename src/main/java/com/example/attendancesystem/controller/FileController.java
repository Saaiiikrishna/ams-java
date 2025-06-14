package com.example.attendancesystem.controller;

import com.example.attendancesystem.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for serving stored files (profile photos, face images)
 */
@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    
    @Autowired
    private FileStorageService fileStorageService;
    
    /**
     * Serve profile photos
     */
    @GetMapping("/profile/{filename:.+}")
    public ResponseEntity<Resource> serveProfilePhoto(@PathVariable String filename,
                                                     Authentication authentication) {
        try {
            // Validate authentication
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            logger.info("Profile photo request - File: {}, User: {}", filename, authentication.getName());
            
            // Get file path
            Path filePath = fileStorageService.getFilePath(filename, true);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                // Determine content type
                String contentType = determineContentType(filename);
                
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
            } else {
                logger.warn("Profile photo not found or not readable: {}", filename);
                return ResponseEntity.notFound().build();
            }
            
        } catch (MalformedURLException e) {
            logger.error("Malformed URL for profile photo: {}", filename, e);
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            logger.error("Error serving profile photo: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Serve face recognition images (for audit purposes)
     */
    @GetMapping("/face/{filename:.+}")
    public ResponseEntity<Resource> serveFaceImage(@PathVariable String filename,
                                                  Authentication authentication) {
        try {
            // Validate authentication - only allow admin access
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            logger.info("Face image request - File: {}, User: {}", filename, authentication.getName());
            
            // Get file path
            Path filePath = fileStorageService.getFilePath(filename, false);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                // Determine content type
                String contentType = determineContentType(filename);
                
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
            } else {
                logger.warn("Face image not found or not readable: {}", filename);
                return ResponseEntity.notFound().build();
            }
            
        } catch (MalformedURLException e) {
            logger.error("Malformed URL for face image: {}", filename, e);
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            logger.error("Error serving face image: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get file storage statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStorageStats(Authentication authentication) {
        try {
            // Validate authentication - only allow admin access
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            logger.info("Storage stats request - User: {}", authentication.getName());
            
            FileStorageService.StorageStats stats = fileStorageService.getStorageStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("profilePhotos", Map.of(
                "count", stats.getProfilePhotosCount(),
                "sizeBytes", stats.getProfilePhotosSize(),
                "sizeMB", String.format("%.2f", stats.getProfilePhotosSize() / (1024.0 * 1024.0))
            ));
            response.put("faceImages", Map.of(
                "count", stats.getFaceImagesCount(),
                "sizeBytes", stats.getFaceImagesSize(),
                "sizeMB", String.format("%.2f", stats.getFaceImagesSize() / (1024.0 * 1024.0))
            ));
            response.put("total", Map.of(
                "files", stats.getTotalFiles(),
                "sizeBytes", stats.getTotalSize(),
                "sizeMB", String.format("%.2f", stats.getTotalSize() / (1024.0 * 1024.0))
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting storage stats", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving storage statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Check if file exists
     */
    @GetMapping("/exists/{type}/{filename:.+}")
    public ResponseEntity<Map<String, Object>> checkFileExists(@PathVariable String type,
                                                              @PathVariable String filename,
                                                              Authentication authentication) {
        try {
            // Validate authentication
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            boolean isProfilePhoto = "profile".equals(type);
            if (!isProfilePhoto && !"face".equals(type)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid file type. Use 'profile' or 'face'");
                return ResponseEntity.badRequest().body(response);
            }
            
            boolean exists = fileStorageService.fileExists(filename, isProfilePhoto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filename", filename);
            response.put("type", type);
            response.put("exists", exists);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking file existence: {}/{}", type, filename, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error checking file existence: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Delete a file (admin only)
     */
    @DeleteMapping("/{type}/{filename:.+}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String type,
                                                         @PathVariable String filename,
                                                         Authentication authentication) {
        try {
            // Validate authentication - only allow admin access
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            logger.info("File deletion request - Type: {}, File: {}, User: {}", 
                       type, filename, authentication.getName());
            
            boolean isProfilePhoto = "profile".equals(type);
            if (!isProfilePhoto && !"face".equals(type)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid file type. Use 'profile' or 'face'");
                return ResponseEntity.badRequest().body(response);
            }
            
            boolean deleted = fileStorageService.deleteFile(filename);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", deleted);
            response.put("filename", filename);
            response.put("type", type);
            response.put("message", deleted ? "File deleted successfully" : "File not found or could not be deleted");
            
            if (deleted) {
                logger.info("File deleted successfully - Type: {}, File: {}", type, filename);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error deleting file: {}/{}", type, filename, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Determine content type based on file extension
     */
    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}
