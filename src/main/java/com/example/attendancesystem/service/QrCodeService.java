package com.example.attendancesystem.service;

import com.example.attendancesystem.model.AttendanceSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

@Service
public class QrCodeService {

    private static final Logger logger = LoggerFactory.getLogger(QrCodeService.class);
    private static final String QR_SECRET = "AMS_QR_SECRET_2024"; // In production, use environment variable

    /**
     * Generate a secure QR code for a session
     */
    public String generateQrCodeForSession(AttendanceSession session) {
        try {
            // Create a unique identifier for this session
            // Use a custom timestamp format without colons to avoid splitting issues
            String timestamp = session.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss.SSSSSS"));
            String sessionData = String.format("%d:%s:%s:%s",
                session.getId(),
                session.getOrganization().getEntityId(),
                timestamp,
                UUID.randomUUID().toString().substring(0, 8)
            );

            // Create a hash for security
            String hash = createSecureHash(sessionData);
            
            // Combine session data with hash
            String qrCode = Base64.getEncoder().encodeToString(
                (sessionData + ":" + hash).getBytes()
            );

            logger.info("Generated QR code for session {}", session.getId());
            return qrCode;

        } catch (Exception e) {
            logger.error("Failed to generate QR code for session {}: {}", session.getId(), e.getMessage());
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Validate a QR code for a session
     */
    public boolean validateQrCode(String qrCode, AttendanceSession session) {
        try {
            logger.info("Validating QR code: '{}'", qrCode);

            // Decode the QR code
            String decodedData = new String(Base64.getDecoder().decode(qrCode));
            logger.info("Decoded QR data: '{}'", decodedData);

            String[] parts = decodedData.split(":");
            logger.info("QR parts count: {}, Parts: {}", parts.length, String.join(" | ", parts));

            if (parts.length != 5) {
                logger.warn("Invalid QR code format - expected 5 parts, got {}", parts.length);
                return false;
            }

            Long sessionId = Long.parseLong(parts[0]);
            String entityId = parts[1];
            String timestamp = parts[2];
            String uuid = parts[3];
            String providedHash = parts[4];

            logger.info("Parsed QR - SessionId: {}, EntityId: {}, Timestamp: {}, UUID: {}, Hash: {}",
                       sessionId, entityId, timestamp, uuid, providedHash);
            logger.info("Session details - ID: {}, EntityId: {}", session.getId(), session.getOrganization().getEntityId());

            // Verify session ID and entity ID match
            if (!sessionId.equals(session.getId()) ||
                !entityId.equals(session.getOrganization().getEntityId())) {
                logger.warn("QR code session/entity mismatch - QR SessionId: {}, Actual: {}, QR EntityId: {}, Actual: {}",
                           sessionId, session.getId(), entityId, session.getOrganization().getEntityId());
                return false;
            }

            // Verify hash
            String expectedHash = createSecureHash(parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3]);
            logger.info("Hash validation - Provided: {}, Expected: {}", providedHash, expectedHash);
            if (!expectedHash.equals(providedHash)) {
                logger.warn("QR code hash validation failed - Provided: {}, Expected: {}", providedHash, expectedHash);
                return false;
            }

            // Check if session is still active (QR code is valid as long as session is active)
            if (session.getEndTime() != null) {
                logger.warn("QR code is invalid - session has ended");
                return false;
            }

            logger.info("QR code validated successfully for session {}", session.getId());
            return true;

        } catch (Exception e) {
            logger.error("QR code validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate QR code data for display (without sensitive information)
     */
    public QrCodeDisplayData generateQrCodeDisplayData(AttendanceSession session) {
        String qrCode = session.getQrCode();
        if (qrCode == null) {
            qrCode = generateQrCodeForSession(session);
        }

        // QR code is valid until session ends
        LocalDateTime expiryTime = session.getEndTime(); // null if session is still active

        return new QrCodeDisplayData(
            qrCode,
            session.getId(),
            session.getName(),
            expiryTime,
            generateQrCodeUrl(qrCode)
        );
    }

    /**
     * Generate URL for QR code (for mobile app deep linking)
     */
    private String generateQrCodeUrl(String qrCode) {
        // This would be your mobile app's deep link URL
        return "ams://checkin?qr=" + qrCode;
    }

    /**
     * Create a secure hash for QR code validation
     */
    private String createSecureHash(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String saltedData = data + QR_SECRET;
        byte[] hash = digest.digest(saltedData.getBytes());
        return Base64.getEncoder().encodeToString(hash).substring(0, 16); // Use first 16 chars
    }

    /**
     * Refresh QR code for a session (generate new one)
     */
    public String refreshQrCodeForSession(AttendanceSession session) {
        logger.info("Refreshing QR code for session {}", session.getId());
        return generateQrCodeForSession(session);
    }

    /**
     * Check if QR code needs refresh based on time
     */
    public boolean shouldRefreshQrCode(AttendanceSession session) {
        if (session.getQrCodeExpiry() == null) {
            return true;
        }
        
        // Refresh if less than 10 minutes remaining
        return LocalDateTime.now().plusMinutes(10).isAfter(session.getQrCodeExpiry());
    }

    /**
     * Data class for QR code display information
     */
    public static class QrCodeDisplayData {
        private final String qrCode;
        private final Long sessionId;
        private final String sessionName;
        private final LocalDateTime expiryTime;
        private final String deepLinkUrl;

        public QrCodeDisplayData(String qrCode, Long sessionId, String sessionName, 
                               LocalDateTime expiryTime, String deepLinkUrl) {
            this.qrCode = qrCode;
            this.sessionId = sessionId;
            this.sessionName = sessionName;
            this.expiryTime = expiryTime;
            this.deepLinkUrl = deepLinkUrl;
        }

        // Getters
        public String getQrCode() { return qrCode; }
        public Long getSessionId() { return sessionId; }
        public String getSessionName() { return sessionName; }
        public LocalDateTime getExpiryTime() { return expiryTime; }
        public String getDeepLinkUrl() { return deepLinkUrl; }
    }
}
