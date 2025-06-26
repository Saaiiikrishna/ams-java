package com.example.attendancesystem.subscriber.controller;

import com.example.attendancesystem.service.QrCodeService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/qr")
@CrossOrigin(origins = "*")
public class QrCodeController {
    
    private static final Logger logger = LoggerFactory.getLogger(QrCodeController.class);
    
    @Autowired
    private QrCodeService qrCodeService;
    
    /**
     * Generate and serve QR code image
     */
    @GetMapping("/image/{hash}")
    public ResponseEntity<byte[]> getQrCodeImage(@PathVariable String hash, 
                                               @RequestParam String data,
                                               @RequestParam(defaultValue = "300") int size) {
        try {
            // Decode the QR code data
            String qrCodeData = new String(Base64.getUrlDecoder().decode(data));
            logger.debug("Generating QR code image for data: {}", qrCodeData);
            
            // Generate QR code image
            BufferedImage qrImage = generateQrCodeImage(qrCodeData, size, size);
            
            // Convert to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(imageBytes.length);
            headers.setCacheControl("public, max-age=3600"); // Cache for 1 hour
            
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("Failed to generate QR code image: {}", e.getMessage());
            return generateFallbackImage(size);
        }
    }
    
    /**
     * Fallback QR code image
     */
    @GetMapping("/image/fallback")
    public ResponseEntity<byte[]> getFallbackQrCodeImage(@RequestParam String data,
                                                        @RequestParam(defaultValue = "300") int size) {
        try {
            String qrCodeData = new String(Base64.getUrlDecoder().decode(data));
            BufferedImage qrImage = generateQrCodeImage(qrCodeData, size, size);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(imageBytes.length);
            
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("Failed to generate fallback QR code image: {}", e.getMessage());
            return generateFallbackImage(size);
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
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.createGraphics();
        
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.BLACK);
        
        for (int i = 0; i < bitMatrix.getWidth(); i++) {
            for (int j = 0; j < bitMatrix.getHeight(); j++) {
                if (bitMatrix.get(i, j)) {
                    graphics.fillRect(i, j, 1, 1);
                }
            }
        }
        
        return image;
    }
    
    /**
     * Generate a simple fallback image when QR generation fails
     */
    private ResponseEntity<byte[]> generateFallbackImage(int size) {
        try {
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            
            // White background
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, size, size);
            
            // Black border
            graphics.setColor(Color.BLACK);
            graphics.drawRect(0, 0, size - 1, size - 1);
            
            // Error text
            graphics.setFont(new Font("Arial", Font.BOLD, size / 20));
            FontMetrics fm = graphics.getFontMetrics();
            String errorText = "QR Error";
            int textWidth = fm.stringWidth(errorText);
            int textHeight = fm.getHeight();
            graphics.drawString(errorText, (size - textWidth) / 2, (size + textHeight) / 2);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(imageBytes.length);
            
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("Failed to generate fallback image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
