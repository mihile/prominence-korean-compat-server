"""Compile and add the server half of inline reforge to the latest cumulative release."""
from pathlib import Path
import argparse, hashlib, io, json, os, subprocess, zipfile

ROOT = Path(__file__).resolve().parents[1]
PREFIX = 'local/prominence/backpackinline/'
SOURCE = ROOT/'src/compat/backpack-inline/java'
RESOURCES = ROOT/'src/compat/backpack-inline/resources'

def encode(value):
    return (json.dumps(value, ensure_ascii=False, indent=2)+'\n').encode('utf-8')

def main():
    parser = argparse.ArgumentParser()
    for name in ['base', 'classpath', 'java-home', 'output']:
        parser.add_argument('--'+name, type=Path, required=True)
    args = parser.parse_args()
    work = args.output.parent/'backpack-inline-work'
    classes = work/'classes'; classes.mkdir(parents=True, exist_ok=True)
    sources = sorted(SOURCE.rglob('*.java'))
    assert len(sources) == 13
    cp = os.pathsep.join([str(args.base.resolve()), *json.loads(args.classpath.read_text('utf-8'))])
    params = ['--release','17','-encoding','UTF-8','-proc:none','-cp',cp,'-d',str(classes.resolve()),*map(str,sources)]
    argfile = work/'compile.args'
    argfile.write_text('\n'.join('"'+p.replace('\\','/')+'"' for p in params), encoding='utf-8')
    result = subprocess.run([str(args.java_home/'bin/javac.exe'),'-J-Xmx256m','@'+str(argfile.resolve())], capture_output=True,text=True,encoding='utf-8',errors='replace')
    (work/'compile.log').write_text(result.stdout+result.stderr,encoding='utf-8')
    assert result.returncode == 0, result.stdout+result.stderr
    with zipfile.ZipFile(args.base) as z:
        assert len(z.namelist()) == len(set(z.namelist()))
        old = {n:z.read(n) for n in z.namelist()}
    meta = json.loads(old['fabric.mod.json'])
    assert meta['environment'] == 'server' and not meta['entrypoints'].get('client')
    assert 'prominence-testosterone-memory.mixins.json' in old and 'prominence-hammer-enchantments.mixins.json' in old
    assert not any(n.startswith(PREFIX) for n in old), 'Explicitly review module replacement before rebasing'
    added = {p.relative_to(classes).as_posix():p.read_bytes() for p in classes.rglob('*.class')}
    assert len(added) == 14
    for n,b in added.items():
        assert n.startswith(PREFIX) and '/client/' not in n
        assert all(v not in b for v in [b'net/minecraft/class_310',b'net/minecraft/client/',b'local/prominence/backpackinline/client/'])
    for p in RESOURCES.iterdir():
        if p.is_file(): added[p.name] = p.read_bytes()
    for p in sources:
        added['META-INF/prominence-korean-compat-source/backpack-inline/'+p.relative_to(SOURCE).as_posix()] = p.read_bytes()
    added['META-INF/notices/backpack-inline.txt'] = b'Original MIT compatibility module. Delegates reforge rules and consumption to installed Tierify. Requires corresponding client support; no client renderer included. Original mods and their licenses are unchanged.\n'
    assert not set(added).intersection(old)
    config = 'prominence-inline-reforge.mixins.json'
    meta['entrypoints'].setdefault('main', []).append('local.prominence.backpackinline.InlineReforge')
    meta['mixins'].append({'config':config,'environment':'server'})
    meta.setdefault('custom', {})['prominence:backpack_inline_reforge'] = {'version':'1.0.0','requiresClientSupport':True,'protocol':'inline_reforge_v1'}
    new = {**old, **added, 'fabric.mod.json':encode(meta)}
    assert all(new[n] == b for n,b in old.items() if n != 'fabric.mod.json')
    stream = io.BytesIO()
    with zipfile.ZipFile(stream,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for n,b in sorted(new.items()):
            info=zipfile.ZipInfo(n,(2026,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;z.writestr(info,b)
    args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_bytes(stream.getvalue())
    report = {'baseSha256':hashlib.sha256(args.base.read_bytes()).hexdigest(),'sha256':hashlib.sha256(args.output.read_bytes()).hexdigest(),
              'addedClasses':14,'preservedClasses':sum(n.endswith('.class') for n in old),
              'allPreviousNonMetadataEntriesByteIdentical':True,'clientClassesIncluded':False,'gameStarted':False,'serverStarted':False}
    (work/'integration.json').write_bytes(encode(report));print(json.dumps(report,indent=2))

if __name__ == '__main__':main()
