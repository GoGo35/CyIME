"""Time one Gradle invocation (not checkout, validation or upload), then verify its ARM64 APK."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
import zipfile

side, output = sys.argv[1:]
assert side in ('A-base', 'B-fork')
out = Path(output).resolve()
out.mkdir(exist_ok=True, parents=True)
rev = subprocess.check_output(['git', 'rev-parse', 'HEAD']).decode().strip()
flags = ['-I', 'scripts/phone-release-abi.gradle', '-PcyimeReleaseAbi=arm64-v8a',
         ':app:assembleDebug', '--no-daemon', '--console=plain', '--max-workers=2',
         '-Pandroid.overridePathCheck=true', '-x', ':app:prepareOfflineTranslateLibraries']
# The unavailable optional translation binaries are omitted on BOTH hosts, never measured as network retries.
if list(Path('app/build/generated/translate-jniLibs/arm64-v8a').glob('*.so')):
    raise RuntimeError('Use a workspace without stale optional translation binaries.')
windows = os.name == 'nt'
cmd = (['cmd', '/d', '/c', 'gradlew.bat'] if windows else ['bash', 'gradlew']) + flags
print(side, rev, ' '.join(cmd), flush=True)
t0 = time.monotonic()
rc = subprocess.call(cmd)
duration = round(time.monotonic() - t0, 3)
record = {'side': side, 'commit': rev, 'gradle_seconds': duration, 'exit_code': rc, 'args': flags}
def report():
    (out / (side + '.json')).write_text(json.dumps(record, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(record), flush=True)
report()
if rc:
    sys.exit(rc)
root = Path('app/build/outputs/apk/debug')
m = json.loads((root / 'output-metadata.json').read_text(encoding='utf-8'))
[e] = m['elements']  # not a different split left over from a multi-ABI build
assert m['applicationId'] == 'com.cyletix.cyime' and e['versionCode'] == 20261548 and e['versionName'] == '1.15.107', e
assert [f['value'] for f in e['filters']] == ['arm64-v8a'], e
apk = root / e['outputFile']
with zipfile.ZipFile(str(apk)) as z:
    libs = [n for n in z.namelist() if n.startswith('lib/') and n.endswith('.so')]
    assert {n.split('/')[1] for n in libs} == {'arm64-v8a'} and any('rime' in n.lower() for n in libs), libs
    assert not any('libcytranslate' in n for n in libs), libs
bt = Path(os.environ['ANDROID_HOME']) / 'build-tools' / '36.0.0'
badging = subprocess.check_output([str(bt / ('aapt.exe' if windows else 'aapt')), 'dump', 'badging', str(apk)])
assert b'application-debuggable' in badging and b"package: name='com.cyletix.cyime' versionCode='20261548' versionName='1.15.107'" in badging
signcmd = (['cmd', '/d', '/c', str(bt / 'apksigner.bat')] if windows else [str(bt / 'apksigner')])
sig = subprocess.check_output(signcmd + ['verify', '--print-certs', str(apk)]).decode(errors='replace')
cert = next(s.split(': ', 1)[1].strip() for s in sig.splitlines() if s.startswith('Signer #1 certificate SHA-256 digest: '))
other = out / ('A-base.json' if side == 'B-fork' else 'B-fork.json')
if other.is_file():
    previous = json.loads(other.read_text(encoding='utf-8')).get('cert_sha256')
    assert not previous or previous == cert, 'pair must share one signer'
name = side + '-' + rev[:9] + '-arm64-debug.apk'
shutil.copyfile(str(apk), str(out / name))
h = hashlib.sha256()
with (out / name).open('rb') as stream:
    for data in iter(lambda: stream.read(1024 * 1024), b''):
        h.update(data)
record.update(apk=name, bytes=apk.stat().st_size, sha256=h.hexdigest(), cert_sha256=cert, verified=True)
report()
