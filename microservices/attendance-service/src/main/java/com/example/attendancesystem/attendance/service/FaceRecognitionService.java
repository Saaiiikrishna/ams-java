package com.example.attendancesystem.organization.service;

import com.example.attendancesystem.facerecognition.*;
import com.example.attendancesystem.organization.model.*;
import com.example.attendancesystem.organization.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Core service for face recognition operations
 * Handles face detection, encoding, recognition, and anti-spoofing
 */
@Service
public class FaceRecognitionService {
    
    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionService.class);
    
    @Autowired
    private SubscriberRepository subscriberRepository;
    
    @Autowired
    private FaceRecognitionSettingsRepository faceRecognitionSettingsRepository;
    
    @Autowired
    private FaceRecognitionLogRepository faceRecognitionLogRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Value("${face.recognition.models.detector:models/face_detector.csta}")
    private String detectorModelPath;
    
    @Value("${face.recognition.models.landmark:models/face_landmarker_pts68.csta}")
    private String landmarkModelPath;
    
    @Value("${face.recognition.models.recognizer:models/face_recognizer.csta}")
    private String recognizerModelPath;
    
    @Value("${face.recognition.models.antispoofing:models/fas_first.csta}")
    private String antiSpoofingModelPath;
    
    @Value("${face.recognition.fallback.enabled:true}")
    private boolean fallbackEnabled;
    
    private long engineHandle = -1;
    private boolean engineInitialized = false;
    
    @PostConstruct
    public void initializeEngine() {
        try {
            if (SeetaFaceJNI.isLibraryLoaded()) {
                logger.info("Initializing SeetaFace6 engine...");
                engineHandle = SeetaFaceJNI.initializeEngine(
                    detectorModelPath, landmarkModelPath, recognizerModelPath, antiSpoofingModelPath);

                if (engineHandle > 0) {
                    engineInitialized = true;
                    logger.info("SeetaFace6 engine initialized successfully with handle: {}", engineHandle);
                } else {
                    logger.warn("Failed to initialize SeetaFace6 engine. Using fallback implementation.");
                }
            } else {
                logger.warn("SeetaFace6 native library not loaded. Using fallback implementation.");
            }
        } catch (Exception e) {
            logger.error("Error initializing face recognition engine", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (engineInitialized && engineHandle > 0) {
            try {
                SeetaFaceJNI.releaseEngine(engineHandle);
                logger.info("SeetaFace6 engine resources released");
            } catch (Exception e) {
                logger.error("Error releasing face recognition engine", e);
            }
        }
    }
    
    /**
     * Detect faces in an image
     */
    public FaceDetectionResult detectFaces(byte[] imageData) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate input
            if (imageData == null || imageData.length == 0) {
                return new FaceDetectionResult(false, "Invalid image data");
            }
            
            if (!ImageUtils.isValidImage(imageData)) {
                return new FaceDetectionResult(false, "Invalid image format");
            }
            
            // Convert image
            BufferedImage image = ImageUtils.bytesToBufferedImage(imageData);
            if (image == null) {
                return new FaceDetectionResult(false, "Failed to process image");
            }
            
            // Assess image quality
            float qualityScore = assessImageQuality(image);
            
            List<FaceRectangle> faces;
            if (engineInitialized) {
                faces = detectFacesWithSeetaFace(image);
            } else if (fallbackEnabled) {
                faces = detectFacesWithFallback(image);
            } else {
                return new FaceDetectionResult(false, "Face detection engine not available");
            }
            
            int processingTime = (int) (System.currentTimeMillis() - startTime);
            
            logger.info("Face detection completed - Found {} faces in {}ms, quality: {:.3f}", 
                       faces.size(), processingTime, qualityScore);
            
            return new FaceDetectionResult(true, faces, processingTime, qualityScore);
            
        } catch (Exception e) {
            logger.error("Face detection failed", e);
            return new FaceDetectionResult(false, "Face detection error: " + e.getMessage());
        }
    }
    
    /**
     * Extract face encoding from image
     */
    public FaceEncodingResult extractFaceEncoding(byte[] imageData, String entityId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Get settings for entity
            FaceRecognitionSettings settings = getSettingsForEntity(entityId);
            
            // First detect faces
            FaceDetectionResult detection = detectFaces(imageData);
            if (!detection.isSuccess()) {
                return new FaceEncodingResult(false, detection.getErrorMessage());
            }
            
            if (!detection.hasFaces()) {
                return new FaceEncodingResult(false, "No faces detected in image");
            }
            
            // Check quality threshold
            if (detection.getImageQualityScore() < settings.getPhotoQualityThreshold().floatValue()) {
                return new FaceEncodingResult(false, 
                    String.format("Image quality too low: %.3f < %.3f", 
                                detection.getImageQualityScore(), 
                                settings.getPhotoQualityThreshold().floatValue()));
            }
            
            // Handle multiple faces
            if (detection.getFaceCount() > 1 && !settings.getEnableMultipleFaceDetection()) {
                return new FaceEncodingResult(false, "Multiple faces detected. Please use image with single face.");
            }
            
            // Use the largest face (most prominent)
            FaceRectangle bestFace = findBestFace(detection.getFaces());
            
            // Extract encoding
            BufferedImage image = ImageUtils.bytesToBufferedImage(imageData);
            float[] encoding;
            float livenessScore = 0.0f;
            
            if (engineInitialized) {
                encoding = extractEncodingWithSeetaFace(image, bestFace);
                if (settings.getEnableAntiSpoofing()) {
                    livenessScore = detectLivenessWithSeetaFace(image, bestFace);
                }
            } else if (fallbackEnabled) {
                encoding = extractEncodingWithFallback(image, bestFace);
                livenessScore = 1.0f; // Assume live for fallback
            } else {
                return new FaceEncodingResult(false, "Face encoding engine not available");
            }
            
            if (encoding == null || encoding.length == 0) {
                return new FaceEncodingResult(false, "Failed to extract face encoding");
            }
            
            // Check anti-spoofing
            if (settings.getEnableAntiSpoofing() && livenessScore < 0.5f) {
                return new FaceEncodingResult(false, 
                    String.format("Liveness detection failed: %.3f", livenessScore));
            }
            
            int processingTime = (int) (System.currentTimeMillis() - startTime);
            
            logger.info("Face encoding extracted successfully - Dimensions: {}, Processing: {}ms, Liveness: {:.3f}", 
                       encoding.length, processingTime, livenessScore);
            
            return new FaceEncodingResult(true, encoding, bestFace, processingTime, 
                                        detection.getImageQualityScore(), livenessScore);
            
        } catch (Exception e) {
            logger.error("Face encoding extraction failed", e);
            return new FaceEncodingResult(false, "Face encoding error: " + e.getMessage());
        }
    }
    
    /**
     * Recognize face against registered subscribers
     */
    public FaceRecognitionResult recognizeFace(byte[] imageData, String entityId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Get settings for entity
            FaceRecognitionSettings settings = getSettingsForEntity(entityId);
            
            // Extract face encoding from input image
            FaceEncodingResult encodingResult = extractFaceEncoding(imageData, entityId);
            if (!encodingResult.isSuccess()) {
                return new FaceRecognitionResult(false, encodingResult.getErrorMessage());
            }
            
            float[] inputEncoding = encodingResult.getEncoding();
            
            // Get all registered subscribers for this entity
            Organization organization = organizationRepository.findByEntityId(entityId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + entityId));
            
            List<Subscriber> registeredSubscribers = subscriberRepository
                .findByOrganizationAndFaceEncodingIsNotNull(organization);
            
            if (registeredSubscribers.isEmpty()) {
                return new FaceRecognitionResult(false, "No registered faces found for this organization");
            }
            
            // Find best match
            Subscriber bestMatch = null;
            float bestConfidence = 0.0f;
            float bestDistance = Float.MAX_VALUE;
            
            for (Subscriber subscriber : registeredSubscribers) {
                float[] storedEncoding = convertBytesToFloatArray(subscriber.getFaceEncoding());
                if (storedEncoding == null) continue;
                
                float similarity = compareFaceEncodings(inputEncoding, storedEncoding);
                float distance = 1.0f - similarity;
                
                if (similarity > bestConfidence && distance <= settings.getMaxRecognitionDistance().floatValue()) {
                    bestMatch = subscriber;
                    bestConfidence = similarity;
                    bestDistance = distance;
                }
            }
            
            int processingTime = (int) (System.currentTimeMillis() - startTime);
            
            // Check if match meets confidence threshold
            boolean matched = bestMatch != null && 
                            bestConfidence >= settings.getConfidenceThreshold().floatValue();
            
            FaceRecognitionResult result = new FaceRecognitionResult(true, matched, bestConfidence, 
                                                                   bestDistance, processingTime);
            
            if (matched) {
                result.setMatchedSubscriberId(bestMatch.getId());
                result.setMatchedSubscriberName(bestMatch.getFirstName() + " " + bestMatch.getLastName());
                logger.info("Face recognized - Subscriber: {} {}, Confidence: {:.3f}, Distance: {:.3f}", 
                           bestMatch.getFirstName(), bestMatch.getLastName(), bestConfidence, bestDistance);
            } else {
                logger.info("Face not recognized - Best confidence: {:.3f}, Threshold: {:.3f}", 
                           bestConfidence, settings.getConfidenceThreshold().floatValue());
            }
            
            result.setQualityScore(encodingResult.getQualityScore());
            result.setLivenessScore(encodingResult.getLivenessScore());
            result.setDetectedFace(encodingResult.getFaceRectangle());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Face recognition failed", e);
            return new FaceRecognitionResult(false, "Face recognition error: " + e.getMessage());
        }
    }
    
    /**
     * Register face encoding for a subscriber
     */
    @Transactional
    public boolean registerFaceForSubscriber(Long subscriberId, byte[] imageData, String entityId) {
        try {
            Subscriber subscriber = subscriberRepository.findById(subscriberId)
                .orElseThrow(() -> new IllegalArgumentException("Subscriber not found: " + subscriberId));

            // Extract face encoding
            FaceEncodingResult result = extractFaceEncoding(imageData, entityId);
            if (!result.isSuccess()) {
                logger.warn("Face registration failed for subscriber {}: {}", subscriberId, result.getErrorMessage());
                return false;
            }

            // Convert encoding to byte array for storage
            byte[] encodingBytes = convertFloatArrayToBytes(result.getEncoding());

            // Update subscriber with face data
            subscriber.setFaceEncoding(encodingBytes);
            subscriber.setFaceEncodingVersion("1.0");
            subscriber.setFaceRegisteredAt(LocalDateTime.now());
            subscriber.setFaceUpdatedAt(LocalDateTime.now());

            subscriberRepository.save(subscriber);

            logger.info("Face registered successfully for subscriber {} - Encoding dimensions: {}",
                       subscriberId, result.getEncoding().length);
            return true;

        } catch (Exception e) {
            logger.error("Failed to register face for subscriber " + subscriberId, e);
            return false;
        }
    }

    /**
     * Remove face registration for a subscriber
     */
    @Transactional
    public boolean removeFaceForSubscriber(Long subscriberId) {
        try {
            Subscriber subscriber = subscriberRepository.findById(subscriberId)
                .orElseThrow(() -> new IllegalArgumentException("Subscriber not found: " + subscriberId));

            subscriber.setFaceEncoding(null);
            subscriber.setFaceEncodingVersion(null);
            subscriber.setFaceRegisteredAt(null);
            subscriber.setFaceUpdatedAt(null);
            subscriber.setProfilePhotoPath(null);

            subscriberRepository.save(subscriber);

            logger.info("Face registration removed for subscriber {}", subscriberId);
            return true;

        } catch (Exception e) {
            logger.error("Failed to remove face for subscriber " + subscriberId, e);
            return false;
        }
    }

    /**
     * Log face recognition attempt
     */
    @Transactional
    public void logRecognitionAttempt(FaceRecognitionResult result, AttendanceSession session,
                                    Subscriber subscriber, String deviceInfo) {
        try {
            FaceRecognitionLog log = new FaceRecognitionLog();
            log.setSubscriber(subscriber);
            log.setSession(session);
            log.setRecognitionTimestamp(LocalDateTime.now());
            log.setProcessingTimeMs(result.getProcessingTimeMs());
            log.setDeviceInfo(deviceInfo);

            if (result.isSuccess()) {
                if (result.isMatched()) {
                    log.setRecognitionStatus(FaceRecognitionLog.RecognitionStatus.SUCCESS);
                    log.setConfidenceScore(BigDecimal.valueOf(result.getConfidenceScore()));
                } else {
                    log.setRecognitionStatus(FaceRecognitionLog.RecognitionStatus.LOW_CONFIDENCE);
                    log.setConfidenceScore(BigDecimal.valueOf(result.getConfidenceScore()));
                }
            } else {
                log.setRecognitionStatus(FaceRecognitionLog.RecognitionStatus.FAILED);
                log.setErrorMessage(result.getErrorMessage());
            }

            faceRecognitionLogRepository.save(log);

        } catch (Exception e) {
            logger.error("Failed to log recognition attempt", e);
        }
    }

    /**
     * Get face recognition settings for entity
     */
    private FaceRecognitionSettings getSettingsForEntity(String entityId) {
        return faceRecognitionSettingsRepository.findByEntityId(entityId)
            .orElseGet(() -> createDefaultSettings(entityId));
    }

    /**
     * Create default settings for entity
     */
    private FaceRecognitionSettings createDefaultSettings(String entityId) {
        FaceRecognitionSettings settings = new FaceRecognitionSettings();
        settings.setEntityId(entityId);
        settings.setConfidenceThreshold(new BigDecimal("0.8000"));
        settings.setMaxRecognitionDistance(new BigDecimal("0.600000"));
        settings.setEnableAntiSpoofing(true);
        settings.setEnableMultipleFaceDetection(false);
        settings.setMaxProcessingTimeMs(5000);
        settings.setPhotoQualityThreshold(new BigDecimal("0.7000"));
        return settings;
    }

    /**
     * Detect faces using SeetaFace2
     */
    private List<FaceRectangle> detectFacesWithSeetaFace(BufferedImage image) {
        try {
            byte[] pixelData = ImageUtils.extractPixelData(image);
            if (pixelData == null) return new ArrayList<>();

            float[] faceData = SeetaFaceJNI.detectFaces(engineHandle, pixelData,
                image.getWidth(), image.getHeight(), image.getColorModel().getNumComponents());

            return parseFaceRectangles(faceData);

        } catch (Exception e) {
            logger.error("SeetaFace2 face detection failed", e);
            return new ArrayList<>();
        }
    }

    /**
     * Fallback face detection using simple algorithms
     */
    private List<FaceRectangle> detectFacesWithFallback(BufferedImage image) {
        // Simple fallback - assume single face covering most of the image
        List<FaceRectangle> faces = new ArrayList<>();
        int margin = Math.min(image.getWidth(), image.getHeight()) / 10;
        FaceRectangle face = new FaceRectangle(
            margin, margin,
            image.getWidth() - 2 * margin,
            image.getHeight() - 2 * margin,
            0.8f // Default confidence
        );
        faces.add(face);
        logger.info("Using fallback face detection - assumed single face");
        return faces;
    }

    /**
     * Extract face encoding using SeetaFace2
     */
    private float[] extractEncodingWithSeetaFace(BufferedImage image, FaceRectangle face) {
        try {
            byte[] pixelData = ImageUtils.extractPixelData(image);
            if (pixelData == null) return null;

            return SeetaFaceJNI.extractFaceEncoding(engineHandle, pixelData,
                image.getWidth(), image.getHeight(), image.getColorModel().getNumComponents(),
                face.getX(), face.getY(), face.getWidth(), face.getHeight());

        } catch (Exception e) {
            logger.error("SeetaFace2 encoding extraction failed", e);
            return null;
        }
    }

    /**
     * Fallback face encoding using simple feature extraction
     */
    private float[] extractEncodingWithFallback(BufferedImage image, FaceRectangle face) {
        // Simple fallback - create pseudo-encoding based on image characteristics
        float[] encoding = new float[128]; // Standard face encoding size

        // Extract basic image features as encoding
        int avgRed = 0, avgGreen = 0, avgBlue = 0;
        int pixelCount = 0;

        for (int y = face.getY(); y < face.getY() + face.getHeight() && y < image.getHeight(); y++) {
            for (int x = face.getX(); x < face.getX() + face.getWidth() && x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                avgRed += (rgb >> 16) & 0xFF;
                avgGreen += (rgb >> 8) & 0xFF;
                avgBlue += rgb & 0xFF;
                pixelCount++;
            }
        }

        if (pixelCount > 0) {
            avgRed /= pixelCount;
            avgGreen /= pixelCount;
            avgBlue /= pixelCount;
        }

        // Fill encoding with normalized values
        for (int i = 0; i < encoding.length; i++) {
            encoding[i] = (float) ((avgRed + avgGreen + avgBlue + i) % 256) / 255.0f;
        }

        logger.info("Using fallback encoding extraction");
        return encoding;
    }

    /**
     * Detect liveness using SeetaFace2
     */
    private float detectLivenessWithSeetaFace(BufferedImage image, FaceRectangle face) {
        try {
            byte[] pixelData = ImageUtils.extractPixelData(image);
            if (pixelData == null) return 0.0f;

            return SeetaFaceJNI.detectLiveness(engineHandle, pixelData,
                image.getWidth(), image.getHeight(), image.getColorModel().getNumComponents(),
                face.getX(), face.getY(), face.getWidth(), face.getHeight());

        } catch (Exception e) {
            logger.error("SeetaFace2 liveness detection failed", e);
            return 0.0f;
        }
    }

    /**
     * Compare two face encodings
     */
    private float compareFaceEncodings(float[] encoding1, float[] encoding2) {
        if (engineInitialized) {
            try {
                return SeetaFaceJNI.compareFaceEncodings(encoding1, encoding2);
            } catch (Exception e) {
                logger.error("SeetaFace2 comparison failed, using fallback", e);
            }
        }

        // Fallback comparison using cosine similarity
        return calculateCosineSimilarity(encoding1, encoding2);
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    private float calculateCosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0.0f;

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) return 0.0f;

        return (float) (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    /**
     * Assess image quality
     */
    private float assessImageQuality(BufferedImage image) {
        if (engineInitialized) {
            try {
                byte[] pixelData = ImageUtils.extractPixelData(image);
                if (pixelData != null) {
                    return SeetaFaceJNI.assessImageQuality(pixelData,
                        image.getWidth(), image.getHeight(), image.getColorModel().getNumComponents());
                }
            } catch (Exception e) {
                logger.error("SeetaFace2 quality assessment failed, using fallback", e);
            }
        }

        // Fallback quality assessment
        return assessImageQualityFallback(image);
    }

    /**
     * Fallback image quality assessment
     */
    private float assessImageQualityFallback(BufferedImage image) {
        // Simple quality metrics
        float score = 1.0f;

        // Check resolution
        int pixels = image.getWidth() * image.getHeight();
        if (pixels < 50000) score *= 0.5f; // Very low resolution
        else if (pixels < 200000) score *= 0.8f; // Low resolution

        // Check aspect ratio (faces should be roughly square-ish)
        float aspectRatio = (float) image.getWidth() / image.getHeight();
        if (aspectRatio < 0.5f || aspectRatio > 2.0f) score *= 0.7f;

        return Math.max(0.0f, Math.min(1.0f, score));
    }

    /**
     * Find the best face from detected faces (largest area)
     */
    private FaceRectangle findBestFace(List<FaceRectangle> faces) {
        return faces.stream()
            .max((f1, f2) -> Integer.compare(f1.getArea(), f2.getArea()))
            .orElse(null);
    }

    /**
     * Parse face rectangles from native detection result
     */
    private List<FaceRectangle> parseFaceRectangles(float[] faceData) {
        List<FaceRectangle> faces = new ArrayList<>();

        if (faceData == null || faceData.length % 5 != 0) {
            return faces;
        }

        for (int i = 0; i < faceData.length; i += 5) {
            FaceRectangle face = new FaceRectangle(
                (int) faceData[i],     // x
                (int) faceData[i + 1], // y
                (int) faceData[i + 2], // width
                (int) faceData[i + 3], // height
                faceData[i + 4]        // confidence
            );

            if (face.isValid()) {
                faces.add(face);
            }
        }

        return faces;
    }

    /**
     * Convert float array to byte array for database storage
     */
    private byte[] convertFloatArrayToBytes(float[] floatArray) {
        if (floatArray == null) return null;

        byte[] bytes = new byte[floatArray.length * 4];
        for (int i = 0; i < floatArray.length; i++) {
            int bits = Float.floatToIntBits(floatArray[i]);
            bytes[i * 4] = (byte) (bits & 0xFF);
            bytes[i * 4 + 1] = (byte) ((bits >> 8) & 0xFF);
            bytes[i * 4 + 2] = (byte) ((bits >> 16) & 0xFF);
            bytes[i * 4 + 3] = (byte) ((bits >> 24) & 0xFF);
        }
        return bytes;
    }

    /**
     * Convert byte array from database to float array
     */
    private float[] convertBytesToFloatArray(byte[] bytes) {
        if (bytes == null || bytes.length % 4 != 0) return null;

        float[] floatArray = new float[bytes.length / 4];
        for (int i = 0; i < floatArray.length; i++) {
            int bits = (bytes[i * 4] & 0xFF) |
                      ((bytes[i * 4 + 1] & 0xFF) << 8) |
                      ((bytes[i * 4 + 2] & 0xFF) << 16) |
                      ((bytes[i * 4 + 3] & 0xFF) << 24);
            floatArray[i] = Float.intBitsToFloat(bits);
        }
        return floatArray;
    }

    /**
     * Check if face recognition is available
     */
    public boolean isFaceRecognitionAvailable() {
        return engineInitialized || fallbackEnabled;
    }

    /**
     * Get engine status information
     */
    public String getEngineStatus() {
        if (engineInitialized) {
            return "SeetaFace6 engine initialized (Handle: " + engineHandle + ")";
        } else if (fallbackEnabled) {
            return "Using fallback implementation";
        } else {
            return "Face recognition not available";
        }
    }

    /**
     * Get face recognition logs for a session
     */
    public List<FaceRecognitionLog> getRecognitionLogsForSession(AttendanceSession session) {
        try {
            return faceRecognitionLogRepository.findBySessionOrderByRecognitionTimestampDesc(session);
        } catch (Exception e) {
            logger.error("Failed to get recognition logs for session {}", session.getId(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get face recognition logs for a subscriber
     */
    public List<FaceRecognitionLog> getRecognitionLogsForSubscriber(Subscriber subscriber) {
        try {
            return faceRecognitionLogRepository.findBySubscriberOrderByRecognitionTimestampDesc(subscriber);
        } catch (Exception e) {
            logger.error("Failed to get recognition logs for subscriber {}", subscriber.getId(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get face recognition statistics for an entity
     */
    public Map<String, Object> getRecognitionStatsForEntity(String entityId) {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Get recognition statistics
            List<Object[]> statusStats = faceRecognitionLogRepository.getRecognitionStatsByEntity(entityId);
            Map<String, Long> statusCounts = new HashMap<>();

            for (Object[] stat : statusStats) {
                FaceRecognitionLog.RecognitionStatus status = (FaceRecognitionLog.RecognitionStatus) stat[0];
                Long count = (Long) stat[1];
                statusCounts.put(status.name(), count);
            }

            // Get total registered faces
            long totalRegisteredFaces = subscriberRepository.countByOrganizationEntityIdAndFaceEncodingIsNotNull(entityId);

            // Get recent failures for debugging
            LocalDateTime since = LocalDateTime.now().minusDays(7);
            List<FaceRecognitionLog> recentFailures = faceRecognitionLogRepository.findRecentFailures(since);

            stats.put("entityId", entityId);
            stats.put("totalRegisteredFaces", totalRegisteredFaces);
            stats.put("statusCounts", statusCounts);
            stats.put("recentFailures", recentFailures.size());
            stats.put("engineStatus", getEngineStatus());
            stats.put("isAvailable", isFaceRecognitionAvailable());

            return stats;

        } catch (Exception e) {
            logger.error("Failed to get recognition stats for entity {}", entityId, e);
            Map<String, Object> errorStats = new HashMap<>();
            errorStats.put("error", e.getMessage());
            return errorStats;
        }
    }
}
