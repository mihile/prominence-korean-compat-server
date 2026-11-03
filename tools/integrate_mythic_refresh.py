"""Coalesce the native world-independent Mythic config callback per server tick."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, subprocess, zipfile
ROOT = Path(__file__).resolve().parents[1]


def enc(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode('utf8')


def main():
    parser = argparse.ArgumentParser()
    for name in ['base', 'output', 'classpath', 'java-home']:
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    work = args.output.parent / 'mythic-refresh-work'
    classes = work / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    source = ROOT / 'src/compat/mythic-refresh/java'
    sources = sorted(source.rglob('*.java'))
    cp = ';'.join([str(args.base.resolve()), *json.loads(args.classpath.read_text('utf8'))])
    options = ['--release', '17', '-proc:none', '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes.resolve()), *map(str, sources)]
    file = work / 'compile.args'
    file.write_text('\n'.join('"' + s.replace('\\', '/') + '"' for s in options), 'utf8')
    result = subprocess.run([str(args.java_home / 'bin/javac.exe'), '-J-Xmx256m', '@' + str(file.resolve())], capture_output=True, text=True, encoding='utf8', errors='replace')
    (work / 'compile.log').write_text(result.stdout + result.stderr, 'utf8')
    assert result.returncode == 0, result.stdout + result.stderr
    with zipfile.ZipFile(args.base) as jar:
        old = {n: jar.read(n) for n in jar.namelist()}
        assert len(old) == len(jar.namelist())
    meta = json.loads(old['fabric.mod.json'])
    assert meta['environment'] == 'server' and not meta['entrypoints'].get('client')
    added = {p.relative_to(classes).as_posix(): p.read_bytes() for p in classes.rglob('*.class')}
    assert len(added) == 4
    config = 'prominence-mythic-refresh.mixins.json'
    added[config] = enc({'required': True, 'minVersion': '0.8', 'compatibilityLevel': 'JAVA_17',
                         'package': 'local.prominence.mythicrefresh.mixin', 'plugin': 'local.prominence.mythicrefresh.RefreshGuard',
                         'mixins': ['RefreshMarkerMixin'], 'injectors': {'defaultRequire': 1}})
    added['prominence-mythic-refresh-pins.json'] = (ROOT / 'src/compat/mythic-refresh/resources/prominence-mythic-refresh-pins.json').read_bytes()
    for p in sources:
        added['META-INF/prominence-korean-compat-source/mythic-refresh/' + p.relative_to(source).as_posix()] = p.read_bytes()
    added['META-INF/notices/mythic-refresh.txt'] = b'Original guarded coalescing of successful world-independent Mythic Upgrades config refreshes per server tick. Native callback retained. MIT; no original mod classes included.\n'
    assert not set(added) & set(old)
    meta['mixins'].append({'config': config, 'environment': 'server'})
    new = {**old, **added, 'fabric.mod.json': enc(meta)}
    assert all(new[n] == value for n, value in old.items() if n != 'fabric.mod.json')
    spec = importlib.util.spec_from_file_location('build_common', ROOT / 'tools/build.py')
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    args.output.write_bytes(module.packed(new))
    report = {'previousClassesPreserved': sum(n.endswith('.class') for n in old), 'addedClasses': 4,
              'allNonMetadataEntriesPreserved': True, 'sha256': hashlib.sha256(args.output.read_bytes()).hexdigest(), 'serverStarted': False}
    (work / 'integration.json').write_bytes(enc(report))
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
