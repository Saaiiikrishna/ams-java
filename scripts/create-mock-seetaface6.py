#!/usr/bin/env python3
"""
Create Mock SeetaFace6 Files for Testing
Creates placeholder files to test the integration without real SeetaFace6 components
"""

import os
import sys
import platform
from pathlib import Path

class MockSeetaFace6Creator:
    def __init__(self):
        self.script_dir = Path(__file__).parent
        self.project_root = self.script_dir.parent
        self.native_dir = self.project_root / "src" / "main" / "resources" / "native"
        self.system = platform.system().lower()
        
        # Platform-specific paths
        if self.system == "windows":
            self.lib_dir = self.native_dir / "lib" / "windows" / "x64"
        elif self.system == "linux":
            self.lib_dir = self.native_dir / "lib" / "linux" / "x64"
        else:
            self.lib_dir = self.native_dir / "lib" / "macos"
        
        self.models_dir = self.native_dir / "models"
        
        # Ensure directories exist
        self.lib_dir.mkdir(parents=True, exist_ok=True)
        self.models_dir.mkdir(parents=True, exist_ok=True)
    
    def create_mock_libraries(self):
        """Create mock library files for testing"""
        print(f"📚 Creating mock libraries for {self.system}...")
        
        if self.system == "windows":
            mock_libs = [
                "SeetaFace.dll",
                "SeetaNet.dll",
                "seetaface_jni.dll"
            ]
        elif self.system == "linux":
            mock_libs = [
                "libSeetaFace.so",
                "libSeetaNet.so", 
                "libseetaface_jni.so"
            ]
        else:  # macOS
            mock_libs = [
                "libSeetaFace.dylib",
                "libSeetaNet.dylib",
                "libseetaface_jni.dylib"
            ]
        
        for lib_name in mock_libs:
            lib_path = self.lib_dir / lib_name
            
            # Create mock library content
            mock_content = f"""
# Mock {lib_name} for testing
# This is a placeholder file created for testing the SeetaFace6 integration
# Replace with actual SeetaFace6 library files for production use

Platform: {self.system}
Created: {self.get_timestamp()}
Purpose: Testing integration without real SeetaFace6 components

Note: This file should be replaced with the actual SeetaFace6 library
downloaded from the official sources.
""".strip()
            
            with open(lib_path, 'w', encoding='utf-8') as f:
                f.write(mock_content)
            
            print(f"   ✅ Created: {lib_name}")
        
        return len(mock_libs)
    
    def create_mock_models(self):
        """Create mock model files for testing"""
        print("🧠 Creating mock model files...")
        
        mock_models = [
            ("face_detector.csta", "Face Detection Model"),
            ("face_landmarker_pts68.csta", "68-Point Facial Landmark Model"),
            ("face_recognizer.csta", "Face Recognition Model"),
            ("fas_first.csta", "Anti-Spoofing Model (Stage 1)"),
            ("fas_second.csta", "Anti-Spoofing Model (Stage 2)"),
            ("quality_lbn.csta", "Image Quality Assessment Model"),
            ("face_recognizer_light.csta", "Lightweight Face Recognition Model")
        ]
        
        for model_name, description in mock_models:
            model_path = self.models_dir / model_name
            
            # Create mock model content (binary-like data)
            mock_content = f"""
# Mock {model_name} for testing
# {description}
# This is a placeholder file created for testing the SeetaFace6 integration

Platform: {self.system}
Created: {self.get_timestamp()}
Model Type: {description}
Purpose: Testing integration without real SeetaFace6 models

Note: This file should be replaced with the actual SeetaFace6 model
downloaded from the official sources.

Mock binary data follows:
""".strip()
            
            # Add some mock binary-like data
            mock_binary = b'\x00\x01\x02\x03' * 1000  # 4KB of mock data
            
            with open(model_path, 'wb') as f:
                f.write(mock_content.encode('utf-8'))
                f.write(b'\n\n')
                f.write(mock_binary)
            
            print(f"   ✅ Created: {model_name} ({len(mock_content) + len(mock_binary)} bytes)")
        
        return len(mock_models)
    
    def create_readme(self):
        """Create README for mock files"""
        readme_path = self.native_dir / "MOCK_FILES_README.md"
        
        readme_content = f"""# Mock SeetaFace6 Files

⚠️ **WARNING: These are MOCK files for testing purposes only!**

## What are these files?

These files are placeholders created to test the SeetaFace6 integration without requiring the actual SeetaFace6 components. They allow you to:

1. Test the application startup and initialization
2. Verify the directory structure is correct
3. Test the fallback behavior
4. Ensure the JNI interface is properly configured

## What they are NOT:

- ❌ Real SeetaFace6 libraries
- ❌ Functional face recognition models
- ❌ Production-ready components

## For Production Use:

To use real SeetaFace6 components:

1. **Delete these mock files**
2. **Download real SeetaFace6 components** from official sources:
   - Windows libs: https://pan.baidu.com/s/1_rFID6k6Istmu8QJkHpbFw (code: iqjk)
   - Linux libs: https://pan.baidu.com/s/1tOq12SdpUtuybe48cMuwag (code: lc44)
   - Models Part I: https://pan.baidu.com/s/1LlXe2-YsUxQMe-MLzhQ2Aw (code: ngne)
   - Models Part II: https://pan.baidu.com/s/1xjciq-lkzEBOZsTfVYAT9g (code: t6j0)

3. **Extract and copy** the real files to replace these mock files
4. **Build the JNI bridge** using the provided build scripts
5. **Test with real face recognition** functionality

## Current Mock Files:

### Libraries ({self.lib_dir.relative_to(self.project_root)}):
- Mock library files for {self.system} platform
- These will NOT provide actual face recognition functionality

### Models ({self.models_dir.relative_to(self.project_root)}):
- Mock model files with placeholder data
- These will NOT perform actual face detection/recognition

## Testing:

With these mock files, you can:
- ✅ Start the application successfully
- ✅ See SeetaFace6 initialization attempts
- ✅ Test the fallback implementation
- ✅ Verify API endpoints work correctly

## Next Steps:

1. Run verification: `python scripts/verify-seetaface6-setup.py`
2. Start the application: `./mvnw spring-boot:run`
3. Check logs for SeetaFace6 initialization messages
4. Test face recognition endpoints (will use fallback)

Created: {self.get_timestamp()}
Platform: {self.system}
"""
        
        with open(readme_path, 'w', encoding='utf-8') as f:
            f.write(readme_content)
        
        print(f"📝 Created README: {readme_path.name}")
        return readme_path
    
    def get_timestamp(self):
        """Get current timestamp"""
        from datetime import datetime
        return datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    def run_creation(self):
        """Run the mock file creation process"""
        print("🎭 SeetaFace6 Mock File Creator")
        print("=" * 50)
        print(f"Platform: {self.system}")
        print(f"Target Directory: {self.native_dir}")
        print()
        
        print("⚠️  WARNING: Creating MOCK files for testing only!")
        print("   These are NOT real SeetaFace6 components!")
        print()
        
        # Create mock libraries
        lib_count = self.create_mock_libraries()
        print()
        
        # Create mock models
        model_count = self.create_mock_models()
        print()
        
        # Create README
        readme_path = self.create_readme()
        print()
        
        print("✅ Mock file creation completed!")
        print(f"   Libraries created: {lib_count}")
        print(f"   Models created: {model_count}")
        print(f"   README: {readme_path.name}")
        print()
        
        print("🎯 Next Steps:")
        print("1. Run verification: python scripts/verify-seetaface6-setup.py")
        print("2. Start application: ./mvnw spring-boot:run")
        print("3. Check logs for SeetaFace6 initialization")
        print("4. Test face recognition endpoints")
        print()
        
        print("🔄 To use real SeetaFace6:")
        print("1. Delete mock files")
        print("2. Download real components from official sources")
        print("3. Follow SEETAFACE6_SETUP.md instructions")

def main():
    try:
        creator = MockSeetaFace6Creator()
        creator.run_creation()
        return 0
    except Exception as e:
        print(f"❌ Mock creation failed: {e}")
        return 1

if __name__ == "__main__":
    sys.exit(main())
