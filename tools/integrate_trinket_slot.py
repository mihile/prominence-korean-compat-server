"""Add slot regex reuse and Windows spark background suppression cumulatively."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, subprocess, zipfile
ROOT = Path(__file__).resolve().parents[1]


def enc(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode('utf8')


def main():
    parser = argparse.ArgumentParser()
    for name in ['base', 'output', 'classpath', 'java-home', 'mods']:
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    work = args.output.parent / 'slot-normalization-work'
    classes = work / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    roots = {name: ROOT / ('src/compat/' + name + '/java') for name in ['trinket-slot-normalization', 'spark-background']}
    sources = sorted(p for source in roots.values() for p in source.rglob('*.java'))
    cp = ';'.join([str(args.base.resolve()), *[str(p.resolve()) for p in args.mods.glob('*.jar')],
                   *json.loads(args.classpath.read_text('utf8'))])
    params = ['--release', '17', '-proc:none', '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes.resolve()), *map(str, sources)]
    argfile = work / 'compile.args'
    argfile.write_text('\n'.join('"' + p.replace('\\', '/') + '"' for p in params), 'utf8')
    result = subprocess.run([str(args.java_home / 'bin/javac.exe'), '-J-Xmx256m', '@' + str(argfile.resolve())],
                            capture_output=True, text=True, encoding='utf8', errors='replace')
    (work / 'compile.log').write_text(result.stdout + result.stderr, 'utf8')
    assert result.returncode == 0, result.stdout + result.stderr
    with zipfile.ZipFile(args.base) as jar:
        old = {name: jar.read(name) for name in jar.namelist()}
        assert len(old) == len(jar.namelist())
    meta = json.loads(old['fabric.mod.json'])
    assert meta['environment'] == 'server' and not meta['entrypoints'].get('client')
    added = {p.relative_to(classes).as_posix(): p.read_bytes() for p in classes.rglob('*.class')}
    config = 'prominence-trinket-slot-normalization.mixins.json'
    added[config] = enc({'required': True, 'minVersion': '0.8', 'compatibilityLevel': 'JAVA_17',
                         'package': 'local.prominence.trinketslot.mixin', 'plugin': 'local.prominence.trinketslot.SlotNameGuard',
                         'mixins': ['SlotNamePatternMixin'], 'injectors': {'defaultRequire': 1}})
    spark_config = 'prominence-spark-background.mixins.json'
    added[spark_config] = enc({'required': True, 'minVersion': '0.8', 'compatibilityLevel': 'JAVA_17',
                               'package': 'local.prominence.sparkbackground.mixin', 'plugin': 'local.prominence.sparkbackground.SparkBackgroundGuard',
                               'mixins': ['WindowsBackgroundSamplerMixin'], 'injectors': {'defaultRequire': 1}})
    for module, source in roots.items():
        for p in source.rglob('*.java'):
            added['META-INF/prominence-korean-compat-source/' + module + '/' + p.relative_to(source).as_posix()] = p.read_bytes()
    added['META-INF/notices/trinket-slot-normalization.txt'] = b'Reuse an immutable regex for the native Accessories Trinkets Compat Layer slot-name filter. No equipment results cached; no original mod classes included. MIT.\n'
    added['META-INF/notices/spark-background.txt'] = b'Disable continuous automatic Java sampling only on reviewed spark 1.10.53 on Windows. Manual profiling and metrics stay native. No original mod classes included. MIT.\n'
    assert not set(added) & set(old)
    meta['mixins'].append({'config': config, 'environment': 'server'})
    meta['mixins'].append({'config': spark_config, 'environment': 'server'})
    new = {**old, **added, 'fabric.mod.json': enc(meta)}
    assert all(new[name] == value for name, value in old.items() if name != 'fabric.mod.json')
    spec = importlib.util.spec_from_file_location('build_common', ROOT / 'tools/build.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    args.output.write_bytes(module.packed(new))
    report = {'preservedExistingClasses': sum(name.endswith('.class') for name in old),
              'addedClasses': sum(name.endswith('.class') for name in added), 'allNonMetadataEntriesPreserved': True,
              'sha256': hashlib.sha256(args.output.read_bytes()).hexdigest(), 'serverStarted': False}
    (work / 'integration.json').write_bytes(enc(report))
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
