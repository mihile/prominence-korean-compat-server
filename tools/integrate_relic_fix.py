"""Merge guarded relic ownership message fix; no operating files are written."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]
def enc(v):return (json.dumps(v,ensure_ascii=False,indent=2)+'\n').encode('utf8')
def main():
 a=argparse.ArgumentParser()
 for n in ['base','output','classpath','java-home','mods']:a.add_argument('--'+n,type=Path,required=True)
 args=a.parse_args();work=args.output.parent/'relic-work';classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
 source=ROOT/'src/compat/relic-fix/java';sources=sorted(source.rglob('*.java'))
 cp=';'.join([str(args.base.resolve()),*[str(p.resolve()) for p in args.mods.glob('*.jar')],*json.loads(args.classpath.read_text('utf8'))])
 params=['--release','17','-proc:none','-encoding','UTF-8','-cp',cp,'-d',str(classes.resolve()),*map(str,sources)]
 argfile=work/'compile.args';argfile.write_text('\n'.join('"'+p.replace('\\','/')+'"' for p in params),'utf8')
 p=subprocess.run([str(args.java_home/'bin/javac.exe'),'-J-Xmx384m','@'+str(argfile.resolve())],capture_output=True,text=True,encoding='utf8',errors='replace');(work/'compile.log').write_text(p.stdout+p.stderr,'utf8');assert p.returncode==0,p.stdout+p.stderr
 with zipfile.ZipFile(args.base) as z:old={n:z.read(n) for n in z.namelist()};assert len(old)==len(z.namelist())
 meta=json.loads(old['fabric.mod.json']);assert meta['environment']=='server' and not meta['entrypoints'].get('client')
 added={p.relative_to(classes).as_posix():p.read_bytes() for p in classes.rglob('*.class')}
 cfg='prominence-relic-fix.mixins.json'
 added[cfg]=enc({'required':True,'minVersion':'0.8','compatibilityLevel':'JAVA_17','package':'local.prominence.relicfix.mixin','plugin':'local.prominence.relicfix.RelicFixGuard','mixins':['RelicRecoveryMessageMixin'],'injectors':{'defaultRequire':1}})
 for p in sources:added['META-INF/prominence-korean-compat-source/relic-fix/'+p.relative_to(source).as_posix()]=p.read_bytes()
 added['META-INF/notices/relic-fix.txt']=b'Relic recovery name compatibility, MIT. Original Prominent mod unchanged.\n'
 assert not set(added)&set(old)
 meta['mixins'].append({'config':cfg,'environment':'server'})
 new={**old,**added,'fabric.mod.json':enc(meta)};assert all(new[n]==b for n,b in old.items() if n!='fabric.mod.json')
 spec=importlib.util.spec_from_file_location('build_common',ROOT/'tools/build.py');mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
 args.output.write_bytes(mod.packed(new));report={'preservedExistingClasses':sum(n.endswith('.class') for n in old),'addedClasses':sum(n.endswith('.class') for n in added),'allNonMetadataEntriesPreserved':True,'sha256':hashlib.sha256(args.output.read_bytes()).hexdigest(),'serverStarted':False}
 (work/'integration.json').write_bytes(enc(report));print(json.dumps(report,indent=2))
if __name__=='__main__':main()
