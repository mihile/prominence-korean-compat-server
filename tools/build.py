"""Build into a staging directory; never change a running server or any client artifact."""
from pathlib import Path
import argparse, hashlib, io, json, os, subprocess, zipfile

ROOT = Path(__file__).resolve().parents[1]
FILE = 'prominence-korean-compat-server-1.0.0.jar'
PREFIX = 'local/prominence/serverupdate/'

def sha(b): return hashlib.sha256(b).hexdigest()
def enc(o): return (json.dumps(o, ensure_ascii=False, indent=2)+'\n').encode('utf8')
def packed(entries):
    stream=io.BytesIO()
    with zipfile.ZipFile(stream,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for name,raw in sorted(entries.items()):
            info=zipfile.ZipInfo(name,(2026,1,1,0,0,0)); info.compress_type=zipfile.ZIP_DEFLATED
            info.external_attr=0o100644<<16; z.writestr(info,raw)
    return stream.getvalue()

def main():
    a=argparse.ArgumentParser()
    for name in ['base','classpath','java-home','output']: a.add_argument('--'+name,type=Path,required=True)
    a.add_argument('--build',type=int,required=True); args=a.parse_args()
    work=args.output/'work'; classes=work/'classes'; classes.mkdir(parents=True,exist_ok=True)
    sources=sorted((ROOT/'src/main/java').rglob('*.java'))
    cp=os.pathsep.join([str(args.base.resolve()),*json.loads(args.classpath.read_text('utf8'))])
    options=['--release','17','-encoding','UTF-8','-proc:none','-classpath',cp,'-d',str(classes.resolve()),*map(str,sources)]
    argfile=work/'compile.args'; argfile.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in options),encoding='utf8')
    run=subprocess.run([str(args.java_home/'bin/javac.exe'),'-J-Xmx384m','@'+str(argfile.resolve())],capture_output=True,text=True,encoding='utf8',errors='replace')
    (work/'compile.log').write_text(run.stdout+run.stderr,encoding='utf8'); assert run.returncode==0,run.stdout+run.stderr
    with zipfile.ZipFile(args.base) as z:
        assert len(z.namelist())==len(set(z.namelist())); old={n:z.read(n) for n in z.namelist()}
    meta=json.loads(old['fabric.mod.json'])
    assert meta['id']=='prominence_korean_compat' and meta['version']=='1.0.0'
    assert not meta['entrypoints'].get('client') and not any(n.startswith('local/prominence/autoupdate/') for n in old)
    assert 'prominence-testosterone-memory.mixins.json' in old,'Preserve the latest prepared server fix'
    prior=meta.get('custom',{}).get('prominenceServerUpdate',{}).get('build',0)
    assert args.build>prior,'Build must increase'
    new={n:b for n,b in old.items() if not n.startswith((PREFIX,'META-INF/prominence-korean-compat-source/server-update/'))}
    added={p.relative_to(classes).as_posix():p.read_bytes() for p in classes.rglob('*.class')}; new.update(added)
    new['assets/prominence_korean_compat/server-update-public-key.der']=(ROOT/'public-key.der').read_bytes()
    new['assets/prominence_korean_compat/server-update-build.properties']=(f'build={args.build}\nchannel=server\npack=4.1.3\nminecraft=1.20.1\nfilename={FILE}\n').encode('ascii')
    meta['name']='Prominence Korean Compat Server'; meta['environment']='server'
    entry=meta['entrypoints'].setdefault('server',[])
    if 'local.prominence.serverupdate.ServerHealth' not in entry: entry.append('local.prominence.serverupdate.ServerHealth')
    meta.setdefault('custom',{})['prominenceServerUpdate']={'build':args.build,'channel':'server','pack':'4.1.3','healthSeconds':60}
    url='https://github.com/mihile/prominence-korean-compat-server'
    meta.setdefault('contact',{}).update(homepage=url,sources=url,issues=url+'/issues')
    new['fabric.mod.json']=enc(meta)
    for p in sources: new['META-INF/prominence-korean-compat-source/server-update/'+p.relative_to(ROOT/'src/main/java').as_posix()]=p.read_bytes()
    new['META-INF/notices/server-update.txt']=b'Original dedicated-server signed updater, MIT. Only its own server compatibility JAR is updated, before Minecraft starts. Existing modules retain their original licenses, including the GPL-3.0-only Testosterone fluid-check module. No private keys are distributed.\n'
    new['LICENSES/GPL-3.0-only.txt']=(ROOT/'LICENSES/GPL-3.0-only.txt').read_bytes()
    meta['license']=['MIT','GPL-3.0-only']
    new['fabric.mod.json']=enc(meta)
    preserved=[n for n in old if n.endswith('.class') and not n.startswith(PREFIX)]
    assert all(new[n]==old[n] for n in preserved)
    assert all(new[n]==old[n] for n in old if n!='fabric.mod.json' and not n.startswith(PREFIX) and 'server-update' not in n)
    raw=packed(new); args.output.mkdir(parents=True,exist_ok=True); (args.output/FILE).write_bytes(raw)
    script=(ROOT/'scripts/ServerUpdate.ps1').read_text('utf8').replace('__BOOTSTRAP_SHA256__',sha(raw))
    (args.output/'ServerUpdate.ps1').write_text(script,encoding='utf-8-sig')
    report={'build':args.build,'sha256':sha(raw),'size':len(raw),'baseSha256':sha(args.base.read_bytes()),'preservedClasses':len(preserved),'updaterClasses':len(added),'gameStarted':False}
    (args.output/'build-report.json').write_bytes(enc(report)); print(json.dumps(report,indent=2))
if __name__=='__main__': main()
