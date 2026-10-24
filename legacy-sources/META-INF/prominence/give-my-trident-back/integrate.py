"""Merge the validated server module into the currently installed compatibility JAR."""
import argparse
import hashlib
import json
import shutil
import zipfile
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SERVER = ROOT.parent.parent
TARGET = SERVER / 'mods/prominence-korean-compat-1.0.0.jar'
MODULE = ROOT / 'build/libs/give-my-trident-back-1.0.1+1.20.1.jar'
OUTPUT = ROOT / 'build/integrated/server/mods' / TARGET.name
PREFIX = 'META-INF/prominence/give-my-trident-back/'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read_zip(path):
    with zipfile.ZipFile(path) as archive:
        assert archive.testzip() is None, f'Corrupt ZIP: {path}'
        names = archive.namelist()
        assert len(names) == len(set(names)), f'Duplicate entries: {path}'
        return {name: archive.read(name) for name in names if not name.endswith('/')}


def mod_id(path):
    with zipfile.ZipFile(path) as archive:
        try:
            return json.loads(archive.read('fabric.mod.json'), strict=False).get('id')
        except KeyError:
            return None


def main():
    parser = argparse.ArgumentParser(__doc__)
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--shutdown-confirmed', action='store_true')
    args = parser.parse_args()
    if args.apply and not args.shutdown_confirmed:
        parser.error('Applying requires --shutdown-confirmed after the user confirms shutdown.')
    validation = json.loads((ROOT / 'build/verification/artifact-report.json').read_text(encoding='utf-8-sig'))
    assert sha(MODULE) == validation['sha256'], 'Module differs from validated build'
    assert all(validation['tests'][key] == 0 for key in ('failures', 'errors', 'skipped'))
    base_hash = sha(TARGET)
    base, module = read_zip(TARGET), read_zip(MODULE)
    metadata = json.loads(base['fabric.mod.json'])
    module_metadata = json.loads(module['fabric.mod.json'])
    assert metadata['id'] == 'prominence_korean_compat'
    assert module_metadata['environment'] == 'server'
    assert module_metadata['id'] == 'give_my_trident_back'
    if 'prominence:give_my_trident_back' in metadata.get('custom', {}):
        assert not list((SERVER / 'mods').glob('give-my-trident-back*.jar'))
        print('Already integrated; no changes.')
        return
    merged = dict(base)
    additions = {name: data for name, data in module.items()
                 if name.startswith('dev/mihile/tridentback/') or
                 name in ('give_my_trident_back.mixins.json', 'give-my-trident-back-refmap.json')}
    assert sum(name.endswith('.class') for name in additions) == validation['classCount']
    additions[PREFIX + 'LICENSE'] = module['LICENSE']
    additions[PREFIX + 'original-fabric.mod.json'] = module['fabric.mod.json']
    for path in [ROOT / 'README.md', ROOT / 'build.gradle', ROOT / 'integrate.py', *sorted((ROOT / 'src').rglob('*'))]:
        if path.is_file():
            additions[PREFIX + path.relative_to(ROOT).as_posix()] = path.read_bytes()
    assert not set(additions).intersection(base), 'Integration entry collision'
    merged.update(additions)
    metadata.setdefault('entrypoints', {}).setdefault('main', []).extend(module_metadata['entrypoints']['main'])
    metadata.setdefault('mixins', []).append({'config': 'give_my_trident_back.mixins.json', 'environment': 'server'})
    for key, value in module_metadata['depends'].items():
        assert key not in metadata['depends'] or metadata['depends'][key] == value, f'Dependency collision: {key}'
        metadata['depends'][key] = value
    metadata.setdefault('custom', {})['prominence:give_my_trident_back'] = {
        'version': module_metadata['version'], 'moduleSha256': sha(MODULE),
        'environment': 'server', 'license': 'MIT', 'source': PREFIX + 'src/',
        'upstream': 'formerly mihile/give-my-trident-back; repository retired by owner request'
    }
    merged['fabric.mod.json'] = (json.dumps(metadata, ensure_ascii=False, indent=2) + '\n').encode()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(OUTPUT, 'w', zipfile.ZIP_DEFLATED) as archive:
        for name, data in merged.items():
            archive.writestr(name, data)
    result = read_zip(OUTPUT)
    assert all(result[name] == data for name, data in base.items() if name != 'fabric.mod.json')
    assert all(result[name] == data for name, data in additions.items())
    assert result['give_my_trident_back.mixins.json'] == module['give_my_trident_back.mixins.json']
    report = dict(baseSha256=base_hash, moduleSha256=sha(MODULE), sha256=sha(OUTPUT),
                  artifact=str(OUTPUT), preservedEntries=len(base)-1,
                  moduleClasses=validation['classCount'], tests=validation['tests'],
                  serverStarted=False, worldModified=False, applied=False)
    if args.apply:
        assert sha(TARGET) == base_hash, 'Target changed during packaging'
        standalone = []
        for path in (SERVER / 'mods').glob('*.jar'):
            if mod_id(path) == module_metadata['id']:
                assert sha(path) == sha(MODULE), f'Unexpected standalone version: {path}'
                standalone.append(path)
        backup = SERVER / 'maintenance-backups' / ('trident-integrated-' + datetime.now().strftime('%Y%m%d-%H%M%S'))
        backup.mkdir(parents=True)
        shutil.copy2(TARGET, backup / TARGET.name)
        for path in standalone:
            shutil.copy2(path, backup / path.name)
            assert sha(backup / path.name) == sha(path)
        config = SERVER / 'config/give-my-trident-back.json'
        config_hash = sha(config) if config.exists() else None
        if config.exists():
            shutil.copy2(config, backup / config.name)
        assert sha(backup / TARGET.name) == base_hash
        staged = TARGET.with_suffix('.jar.trident-integrated.tmp')
        shutil.copy2(OUTPUT, staged)
        staged.replace(TARGET)
        assert sha(TARGET) == report['sha256']
        for path in standalone:
            path.unlink()
        assert not any(mod_id(path) == module_metadata['id'] for path in (SERVER / 'mods').glob('*.jar'))
        assert (sha(config) if config.exists() else None) == config_hash
        report.update(applied=True, installed=str(TARGET), backup=str(backup),
                      removedStandalone=[str(path) for path in standalone], configPreserved=True,
                      configSha256=config_hash, shutdownConfirmedByUser=True, shutdownProcessCheckPerformed=False)
        (backup / 'integration-report.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    (ROOT / 'build/verification/integration-report.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
