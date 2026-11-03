"""Release an already-integrated cumulative JAR, preserving all code byte-for-byte."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,zipfile
ROOT=Path(__file__).resolve().parents[1]
def sha(b):return hashlib.sha256(b).hexdigest()
def enc(v):return (json.dumps(v,ensure_ascii=False,indent=2)+'\n').encode('utf-8')
def main():
 p=argparse.ArgumentParser();p.add_argument('--base',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--build',type=int,required=True);a=p.parse_args()
 with zipfile.ZipFile(a.base) as z:
  names=z.namelist();assert len(names)==len(set(names));old={n:z.read(n) for n in names}
 meta=json.loads(old['fabric.mod.json']);previous=meta['custom']['prominenceServerUpdate']['build'];assert a.build>previous
 assert meta['id']=='prominence_korean_compat' and meta['version']=='1.0.0' and meta['environment']=='server'
 meta['custom']['prominenceServerUpdate']['build']=a.build
 prop='assets/prominence_korean_compat/server-update-build.properties'
 lines=old[prop].decode('ascii').splitlines();assert sum(s.startswith('build=') for s in lines)==1
 new=dict(old);new['fabric.mod.json']=enc(meta);new[prop]=('\n'.join(('build='+str(a.build) if x.startswith('build=') else x) for x in lines)+'\n').encode('ascii')
 assert all(new[n]==old[n] for n in old if n not in ('fabric.mod.json',prop))
 spec=importlib.util.spec_from_file_location('build_common',ROOT/'tools/build.py');b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
 jar=b.packed(new);a.output.mkdir(parents=True,exist_ok=True);file='prominence-korean-compat-server-1.0.0.jar';(a.output/file).write_bytes(jar)
 classes=a.output/'work/classes'
 for n,data in new.items():
  if n.endswith('.class'):
   dest=classes/n;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(data)
 script=(ROOT/'scripts/ServerUpdate.ps1').read_text('utf8').replace('__BOOTSTRAP_SHA256__',sha(jar))
 (a.output/'ServerUpdate.ps1').write_text(script,encoding='utf-8-sig')
 report={'build':a.build,'sha256':sha(jar),'size':len(jar),'baseSha256':sha(a.base.read_bytes()),'preservedClasses':sum(n.endswith('.class') for n in old),'updaterClasses':sum(n.startswith('local/prominence/serverupdate/') and n.endswith('.class') for n in old),'updaterReusedByteForByte':True,'onlyBuildMetadataChanged':True,'gameStarted':False}
 (a.output/'build-report.json').write_bytes(enc(report));print(json.dumps(report,indent=2))
if __name__=='__main__':main()
