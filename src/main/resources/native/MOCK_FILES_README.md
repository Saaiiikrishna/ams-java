# Mock SeetaFace6 Files

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

### Libraries (src\main\resources\native\lib\windows\x64):
- Mock library files for windows platform
- These will NOT provide actual face recognition functionality

### Models (src\main\resources\native\models):
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

Created: 2025-06-13 20:17:14
Platform: windows
