"""Integrate reviewed armor infusion replacement, preserving the cumulative server modules."""
from pathlib import Path
import argparse,json,os,subprocess,zipfile,importlib.util,hashlib
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser()
for n in ['base','output','classpath','java-home','mods']:p.add_argument('--'+n,type=Path,required=True)
a=p.parse_args();work=a.output.parent/'infusion-work';classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
source=ROOT/'src/compat/infusion/java';cp=[str(a.base.resolve()),*json.loads(a.classpath.read_text('utf8')),*map(str,a.mods.glob('*.jar'))]
options=['--release','17','-proc:none','-encoding','UTF-8','-cp',os.pathsep.join(cp),'-d',str(classes.resolve()),*map(str,source.rglob('*.java'))]
f=work/'compile.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in options),'utf8')
x=subprocess.run([str(a.java_home/'bin/javac.exe'),'@'+str(f)],capture_output=True,text=True,encoding='utf8',errors='replace');(work/'compile.log').write_text(x.stdout+x.stderr,'utf8');assert x.returncode==0,x.stdout+x.stderr
with zipfile.ZipFile(a.base) as z:
 assert len(z.namelist())==len(set(z.namelist()));old={n:z.read(n) for n in z.namelist()}
meta=json.loads(old['fabric.mod.json']);assert meta['environment']=='server' and not meta['entrypoints'].get('client')
added={p.relative_to(classes).as_posix():p.read_bytes() for p in classes.rglob('*.class')}
for p in (ROOT/'src/compat/infusion/resources').glob('*'):added[p.name]=p.read_bytes()
for p in source.rglob('*.java'):added['META-INF/prominence-korean-compat-source/infusion/'+p.relative_to(source).as_posix()]=p.read_bytes()
assert not set(added)&set(old),'Do not replace existing module without review'
meta['mixins'].append('prominence-infusion.mixins.json');new={**old,**added,'fabric.mod.json':(json.dumps(meta,indent=2)+'\n').encode()}
assert all(new[n]==b for n,b in old.items() if n!='fabric.mod.json')
spec=importlib.util.spec_from_file_location('build_common',ROOT/'tools/build.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);a.output.write_bytes(m.packed(new))
report={'preservedClasses':sum(n.endswith('.class') for n in old),'addedClasses':sum(n.endswith('.class') for n in added),'allNonMetadataEntriesPreserved':True,'serverStarted':False}
(work/'report.json').write_text(json.dumps(report,indent=2)+'\n','utf8');print(json.dumps(report,indent=2))
