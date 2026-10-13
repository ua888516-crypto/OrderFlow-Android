#!/usr/bin/env bash
# One-step APK builder for Linux/macOS. Requires Java 17 + internet.
set -e
SDK=$HOME/android-sdk
mkdir -p $SDK/cmdline-tools
OS=linux; [ "$(uname)" = Darwin ] && OS=mac
if [ ! -d $SDK/cmdline-tools/latest ]; then
  curl -Lo /tmp/clt.zip https://dl.google.com/android/repository/commandlinetools-${OS}-11076708_latest.zip
  unzip -q /tmp/clt.zip -d $SDK/cmdline-tools
  mv $SDK/cmdline-tools/cmdline-tools $SDK/cmdline-tools/latest
fi
yes | $SDK/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null || true
$SDK/cmdline-tools/latest/bin/sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
echo "sdk.dir=$SDK" > local.properties
if [ ! -d /tmp/gradle-8.7 ]; then
  curl -Lo /tmp/g.zip https://services.gradle.org/distributions/gradle-8.7-bin.zip
  unzip -q /tmp/g.zip -d /tmp
fi
/tmp/gradle-8.7/bin/gradle assembleDebug
echo "Done: app/build/outputs/apk/debug/app-debug.apk"
