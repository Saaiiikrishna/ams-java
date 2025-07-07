package com.example.attendancesystem.attendance.facerecognition;

/**
 * Face Recognition Settings for attendance service
 * Simplified for microservices independence
 */
public class FaceRecognitionSettings {

    private boolean enabled = false;
    private double threshold = 0.8;
    private String modelPath = "";

    public FaceRecognitionSettings() {
        // Default constructor
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }
}