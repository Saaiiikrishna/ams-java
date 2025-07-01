package com.example.attendancesystem.attendance.service;

import com.example.attendancesystem.attendance.model.AttendanceSession;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.attendancesystem.attendance.client.OrganizationServiceGrpcClient;
import com.example.attendancesystem.attendance.dto.OrganizationDto;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class QrCodeService {

    private static final Logger logger = LoggerFactory.getLogger(QrCodeService.class);
    private static final String QR_SECRET = "AMS_QR_SECRET_2024"; // In production, use environment variable

    @Autowired
    private OrganizationServiceGrpcClient organizationServiceGrpcClient;

    /**
     * Generate a secure QR code for a session
     */
    public String generateQrCodeForSession(AttendanceSession session) {
        try {
            // Create a unique identifier for this session
            // Use a custom timestamp format without colons to avoid splitting issues
            String timestamp = session.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss.SSSSSS"));
            // Get organization data via gRPC
            OrganizationDto organization = organizationServiceGrpcClient.getOrganizationById(session.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("Organization not found"));

            String sessionData = String.format("%d:%s:%s:%s",
                session.getId(),
                organization.getEntityId(),
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

            // Get organization data via gRPC
            OrganizationDto organization = organizationServiceGrpcClient.getOrganizationById(session.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("Organization not found"));

            logger.info("Parsed QR - SessionId: {}, EntityId: {}, Timestamp: {}, UUID: {}, Hash: {}",
                       sessionId, entityId, timestamp, uuid, providedHash);
            logger.info("Session details - ID: {}, EntityId: {}", session.getId(), organization.getEntityId());

            // Verify session ID and entity ID match
            if (!sessionId.equals(session.getId()) ||
                !entityId.equals(organization.getEntityId())) {
                logger.warn("QR code session/entity mismatch - QR SessionId: {}, Actual: {}, QR EntityId: {}, Actual: {}",
                           sessionId, session.getId(), entityId, organization.getEntityId());
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
     * Generate QR code image URL for table QR codes
     * Returns a compact base64 data URL for smaller QR codes
     */
    public String generateQrCodeImageUrl(String qrCode) {
        try {
            logger.debug("Generating QR code image for: {}", qrCode);

            // Generate QR code image using ZXing library
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrCode, BarcodeFormat.QR_CODE, 200, 200, hints);

            // Convert BitMatrix to BufferedImage
            BufferedImage qrImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 200; x++) {
                for (int y = 0; y < 200; y++) {
                    qrImage.setRGB(x, y, bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }

            // Convert to base64 data URL with PNG format for better quality
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            logger.debug("Successfully generated QR code image, size: {} bytes", imageBytes.length);
            return "data:image/png;base64," + base64Image;

        } catch (Exception e) {
            logger.error("Failed to generate QR code image for: {}", qrCode, e);
            // Fallback to SVG placeholder
            return generateQrCodeSvgDataUrl(qrCode);
        }
    }

    /**
     * Generate SVG QR code as fallback
     */
    private String generateQrCodeSvgDataUrl(String qrCode) {
        try {
            String svg = generateQrCodeSvg(qrCode);
            String base64Svg = Base64.getEncoder().encodeToString(svg.getBytes());
            return "data:image/svg+xml;base64," + base64Svg;
        } catch (Exception e) {
            logger.error("Failed to generate SVG QR code for: {}", qrCode, e);
            // Return a simple placeholder
            return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(
                "<svg width='200' height='200' xmlns='http://www.w3.org/2000/svg'><rect width='200' height='200' fill='#f0f0f0'/><text x='100' y='100' text-anchor='middle' fill='#666'>QR Code</text></svg>".getBytes()
            );
        }
    }

    /**
     * Generate QR code image using ZXing
     */
    private BufferedImage generateQrCodeImage(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

        // Use TYPE_INT_RGB for JPEG compatibility
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        // Enable antialiasing for better quality
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // White background
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);

        // Black QR code
        graphics.setColor(Color.BLACK);

        // Calculate scaling to fit the QR code properly
        int matrixWidth = bitMatrix.getWidth();
        int matrixHeight = bitMatrix.getHeight();
        int pixelSize = Math.min(width / matrixWidth, height / matrixHeight);
        int offsetX = (width - (matrixWidth * pixelSize)) / 2;
        int offsetY = (height - (matrixHeight * pixelSize)) / 2;

        for (int i = 0; i < matrixWidth; i++) {
            for (int j = 0; j < matrixHeight; j++) {
                if (bitMatrix.get(i, j)) {
                    graphics.fillRect(offsetX + i * pixelSize, offsetY + j * pixelSize, pixelSize, pixelSize);
                }
            }
        }

        graphics.dispose();
        return image;
    }

    /**
     * Generate SVG representation of QR code (fallback placeholder)
     */
    private String generateQrCodeSvg(String qrCode) {
        // This is a simplified placeholder SVG
        return String.format(
            "<svg width='200' height='200' xmlns='http://www.w3.org/2000/svg'>" +
            "<rect width='200' height='200' fill='white'/>" +
            "<rect x='20' y='20' width='160' height='160' fill='black'/>" +
            "<rect x='40' y='40' width='120' height='120' fill='white'/>" +
            "<text x='100' y='105' text-anchor='middle' font-family='monospace' font-size='8'>%s</text>" +
            "</svg>",
            qrCode.length() > 20 ? qrCode.substring(0, 20) + "..." : qrCode
        );
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
