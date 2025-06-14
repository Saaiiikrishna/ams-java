#!/usr/bin/env python3
"""
SeetaFace6 Setup Verification Script
Checks if all required SeetaFace6 components are properly installed
"""

import os
import sys
import platform
from pathlib import Path

def check_file_exists(file_path, description):
    """Check if a file exists and print status"""
    if os.path.exists(file_path):
        size = os.path.getsize(file_path)
        print(f"✅ {description}: {file_path} ({size:,} bytes)")
        return True
    else:
        print(f"❌ {description}: {file_path} (NOT FOUND)")
        return False

def main():
    print("🔍 SeetaFace6 Setup Verification")
    print("=" * 50)
    
    # Get project root directory
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    native_dir = project_root / "src" / "main" / "resources" / "native"
    
    print(f"Project Root: {project_root}")
    print(f"Native Directory: {native_dir}")
    print()
    
    # Check platform
    system = platform.system().lower()
    arch = platform.machine().lower()
    
    print(f"Platform: {system} {arch}")
    print()
    
    # Determine library directory based on platform
    if system == "windows":
        lib_dir = native_dir / "lib" / "windows" / "x64"
        required_libs = [
            ("SeetaFace.dll", "SeetaFace6 Core Library"),
            ("SeetaNet.dll", "SeetaFace6 Neural Network Library"),
            ("seetaface_jni.dll", "JNI Bridge Library")
        ]
    elif system == "linux":
        lib_dir = native_dir / "lib" / "linux" / "x64"
        required_libs = [
            ("libSeetaFace.so", "SeetaFace6 Core Library"),
            ("libSeetaNet.so", "SeetaFace6 Neural Network Library"),
            ("libseetaface_jni.so", "JNI Bridge Library")
        ]
    elif system == "darwin":  # macOS
        lib_dir = native_dir / "lib" / "macos"
        required_libs = [
            ("libSeetaFace.dylib", "SeetaFace6 Core Library"),
            ("libSeetaNet.dylib", "SeetaFace6 Neural Network Library"),
            ("libseetaface_jni.dylib", "JNI Bridge Library")
        ]
    else:
        print(f"❌ Unsupported platform: {system}")
        return False
    
    # Check native libraries
    print("📚 Checking Native Libraries:")
    print("-" * 30)
    
    lib_status = True
    for lib_file, description in required_libs:
        lib_path = lib_dir / lib_file
        if not check_file_exists(lib_path, description):
            lib_status = False
    
    print()
    
    # Check model files
    print("🧠 Checking Model Files:")
    print("-" * 25)
    
    models_dir = native_dir / "models"
    required_models = [
        ("face_detector.csta", "Face Detection Model"),
        ("face_landmarker_pts68.csta", "68-Point Facial Landmark Model"),
        ("face_recognizer.csta", "Face Recognition Model"),
        ("fas_first.csta", "Anti-Spoofing Model (Stage 1)"),
        ("fas_second.csta", "Anti-Spoofing Model (Stage 2)"),
        ("quality_lbn.csta", "Image Quality Assessment Model")
    ]
    
    optional_models = [
        ("face_recognizer_light.csta", "Lightweight Face Recognition Model"),
        ("gender_predictor.csta", "Gender Prediction Model"),
        ("age_predictor.csta", "Age Prediction Model"),
        ("mask_detector.csta", "Mask Detection Model"),
        ("face_recognizer_mask.csta", "Masked Face Recognition Model")
    ]
    
    model_status = True
    for model_file, description in required_models:
        model_path = models_dir / model_file
        if not check_file_exists(model_path, description):
            model_status = False
    
    print()
    print("🔧 Optional Model Files:")
    print("-" * 25)
    
    for model_file, description in optional_models:
        model_path = models_dir / model_file
        check_file_exists(model_path, description)
    
    print()
    
    # Check Java environment
    print("☕ Checking Java Environment:")
    print("-" * 30)
    
    java_home = os.environ.get('JAVA_HOME')
    if java_home:
        print(f"✅ JAVA_HOME: {java_home}")
        
        # Check for JNI headers
        jni_header = Path(java_home) / "include" / "jni.h"
        if jni_header.exists():
            print(f"✅ JNI Headers: {jni_header}")
        else:
            print(f"❌ JNI Headers: {jni_header} (NOT FOUND)")
            print("   Note: JNI headers are needed for building the JNI bridge")
    else:
        print("❌ JAVA_HOME: Not set")
        print("   Note: JAVA_HOME is needed for building the JNI bridge")
    
    print()
    
    # Overall status
    print("📋 Overall Status:")
    print("-" * 20)
    
    if lib_status and model_status:
        print("✅ SeetaFace6 setup is COMPLETE!")
        print("   All required libraries and models are present.")
        print("   The application should use SeetaFace6 native implementation.")
        return True
    elif model_status:
        print("⚠️  SeetaFace6 setup is PARTIAL")
        print("   Models are present but native libraries are missing.")
        print("   The application will use fallback implementation.")
        print("   To complete setup:")
        print("   1. Download SeetaFace6 development packages")
        print("   2. Build the JNI bridge library")
        print("   3. Copy libraries to the lib directory")
        return False
    else:
        print("❌ SeetaFace6 setup is INCOMPLETE")
        print("   Required components are missing.")
        print("   The application will use fallback implementation.")
        print("   Please follow the setup guide in scripts/download-seetaface6.md")
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
