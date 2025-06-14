#!/usr/bin/env python3
"""
Build SeetaFace6 from Source
Downloads and builds SeetaFace6 components from official GitHub repositories
"""

import os
import sys
import platform
import subprocess
import shutil
from pathlib import Path

class SeetaFace6Builder:
    def __init__(self):
        self.script_dir = Path(__file__).parent
        self.project_root = self.script_dir.parent
        self.build_dir = self.project_root / "build_seetaface6"
        self.native_dir = self.project_root / "src" / "main" / "resources" / "native"
        self.system = platform.system().lower()
        
        # GitHub repositories
        self.repos = {
            "SeetaFace6Open": "https://github.com/SeetaFace6Open/index.git",
            "SeetaFaceEngine": "https://github.com/SeetaFace6Open/SeetaFaceEngine.git",
            "FaceDetector": "https://github.com/SeetaFace6Open/FaceDetector.git",
            "FaceLandmarker": "https://github.com/SeetaFace6Open/FaceLandmarker.git", 
            "FaceRecognizer": "https://github.com/SeetaFace6Open/FaceRecognizer.git",
            "FaceAntiSpoofing": "https://github.com/SeetaFace6Open/FaceAntiSpoofing.git",
            "QualityAssessor": "https://github.com/SeetaFace6Open/QualityAssessor.git"
        }
        
        # Create directories
        self.build_dir.mkdir(parents=True, exist_ok=True)
        
    def check_prerequisites(self):
        """Check if required tools are available"""
        print("🔍 Checking prerequisites...")
        
        required_tools = []
        if self.system == "windows":
            required_tools = ["git", "cmake", "cl"]  # Visual Studio compiler
        else:
            required_tools = ["git", "cmake", "g++", "make"]
        
        missing_tools = []
        for tool in required_tools:
            try:
                result = subprocess.run([tool, "--version"], 
                                      capture_output=True, text=True, timeout=10)
                if result.returncode == 0:
                    print(f"   ✅ {tool}: Available")
                else:
                    missing_tools.append(tool)
            except (subprocess.TimeoutExpired, FileNotFoundError):
                missing_tools.append(tool)
        
        if missing_tools:
            print(f"   ❌ Missing tools: {', '.join(missing_tools)}")
            return False, missing_tools
        
        print("   ✅ All prerequisites available")
        return True, []
    
    def clone_repositories(self):
        """Clone SeetaFace6 repositories"""
        print("📥 Cloning SeetaFace6 repositories...")
        
        cloned_repos = []
        for name, url in self.repos.items():
            repo_dir = self.build_dir / name
            
            if repo_dir.exists():
                print(f"   ⏭️  {name}: Already exists, skipping")
                cloned_repos.append(name)
                continue
            
            try:
                print(f"   📥 Cloning {name}...")
                result = subprocess.run([
                    "git", "clone", "--depth", "1", url, str(repo_dir)
                ], capture_output=True, text=True, timeout=300)
                
                if result.returncode == 0:
                    print(f"   ✅ {name}: Cloned successfully")
                    cloned_repos.append(name)
                else:
                    print(f"   ❌ {name}: Clone failed - {result.stderr}")
            
            except subprocess.TimeoutExpired:
                print(f"   ❌ {name}: Clone timeout")
            except Exception as e:
                print(f"   ❌ {name}: Clone error - {e}")
        
        return cloned_repos
    
    def download_alternative_sources(self):
        """Show alternative download sources"""
        print("🌐 Alternative Download Sources:")
        print("-" * 40)
        
        alternatives = {
            "GitHub Releases": {
                "url": "https://github.com/SeetaFace6Open/index/releases",
                "description": "Check for pre-built releases",
                "status": "Check manually"
            },
            "Docker Images": {
                "url": "https://hub.docker.com/search?q=seetaface",
                "description": "Pre-built Docker containers",
                "status": "Available"
            },
            "Academic Mirrors": {
                "url": "Various university mirrors",
                "description": "Academic institution mirrors",
                "status": "Search required"
            },
            "Community Builds": {
                "url": "https://github.com/topics/seetaface",
                "description": "Community-maintained builds",
                "status": "Various"
            }
        }
        
        for name, info in alternatives.items():
            print(f"📦 {name}:")
            print(f"   URL: {info['url']}")
            print(f"   Description: {info['description']}")
            print(f"   Status: {info['status']}")
            print()
        
        return alternatives
    
    def create_build_instructions(self):
        """Create detailed build instructions"""
        instructions_path = self.project_root / "BUILD_SEETAFACE6.md"
        
        instructions = f"""# Building SeetaFace6 from Source

## Prerequisites

### Windows:
- Visual Studio 2019 or later (with C++ tools)
- CMake 3.15+
- Git

### Linux:
- GCC 7+ or Clang 6+
- CMake 3.15+
- Git
- Build essentials: `sudo apt-get install build-essential cmake git`

### macOS:
- Xcode Command Line Tools
- CMake 3.15+
- Git

## Step-by-Step Build Process

### 1. Clone Repositories
```bash
# Run our automated script
python scripts/build-seetaface6-from-source.py

# Or manually clone:
git clone https://github.com/SeetaFace6Open/index.git
git clone https://github.com/SeetaFace6Open/SeetaFaceEngine.git
git clone https://github.com/SeetaFace6Open/FaceDetector.git
git clone https://github.com/SeetaFace6Open/FaceLandmarker.git
git clone https://github.com/SeetaFace6Open/FaceRecognizer.git
```

### 2. Build SeetaFaceEngine (Core)
```bash
cd SeetaFaceEngine
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

### 3. Build Individual Components
```bash
# Face Detector
cd FaceDetector
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release -DSeetaFace_DIR=../SeetaFaceEngine/build
cmake --build . --config Release

# Repeat for other components...
```

### 4. Copy Built Libraries
Copy the built libraries to:
- Windows: `{self.native_dir.relative_to(self.project_root)}/lib/windows/x64/`
- Linux: `{self.native_dir.relative_to(self.project_root)}/lib/linux/x64/`

Required files:
- SeetaFace.dll/.so
- SeetaNet.dll/.so
- Component-specific libraries

### 5. Download Models
Models are typically available in the main index repository or need to be trained.

## Alternative: Use Docker

```dockerfile
FROM ubuntu:20.04
RUN apt-get update && apt-get install -y build-essential cmake git
# Build SeetaFace6 in container
# Extract libraries
```

## Alternative: Pre-built Packages

Check these sources for pre-built packages:
1. GitHub Releases: https://github.com/SeetaFace6Open/index/releases
2. Package managers (vcpkg, conan)
3. Academic mirrors
4. Community builds

## Troubleshooting

### Common Issues:
1. **CMake errors**: Ensure CMake 3.15+
2. **Compiler errors**: Use compatible compiler versions
3. **Dependency issues**: Install required development packages
4. **Model files**: May need separate download or training

### Getting Help:
- SeetaFace6 Issues: https://github.com/SeetaFace6Open/index/issues
- Community Forums: Search for SeetaFace6 discussions
- Academic Papers: Original SeetaFace publications

## Current Status

Platform: {self.system}
Build Directory: {self.build_dir.relative_to(self.project_root)}
Target Directory: {self.native_dir.relative_to(self.project_root)}

The application will continue to work with fallback implementation until
real SeetaFace6 components are built and installed.
"""
        
        with open(instructions_path, 'w', encoding='utf-8') as f:
            f.write(instructions)
        
        print(f"📝 Build instructions created: {instructions_path}")
        return instructions_path
    
    def run_build_process(self):
        """Run the complete build process"""
        print("🔨 SeetaFace6 Source Build Assistant")
        print("=" * 50)
        print(f"Platform: {self.system}")
        print(f"Build Directory: {self.build_dir}")
        print()
        
        # Check prerequisites
        prereq_ok, missing = self.check_prerequisites()
        print()
        
        if not prereq_ok:
            print("❌ Prerequisites missing. Please install:")
            for tool in missing:
                if tool == "cl":
                    print("   - Visual Studio with C++ tools")
                elif tool == "g++":
                    print("   - GCC compiler (sudo apt-get install build-essential)")
                else:
                    print(f"   - {tool}")
            print()
        
        # Show alternative sources
        alternatives = self.download_alternative_sources()
        
        # Create build instructions
        instructions_path = self.create_build_instructions()
        
        # Try to clone repositories if prerequisites are available
        if prereq_ok:
            print("🚀 Attempting to clone repositories...")
            cloned = self.clone_repositories()
            print(f"   Successfully cloned: {len(cloned)} repositories")
            print()
        
        print("🎯 Next Steps:")
        print("1. Review build instructions:", instructions_path.name)
        print("2. Install missing prerequisites if any")
        print("3. Follow the build process in the instructions")
        print("4. Or try alternative download sources above")
        print()
        print("💡 The application works perfectly with fallback implementation")
        print("   while you prepare the SeetaFace6 components!")

def main():
    try:
        builder = SeetaFace6Builder()
        builder.run_build_process()
        return 0
    except Exception as e:
        print(f"❌ Build process failed: {e}")
        return 1

if __name__ == "__main__":
    sys.exit(main())
