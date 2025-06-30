package com.example.attendancesystem.subscriber.controller;

import com.example.attendancesystem.dto.FaceRecognitionDto;
import com.example.attendancesystem.model.Organization;
import com.example.attendancesystem.model.Subscriber;
import com.example.attendancesystem.repository.SubscriberRepository;
import com.example.attendancesystem.service.FaceRecognitionService;
import com.example.attendancesystem.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(FaceRegistrationController.class)
class FaceRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FaceRecognitionService faceRecognitionService;

    @MockBean
    private SubscriberRepository subscriberRepository;

    @MockBean
    private FileStorageService fileStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    private Subscriber testSubscriber;
    private Organization testOrganization;
    private String testBase64Image;

    @BeforeEach
    void setUp() {
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

        // Create test Base64 image (simple 1x1 pixel image)
        byte[] simpleImage = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, // JPEG header
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x01, 0x00, 0x48, 0x00, 0x48, 0x00, 0x00,
            (byte) 0xFF, (byte) 0xD9 // JPEG end
        };
        testBase64Image = Base64.getEncoder().encodeToString(simpleImage);
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testRegisterFace_Success() throws Exception {
        // Arrange
        FaceRecognitionDto request = new FaceRecognitionDto();
        request.setSubscriberId(1L);
        request.setBase64Image(testBase64Image);
        request.setDeviceInfo("Test Device");

        when(subscriberRepository.findByIdAndOrganizationEntityId(1L, "MSD00001"))
            .thenReturn(Optional.of(testSubscriber));
        when(faceRecognitionService.registerFaceForSubscriber(eq(1L), any(byte[].class), eq("MSD00001")))
            .thenReturn(true);
        when(fileStorageService.storeProfilePhoto(any(byte[].class), eq(1L), anyString()))
            .thenReturn("profile_1_John_Doe_20231213_120000_abc123.jpg");

        // Act & Assert
        mockMvc.perform(post("/api/face/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Face registered successfully"))
                .andExpect(jsonPath("$.subscriberId").value(1))
                .andExpect(jsonPath("$.subscriberName").value("John Doe"))
                .andExpect(jsonPath("$.profilePhotoPath").exists());

        verify(faceRecognitionService).registerFaceForSubscriber(eq(1L), any(byte[].class), eq("MSD00001"));
        verify(fileStorageService).storeProfilePhoto(any(byte[].class), eq(1L), anyString());
        verify(subscriberRepository).save(testSubscriber);
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testRegisterFace_SubscriberNotFound() throws Exception {
        // Arrange
        FaceRecognitionDto request = new FaceRecognitionDto();
        request.setSubscriberId(999L);
        request.setBase64Image(testBase64Image);

        when(subscriberRepository.findByIdAndOrganizationEntityId(999L, "MSD00001"))
            .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/face/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Subscriber not found or access denied"));

        verify(faceRecognitionService, never()).registerFaceForSubscriber(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testRegisterFace_InvalidImage() throws Exception {
        // Arrange
        FaceRecognitionDto request = new FaceRecognitionDto();
        request.setSubscriberId(1L);
        request.setBase64Image("invalid_base64");

        when(subscriberRepository.findByIdAndOrganizationEntityId(1L, "MSD00001"))
            .thenReturn(Optional.of(testSubscriber));

        // Act & Assert
        mockMvc.perform(post("/api/face/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid image format"));

        verify(faceRecognitionService, never()).registerFaceForSubscriber(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testRegisterFace_RegistrationFailed() throws Exception {
        // Arrange
        FaceRecognitionDto request = new FaceRecognitionDto();
        request.setSubscriberId(1L);
        request.setBase64Image(testBase64Image);

        when(subscriberRepository.findByIdAndOrganizationEntityId(1L, "MSD00001"))
            .thenReturn(Optional.of(testSubscriber));
        when(faceRecognitionService.registerFaceForSubscriber(eq(1L), any(byte[].class), eq("MSD00001")))
            .thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/face/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Face registration failed. Please try with a clearer image."));

        verify(faceRecognitionService).registerFaceForSubscriber(eq(1L), any(byte[].class), eq("MSD00001"));
        verify(fileStorageService, never()).storeProfilePhoto(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testGetFaceRegistrationStatus_WithFace() throws Exception {
        // Arrange
        testSubscriber.setFaceEncoding(new byte[]{1, 2, 3, 4});
        testSubscriber.setFaceRegisteredAt(LocalDateTime.now());
        testSubscriber.setFaceUpdatedAt(LocalDateTime.now());
        testSubscriber.setFaceEncodingVersion("1.0");
        testSubscriber.setProfilePhotoPath("profile_1_John_Doe_20231213_120000_abc123.jpg");

        when(subscriberRepository.findByIdAndOrganizationEntityId(1L, "MSD00001"))
            .thenReturn(Optional.of(testSubscriber));

        // Act & Assert
        mockMvc.perform(get("/api/face/status/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.subscriberId").value(1))
                .andExpect(jsonPath("$.subscriberName").value("John Doe"))
                .andExpect(jsonPath("$.hasFaceRecognition").value(true))
                .andExpect(jsonPath("$.profilePhotoPath").value("profile_1_John_Doe_20231213_120000_abc123.jpg"))
                .andExpect(jsonPath("$.faceEncodingVersion").value("1.0"))
                .andExpect(jsonPath("$.faceRegisteredAt").exists())
                .andExpect(jsonPath("$.faceUpdatedAt").exists());
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testGetFaceRegistrationStatus_WithoutFace() throws Exception {
        // Arrange
        when(subscriberRepository.findByIdAndOrganizationEntityId(1L, "MSD00001"))
            .thenReturn(Optional.of(testSubscriber));

        // Act & Assert
        mockMvc.perform(get("/api/face/status/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.subscriberId").value(1))
                .andExpect(jsonPath("$.subscriberName").value("John Doe"))
                .andExpect(jsonPath("$.hasFaceRecognition").value(false))
                .andExpect(jsonPath("$.faceRegisteredAt").doesNotExist())
                .andExpect(jsonPath("$.faceUpdatedAt").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testRemoveFaceRegistration_Success() throws Exception {
        // Arrange
        testSubscriber.setFaceEncoding(new byte[]{1, 2, 3, 4});
        testSubscriber.setProfilePhotoPath("profile_1_John_Doe_20231213_120000_abc123.jpg");

        when(subscriberRepository.findByIdAndOrganizationEntityId(1L, "MSD00001"))
            .thenReturn(Optional.of(testSubscriber));
        when(faceRecognitionService.removeFaceForSubscriber(1L))
            .thenReturn(true);
        when(fileStorageService.deleteFile("profile_1_John_Doe_20231213_120000_abc123.jpg"))
            .thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/face/remove/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Face registration removed successfully"))
                .andExpect(jsonPath("$.subscriberId").value(1))
                .andExpect(jsonPath("$.subscriberName").value("John Doe"));

        verify(faceRecognitionService).removeFaceForSubscriber(1L);
        verify(fileStorageService).deleteFile("profile_1_John_Doe_20231213_120000_abc123.jpg");
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testGetSubscribersWithFaceStatus() throws Exception {
        // Arrange
        Subscriber subscriber2 = new Subscriber();
        subscriber2.setId(2L);
        subscriber2.setFirstName("Jane");
        subscriber2.setLastName("Smith");
        subscriber2.setMobileNumber("0987654321");
        subscriber2.setOrganization(testOrganization);
        subscriber2.setFaceEncoding(new byte[]{1, 2, 3, 4});

        List<Subscriber> allSubscribers = Arrays.asList(testSubscriber, subscriber2);
        List<Subscriber> withFace = Arrays.asList(subscriber2);

        when(subscriberRepository.findAllByOrganizationEntityId("MSD00001"))
            .thenReturn(allSubscribers);
        when(subscriberRepository.findByOrganizationEntityIdAndFaceEncodingIsNotNull("MSD00001"))
            .thenReturn(withFace);

        // Act & Assert
        mockMvc.perform(get("/api/face/subscribers")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.entityId").value("MSD00001"))
                .andExpect(jsonPath("$.totalSubscribers").value(2))
                .andExpect(jsonPath("$.subscribersWithFace").value(1))
                .andExpect(jsonPath("$.subscribersWithoutFace").value(1))
                .andExpect(jsonPath("$.subscribers").isArray())
                .andExpect(jsonPath("$.subscribers[0].id").value(1))
                .andExpect(jsonPath("$.subscribers[0].hasFaceRecognition").value(false))
                .andExpect(jsonPath("$.subscribers[1].id").value(2))
                .andExpect(jsonPath("$.subscribers[1].hasFaceRecognition").value(true));
    }
}
