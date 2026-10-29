"""Merge reviewed common rare reward scaling into the latest cumulative server JAR."""
from pathlib import Path
import argparse,hashlib,io,json,os,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]
def enc(v):return (json.dumps(v,ensure_ascii=False,indent=2)+'\n').encode('utf8')
def main():
 p=argparse.ArgumentParser()
 for n in ['base','classpath','java-home','output']:p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();source=ROOT/'src/compat/reward-scaling/java';resources=ROOT/'src/compat/reward-scaling/resources'
 work=a.output.parent/'reward-scaling-work';classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
 sources=sorted(source.rglob('*.java'));assert len(sources)>=16
 opts=['--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join([str(a.base.resolve()),*json.loads(a.classpath.read_text('utf8'))]),'-d',str(classes),*map(str,sources)]
 af=work/'compile.args';af.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in opts),encoding='utf8')
 result=subprocess.run([str(a.java_home/'bin/javac.exe'),'@'+str(af)],capture_output=True,text=True,encoding='utf8',errors='replace');(work/'compile.log').write_text(result.stdout+result.stderr,encoding='utf8');assert result.returncode==0,result.stdout+result.stderr
 with zipfile.ZipFile(a.base) as z:old={n:z.read(n) for n in z.namelist()};assert len(old)==len(z.namelist())
 meta=json.loads(old['fabric.mod.json']);assert meta['environment']=='server' and not meta['entrypoints'].get('client')
 added={f.relative_to(classes).as_posix():f.read_bytes() for f in classes.rglob('*.class')};assert len(added)>=18
 for n,b in added.items():assert n.startswith('local/prominence/rewards/') and b'net/minecraft/class_310' not in b and b'net/minecraft/client/' not in b
 added.update({f.relative_to(resources).as_posix():f.read_bytes() for f in resources.rglob('*') if f.is_file()})
 for f in sources:added['META-INF/prominence-korean-compat-source/reward-scaling/'+f.relative_to(source).as_posix()]=f.read_bytes()
 added['META-INF/notices/reward-scaling.txt']=b'Original MIT compatibility code. Data-driven missing rare-drop Looting/Luck bonuses. Preserve native existing bonuses, conditions and quantities. Exact Minecraft1.20.1 and EMI Loot fingerprints.\n'
 owned=lambda n:n.startswith(("local/prominence/rewards/","META-INF/prominence-korean-compat-source/reward-scaling/")) or n in ("prominence-reward-scaling.mixins.json","prominence-reward-scaling-pins.json","assets/prominence_korean_compat/reward_scaling_policy.json","assets/prominence_korean_compat/reward_quest_items.json","META-INF/notices/reward-scaling.txt")
 assert all(owned(n) for n in set(added).intersection(old))

 if 'prominence-reward-scaling.mixins.json' not in meta['mixins']:meta['mixins'].append('prominence-reward-scaling.mixins.json')
 main=meta['entrypoints'].setdefault('main',[])
 if 'local.prominence.rewards.RewardCommonInit' not in main:main.append('local.prominence.rewards.RewardCommonInit')
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
