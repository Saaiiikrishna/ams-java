#!/usr/bin/env python3
"""
SeetaFace6 Setup and Download Assistant
Automates the download and setup of SeetaFace6 components
"""

import os
import sys
import platform
import shutil
from pathlib import Path

class SeetaFace6Setup:
    def __init__(self):
        self.script_dir = Path(__file__).parent
        self.project_root = self.script_dir.parent
        self.native_dir = self.project_root / "src" / "main" / "resources" / "native"
        self.system = platform.system().lower()
        self.arch = platform.machine().lower()
        
        # Create directories
        self.lib_dir = self.native_dir / "lib" / self.get_platform_dir() / "x64"
        self.models_dir = self.native_dir / "models"
        self.temp_dir = self.project_root / "temp_seetaface6"
        
        # Ensure directories exist
        self.lib_dir.mkdir(parents=True, exist_ok=True)
        self.models_dir.mkdir(parents=True, exist_ok=True)
        self.temp_dir.mkdir(parents=True, exist_ok=True)
    
    def get_platform_dir(self):
        if self.system == "windows":
            return "windows"
        elif self.system == "linux":
            return "linux"
        elif self.system == "darwin":
            return "macos"
        else:
            raise ValueError(f"Unsupported platform: {self.system}")
    
    def print_banner(self):
        print("=" * 60)
        print("🚀 SeetaFace6 Setup Assistant")
        print("=" * 60)
        print(f"Platform: {self.system} {self.arch}")
        print(f"Project Root: {self.project_root}")
        print(f"Native Directory: {self.native_dir}")
        print()
    
    def check_existing_files(self):
        """Check for existing SeetaFace6 files"""
        print("🔍 Checking existing files...")

        # Check libraries
        lib_files = list(self.lib_dir.glob("*"))
        model_files = list(self.models_dir.glob("*.csta"))

        print(f"   Libraries found: {len(lib_files)}")
        for lib in lib_files:
            print(f"     - {lib.name}")

        print(f"   Models found: {len(model_files)}")
        for model in model_files:
            print(f"     - {model.name}")

        return len(lib_files), len(model_files)
    
    def show_download_options(self):
        """Show available download options"""
        print("📥 Download Options:")
        print("-" * 30)

        print("🌐 Option 1: Official Baidu Pan (Recommended)")
        print("   - Most up-to-date versions")
        print("   - Official support")
        print("   - Requires Baidu Pan account")
        print()

        print("🔗 Option 2: Alternative Sources")
        print("   - GitHub releases (if available)")
        print("   - Community mirrors")
        print("   - May have older versions")
        print()

        print("🛠️ Option 3: Build from Source")
        print("   - Latest development version")
        print("   - Requires build tools")
        print("   - More complex setup")
        print()
    
    def download_from_official_sources(self):
        """Download from official Baidu Pan sources"""
        print("📋 Official SeetaFace6 Download Links:")
        print("-" * 40)
        
        official_links = {
            "Windows Development Package": {
                "url": "https://pan.baidu.com/s/1_rFID6k6Istmu8QJkHpbFw",
                "code": "iqjk",
                "description": "Windows x64 libraries (SeetaFace.dll, SeetaNet.dll, etc.)"
            },
            "Linux Development Package": {
                "url": "https://pan.baidu.com/s/1tOq12SdpUtuybe48cMuwag", 
                "code": "lc44",
                "description": "Linux x64 libraries (libSeetaFace.so, libSeetaNet.so, etc.)"
            },
            "Model Files Part I": {
                "url": "https://pan.baidu.com/s/1LlXe2-YsUxQMe-MLzhQ2Aw",
                "code": "ngne", 
                "description": "Core models (face_detector.csta, face_recognizer.csta, etc.)"
            },
            "Model Files Part II": {
                "url": "https://pan.baidu.com/s/1xjciq-lkzEBOZsTfVYAT9g",
                "code": "t6j0",
                "description": "Additional models (face_recognizer_light.csta, etc.)"
            }
        }
        
        for name, info in official_links.items():
            print(f"📦 {name}:")
            print(f"   URL: {info['url']}")
            print(f"   Code: {info['code']}")
            print(f"   Description: {info['description']}")
            print()
        
        return official_links
    
    def create_manual_setup_guide(self):
        """Create manual setup instructions"""
        guide_path = self.project_root / "SEETAFACE6_SETUP.md"
        
        guide_content = f"""# SeetaFace6 Manual Setup Guide

## Required Downloads

### 1. Platform-Specific Libraries

**For Windows (x64):**
- Download: [Baidu Pan](https://pan.baidu.com/s/1_rFID6k6Istmu8QJkHpbFw) code: `iqjk`
- Extract and copy these files to: `{self.lib_dir.relative_to(self.project_root)}/`
  - SeetaFace.dll
  - SeetaNet.dll
  - opencv_world*.dll (if included)
  - Any other .dll dependencies

**For Linux (x64):**
- Download: [Baidu Pan](https://pan.baidu.com/s/1tOq12SdpUtuybe48cMuwag) code: `lc44`
- Extract and copy these files to: `{self.lib_dir.relative_to(self.project_root)}/`
  - libSeetaFace.so
  - libSeetaNet.so
  - Any other .so dependencies

### 2. Model Files

**Core Models (Required):**
- Download: [Baidu Pan](https://pan.baidu.com/s/1LlXe2-YsUxQMe-MLzhQ2Aw) code: `ngne`
- Extract and copy these files to: `{self.models_dir.relative_to(self.project_root)}/`
  - face_detector.csta
  - face_landmarker_pts68.csta
  - face_recognizer.csta
  - fas_first.csta
  - fas_second.csta
  - quality_lbn.csta

**Additional Models (Optional):**
- Download: [Baidu Pan](https://pan.baidu.com/s/1xjciq-lkzEBOZsTfVYAT9g) code: `t6j0`
- Extract and copy to: `{self.models_dir.relative_to(self.project_root)}/`
  - face_recognizer_light.csta

## Verification

After copying files, run:
```bash
python scripts/verify-seetaface6-setup.py
```

## Building JNI Bridge

After setting up libraries and models:
```bash
# Windows
scripts/build-jni-windows.bat

# Linux
scripts/build-jni-linux.sh
```

## Directory Structure

Your final structure should look like:
```
{self.native_dir.relative_to(self.project_root)}/
├── lib/
│   └── {self.get_platform_dir()}/
│       └── x64/
│           ├── SeetaFace.dll (Windows) or libSeetaFace.so (Linux)
│           ├── SeetaNet.dll (Windows) or libSeetaNet.so (Linux)
│           └── [other dependencies]
└── models/
    ├── face_detector.csta
    ├── face_landmarker_pts68.csta
    ├── face_recognizer.csta
    ├── fas_first.csta
    ├── fas_second.csta
    └── quality_lbn.csta
```

## Troubleshooting

1. **Library not found errors:**
   - Ensure all .dll/.so files are in the correct directory
   - Check file permissions (Linux/macOS)
   - Verify architecture matches (x64)

2. **Model loading errors:**
   - Verify all .csta files are present and not corrupted
   - Check file sizes match expected values
   - Ensure sufficient disk space

3. **JNI compilation errors:**
   - Verify JAVA_HOME is set correctly
   - Install required build tools (Visual Studio/GCC)
   - Check CMake is installed and accessible

## Support

For issues with SeetaFace6:
- Official repository: https://github.com/SeetaFace6Open/index
- Commercial support: bd@seetatech.com
"""
        
        with open(guide_path, 'w', encoding='utf-8') as f:
            f.write(guide_content)
        
        print(f"📝 Manual setup guide created: {guide_path}")
        return guide_path
    
    def run_setup(self):
        """Run the complete setup process"""
        self.print_banner()
        
        # Check for existing files
        lib_count, model_count = self.check_existing_files()
        print()

        # Show download options
        self.show_download_options()
        
        # Show official download links
        official_links = self.download_from_official_sources()
        
        # Create manual setup guide
        guide_path = self.create_manual_setup_guide()
        
        print("🎯 Next Steps:")
        print("1. Download files using the links above")
        print("2. Extract and copy files to the specified directories")
        print("3. Run verification: python scripts/verify-seetaface6-setup.py")
        print("4. Build JNI bridge using the provided scripts")
        print()
        print(f"📖 Detailed instructions: {guide_path}")
        
        # Cleanup
        if self.temp_dir.exists():
            shutil.rmtree(self.temp_dir)

def main():
    try:
        setup = SeetaFace6Setup()
        setup.run_setup()
        return 0
    except Exception as e:
        print(f"❌ Setup failed: {e}")
        return 1

if __name__ == "__main__":
    sys.exit(main())
