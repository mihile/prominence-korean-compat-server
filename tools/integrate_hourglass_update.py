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
    work = args.output.parent / 'hourglass-balance-work'
    classes = work / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    source = ROOT / 'src/compat/hourglass-balance/java'
    sources = sorted(source.rglob('*.java'))
    cp = ';'.join([str(args.base.resolve()), str((ROOT.parent / 'hourglass-balance-20261104/prominentbase.jar').resolve()), *json.loads(args.classpath.read_text('utf8'))])
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
    assert len(added) >= 8
    config = 'prominence-hourglass-balance.mixins.json'
    added[config] = enc({'required': True, 'minVersion': '0.8', 'compatibilityLevel': 'JAVA_17',
                         'package': 'local.prominence.hourglass.mixin', 'plugin': 'local.prominence.hourglass.BalanceGuard',
                         'mixins': ['ChallengeMixin','BossMixin','AffixMixin','WorldMixin','FacelessMixin','PortalMixin'], 'injectors': {'defaultRequire': 1}})
    added['hourglass-pins.json'] = (ROOT / 'src/compat/hourglass-balance/resources/hourglass-pins.json').read_bytes()
    for p in sources:
        added['META-INF/prominence-korean-compat-source/hourglass-balance/' + p.relative_to(source).as_posix()] = p.read_bytes()
    added['META-INF/notices/hourglass-balance.txt'] = b'Hourglass and Faceless balance adjustments. MIT; no original mod classes included.\n'
    assert 'local/prominence/hourglass/Balance.class' in old
    assert all(n not in old or n.startswith(('local/prominence/hourglass/', 'META-INF/prominence-korean-compat-source/hourglass-balance/')) or n in {config, 'hourglass-pins.json', 'META-INF/notices/hourglass-balance.txt'} for n in added)
    assert sum(1 for item in meta['mixins'] if isinstance(item, dict) and item.get('config') == config) == 1
    new = {**old, **added, 'fabric.mod.json': enc(meta)}
    assert all(new[n] == value for n, value in old.items() if n not in added)
    spec = importlib.util.spec_from_file_location('build_common', ROOT / 'tools/build.py')
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    args.output.write_bytes(module.packed(new))
    report = {'previousClassesPreserved': sum(n.endswith('.class') for n in old), 'addedClasses': sum(n.endswith('.class') for n in added),
              'unrelatedEntriesPreserved': True, 'sha256': hashlib.sha256(args.output.read_bytes()).hexdigest(), 'serverStarted': False}
    (work / 'integration.json').write_bytes(enc(report))
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
