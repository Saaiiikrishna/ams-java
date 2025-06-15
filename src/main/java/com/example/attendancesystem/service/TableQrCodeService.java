package com.example.attendancesystem.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating QR codes specifically for restaurant tables
 * Based on the proven QrCodeService implementation for attendance
 */
@Service
public class TableQrCodeService {

    private static final Logger logger = LoggerFactory.getLogger(TableQrCodeService.class);

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Generate QR code URL for table QR codes
     * Instead of generating large base64 images, return a simple URL that can be used to generate QR codes
     */
    public String generateTableQrCodeImageUrl(String qrCodeData) {
        try {
            logger.debug("Generating table QR code URL for data: {}", qrCodeData);

            // Instead of generating a large base64 image, return a simple QR code generation URL
            // This avoids the database column size issue
            String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" +
                               java.net.URLEncoder.encode(qrCodeData, "UTF-8");

            logger.debug("Successfully generated table QR code URL: {}", qrCodeUrl);
            return qrCodeUrl;

        } catch (Exception e) {
            logger.error("Failed to generate table QR code URL for: {}", qrCodeData, e);
            // Fallback to a simple placeholder URL
            return "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=TABLE_QR_ERROR";
        }
    }

    /**
     * Generate QR code image using ZXing - same implementation as attendance QR codes
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
     * Generate SVG QR code as fallback - same as attendance QR codes
     */
    private String generateTableQrCodeSvgDataUrl(String qrCodeData) {
        try {
            String svg = generateTableQrCodeSvg(qrCodeData);
            String base64Svg = Base64.getEncoder().encodeToString(svg.getBytes());
            return "data:image/svg+xml;base64," + base64Svg;
        } catch (Exception e) {
            logger.error("Failed to generate SVG table QR code for: {}", qrCodeData, e);
            // Return a simple placeholder
            return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(
                "<svg width='200' height='200' xmlns='http://www.w3.org/2000/svg'><rect width='200' height='200' fill='#f0f0f0'/><text x='100' y='100' text-anchor='middle' fill='#666'>Table QR</text></svg>".getBytes()
            );
        }
    }

    /**
     * Generate SVG representation of QR code (fallback placeholder)
     */
    private String generateTableQrCodeSvg(String qrCodeData) {
        // This is a simplified placeholder SVG - same pattern as attendance QR codes
        return String.format(
            "<svg width='200' height='200' xmlns='http://www.w3.org/2000/svg'>" +
            "<rect width='200' height='200' fill='white'/>" +
            "<rect x='20' y='20' width='160' height='160' fill='black'/>" +
            "<rect x='40' y='40' width='120' height='120' fill='white'/>" +
            "<text x='100' y='95' text-anchor='middle' font-family='monospace' font-size='10'>TABLE</text>" +
            "<text x='100' y='110' text-anchor='middle' font-family='monospace' font-size='8'>%s</text>" +
            "</svg>",
            qrCodeData.length() > 15 ? qrCodeData.substring(0, 15) + "..." : qrCodeData
        );
    }

    /**
     * Generate the menu URL that the QR code will point to
     * MOBILE ACCESS FIX: Use port 3000 for better mobile compatibility
     */
    public String generateMenuUrl(String entityId, Integer tableNumber, String qrCode) {
        // MOBILE ACCESS FIX: Use port 8080 with firewall configured
        return String.format("http://restaurant.local:8080/menu.html?entityId=%s&table=%d&qr=%s",
                            entityId, tableNumber, qrCode);
    }

    /**
     * Generate the menu URL using table ID
     * MOBILE ACCESS FIX: Use port 3000 for better mobile compatibility
     */
    public String generateTableMenuUrl(Long tableId) {
        // MOBILE ACCESS FIX: Use port 8080 with firewall configured
        // Note: This method needs table info to generate proper URL, will be handled in calling code
        return String.format("http://restaurant.local:8080/menu.html?tableId=%d", tableId);
    }

    /**
     * Generate the menu URL using table number (for QR code generation)
     * This should use table ID, not table number for API consistency
     */
    public String generateTableMenuUrlByNumber(Integer tableNumber) {
        // This method is deprecated - use generateTableMenuUrlById instead
        logger.warn("Using deprecated method generateTableMenuUrlByNumber. Use generateTableMenuUrlById instead.");
        return String.format("http://restaurant.local:57977/menu/table/%d", tableNumber);
    }

    /**
     * Generate the menu URL using table ID (for QR code generation)
     * MOBILE ACCESS FIX: Use port 3000 for better mobile compatibility
     */
    public String generateTableMenuUrlById(Long tableId) {
        // MOBILE ACCESS FIX: Use port 8080 with firewall configured
        // Note: This method needs table info to generate proper URL, will be handled in calling code
        return String.format("http://restaurant.local:8080/menu.html?tableId=%d", tableId);
    }

    /**
     * Generate a larger QR code image for printing (300x300)
     */
    public String generatePrintableQrCodeImageUrl(String qrCodeData) {
        try {
            logger.debug("Generating printable table QR code image for data: {}", qrCodeData);
            
            // Generate larger QR code image for printing
            BufferedImage qrImage = generateQrCodeImage(qrCodeData, 300, 300);

            // Convert to base64 data URL with PNG format
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            logger.debug("Successfully generated printable table QR code image, size: {} bytes", imageBytes.length);
            return "data:image/png;base64," + base64Image;
            
        } catch (Exception e) {
            logger.error("Failed to generate printable table QR code image for: {}", qrCodeData, e);
            // Fallback to regular size
            return generateTableQrCodeImageUrl(qrCodeData);
        }
    }
}
