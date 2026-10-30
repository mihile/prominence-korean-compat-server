"""Integrate the reviewed common arrival module; never install or start the server."""
from pathlib import Path
import argparse,json,hashlib,os,shutil,subprocess,zipfile,io

ROOT=Path(__file__).resolve().parents[1]
def main():
    p=argparse.ArgumentParser()
    for n in ['source-client','base','classpath','java-home','output']:p.add_argument('--'+n,type=Path,required=True)
    a=p.parse_args();source=a.source_client.resolve()/'src/main/java/local/prominence/arrival'
    own=ROOT/'src/compat/arrival-protection';java=own/'java';resources=own/'resources';resources.mkdir(parents=True,exist_ok=True)
    sources=[]
    for f in source.rglob('*.java'):
        relative=f.relative_to(source)
        if relative.parts[0]=='client':continue
        target=java/'local/prominence/arrival'/relative;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(f,target);sources.append(target)
    assert len(sources)==7
    for name in ['prominence-arrival-pins.json','prominence-arrival.mixins.json']:shutil.copy2(a.source_client/'src/main/resources'/name,resources/name)
    work=a.output.resolve().parent/'arrival-work';classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
    cp=os.pathsep.join([str(a.base.resolve()),*json.loads(a.classpath.read_text('utf8'))]);af=work/'compile.args'
    opts=['--release','17','-encoding','UTF-8','-proc:none','-cp',cp,'-d',str(classes),*map(str,sorted(sources))]
    af.write_text('\n'.join('"'+v.replace('\\','/')+'"' for v in opts),'utf8')
    q=subprocess.run([str(a.java_home/'bin/javac.exe'),'@'+str(af)],capture_output=True,text=True,encoding='utf8',errors='replace');(work/'compile.log').write_text(q.stdout+q.stderr,'utf8');assert q.returncode==0,q.stdout+q.stderr
    with zipfile.ZipFile(a.base) as z:
        assert len(z.namelist())==len(set(z.namelist()));old={n:z.read(n) for n in z.namelist()}
    meta=json.loads(old['fabric.mod.json']);assert meta['environment']=='server' and not meta['entrypoints'].get('client')
    added={f.relative_to(classes).as_posix():f.read_bytes() for f in classes.rglob('*.class')}
    assert added and all(n.startswith('local/prominence/arrival/') and '/client/' not in n and b'net/minecraft/class_310' not in b and b'net/minecraft/client/' not in b for n,b in added.items())
    for f in sources:added['META-INF/prominence-korean-compat-source/arrival-protection/'+f.relative_to(java).as_posix()]=f.read_bytes()
    for f in resources.iterdir():added[f.name]=f.read_bytes()
    assert not set(old)&set(added),'Do not silently replace a previous arrival module'
    meta['mixins'].append('prominence-arrival.mixins.json');meta['entrypoints'].setdefault('main',[]).append('local.prominence.arrival.ArrivalServer')
    new={**old,**added,'fabric.mod.json':(json.dumps(meta,ensure_ascii=False,indent=2)+'\n').encode('utf8')}
    assert all(new[n]==raw for n,raw in old.items() if n!='fabric.mod.json')
    out=io.BytesIO()
    with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for n,raw in sorted(new.items()):
            info=zipfile.ZipInfo(n,(2026,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;z.writestr(info,raw)
    a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_bytes(out.getvalue())
    report={'preservedClasses':sum(n.endswith('.class') for n in old),'addedClasses':sum(n.endswith('.class') for n in added),'sourceFiles':len(sources),'sha256':hashlib.sha256(out.getvalue()).hexdigest(),'gameStarted':False,'serverStarted':False}
    (work/'integration.json').write_text(json.dumps(report,indent=2),'utf8');print(json.dumps(report,indent=2))
if __name__=='__main__':main()
