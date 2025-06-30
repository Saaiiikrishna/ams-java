#include <jni.h>
#include <string>
#include <vector>
#include <memory>
#include <iostream>

// SeetaFace6 includes
#include "seeta/FaceDetector.h"
#include "seeta/FaceLandmarker.h"
#include "seeta/FaceRecognizer.h"
#include "seeta/FaceAntiSpoofing.h"
#include "seeta/QualityAssessor.h"

// Engine structure to hold SeetaFace6 components
struct SeetaFaceEngine {
    std::unique_ptr<seeta::FaceDetector> detector;
    std::unique_ptr<seeta::FaceLandmarker> landmarker;
    std::unique_ptr<seeta::FaceRecognizer> recognizer;
    std::unique_ptr<seeta::FaceAntiSpoofing> antiSpoofing;
    std::unique_ptr<seeta::QualityAssessor> qualityAssessor;
    
    bool initialized = false;
    std::string lastError;
};

// Global engine storage
static std::vector<std::unique_ptr<SeetaFaceEngine>> engines;
static std::string globalLastError;

// Helper function to convert Java string to C++ string
std::string jstring2string(JNIEnv *env, jstring jStr) {
    if (!jStr) return "";
    
    const jclass stringClass = env->GetObjectClass(jStr);
    const jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(jStr, getBytes, env->NewStringUTF("UTF-8"));
    
    size_t length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte* pBytes = env->GetByteArrayElements(stringJbytes, NULL);
    
    std::string ret = std::string((char*)pBytes, length);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);
    
    env->DeleteLocalRef(stringJbytes);
    env->DeleteLocalRef(stringClass);
    return ret;
}

// Helper function to convert byte array to SeetaNet::ImageData
seeta::cv::ImageData convertToImageData(JNIEnv *env, jbyteArray imageData, jint width, jint height, jint channels) {
    jbyte* imageBytes = env->GetByteArrayElements(imageData, NULL);
    
    seeta::cv::ImageData image;
    image.width = width;
    image.height = height;
    image.channels = channels;
    image.data = reinterpret_cast<uint8_t*>(imageBytes);
    
    return image;
}

extern "C" {

/*
 * Class:     com_example_attendancesystem_facerecognition_SeetaFaceJNI
 * Method:    initializeEngine
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_example_attendancesystem_facerecognition_SeetaFaceJNI_initializeEngine
  (JNIEnv *env, jclass clazz, jstring detectorModelPath, jstring landmarkModelPath, 
   jstring recognizerModelPath, jstring antiSpoofingModelPath) {
    
    try {
        auto engine = std::make_unique<SeetaFaceEngine>();
        
        // Convert Java strings to C++ strings
        std::string detectorPath = jstring2string(env, detectorModelPath);
        std::string landmarkPath = jstring2string(env, landmarkModelPath);
        std::string recognizerPath = jstring2string(env, recognizerModelPath);
        std::string antiSpoofPath = jstring2string(env, antiSpoofingModelPath);
        
        // Initialize Face Detector
        seeta::ModelSetting detectorSetting;
        detectorSetting.append(detectorPath);
        detectorSetting.set_device(seeta::ModelSetting::CPU);
        engine->detector = std::make_unique<seeta::FaceDetector>(detectorSetting);
        
        // Initialize Face Landmarker
        seeta::ModelSetting landmarkSetting;
        landmarkSetting.append(landmarkPath);
        landmarkSetting.set_device(seeta::ModelSetting::CPU);
        engine->landmarker = std::make_unique<seeta::FaceLandmarker>(landmarkSetting);
        
        // Initialize Face Recognizer
        seeta::ModelSetting recognizerSetting;
        recognizerSetting.append(recognizerPath);
        recognizerSetting.set_device(seeta::ModelSetting::CPU);
        engine->recognizer = std::make_unique<seeta::FaceRecognizer>(recognizerSetting);
        
        // Initialize Anti-Spoofing (optional)
        if (!antiSpoofPath.empty()) {
            seeta::ModelSetting antiSpoofSetting;
            antiSpoofSetting.append(antiSpoofPath);
            antiSpoofSetting.set_device(seeta::ModelSetting::CPU);
            engine->antiSpoofing = std::make_unique<seeta::FaceAntiSpoofing>(antiSpoofSetting);
        }
        
        engine->initialized = true;
        
        // Store engine and return handle
        engines.push_back(std::move(engine));
        return static_cast<jlong>(engines.size() - 1);
        
    } catch (const std::exception& e) {
        globalLastError = "Failed to initialize SeetaFace6 engine: " + std::string(e.what());
        return -1;
    }
}

/*
 * Class:     com_example_attendancesystem_facerecognition_SeetaFaceJNI
 * Method:    releaseEngine
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_example_attendancesystem_facerecognition_SeetaFaceJNI_releaseEngine
  (JNIEnv *env, jclass clazz, jlong engineHandle) {
    
    try {
        if (engineHandle >= 0 && engineHandle < engines.size()) {
            engines[engineHandle].reset();
        }
    } catch (const std::exception& e) {
        globalLastError = "Failed to release engine: " + std::string(e.what());
    }
}

/*
 * Class:     com_example_attendancesystem_facerecognition_SeetaFaceJNI
 * Method:    detectFaces
 * Signature: (J[BIII)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_example_attendancesystem_facerecognition_SeetaFaceJNI_detectFaces
  (JNIEnv *env, jclass clazz, jlong engineHandle, jbyteArray imageData, jint width, jint height, jint channels) {
    
    try {
        if (engineHandle < 0 || engineHandle >= engines.size() || !engines[engineHandle] || !engines[engineHandle]->initialized) {
            globalLastError = "Invalid engine handle";
            return nullptr;
        }
        
        auto& engine = engines[engineHandle];
        seeta::cv::ImageData image = convertToImageData(env, imageData, width, height, channels);
        
        // Detect faces
        auto faces = engine->detector->detect(image);
        
        // Convert results to float array [x1, y1, w1, h1, conf1, x2, y2, w2, h2, conf2, ...]
        std::vector<float> results;
        for (const auto& face : faces) {
            results.push_back(static_cast<float>(face.pos.x));
            results.push_back(static_cast<float>(face.pos.y));
            results.push_back(static_cast<float>(face.pos.width));
            results.push_back(static_cast<float>(face.pos.height));
            results.push_back(face.score);
        }
        
        // Create Java float array
        jfloatArray result = env->NewFloatArray(results.size());
        env->SetFloatArrayRegion(result, 0, results.size(), results.data());
        
        // Release image data
        env->ReleaseByteArrayElements(imageData, reinterpret_cast<jbyte*>(image.data), JNI_ABORT);
        
        return result;
        
    } catch (const std::exception& e) {
        globalLastError = "Face detection failed: " + std::string(e.what());
        return nullptr;
    }
}

/*
 * Class:     com_example_attendancesystem_facerecognition_SeetaFaceJNI
 * Method:    extractFaceEncoding
 * Signature: (J[BIIIIII)[F
 */
JNIEXPORT jfloatArray JNICALL Java_com_example_attendancesystem_facerecognition_SeetaFaceJNI_extractFaceEncoding
  (JNIEnv *env, jclass clazz, jlong engineHandle, jbyteArray imageData, jint width, jint height, jint channels,
   jint faceX, jint faceY, jint faceWidth, jint faceHeight) {
    
    try {
        if (engineHandle < 0 || engineHandle >= engines.size() || !engines[engineHandle] || !engines[engineHandle]->initialized) {
            globalLastError = "Invalid engine handle";
            return nullptr;
        }
        
        auto& engine = engines[engineHandle];
        seeta::cv::ImageData image = convertToImageData(env, imageData, width, height, channels);
        
        // Create face rectangle
        seeta::cv::Rect faceRect(faceX, faceY, faceWidth, faceHeight);
        
        // Extract landmarks
        auto landmarks = engine->landmarker->mark(image, faceRect);
        
        // Extract face encoding
        auto encoding = engine->recognizer->Extract(image, landmarks);
        
        // Convert to Java float array
        jfloatArray result = env->NewFloatArray(encoding.size());
        env->SetFloatArrayRegion(result, 0, encoding.size(), encoding.data());
        
        // Release image data
        env->ReleaseByteArrayElements(imageData, reinterpret_cast<jbyte*>(image.data), JNI_ABORT);
        
        return result;
        
    } catch (const std::exception& e) {
        globalLastError = "Face encoding extraction failed: " + std::string(e.what());
        return nullptr;
    }
}

/*
 * Class:     com_example_attendancesystem_facerecognition_SeetaFaceJNI
 * Method:    compareFaceEncodings
 * Signature: ([F[F)F
 */
JNIEXPORT jfloat JNICALL Java_com_example_attendancesystem_facerecognition_SeetaFaceJNI_compareFaceEncodings
  (JNIEnv *env, jclass clazz, jfloatArray encoding1, jfloatArray encoding2) {
    
    try {
        jsize len1 = env->GetArrayLength(encoding1);
        jsize len2 = env->GetArrayLength(encoding2);
        
        if (len1 != len2) {
            globalLastError = "Encoding lengths do not match";
            return 0.0f;
        }
        
        jfloat* enc1 = env->GetFloatArrayElements(encoding1, NULL);
        jfloat* enc2 = env->GetFloatArrayElements(encoding2, NULL);
        
        // Calculate similarity using SeetaFace6's method
        float similarity = seeta::FaceRecognizer::similarity(enc1, enc2);
        
        env->ReleaseFloatArrayElements(encoding1, enc1, JNI_ABORT);
        env->ReleaseFloatArrayElements(encoding2, enc2, JNI_ABORT);
        
        return similarity;
        
    } catch (const std::exception& e) {
        globalLastError = "Face encoding comparison failed: " + std::string(e.what());
        return 0.0f;
    }
}

/*
 * Class:     com_example_attendancesystem_facerecognition_SeetaFaceJNI
 * Method:    detectLiveness
 * Signature: (J[BIIIIII)F
 */
JNIEXPORT jfloat JNICALL Java_com_example_attendancesystem_facerecognition_SeetaFaceJNI_detectLiveness
  (JNIEnv *env, jclass clazz, jlong engineHandle, jbyteArray imageData, jint width, jint height, jint channels,
   jint faceX, jint faceY, jint faceWidth, jint faceHeight) {
    
    try {
        if (engineHandle < 0 || engineHandle >= engines.size() || !engines[engineHandle] || !engines[engineHandle]->initialized) {
            globalLastError = "Invalid engine handle";
            return 0.0f;
        }
        
        auto& engine = engines[engineHandle];
        if (!engine->antiSpoofing) {
            globalLastError = "Anti-spoofing not initialized";
            return 1.0f; // Assume live if anti-spoofing not available
        }
        
        seeta::cv::ImageData image = convertToImageData(env, imageData, width, height, channels);
        seeta::cv::Rect faceRect(faceX, faceY, faceWidth, faceHeight);
        
        // Perform liveness detection
        auto result = engine->antiSpoofing->Predict(image, faceRect);
        
        // Release image data
        env->ReleaseByteArrayElements(imageData, reinterpret_cast<jbyte*>(image.data), JNI_ABORT);
        
        return result.score;
        
    } catch (const std::exception& e) {
        globalLastError = "Liveness detection failed: " + std::string(e.what());
        return 0.0f;
    }
}

/*
 * Class:     com_example_attendancesystem_facerecognition_SeetaFaceJNI
 * Method:    assessImageQuality
 * Signature: ([BIII)F
 */
JNIEXPORT jfloat JNICALL Java_com_example_attendancesystem_facerecognition_SeetaFaceJNI_assessImageQuality
  (JNIEnv *env, jclass clazz, jbyteArray imageData, jint width, jint height, jint channels) {
    
    try {
        seeta::cv::ImageData image = convertToImageData(env, imageData, width, height, channels);
        
        // Simple quality assessment based on image properties
        // This is a placeholder - you can implement more sophisticated quality assessment
        float quality = 1.0f;
        
        // Check resolution
        if (width < 100 || height < 100) quality *= 0.5f;
        else if (width < 200 || height < 200) quality *= 0.8f;
        
        // Check aspect ratio
        float aspectRatio = static_cast<float>(width) / height;
        if (aspectRatio < 0.5f || aspectRatio > 2.0f) quality *= 0.7f;
        
        // Release image data
        env->ReleaseByteArrayElements(imageData, reinterpret_cast<jbyte*>(image.data), JNI_ABORT);
        
        return quality;
        
    } catch (const std::exception& e) {
        globalLastError = "Image quality assessment failed: " + std::string(e.what());
        return 0.0f;
    }
}

/*
 * Class:     com_example_attendancesystem_facerecognition_SeetaFaceJNI
 * Method:    getLastError
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_example_attendancesystem_facerecognition_SeetaFaceJNI_getLastError
  (JNIEnv *env, jclass clazz) {
    
    return env->NewStringUTF(globalLastError.c_str());
}

} // extern "C"
