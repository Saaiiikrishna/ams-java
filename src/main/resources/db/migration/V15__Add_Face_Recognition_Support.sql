-- Migration V15: Add face recognition support to attendance system
-- This migration adds face encoding storage, profile photos, and face recognition audit logs

-- Add face recognition fields to subscribers table
ALTER TABLE subscribers 
ADD COLUMN IF NOT EXISTS profile_photo_path VARCHAR(500),
ADD COLUMN IF NOT EXISTS face_encoding BYTEA,
ADD COLUMN IF NOT EXISTS face_encoding_version VARCHAR(20) DEFAULT '1.0',
ADD COLUMN IF NOT EXISTS face_registered_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS face_updated_at TIMESTAMP;

-- Add comments to clarify the column purposes
COMMENT ON COLUMN subscribers.profile_photo_path IS 'File path to subscriber profile photo';
COMMENT ON COLUMN subscribers.face_encoding IS 'Binary face encoding data for recognition';
COMMENT ON COLUMN subscribers.face_encoding_version IS 'Version of face encoding algorithm used';
COMMENT ON COLUMN subscribers.face_registered_at IS 'Timestamp when face was first registered';
COMMENT ON COLUMN subscribers.face_updated_at IS 'Timestamp when face encoding was last updated';

-- Create face_recognition_logs table for audit trail
CREATE TABLE IF NOT EXISTS face_recognition_logs (
    id SERIAL PRIMARY KEY,
    subscriber_id BIGINT REFERENCES subscribers(id) ON DELETE CASCADE,
    session_id BIGINT REFERENCES attendance_sessions(id) ON DELETE CASCADE,
    recognition_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confidence_score DECIMAL(5,4), -- Confidence score between 0.0000 and 1.0000
    recognition_status VARCHAR(20) NOT NULL, -- SUCCESS, FAILED, LOW_CONFIDENCE, ERROR
    processing_time_ms INTEGER, -- Time taken for recognition in milliseconds
    image_path VARCHAR(500), -- Path to the captured image for audit
    error_message TEXT, -- Error details if recognition failed
    device_info TEXT, -- Device information where recognition was performed
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_face_recognition_logs_subscriber ON face_recognition_logs(subscriber_id);
CREATE INDEX IF NOT EXISTS idx_face_recognition_logs_session ON face_recognition_logs(session_id);
CREATE INDEX IF NOT EXISTS idx_face_recognition_logs_timestamp ON face_recognition_logs(recognition_timestamp);
CREATE INDEX IF NOT EXISTS idx_face_recognition_logs_status ON face_recognition_logs(recognition_status);
CREATE INDEX IF NOT EXISTS idx_subscribers_face_encoding ON subscribers(face_encoding) WHERE face_encoding IS NOT NULL;

-- Add constraints
ALTER TABLE face_recognition_logs 
ADD CONSTRAINT chk_confidence_score CHECK (confidence_score >= 0.0 AND confidence_score <= 1.0),
ADD CONSTRAINT chk_recognition_status CHECK (recognition_status IN ('SUCCESS', 'FAILED', 'LOW_CONFIDENCE', 'ERROR', 'MULTIPLE_FACES', 'NO_FACE_DETECTED'));

-- Create face_recognition_settings table for configuration
CREATE TABLE IF NOT EXISTS face_recognition_settings (
    id SERIAL PRIMARY KEY,
    entity_id VARCHAR(8) NOT NULL REFERENCES organizations(entity_id) ON DELETE CASCADE,
    confidence_threshold DECIMAL(5,4) NOT NULL DEFAULT 0.8000, -- Minimum confidence for successful recognition
    max_recognition_distance DECIMAL(8,6) NOT NULL DEFAULT 0.6000, -- Maximum distance for face matching
    enable_anti_spoofing BOOLEAN NOT NULL DEFAULT true,
    enable_multiple_face_detection BOOLEAN NOT NULL DEFAULT false,
    max_processing_time_ms INTEGER NOT NULL DEFAULT 5000, -- Maximum time allowed for processing
    photo_quality_threshold DECIMAL(5,4) NOT NULL DEFAULT 0.7000, -- Minimum photo quality score
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(entity_id)
);

-- Add comments for settings table
COMMENT ON TABLE face_recognition_settings IS 'Face recognition configuration per organization';
COMMENT ON COLUMN face_recognition_settings.confidence_threshold IS 'Minimum confidence score for successful recognition (0.0-1.0)';
COMMENT ON COLUMN face_recognition_settings.max_recognition_distance IS 'Maximum euclidean distance for face matching';
COMMENT ON COLUMN face_recognition_settings.enable_anti_spoofing IS 'Enable liveness detection to prevent photo spoofing';
COMMENT ON COLUMN face_recognition_settings.enable_multiple_face_detection IS 'Allow recognition when multiple faces are detected';
COMMENT ON COLUMN face_recognition_settings.max_processing_time_ms IS 'Maximum processing time before timeout';
COMMENT ON COLUMN face_recognition_settings.photo_quality_threshold IS 'Minimum photo quality score for registration';

-- Insert default settings for existing organizations
INSERT INTO face_recognition_settings (entity_id, confidence_threshold, max_recognition_distance, enable_anti_spoofing, enable_multiple_face_detection, max_processing_time_ms, photo_quality_threshold)
SELECT entity_id, 0.8000, 0.6000, true, false, 5000, 0.7000
FROM organizations 
WHERE entity_id IS NOT NULL
ON CONFLICT (entity_id) DO NOTHING;

-- Update session_checkin_methods to include FACE_RECOGNITION
-- This will be handled by the application layer when creating new sessions

-- Create trigger to update face_updated_at timestamp
CREATE OR REPLACE FUNCTION update_face_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.face_encoding IS DISTINCT FROM NEW.face_encoding THEN
        NEW.face_updated_at = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_face_updated_at
    BEFORE UPDATE ON subscribers
    FOR EACH ROW
    EXECUTE FUNCTION update_face_updated_at();

-- Add verification query to check migration success
SELECT 
    'Face Recognition Migration Check' as check_type,
    COUNT(*) as total_subscribers,
    COUNT(profile_photo_path) as subscribers_with_photos,
    COUNT(face_encoding) as subscribers_with_face_encoding,
    (SELECT COUNT(*) FROM face_recognition_settings) as organizations_with_settings
FROM subscribers;
