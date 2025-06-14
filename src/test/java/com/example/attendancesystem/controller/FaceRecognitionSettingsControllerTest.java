package com.example.attendancesystem.controller;

import com.example.attendancesystem.model.FaceRecognitionSettings;
import com.example.attendancesystem.repository.FaceRecognitionSettingsRepository;
import com.example.attendancesystem.service.FaceRecognitionService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(FaceRecognitionSettingsController.class)
class FaceRecognitionSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FaceRecognitionSettingsRepository settingsRepository;

    @MockBean
    private FaceRecognitionService faceRecognitionService;

    @Autowired
    private ObjectMapper objectMapper;

    private FaceRecognitionSettings testSettings;

    @BeforeEach
    void setUp() {
        testSettings = new FaceRecognitionSettings();
        testSettings.setId(1L);
        testSettings.setEntityId("MSD00001");
        testSettings.setConfidenceThreshold(new BigDecimal("0.8000"));
        testSettings.setMaxRecognitionDistance(new BigDecimal("0.600000"));
        testSettings.setEnableAntiSpoofing(true);
        testSettings.setEnableMultipleFaceDetection(false);
        testSettings.setMaxProcessingTimeMs(5000);
        testSettings.setPhotoQualityThreshold(new BigDecimal("0.7000"));
        testSettings.setCreatedAt(LocalDateTime.now());
        testSettings.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testGetSettings_ExistingSettings() throws Exception {
        // Arrange
        when(settingsRepository.findByEntityId("MSD00001"))
            .thenReturn(Optional.of(testSettings));
        when(faceRecognitionService.getEngineStatus())
            .thenReturn("SeetaFace6 engine initialized");
        when(faceRecognitionService.isFaceRecognitionAvailable())
            .thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/face/settings")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.entityId").value("MSD00001"))
                .andExpect(jsonPath("$.settings.confidenceThreshold").value(0.8000))
                .andExpect(jsonPath("$.settings.maxRecognitionDistance").value(0.600000))
                .andExpect(jsonPath("$.settings.enableAntiSpoofing").value(true))
                .andExpect(jsonPath("$.settings.enableMultipleFaceDetection").value(false))
                .andExpect(jsonPath("$.settings.maxProcessingTimeMs").value(5000))
                .andExpect(jsonPath("$.settings.photoQualityThreshold").value(0.7000))
                .andExpect(jsonPath("$.engineStatus").value("SeetaFace6 engine initialized"))
                .andExpect(jsonPath("$.isAvailable").value(true));

        verify(settingsRepository).findByEntityId("MSD00001");
        verify(faceRecognitionService).getEngineStatus();
        verify(faceRecognitionService).isFaceRecognitionAvailable();
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testGetSettings_CreateDefaultSettings() throws Exception {
        // Arrange
        when(settingsRepository.findByEntityId("MSD00001"))
            .thenReturn(Optional.empty());
        when(settingsRepository.save(any(FaceRecognitionSettings.class)))
            .thenReturn(testSettings);
        when(faceRecognitionService.getEngineStatus())
            .thenReturn("Using fallback implementation");
        when(faceRecognitionService.isFaceRecognitionAvailable())
            .thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/face/settings")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.entityId").value("MSD00001"))
                .andExpect(jsonPath("$.settings").exists())
                .andExpect(jsonPath("$.engineStatus").value("Using fallback implementation"));

        verify(settingsRepository).findByEntityId("MSD00001");
        verify(settingsRepository).save(any(FaceRecognitionSettings.class));
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testUpdateSettings_Success() throws Exception {
        // Arrange
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("confidenceThreshold", 0.85);
        updateRequest.put("maxRecognitionDistance", 0.55);
        updateRequest.put("enableAntiSpoofing", false);
        updateRequest.put("maxProcessingTimeMs", 4000);

        when(settingsRepository.findByEntityId("MSD00001"))
            .thenReturn(Optional.of(testSettings));
        when(settingsRepository.save(any(FaceRecognitionSettings.class)))
            .thenReturn(testSettings);

        // Act & Assert
        mockMvc.perform(put("/api/face/settings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Settings updated successfully"))
                .andExpect(jsonPath("$.entityId").value("MSD00001"))
                .andExpect(jsonPath("$.settings").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(settingsRepository).findByEntityId("MSD00001");
        verify(settingsRepository).save(any(FaceRecognitionSettings.class));
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testUpdateSettings_InvalidValues() throws Exception {
        // Arrange
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("confidenceThreshold", 1.5); // Invalid - too high
        updateRequest.put("maxProcessingTimeMs", 50000); // Invalid - too high

        when(settingsRepository.findByEntityId("MSD00001"))
            .thenReturn(Optional.of(testSettings));

        // Act & Assert
        mockMvc.perform(put("/api/face/settings")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid settings")));

        verify(settingsRepository).findByEntityId("MSD00001");
        verify(settingsRepository, never()).save(any(FaceRecognitionSettings.class));
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testResetSettings_Success() throws Exception {
        // Arrange
        when(settingsRepository.save(any(FaceRecognitionSettings.class)))
            .thenReturn(testSettings);

        // Act & Assert
        mockMvc.perform(post("/api/face/settings/reset")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Settings reset to default values"))
                .andExpect(jsonPath("$.entityId").value("MSD00001"))
                .andExpect(jsonPath("$.settings").exists())
                .andExpect(jsonPath("$.resetAt").exists());

        verify(settingsRepository).save(any(FaceRecognitionSettings.class));
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testGetRecommendations_SmallEntityHighSecurity() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/face/settings/recommendations")
                .param("entitySize", "small")
                .param("usagePattern", "high_security")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.entityId").value("MSD00001"))
                .andExpect(jsonPath("$.entitySize").value("small"))
                .andExpect(jsonPath("$.usagePattern").value("high_security"))
                .andExpect(jsonPath("$.recommendations").exists())
                .andExpect(jsonPath("$.recommendations.confidenceThreshold").exists())
                .andExpect(jsonPath("$.recommendations.enableAntiSpoofing").value(true))
                .andExpect(jsonPath("$.description").value(containsString("small organizations")))
                .andExpect(jsonPath("$.description").value(containsString("high security")));
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testGetRecommendations_LargeEntityHighThroughput() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/face/settings/recommendations")
                .param("entitySize", "large")
                .param("usagePattern", "high_throughput")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.entitySize").value("large"))
                .andExpect(jsonPath("$.usagePattern").value("high_throughput"))
                .andExpect(jsonPath("$.recommendations.enableMultipleFaceDetection").value(true))
                .andExpect(jsonPath("$.description").value(containsString("large organizations")))
                .andExpect(jsonPath("$.description").value(containsString("high throughput")));
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testGetRecommendations_DefaultParameters() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/face/settings/recommendations")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.entitySize").value("medium"))
                .andExpect(jsonPath("$.usagePattern").value("standard"))
                .andExpect(jsonPath("$.recommendations").exists())
                .andExpect(jsonPath("$.description").value(containsString("medium-sized organizations")))
                .andExpect(jsonPath("$.description").value(containsString("standard usage")));
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testTestSettings_Success() throws Exception {
        // Arrange
        when(settingsRepository.findByEntityId("MSD00001"))
            .thenReturn(Optional.of(testSettings));
        when(faceRecognitionService.isFaceRecognitionAvailable())
            .thenReturn(true);
        when(faceRecognitionService.getEngineStatus())
            .thenReturn("SeetaFace6 engine initialized");

        // Act & Assert
        mockMvc.perform(post("/api/face/settings/test")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.entityId").value("MSD00001"))
                .andExpect(jsonPath("$.settings").exists())
                .andExpect(jsonPath("$.performanceMetrics").exists())
                .andExpect(jsonPath("$.performanceMetrics.engineAvailable").value(true))
                .andExpect(jsonPath("$.performanceMetrics.engineStatus").value("SeetaFace6 engine initialized"))
                .andExpect(jsonPath("$.testResult").value("PASS"))
                .andExpect(jsonPath("$.message").value("Face recognition engine is working correctly"));

        verify(settingsRepository).findByEntityId("MSD00001");
        verify(faceRecognitionService).isFaceRecognitionAvailable();
        verify(faceRecognitionService).getEngineStatus();
    }

    @Test
    @WithMockUser(username = "admin@MSD00001")
    void testTestSettings_FallbackMode() throws Exception {
        // Arrange
        when(settingsRepository.findByEntityId("MSD00001"))
            .thenReturn(Optional.of(testSettings));
        when(faceRecognitionService.isFaceRecognitionAvailable())
            .thenReturn(true);
        when(faceRecognitionService.getEngineStatus())
            .thenReturn("Using fallback implementation");

        // Act & Assert
        mockMvc.perform(post("/api/face/settings/test")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.testResult").value("PASS"))
                .andExpect(jsonPath("$.performanceMetrics.engineStatus").value("Using fallback implementation"));
    }
}
