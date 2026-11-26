#!/usr/bin/env bash
# Build the fork's standard ARM64 release, independently of the signing job.
set -euo pipefail
sdk="$ANDROID_HOME/build-tools/36.0.0"
up="$RUNNER_TEMP/CyIME-1.15.108-arm64-v8a.apk"
sha=6ab1a8ccb5aa556c16f95d6dcd5ff29559ec31f2ac6736652e2c546257d2ba37
url=https://github.com/Cyletix/CyIME/releases/download/1.15.108
curl --fail --location --silent --show-error --retry 3 "$url/${up##*/}" -o "$up"
printf '%s  %s\n' "$sha" "$up" | sha256sum --check
# The standalone unstripped translation libraries aren't consistently available.
# Use the exact shipped ARM64 library from the pinned upstream release, not a stub.
lib=lib/arm64-v8a/libcytranslate.so
dir=app/build/generated/translate-jniLibs/arm64-v8a
mkdir -p "$dir"
unzip -p "$up" "$lib" > "$dir/libcytranslate.so"
test -s "$dir/libcytranslate.so"
test ! -e app/keystore.properties
flags=(-I scripts/phone-release-abi.gradle -PcyimeReleaseAbi=arm64-v8a
       --console=plain --max-workers=2 -x :app:prepareOfflineTranslateLibraries)
bash gradlew "${flags[@]}" -I scripts/personal-108-tests.gradle \
  :app:testDebugUnitTest :plugin-core:testDebugUnitTest
bash gradlew "${flags[@]}" :app:assembleRelease
mapfile -t files < <(find app/build/outputs/apk/release -name '*.apk' -type f)
test "${#files[@]}" -eq 1
apk=${files[0]}
test -s app/build/outputs/mapping/release/mapping.txt
"$sdk/aapt" dump badging "$apk" > "$RUNNER_TEMP/badging.txt"
grep -F "package: name='com.cyletix.cyime' versionCode='20261551' versionName='1.15.108'" "$RUNNER_TEMP/badging.txt"
grep -Fx "native-code: 'arm64-v8a'" "$RUNNER_TEMP/badging.txt"
if grep -q '^application-debuggable' "$RUNNER_TEMP/badging.txt"; then exit 1; fi
cmp "$dir/libcytranslate.so" <(unzip -p "$apk" "$lib")
python3 - "$apk" <<'PY'
import sys, zipfile
with zipfile.ZipFile(sys.argv[1]) as apk:
    dex = [n for n in apk.namelist() if n.startswith('classes') and n.endswith('.dex')]
    assert dex and all(b'CyIME107Timing' not in apk.read(n) for n in dex), 'Unexpected diagnostic probe'
    assert any(n.endswith('/cyime_en_memory.lua') for n in apk.namelist()), 'Missing English memory Lua'
PY
mkdir -p "$RUNNER_TEMP/candidate"
cp "$apk" "$RUNNER_TEMP/candidate/unsigned.apk"
cp docs/fork/1.15.108.md "$RUNNER_TEMP/candidate/release-notes.md"
