# yLauncher — development commands
# Requires: JDK 17, Android SDK, adb

export JAVA_HOME := env("JAVA_HOME", "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home")
export ANDROID_HOME := env("ANDROID_HOME", env("HOME", "") + "/Library/Android/sdk")

gradlew := "./gradlew --no-daemon -q"

# List available recipes
default:
    @just --list

# Compile Kotlin sources (type-check without full APK build)
typecheck:
    {{ gradlew }} compileDebugKotlin

# Run Android lint and auto-fix what it can
lint-fix:
    {{ gradlew }} lintFix 2>/dev/null || {{ gradlew }} lintDebug

# Build debug APK
build:
    {{ gradlew }} assembleDebug

# Install on connected device (USB or WiFi — use `just connect` first for WiFi)
deploy: build
    adb install -r app/build/outputs/apk/debug/app-debug.apk

# Connect to a device over WiFi (pass IP, e.g. `just connect 192.168.1.42`)
connect ip port="5555":
    adb connect {{ ip }}:{{ port }}

# Run all checks (typecheck + lint)
check: typecheck lint-fix

# Run unit tests (JVM)
test:
    {{ gradlew }} testDebugUnitTest

# Run instrumented tests (requires connected device/emulator)
test-device:
    {{ gradlew }} connectedDebugAndroidTest

# Run all tests
test-all: test test-device

# Clean build artifacts
clean:
    {{ gradlew }} clean
