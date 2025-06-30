package com.example.attendancesystem.attendance.facerecognition;

/**
 * Represents a detected face rectangle with additional metadata
 */
public class FaceRectangle {
    
    private int x;
    private int y;
    private int width;
    private int height;
    private float confidence;
    private float livenessScore;
    
    public FaceRectangle() {}
    
    public FaceRectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public FaceRectangle(int x, int y, int width, int height, float confidence) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.confidence = confidence;
    }
    
    // Getters and Setters
    public int getX() {
        return x;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getY() {
        return y;
    }
    
    public void setY(int y) {
        this.y = y;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public float getConfidence() {
        return confidence;
    }
    
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    public float getLivenessScore() {
        return livenessScore;
    }
    
    public void setLivenessScore(float livenessScore) {
        this.livenessScore = livenessScore;
    }
    
    // Utility methods
    public int getCenterX() {
        return x + width / 2;
    }
    
    public int getCenterY() {
        return y + height / 2;
    }
    
    public int getArea() {
        return width * height;
    }
    
    public boolean isValid() {
        return width > 0 && height > 0;
    }
    
    @Override
    public String toString() {
        return String.format("FaceRectangle{x=%d, y=%d, width=%d, height=%d, confidence=%.3f, liveness=%.3f}", 
                           x, y, width, height, confidence, livenessScore);
    }
}
