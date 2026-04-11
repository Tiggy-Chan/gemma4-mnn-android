#!/bin/bash
# Apply patches to MNN submodule after cloning
# Usage: ./setup.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MNN_DIR="${SCRIPT_DIR}/MNN"

echo "========================================"
echo "Setting up llm-on-device"
echo "========================================"

# Check MNN exists
if [ ! -d "${MNN_DIR}" ]; then
    echo "ERROR: MNN directory not found."
    echo "Run: git submodule update --init --recursive"
    exit 1
fi

# Apply patches
echo ""
echo "Applying patches to MNN..."

cd "${MNN_DIR}"

for patch in "${SCRIPT_DIR}"/patches/*.patch; do
    if [ -f "$patch" ]; then
        echo "  $(basename "$patch")"
        git apply --check "$patch" 2>/dev/null && git apply "$patch" || echo "    (already applied or failed)"
    fi
done

echo ""
echo "Setup complete!"
echo ""
echo "Next steps:"
echo "  1. Download base model: hf download TrevorJS/gemma-4-E2B-it-uncensored --local-dir models/gemma-4-E2B-it-uncensored"
echo "  2. Convert: ./convert.sh"
echo "  3. Build Android APK: see docs/build-apk.md"
