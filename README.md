# Gemma 4 MNN Converter

Convert [Gemma 4](https://huggingface.co/google/gemma-4-E2B-it) models to [MNN](https://github.com/alibaba/MNN) format with INT4 HQQ, INT8, and BF16 quantization. Includes a custom Android APK build with Gemma 4 support.

## Quick Start

### 1. Clone

```bash
git clone --recurse-submodules https://github.com/Tiggy-Chan/llm-on-device.git
cd llm-on-device
```

Or if you already cloned without submodules:

```bash
git submodule update --init --recursive
```

### 1b. Apply Patches

This repo includes patches for the MNN submodule (Gemma 4 conversion fix and Android build workaround):

```bash
./setup.sh
```

### 2. Download Base Model

```bash
pip install huggingface_hub
hf download TrevorJS/gemma-4-E2B-it-uncensored \
  --local-dir models/gemma-4-E2B-it-uncensored
```

### 3. Convert to MNN

```bash
# INT4 HQQ (default, ~3.5 GB)
./convert.sh

# INT8 (~5.7 GB)
./convert.sh --quant-bit 8 --embed-bit 8 --no-hqq

# BF16 full precision (~9.5 GB)
./convert.sh --quant-bit 16 --embed-bit 16 --no-hqq
```

### 4. Run Inference

```bash
echo "Hello, who are you?" > prompt.txt
./MNN/build_llm/llm_demo ./output/gemma-4-E2B-it-uncensored-mnn-int4/config.json prompt.txt
```

## Pre-converted Models

| Quantization | Size | Download |
|-------------|------|----------|
| **INT4 HQQ** | 3.5 GB | [Tiggy-Chan/gemma-4-E2B-it-uncensored-mnn-int4](https://huggingface.co/Tiggy-Chan/gemma-4-E2B-it-uncensored-mnn-int4) |
| **INT8** | 5.7 GB | [Tiggy-Chan/gemma-4-E2B-it-uncensored-mnn-int8](https://huggingface.co/Tiggy-Chan/gemma-4-E2B-it-uncensored-mnn-int8) |
| **BF16** | 9.5 GB | [Tiggy-Chan/gemma-4-E2B-it-uncensored-mnn-bf16](https://huggingface.co/Tiggy-Chan/gemma-4-E2B-it-uncensored-mnn-bf16) |

Download with:

```bash
hf download Tiggy-Chan/gemma-4-E2B-it-uncensored-mnn-int4 --local-dir ./output/gemma-4-E2B-it-uncensored-mnn-int4
```

## Android APK

This repo includes instructions for building a custom MNN Chat Android APK with Gemma 4 support. The official MNN Chat app (v0.8.2.2 on Google Play) does not yet include Gemma 4 support, but the MNN source code does (commit `ba76938`, merged 2026-04-09).

### Build Requirements

- **NDK**: r27c (`android-ndk-r27c`)
- **Java**: JDK 17
- **CMake**: 3.22+
- **Android SDK**: API 24+

### Build Steps

```bash
# 1. Build MNN for Android
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

# 2. Copy .so files to Android project
# (See docs/build-apk.md for detailed steps)

# 3. Build APK
cd ../apps/Android/MnnLlmChat
./gradlew assembleDebug
```

See [docs/build-apk.md](docs/build-apk.md) for the complete guide.

## Model Architecture

Gemma 4 E2B (Expert 2B) specifications:

| Property | Value |
|----------|-------|
| Hidden size | 1536 |
| Layers | 35 |
| Attention | Mixed (sliding_window=512 + full attention) |
| KV sharing | Layers 15-34 share with layers 13/14 |
| PLE embeddings | Per-Layer Embeddings (unique to Gemma 4) |
| Head dim | 256 |

## Conversion Script Options

```bash
./convert.sh [OPTIONS]

Options:
  --quant-bit 4|8|16    Weight quantization bits (default: 4)
  --embed-bit 4|8|16    PLE embedding quantization bits (default: 4)
  --hqq                 Use HQQ quantization (default: enabled)
  --no-hqq              Disable HQQ
  --output-dir DIR      Custom output directory
```

Output directories are auto-named:
- `output/gemma-4-E2B-it-uncensored-mnn-int4` (INT4 HQQ)
- `output/gemma-4-E2B-it-uncensored-mnn-int8` (INT8)
- `output/gemma-4-E2B-it-uncensored-mnn-bf16` (BF16)

## File Structure

```
gemma2mnn/
├── convert.sh              # Main conversion script
├── docs/
│   └── build-apk.md        # Android APK build guide
├── models/                 # Download base models (gitignored)
├── output/                 # Converted MNN models (gitignored)
└── MNN/                    # MNN inference engine (git submodule)
```

## License

This project's scripts and documentation are licensed under the **Apache License 2.0**. See [LICENSE](LICENSE).

The MNN engine is copyright of Alibaba Group and licensed under Apache 2.0.

Gemma 4 models are copyright of Google DeepMind and licensed under Apache 2.0.

## Acknowledgments

- **MNN**: [Alibaba MNN](https://github.com/alibaba/MNN) — lightweight inference engine
- **Gemma 4**: [google/gemma-4-E2B-it](https://huggingface.co/google/gemma-4-E2B-it) — base model by Google DeepMind
- **Uncensored variant**: [TrevorJS/gemma-4-E2B-it-uncensored](https://huggingface.co/TrevorJS/gemma-4-E2B-it-uncensored) by TrevorJS
- **HQQ**: [Half-Quadratic Quantization](https://github.com/ModelCloud/HQQ)
