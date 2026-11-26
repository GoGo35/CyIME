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
# Robolectric resolves the same SDK on first use in hundreds of tests. Fetch it
# once into its normal Maven cache; fail here, not 877 times, on a network error.
sdkver=14-robolectric-10818077-i7
maven_path=org/robolectric/android-all-instrumented/$sdkver
maven_file=android-all-instrumented-$sdkver
mirror=https://repo.maven.apache.org/maven2
(
  mkdir -p "$HOME/.m2/repository/$maven_path"
  cd "$HOME/.m2/repository/$maven_path"
  for ext in pom jar; do
    curl -fsSL --retry 3 --retry-max-time 120 \
      "$mirror/$maven_path/$maven_file.$ext" -o "$maven_file.$ext"
  done
  sha512sum --check <<SUMS
fb97a3028bb1eb4c4b3d1ab9c23c8b6b245842139f76194afc16afe002a212c2f93baff400a83cb6f35e525d0753e5b13aeda172fbdf0f628b25d21aa4a065d6  $maven_file.pom
7cdc0b1306e7865f1acd25a363073149654caee5b8e278c5ae37ff4724185724eff39f0b0111800b521e640bb28698d9d2e655134a801569a0f07470e7b5f455  $maven_file.jar
SUMS
)
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
