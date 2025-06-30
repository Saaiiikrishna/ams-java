package com.example.attendancesystem.attendance.facerecognition;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Utility class for image processing operations
 */
public class ImageUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);
    private static boolean openCvLoaded = false;
    
    static {
        try {
            nu.pattern.OpenCV.loadLocally();
            openCvLoaded = true;
            logger.info("OpenCV loaded successfully");
        } catch (Exception e) {
            logger.warn("Failed to load OpenCV: {}", e.getMessage());
        }
    }
    
    /**
     * Convert Base64 string to byte array
     */
    public static byte[] base64ToBytes(String base64Image) {
        try {
            // Remove data URL prefix if present
            if (base64Image.startsWith("data:image/")) {
                int commaIndex = base64Image.indexOf(',');
                if (commaIndex > 0) {
                    base64Image = base64Image.substring(commaIndex + 1);
                }
            }
            return Base64.getDecoder().decode(base64Image);
        } catch (Exception e) {
            logger.error("Failed to decode Base64 image", e);
            return null;
        }
    }
    
    /**
     * Convert byte array to BufferedImage
     */
    public static BufferedImage bytesToBufferedImage(byte[] imageBytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException e) {
            logger.error("Failed to convert bytes to BufferedImage", e);
            return null;
        }
    }
    
    /**
     * Convert BufferedImage to byte array
     */
    public static byte[] bufferedImageToBytes(BufferedImage image, String format) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, format, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            logger.error("Failed to convert BufferedImage to bytes", e);
            return null;
        }
    }
    
    /**
     * Convert BufferedImage to OpenCV Mat
     */
    public static Mat bufferedImageToMat(BufferedImage image) {
        if (!openCvLoaded) {
            logger.warn("OpenCV not loaded, cannot convert BufferedImage to Mat");
            return null;
        }
        
        try {
            byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
            mat.put(0, 0, pixels);
            return mat;
        } catch (Exception e) {
            logger.error("Failed to convert BufferedImage to Mat", e);
            return null;
        }
    }
    
    /**
     * Convert OpenCV Mat to BufferedImage
     */
    public static BufferedImage matToBufferedImage(Mat mat) {
        if (!openCvLoaded) {
            logger.warn("OpenCV not loaded, cannot convert Mat to BufferedImage");
            return null;
        }
        
        try {
            int type = BufferedImage.TYPE_BYTE_GRAY;
            if (mat.channels() > 1) {
                type = BufferedImage.TYPE_3BYTE_BGR;
            }
            
            int bufferSize = mat.channels() * mat.cols() * mat.rows();
            byte[] buffer = new byte[bufferSize];
            mat.get(0, 0, buffer); // get all pixels
            
            BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
            final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
            
            return image;
        } catch (Exception e) {
            logger.error("Failed to convert Mat to BufferedImage", e);
            return null;
        }
    }
    
    /**
     * Resize image maintaining aspect ratio
     */
    public static BufferedImage resizeImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
        try {
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            
            // Calculate new dimensions maintaining aspect ratio
            double aspectRatio = (double) originalWidth / originalHeight;
            int newWidth = maxWidth;
            int newHeight = (int) (maxWidth / aspectRatio);
            
            if (newHeight > maxHeight) {
                newHeight = maxHeight;
                newWidth = (int) (maxHeight * aspectRatio);
            }
            
            if (openCvLoaded) {
                return resizeWithOpenCV(originalImage, newWidth, newHeight);
            } else {
                return resizeWithJava(originalImage, newWidth, newHeight);
            }
        } catch (Exception e) {
            logger.error("Failed to resize image", e);
            return originalImage;
        }
    }
    
    /**
     * Resize image using OpenCV
     */
    private static BufferedImage resizeWithOpenCV(BufferedImage image, int width, int height) {
        Mat mat = bufferedImageToMat(image);
        if (mat == null) return image;
        
        Mat resized = new Mat();
        Imgproc.resize(mat, resized, new Size(width, height));
        
        BufferedImage result = matToBufferedImage(resized);
        return result != null ? result : image;
    }
    
    /**
     * Resize image using Java AWT
     */
    private static BufferedImage resizeWithJava(BufferedImage originalImage, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, originalImage.getType());
        java.awt.Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                           java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, width, height, null);
        g2d.dispose();
        return resizedImage;
    }
    
    /**
     * Convert image to grayscale
     */
    public static BufferedImage toGrayscale(BufferedImage colorImage) {
        if (openCvLoaded) {
            return toGrayscaleWithOpenCV(colorImage);
        } else {
            return toGrayscaleWithJava(colorImage);
        }
    }
    
    /**
     * Convert to grayscale using OpenCV
     */
    private static BufferedImage toGrayscaleWithOpenCV(BufferedImage colorImage) {
        Mat colorMat = bufferedImageToMat(colorImage);
        if (colorMat == null) return colorImage;
        
        Mat grayMat = new Mat();
        Imgproc.cvtColor(colorMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        
        BufferedImage result = matToBufferedImage(grayMat);
        return result != null ? result : colorImage;
    }
    
    /**
     * Convert to grayscale using Java AWT
     */
    private static BufferedImage toGrayscaleWithJava(BufferedImage colorImage) {
        BufferedImage grayImage = new BufferedImage(
            colorImage.getWidth(), colorImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        java.awt.Graphics2D g2d = grayImage.createGraphics();
        g2d.drawImage(colorImage, 0, 0, null);
        g2d.dispose();
        return grayImage;
    }
    
    /**
     * Extract raw pixel data from BufferedImage
     */
    public static byte[] extractPixelData(BufferedImage image) {
        try {
            return ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        } catch (Exception e) {
            logger.error("Failed to extract pixel data", e);
            return null;
        }
    }
    
    /**
     * Validate image format and quality
     */
    public static boolean isValidImage(byte[] imageBytes) {
        try {
            BufferedImage image = bytesToBufferedImage(imageBytes);
            return image != null && image.getWidth() > 0 && image.getHeight() > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if OpenCV is available
     */
    public static boolean isOpenCvLoaded() {
        return openCvLoaded;
    }
}
