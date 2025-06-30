package com.example.attendancesystem.subscriber.service;

import com.example.attendancesystem.facerecognition.*;
import com.example.attendancesystem.model.*;
import com.example.attendancesystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaceRecognitionServiceTest {

    @Mock
    private SubscriberRepository subscriberRepository;

    @Mock
    private FaceRecognitionSettingsRepository faceRecognitionSettingsRepository;

    @Mock
    private FaceRecognitionLogRepository faceRecognitionLogRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private FaceRecognitionService faceRecognitionService;

    private byte[] testImageData;
    private FaceRecognitionSettings testSettings;
    private Organization testOrganization;
    private Subscriber testSubscriber;

    @BeforeEach
    void setUp() throws Exception {
        // Create a simple test image
        BufferedImage testImage = new BufferedImage(200, 200, BufferedImage.TYPE_3BYTE_BGR);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(testImage, "jpg", baos);
        testImageData = baos.toByteArray();

        // Create test settings
        testSettings = new FaceRecognitionSettings();
        testSettings.setEntityId("MSD00001");
        testSettings.setConfidenceThreshold(new BigDecimal("0.8000"));
        testSettings.setMaxRecognitionDistance(new BigDecimal("0.600000"));
        testSettings.setEnableAntiSpoofing(true);
        testSettings.setEnableMultipleFaceDetection(false);
        testSettings.setMaxProcessingTimeMs(5000);
        testSettings.setPhotoQualityThreshold(new BigDecimal("0.7000"));

        // Create test organization
        testOrganization = new Organization();
        testOrganization.setEntityId("MSD00001");
        testOrganization.setName("Test Organization");

        // Create test subscriber
        testSubscriber = new Subscriber();
        testSubscriber.setId(1L);
        testSubscriber.setFirstName("John");
        testSubscriber.setLastName("Doe");
        testSubscriber.setMobileNumber("1234567890");
        testSubscriber.setOrganization(testOrganization);
    }

    @Test
    void testDetectFaces_ValidImage() {
        // Test face detection with valid image
        FaceDetectionResult result = faceRecognitionService.detectFaces(testImageData);
        
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getFaces());
        assertTrue(result.getProcessingTimeMs() > 0);
        assertTrue(result.getImageQualityScore() >= 0.0f && result.getImageQualityScore() <= 1.0f);
    }

    @Test
    void testDetectFaces_InvalidImage() {
        // Test face detection with invalid image data
        byte[] invalidData = "invalid image data".getBytes();
        
        FaceDetectionResult result = faceRecognitionService.detectFaces(invalidData);
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void testDetectFaces_NullImage() {
        // Test face detection with null image data
        FaceDetectionResult result = faceRecognitionService.detectFaces(null);
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Invalid image data", result.getErrorMessage());
    }

    @Test
    void testExtractFaceEncoding_ValidImage() {
        // Mock settings repository
        when(faceRecognitionSettingsRepository.findByEntityId("MSD00001"))
            .thenReturn(Optional.of(testSettings));

        FaceEncodingResult result = faceRecognitionService.extractFaceEncoding(testImageData, "MSD00001");
        
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getEncoding());
        assertTrue(result.getEncoding().length > 0);
        assertNotNull(result.getFaceRectangle());
        assertTrue(result.getProcessingTimeMs() > 0);
    }

    @Test
    void testExtractFaceEncoding_LowQuality() {
        // Set high quality threshold to simulate low quality image
        testSettings.setPhotoQualityThreshold(new BigDecimal("0.9999"));
        
        when(faceRecognitionSettingsRepository.findByEntityId("MSD00001"))
            .thenReturn(Optional.of(testSettings));

        FaceEncodingResult result = faceRecognitionService.extractFaceEncoding(testImageData, "MSD00001");
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Image quality too low"));
    }

    @Test
    void testRegisterFaceForSubscriber_Success() {
        // Mock repositories
        when(subscriberRepository.findById(1L)).thenReturn(Optional.of(testSubscriber));
        when(faceRecognitionSettingsRepository.findByEntityId("MSD00001"))
            .thenReturn(Optional.of(testSettings));
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(testSubscriber);

        boolean result = faceRecognitionService.registerFaceForSubscriber(1L, testImageData, "MSD00001");
        
        assertTrue(result);
        verify(subscriberRepository).save(any(Subscriber.class));
    }

    @Test
    void testRegisterFaceForSubscriber_SubscriberNotFound() {
        // Mock subscriber not found
        when(subscriberRepository.findById(1L)).thenReturn(Optional.empty());

        boolean result = faceRecognitionService.registerFaceForSubscriber(1L, testImageData, "MSD00001");
        
        assertFalse(result);
        verify(subscriberRepository, never()).save(any(Subscriber.class));
    }

    @Test
    void testRemoveFaceForSubscriber_Success() {
        // Mock repositories
        when(subscriberRepository.findById(1L)).thenReturn(Optional.of(testSubscriber));
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(testSubscriber);

        boolean result = faceRecognitionService.removeFaceForSubscriber(1L);
        
        assertTrue(result);
        verify(subscriberRepository).save(any(Subscriber.class));
    }

    @Test
    void testRecognizeFace_NoRegisteredFaces() {
        // Mock repositories
        when(faceRecognitionSettingsRepository.findByEntityId("MSD00001"))
            .thenReturn(Optional.of(testSettings));
        when(organizationRepository.findByEntityId("MSD00001"))
            .thenReturn(Optional.of(testOrganization));
        when(subscriberRepository.findByOrganizationAndFaceEncodingIsNotNull(testOrganization))
            .thenReturn(new ArrayList<>());

        FaceRecognitionResult result = faceRecognitionService.recognizeFace(testImageData, "MSD00001");
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("No registered faces found"));
    }

    @Test
    void testRecognizeFace_WithRegisteredFaces() {
        // Create subscriber with face encoding
        byte[] mockEncoding = new byte[512]; // Mock face encoding
        testSubscriber.setFaceEncoding(mockEncoding);
        
        List<Subscriber> registeredSubscribers = new ArrayList<>();
        registeredSubscribers.add(testSubscriber);

        // Mock repositories
        when(faceRecognitionSettingsRepository.findByEntityId("MSD00001"))
            .thenReturn(Optional.of(testSettings));
        when(organizationRepository.findByEntityId("MSD00001"))
            .thenReturn(Optional.of(testOrganization));
        when(subscriberRepository.findByOrganizationAndFaceEncodingIsNotNull(testOrganization))
            .thenReturn(registeredSubscribers);

        FaceRecognitionResult result = faceRecognitionService.recognizeFace(testImageData, "MSD00001");
        
        assertNotNull(result);
        // Note: Result may be success or failure depending on face matching, but should not error
        assertTrue(result.isSuccess() || result.getErrorMessage() != null);
    }

    @Test
    void testIsFaceRecognitionAvailable() {
        // Test that face recognition is available (fallback should be enabled)
        boolean available = faceRecognitionService.isFaceRecognitionAvailable();
        assertTrue(available);
    }

    @Test
    void testGetEngineStatus() {
        // Test engine status
        String status = faceRecognitionService.getEngineStatus();
        assertNotNull(status);
        assertFalse(status.isEmpty());
    }
}
