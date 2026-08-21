#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
export JAVA_HOME="$PROJECT_DIR/.tools/jdk"
export ANDROID_SDK_ROOT="$PROJECT_DIR/.tools/android-sdk"
export PATH="$JAVA_HOME/bin:$PATH"

exec "$PROJECT_DIR/gradlew" --no-daemon "$@"
