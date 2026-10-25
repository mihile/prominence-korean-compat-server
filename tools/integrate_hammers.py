"""Integrate guarded server mining compatibility into an explicitly supplied cumulative base."""
from pathlib import Path
import argparse, hashlib, io, json, subprocess, zipfile
ROOT = Path(__file__).resolve().parents[1]
def enc(v): return (json.dumps(v,ensure_ascii=False,indent=2)+'\n').encode('utf8')
def main():
    a=argparse.ArgumentParser()
    for n in ['base','output','classpath','java-home','hammers']:a.add_argument('--'+n,type=Path,required=True)
    args=a.parse_args();work=args.output.parent/'hammer-work';classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
    sources=sorted((ROOT/'src/compat/java').rglob('*.java'))
    cp=';'.join([str(args.base.resolve()),str(args.hammers.resolve()),*json.loads(args.classpath.read_text('utf8'))])
    params=['--release','17','-proc:none','-encoding','UTF-8','-classpath',cp,'-d',str(classes.resolve()),*map(str,sources)]
    argfile=work/'compile.args';argfile.write_text('\n'.join('"'+p.replace('\\','/')+'"' for p in params),encoding='utf8')
    subprocess.run([str(args.java_home/'bin/javac.exe'),'-J-Xmx256m','@'+str(argfile.resolve())],check=True)
    with zipfile.ZipFile(args.base) as z:
        assert len(z.namelist())==len(set(z.namelist()));old={n:z.read(n) for n in z.namelist()}
    meta=json.loads(old['fabric.mod.json']);assert meta['environment']=='server'
    assert 'prominence-testosterone-memory.mixins.json' in old
    config='prominence-hammer-enchantments.mixins.json';assert config not in old,'Rebase explicitly before replacing a prior module'
    added={p.relative_to(classes).as_posix():p.read_bytes() for p in classes.rglob('*.class')}
    added[config]=enc({'required':True,'minVersion':'0.8','compatibilityLevel':'JAVA_17','package':'local.prominence.hammercompat.mixin',
        'plugin':'local.prominence.hammercompat.HammerGuard','mixins':['HammerAccess','HammerAreaMixin','HammerPlayerBreakMixin'],'injectors':{'defaultRequire':1}})
    for p in sources:added['META-INF/prominence-korean-compat-source/hammer-enchantments/'+p.relative_to(ROOT/'src/compat/java').as_posix()]=p.read_bytes()
    added['META-INF/notices/hammer-enchantments.txt']=b'GPL-3.0-only. Compatibility with Just Hammers 20.1.5+mc1.20.1 by ErrorMikey. Original mod unchanged. Defers area mining until a successful source break, then uses the standard Fabric player break pipeline.\n'
    assert not set(added)&set(old),'Must not replace any existing module'
    meta['mixins'].append({'config':config,'environment':'server'})
    meta.setdefault('custom',{})['prominence:hammer_enchantments']={'version':'1.0.0','targetVersion':'20.1.5+mc1.20.1','environment':'server','license':'GPL-3.0-only'}
    new={**old,**added,'fabric.mod.json':enc(meta)}
    with zipfile.ZipFile(args.output,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for n,raw in sorted(new.items()):z.writestr(n,raw)
    assert all(new[n]==raw for n,raw in old.items() if n!='fabric.mod.json')
    report={'baseSha256':hashlib.sha256(args.base.read_bytes()).hexdigest(),'sha256':hashlib.sha256(args.output.read_bytes()).hexdigest(),
        'existingEntriesPreserved':len(old)-1,'addedClasses':sum(n.endswith('.class') for n in added),'serverStarted':False}
    (work/'integration.json').write_bytes(enc(report));print(json.dumps(report,indent=2))
if __name__=='__main__':main()
