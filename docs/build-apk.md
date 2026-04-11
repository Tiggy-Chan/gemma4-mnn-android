# Build Android APK with Gemma 4 Support

The official MNN Chat app (v0.8.2.2 on Google Play) does not yet include Gemma 4 support. This guide shows how to build a custom APK using the MNN source code which already has Gemma 4 support (commit `ba76938`, merged 2026-04-09).

## Prerequisites

| Tool | Version | How to Install |
|------|---------|---------------|
| NDK | r27c | `sdkmanager "ndk;27.2.12479018"` |
| JDK | 17 | `sdkmanager "commandlinetools"` or your package manager |
| Android SDK | API 24+ | via Android Studio or `sdkmanager` |
| CMake | 3.22+ | `apt install cmake` or `pip install cmake` |
| Gradle | 8.9+ | bundled with Android Studio |

Set environment variables:

```bash
export ANDROID_NDK=/path/to/android-ndk-r27c
export ANDROID_HOME=/path/to/Android/Sdk
export JAVA_HOME=/path/to/jdk-17
```

## Step 1: Build MNN for Android

```bash
cd MNN
mkdir -p build_android && cd build_android

cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-24 \
  -DMNN_LOW_MEMORY=true \
  -DMNN_CPU_WEIGHT_DEQUANT_GEMM=true \
  -DMNN_BUILD_LLM=true \
  -DMNN_SUPPORT_TRANSFORMER_FUSE=true \
  -DMNN_SEP_BUILD=OFF \
  -DMNN_BUILD_FOR_ANDROID_COMMAND=true

make -j$(nproc)
```

**Why `SEP_BUILD=OFF`**: The default `ON` splits MNN into multiple `.so` files, but the Android app's TTS module expects Express symbols in `libMNN.so`. Setting it `OFF` produces a unified `libMNN.so` with all symbols.

## Step 2: Copy `.so` Files to Android Project

The Android project expects three `.so` files. With `SEP_BUILD=OFF` we only have one, so we copy it three times:

```bash
JNI_DIR=../apps/Android/MnnLlmChat/app/src/main/jniLibs/arm64-v8a
mkdir -p "$JNI_DIR"

cp libMNN.so "$JNI_DIR/libMNN.so"
cp libMNN.so "$JNI_DIR/libMNN_Express.so"
cp libMNN.so "$JNI_DIR/libllm.so"
```

If there's a TTS module with its own `jniLibs`:

```bash
TTS_JNI=../apps/Android/MnnLlmChat/app-tts/src/main/jniLibs/arm64-v8a
mkdir -p "$TTS_JNI"
cp libMNN.so "$TTS_JNI/libMNN.so"
cp libMNN.so "$TTS_JNI/libMNN_Express.so"
cp libMNN.so "$TTS_JNI/libllm.so"
```

## Step 3: Disable Firebase (if JDK < 17 for Gradle)

The root `build.gradle` includes Firebase plugins that require JVM 17+. If you encounter build errors:

Edit `apps/Android/MnnLlmChat/build.gradle`:

```diff
-id 'com.google.gms.google-services' version '4.4.3' apply false
-id 'com.google.firebase.crashlytics' version '3.0.3' apply false
+// id 'com.google.gms.google-services' version '4.4.3' apply false
+// id 'com.google.firebase.crashlytics' version '3.0.3' apply false
```

## Step 4: Build APK

```bash
cd apps/Android/MnnLlmChat

# Debug build (easiest for personal use)
./gradlew assembleDebug

# The APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

For a release build:

```bash
./gradlew assembleRelease
```

## Step 5: Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Open MNN Chat on your device
2. Tap "Add Local Model"
3. Select the folder containing your converted Gemma 4 MNN model
4. Start chatting

## Troubleshooting

| Error | Solution |
|-------|----------|
| `Firebase requires JVM 17` | Comment out Firebase plugins in root `build.gradle` (Step 3) |
| `libMNN_Express.so not found` | Copy `libMNN.so` as `libMNN_Express.so` (Step 2) |
| `libllm.so not found` | Copy `libMNN.so` as `libllm.so` (Step 2) |
| `UnsatisfiedLinkError` | Ensure `SEP_BUILD=OFF` was used during MNN build |
| Model not loading | Verify MNN commit >= `ba76938` (Gemma 4 support) |
