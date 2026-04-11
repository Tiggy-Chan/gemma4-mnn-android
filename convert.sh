#!/bin/bash
# Gemma-4-E2B-it-uncensored to MNN converter
# Usage: ./convert.sh [--quant-bit 4|8|16] [--embed-bit 4|8|16] [--hqq] [--no-hqq] [--output-dir DIR]

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MODEL_DIR="${SCRIPT_DIR}/models/gemma-4-E2B-it-uncensored"
MNN_DIR="${SCRIPT_DIR}/MNN"

# Default options
QUANT_BIT=4
EMBED_BIT=4
HQQ_FLAG="--hqq"
OUTPUT_DIR=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --quant-bit) QUANT_BIT="$2"; shift 2;;
        --embed-bit) EMBED_BIT="$2"; shift 2;;
        --hqq) HQQ_FLAG="--hqq"; shift;;
        --no-hqq) HQQ_FLAG=""; shift;;
        --output-dir) OUTPUT_DIR="$2"; shift 2;;
        *) echo "Unknown option: $1"; exit 1;;
    esac
done

# Determine output directory
if [ -z "${OUTPUT_DIR}" ]; then
    if [ "${QUANT_BIT}" -eq 16 ]; then
        OUTPUT_DIR="${SCRIPT_DIR}/output/gemma-4-E2B-it-uncensored-mnn-bf16"
    else
        OUTPUT_DIR="${SCRIPT_DIR}/output/gemma-4-E2B-it-uncensored-mnn-int${QUANT_BIT}"
    fi
fi

MNN_CONVERT="${MNN_DIR}/build_llm/MNNConvert"

echo "========================================"
echo "Gemma-4-E2B-it-uncensored -> MNN Converter"
echo "========================================"
echo "Model dir:  ${MODEL_DIR}"
echo "Output dir: ${OUTPUT_DIR}"
echo "Weight bits: ${QUANT_BIT}"
echo "Embed bits: ${EMBED_BIT}"
echo "HQQ:        ${HQQ_FLAG:-disabled}"
echo "========================================"

# Check model exists
if [ ! -f "${MODEL_DIR}/config.json" ]; then
    echo "ERROR: Model not found at ${MODEL_DIR}"
    echo "Run: hf download TrevorJS/gemma-4-E2B-it-uncensored --local-dir models/gemma-4-E2B-it-uncensored"
    exit 1
fi

# Create output directory
mkdir -p "${OUTPUT_DIR}"

# Run llmexport
cd "${MNN_DIR}/transformers/llm/export"

EXTRA_ARGS=""
if [ "${EMBED_BIT}" -ne 16 ]; then
    EXTRA_ARGS="--embed_bit ${EMBED_BIT}"
fi

python3 llmexport.py \
    --path "${MODEL_DIR}" \
    --export mnn \
    --quant_bit "${QUANT_BIT}" \
    ${EXTRA_ARGS} \
    --dst_path "${OUTPUT_DIR}" \
    --mnnconvert "${MNN_CONVERT}" \
    ${HQQ_FLAG} \
    --verbose

echo "========================================"
echo "Conversion complete!"
echo "Output: ${OUTPUT_DIR}"
echo "========================================"
echo "Files:"
ls -lh "${OUTPUT_DIR}/"
echo "Total size: $(du -sh "${OUTPUT_DIR}" | cut -f1)"
