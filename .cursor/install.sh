#!/usr/bin/env bash
# Idempotent Cloud Agent setup for the Products Android app.
# Installs the Android SDK, points Gradle at it, and warms the build cache.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
export ANDROID_HOME="$ANDROID_SDK_ROOT"

# Pinned command-line tools release (Android SDK Command-line Tools 12.0).
CMDLINE_VERSION="11076708"
CMDLINE_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_VERSION}_latest.zip"

SDKMANAGER="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"

# 1. Install the Android command-line tools if they are not already present.
if [ ! -x "$SDKMANAGER" ]; then
  echo "Installing Android command-line tools into $ANDROID_SDK_ROOT ..."
  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
  tmp_zip="$(mktemp)"
  curl -fsSL -o "$tmp_zip" "$CMDLINE_URL"
  rm -rf "$ANDROID_SDK_ROOT/cmdline-tools/latest" "$ANDROID_SDK_ROOT/cmdline-tools/tmp"
  unzip -q "$tmp_zip" -d "$ANDROID_SDK_ROOT/cmdline-tools/tmp"
  mv "$ANDROID_SDK_ROOT/cmdline-tools/tmp/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  rmdir "$ANDROID_SDK_ROOT/cmdline-tools/tmp"
  rm -f "$tmp_zip"
fi

# 2. Accept licenses and install the SDK packages this project builds against
#    (compileSdk 35). sdkmanager is a no-op when packages are already present.
yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true
"$SDKMANAGER" "platform-tools" "platforms;android-35" "build-tools;35.0.0"

# 3. Point Gradle at the SDK via local.properties (gitignored, machine-local).
if [ ! -f local.properties ] || ! grep -qs '^sdk.dir=' local.properties; then
  echo "sdk.dir=$ANDROID_SDK_ROOT" >> local.properties
fi

# 4. The committed wrapper lacks the executable bit; restore it locally.
chmod +x gradlew

# 5. Warm the Gradle dependency/build cache and validate the toolchain.
./gradlew --no-daemon assembleDebug testDebugUnitTest

echo "Android environment ready. SDK at $ANDROID_SDK_ROOT"
