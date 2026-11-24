#!/usr/bin/env bash
# Run only in the disposable Actions checkout. Both sides use the same runner, wrapper,
# signing key, application sandbox, ARM64 split and optimized release build type.
set -euo pipefail
: "${GITHUB_ACTIONS:?requires an Actions checkout}" "${RUNNER_TEMP:?}" "${GITHUB_SHA:?}"
base=6d596516d4ac676edd01c774a82a6cc519ade93b # upstream 1.15.105
out="$RUNNER_TEMP/switch-pair"
mkdir -p "$out"
umask 077
pw=$(openssl rand -hex 24)
keytool -genkeypair -alias comparison -keyalg RSA -keysize 2048 -validity 730 -dname 'CN=CyIME comparison only' \
  -keystore "$RUNNER_TEMP/comparison.p12" -storetype PKCS12 -storepass "$pw" -keypass "$pw"
printf 'storeFile=%s\nstorePassword=%s\nkeyAlias=comparison\nkeyPassword=%s\n' "$RUNNER_TEMP/comparison.p12" "$pw" "$pw" >app/keystore.properties
unset pw
bt=$(dirname "$(find "$ANDROID_HOME/build-tools" -name apksigner | sort -V | tail -1)")
for side in A-base B-fork; do
  sha=$base; if [[ $side == B-fork ]]; then sha=$GITHUB_SHA; fi
  git switch --detach "$sha"
  [[ $(git rev-parse HEAD) == "$sha" ]]
  # Normalize only the Gradle download mirror; the pinned distribution checksum stays unchanged.
  sed -i 's@^distributionUrl=.*@distributionUrl=https\\://services.gradle.org/distributions/gradle-9.5.0-bin.zip@' gradle/wrapper/gradle-wrapper.properties
  python3 - <<'PY'
from pathlib import Path
changes = {
    'app/build.gradle.kts': [('applicationId = "com.cyletix.cyime"', 'applicationId = "com.cyletix.cyime.switchpair"'),
        ('abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")', 'abiFilters += "arm64-v8a"'),
        ('include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")', 'include("arm64-v8a")'), ('isUniversalApk = true', 'isUniversalApk = false')],
    'app/src/main/res/values/strings.xml': [('name="app_name" translatable="false">CyIME<', 'name="app_name" translatable="false">CyIME A/B<')],
    'app/src/main/AndroidManifest.xml': [('android:label="CyIME"', 'android:label="CyIME A/B"')],
}
for path, edits in changes.items():
    f=Path(path); s=f.read_text()
    for old,new in edits:
        if s.count(old)!=1: raise ValueError(f'{path}: expected one match: {old}')
        s=s.replace(old,new)
    f.write_text(s)
PY
  version="1.15.105-$side"
  echo "::group::$side: build $sha"
  if ! ./gradlew :app:assembleRelease -PversionName="$version" -PversionCode=20261515 --no-daemon --console=plain >"$RUNNER_TEMP/gradle-$side.log" 2>&1; then
    tail -120 "$RUNNER_TEMP/gradle-$side.log"
    grep -E '^e: |^ERROR:|^> [^ ]|^Execution failed' "$RUNNER_TEMP/gradle-$side.log" | tail -20 | while IFS= read -r msg; do echo "::error::${msg//'%'/'%25'}"; done
    exit 1
  fi
  echo '::endgroup::'
  test -s app/build/outputs/mapping/release/mapping.txt
  apk=$(python3 - "$version" <<'PY'
import json, sys
from pathlib import Path
root=Path('app/build/outputs/apk/release')
[e]=[e for e in json.loads((root/'output-metadata.json').read_text())['elements'] if any(f['value']=='arm64-v8a' for f in e['filters'])]
assert e['versionName']==sys.argv[1], e['versionName']
print(root/e['outputFile'])
PY
)
  name="$side-${sha:0:7}-arm64-v8a.apk"
  cp "$apk" "$out/$name"
  "$bt/zipalign" -c -P 16 4 "$out/$name"
  "$bt/apksigner" verify --verbose --min-sdk-version 23 --print-certs "$out/$name" > "$RUNNER_TEMP/sig-$side"
  grep -Fq 'Verified using v1 scheme (JAR signing): true' "$RUNNER_TEMP/sig-$side"
  grep -Fq 'Verified using v2 scheme (APK Signature Scheme v2): true' "$RUNNER_TEMP/sig-$side"
  "$bt/aapt" dump badging "$out/$name" > "$RUNNER_TEMP/badging-$side"
  grep -Fq "package: name='com.cyletix.cyime.switchpair' versionCode='20261515' versionName='$version'" "$RUNNER_TEMP/badging-$side"
  grep -Fxq "native-code: 'arm64-v8a'" "$RUNNER_TEMP/badging-$side"
  if grep -q 'application-debuggable' "$RUNNER_TEMP/badging-$side"; then echo '::error::Not a release APK'; exit 1; fi
  printf '%s %s %s\n' "$side" "$sha" "$version" >> "$out/provenance.txt"
  (cd "$out"; sha256sum "$name" >> SHA256SUMS)
  git restore -- app/build.gradle.kts app/src/main/res/values/strings.xml app/src/main/AndroidManifest.xml gradle/wrapper/gradle-wrapper.properties
  echo "Verified $name (non-debuggable, R8 mapping, v1+v2, ARM64)"
done
cmp <(grep 'Signer #1 certificate SHA-256 digest:' "$RUNNER_TEMP/sig-A-base") <(grep 'Signer #1 certificate SHA-256 digest:' "$RUNNER_TEMP/sig-B-fork")
cat "$out/provenance.txt" "$out/SHA256SUMS" >> "$GITHUB_STEP_SUMMARY"
