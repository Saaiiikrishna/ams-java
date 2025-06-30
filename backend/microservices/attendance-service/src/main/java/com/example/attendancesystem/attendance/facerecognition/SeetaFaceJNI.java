package com.example.attendancesystem.attendance.facerecognition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JNI wrapper for SeetaFace2 native library
 * Provides Java interface to SeetaFace2 face detection and recognition capabilities
 */
public class SeetaFaceJNI {
    
    private static final Logger logger = LoggerFactory.getLogger(SeetaFaceJNI.class);
    private static boolean libraryLoaded = false;
    
    static {
        try {
            loadNativeLibrary();
        } catch (Exception e) {
            logger.error("Failed to load SeetaFace2 native library", e);
        }
    }
    
    /**
     * Load the native SeetaFace6 library
     */
    private static void loadNativeLibrary() {
        try {
            // Try to load from system library path first
            System.loadLibrary("seetaface_jni");
            libraryLoaded = true;
            logger.info("SeetaFace6 JNI library loaded successfully from system path");
        } catch (UnsatisfiedLinkError e1) {
            try {
                // Try to load from resources
                String osName = System.getProperty("os.name").toLowerCase();
                String arch = System.getProperty("os.arch").toLowerCase();
                String libraryName = getLibraryName(osName, arch);

                // Load from classpath resources
                System.loadLibrary(libraryName);
                libraryLoaded = true;
                logger.info("SeetaFace6 JNI library loaded successfully: {}", libraryName);
            } catch (UnsatisfiedLinkError e2) {
                logger.warn("SeetaFace6 native library not found. Face recognition will use fallback implementation.");
                logger.debug("Library loading errors - System: {}, Resources: {}", e1.getMessage(), e2.getMessage());
            }
        }
    }
    
    /**
     * Get platform-specific library name
     */
    private static String getLibraryName(String osName, String arch) {
        if (osName.contains("windows")) {
            return arch.contains("64") ? "seetaface_jni" : "seetaface_jni";
        } else if (osName.contains("linux")) {
            return arch.contains("64") ? "seetaface_jni" : "seetaface_jni";
        } else if (osName.contains("mac")) {
            return "seetaface_jni";
        }
        return "seetaface_jni";
    }
    
    /**
     * Check if native library is loaded
     */
    public static boolean isLibraryLoaded() {
        return libraryLoaded;
    }
    
    // Native method declarations
    
    /**
     * Initialize SeetaFace6 engine with model files
     * @param detectorModelPath Path to face detector model
     * @param landmarkModelPath Path to facial landmark model
     * @param recognizerModelPath Path to face recognizer model
     * @param antiSpoofingModelPath Path to anti-spoofing model (optional)
     * @return Engine handle or -1 if failed
     */
    public static native long initializeEngine(String detectorModelPath, 
                                             String landmarkModelPath,
                                             String recognizerModelPath, 
                                             String antiSpoofingModelPath);
    
    /**
     * Release SeetaFace6 engine resources
     * @param engineHandle Engine handle from initialization
     */
    public static native void releaseEngine(long engineHandle);
    
    /**
     * Detect faces in image
     * @param engineHandle Engine handle
     * @param imageData Image data as byte array
     * @param width Image width
     * @param height Image height
     * @param channels Image channels (1 for grayscale, 3 for RGB)
     * @return Array of face rectangles [x1, y1, w1, h1, x2, y2, w2, h2, ...]
     */
    public static native float[] detectFaces(long engineHandle, byte[] imageData, 
                                           int width, int height, int channels);
    
    /**
     * Extract face encoding/features
     * @param engineHandle Engine handle
     * @param imageData Image data as byte array
     * @param width Image width
     * @param height Image height
     * @param channels Image channels
     * @param faceX Face rectangle X coordinate
     * @param faceY Face rectangle Y coordinate
     * @param faceWidth Face rectangle width
     * @param faceHeight Face rectangle height
     * @return Face encoding as float array (512 dimensions)
     */
    public static native float[] extractFaceEncoding(long engineHandle, byte[] imageData,
                                                   int width, int height, int channels,
                                                   int faceX, int faceY, int faceWidth, int faceHeight);
    
    /**
     * Compare two face encodings
     * @param encoding1 First face encoding
     * @param encoding2 Second face encoding
     * @return Similarity score (0.0 to 1.0, higher is more similar)
     */
    public static native float compareFaceEncodings(float[] encoding1, float[] encoding2);
    
    /**
     * Perform anti-spoofing detection
     * @param engineHandle Engine handle
     * @param imageData Image data as byte array
     * @param width Image width
     * @param height Image height
     * @param channels Image channels
     * @param faceX Face rectangle X coordinate
     * @param faceY Face rectangle Y coordinate
     * @param faceWidth Face rectangle width
     * @param faceHeight Face rectangle height
     * @return Liveness score (0.0 to 1.0, higher means more likely to be real)
     */
    public static native float detectLiveness(long engineHandle, byte[] imageData,
                                            int width, int height, int channels,
                                            int faceX, int faceY, int faceWidth, int faceHeight);
    
    /**
     * Assess image quality for face recognition
     * @param imageData Image data as byte array
     * @param width Image width
     * @param height Image height
     * @param channels Image channels
     * @return Quality score (0.0 to 1.0, higher is better quality)
     */
    public static native float assessImageQuality(byte[] imageData, int width, int height, int channels);
    
    /**
     * Get last error message from native library
     * @return Error message string
     */
    public static native String getLastError();
}
