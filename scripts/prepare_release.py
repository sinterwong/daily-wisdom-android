"""Package the APK produced by this CI run for a tag-bound GitHub Release."""
import argparse
import hashlib
import os
from pathlib import Path
import re
import shutil

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--tag', required=True)
parser.add_argument('--artifact-dir', type=Path)
parser.add_argument('--output-dir', type=Path, default=Path('release-assets'))
args = parser.parse_args()
version = re.search(r"versionName\s+['\"]([^'\"]+)['\"]", (root / 'app/build.gradle').read_text()).group(1)
if not re.fullmatch(r'v\d+\.\d+\.\d+(?:-[A-Za-z0-9.-]+)?', args.tag) or args.tag != 'v' + version:
    raise SystemExit(f'Tag {args.tag!r} must match app version v{version}')
if args.artifact_dir is None:
    print(f'Validated release tag: {args.tag}')
    raise SystemExit(0)

apk = args.artifact_dir / 'daily-wisdom.apk'
expected = (args.artifact_dir / 'SHA256SUMS.txt').read_text().split()[0]
if hashlib.sha256(apk.read_bytes()).hexdigest() != expected:
    raise SystemExit('CI APK checksum mismatch')
out = args.output_dir
out.mkdir(parents=True, exist_ok=True)
shutil.copyfile(apk, out / f'daily-wisdom-{args.tag}.apk')
for source in ['examples/my-library.json', 'docs/library.schema.json', 'docs/json-format.md']:
    shutil.copyfile(root / source, out / Path(source).name)
files = sorted(p for p in out.iterdir() if p.is_file() and p.name != 'SHA256SUMS.txt')
(out / 'SHA256SUMS.txt').write_text(''.join(f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}\n' for p in files))
repository = os.environ.get('GITHUB_REPOSITORY', 'sinterwong/daily-wisdom-android')
run_id = os.environ.get('GITHUB_RUN_ID', '')
commit = os.environ.get('GITHUB_SHA', '')
Path('release-notes.md').write_text(f'''一日一句 {args.tag}：通用离线 Android 每日内容卡片。

- Android 8.0 及以上；支持标准桌面小组件。
- 每日随机、收藏、停留、多句库独立进度。
- 支持 JSON 导入导出；附格式说明、JSON Schema 和可直接导入的示例。

下载 `daily-wisdom-{args.tag}.apk` 安装，打开后点击「放到手机桌面」。

**当前为调试签名试用包。** 不同版本签名可能不同；如需卸载旧版再安装，收藏和阅读进度会被清空。固定发布签名尚未配置。

APK 由此 tag 对应的 CI 构建，在单元测试、Lint 和 Android 15 模拟器测试通过后自动附加。`SHA256SUMS.txt` 包含附件校验值。

提交：`{commit}`  
[查看本次 CI](https://github.com/{repository}/actions/runs/{run_id})
''')
print('Prepared:', ', '.join(p.name for p in sorted(out.iterdir())))
