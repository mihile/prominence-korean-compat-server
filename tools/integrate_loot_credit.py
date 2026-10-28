"""Merge reviewed common weapon loot attribution into the latest cumulative server JAR."""
from pathlib import Path
import argparse,hashlib,io,json,os,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]
def enc(v):return (json.dumps(v,ensure_ascii=False,indent=2)+'\n').encode('utf8')
def main():
 p=argparse.ArgumentParser()
 for n in ['base','classpath','java-home','output']:p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();source=ROOT/'src/compat/loot-credit/java';resources=ROOT/'src/compat/loot-credit/resources'
 work=a.output.parent/'loot-credit-work';classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
 sources=sorted(source.rglob('*.java'));assert len(sources)>=16
 opts=['--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join([str(a.base.resolve()),*json.loads(a.classpath.read_text('utf8'))]),'-d',str(classes),*map(str,sources)]
 af=work/'compile.args';af.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in opts),encoding='utf8')
 result=subprocess.run([str(a.java_home/'bin/javac.exe'),'@'+str(af)],capture_output=True,text=True,encoding='utf8',errors='replace');(work/'compile.log').write_text(result.stdout+result.stderr,encoding='utf8');assert result.returncode==0,result.stdout+result.stderr
 with zipfile.ZipFile(a.base) as z:old={n:z.read(n) for n in z.namelist()};assert len(old)==len(z.namelist())
 meta=json.loads(old['fabric.mod.json']);assert meta['environment']=='server' and not meta['entrypoints'].get('client')
 added={f.relative_to(classes).as_posix():f.read_bytes() for f in classes.rglob('*.class')};assert len(added)>=18
 for n,b in added.items():assert n.startswith('local/prominence/lootcredit/') and b'net/minecraft/class_310' not in b and b'net/minecraft/client/' not in b
 added.update({f.name:f.read_bytes() for f in resources.iterdir() if f.is_file()})
 for f in sources:added['META-INF/prominence-korean-compat-source/loot-credit/'+f.relative_to(source).as_posix()]=f.read_bytes()
 added['META-INF/notices/loot-credit.txt']=b'Original MIT compatibility code. Preserve originating weapon loot bonuses for extra hits, projectiles and attributed status effects. Does not alter combat damage sources or amounts. Exact mod fingerprints.\n'
 owned=lambda n:n.startswith(("local/prominence/lootcredit/","META-INF/prominence-korean-compat-source/loot-credit/")) or n in ("prominence-loot-credit.mixins.json","prominence-loot-credit-pins.json","META-INF/notices/loot-credit.txt")
 assert all(owned(n) for n in set(added).intersection(old))
 
 if 'prominence-loot-credit.mixins.json' not in meta['mixins']:meta['mixins'].append('prominence-loot-credit.mixins.json')
 new={**old,**added,'fabric.mod.json':enc(meta)}
 assert all(new[n]==b for n,b in old.items() if n!='fabric.mod.json' and not owned(n))
 out=io.BytesIO()
 with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
  for n,b in sorted(new.items()):
   info=zipfile.ZipInfo(n,(2026,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;z.writestr(info,b)
 a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_bytes(out.getvalue())
 report={'preservedExistingClasses':sum(n.endswith('.class') for n in old),'addedClasses':sum(n.endswith('.class') for n in added),'allOtherNonMetadataEntriesPreserved':True,'sha256':hashlib.sha256(out.getvalue()).hexdigest(),'gameStarted':False,'serverStarted':False}
 (work/'integration.json').write_bytes(enc(report));print(json.dumps(report,indent=2))
if __name__=='__main__':main()
