#!/usr/bin/env bash
set -euo pipefail
gradle :app:assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk Thrive11-v1.0-debug.apk
echo "Created: Thrive11-v1.0-debug.apk"
