"""Prepare signed server assets, or publish an immutable increasing build against a full source SHA."""
from pathlib import Path
import argparse, hashlib, json, os, re, shutil, subprocess

ROOT=Path(__file__).resolve().parents[1]
REPO='mihile/prominence-korean-compat-server'
FILE='prominence-korean-compat-server-1.0.0.jar'

def run(args,**kw): return subprocess.run(list(map(str,args)),check=True,**kw)
def main():
    a=argparse.ArgumentParser()
    for name in ['build-dir','signing-key','java-home','notes-file']:a.add_argument('--'+name,type=Path,required=True)
    a.add_argument('--tag',required=True);a.add_argument('--prepare-only',action='store_true');args=a.parse_args()
    assert not args.signing_key.resolve().is_relative_to(ROOT.resolve()),'Keep private signing key outside repository'
    build=args.build_dir.resolve();report=json.loads((build/'build-report.json').read_text('utf8'));jar=build/FILE
    raw=jar.read_bytes();assert hashlib.sha256(raw).hexdigest()==report['sha256'] and len(raw)==report['size']
    match=re.fullmatch(r'build-(\d{8})\.(\d{1,2})',args.tag)
    assert match and report['build']==int(match[1]+match[2].zfill(2)),'Tag/build mismatch'
    release=build/'release';release.mkdir(exist_ok=True);shutil.copy2(jar,release/FILE)
    p={'schema':'1','channel':'server','mod':'prominence_korean_compat','pack':'4.1.3','minecraft':'1.20.1','version':'1.0.0','filename':FILE,
       'build':str(report['build']),'size':str(len(raw)),'sha256':report['sha256'],'tag':args.tag,'url':f'https://github.com/{REPO}/releases/download/{args.tag}/{FILE}'}
    payload=build/'manifest-payload.properties';payload.write_text(''.join(k+'='+v+'\n' for k,v in p.items()),encoding='ascii')
    tools=build/'maintainer';tools.mkdir(exist_ok=True)
    java=args.java_home/'bin/java.exe';javac=args.java_home/'bin/javac.exe'
    run([javac,'--release','17','-cp',jar,'-d',tools,ROOT/'tools/SignRelease.java',ROOT/'tools/VerifyRelease.java'])
    run([java,'-cp',tools,'SignRelease','sign',args.signing_key,payload,release/'server-update.properties'])
    run([java,'-cp',str(tools)+os.pathsep+str(jar),'VerifyRelease',release/'server-update.properties',jar])
    # The bootstrap bundle is extracted only with Minecraft and its old restart loop stopped.
    bundle={'.prominence-server-update/bootstrap.jar':raw,
            '.prominence-server-update/bootstrap.properties':(release/'server-update.properties').read_bytes(),
            'maintenance/ServerUpdate.ps1':(build/'ServerUpdate.ps1').read_bytes(),
            'Install-ServerUpdater.ps1':(ROOT/'scripts/Install-ServerUpdater.ps1').read_bytes(),
            'SERVER-UPDATER-README.md':(ROOT/'docs/INSTALL.md').read_bytes()}
    import zipfile
    with zipfile.ZipFile(release/'server-updater-bootstrap.zip','w',zipfile.ZIP_DEFLATED) as z:
        for n,b in bundle.items():z.writestr(n,b)
    assets=[FILE,'server-update.properties','server-updater-bootstrap.zip']
    (release/'SHA256SUMS.txt').write_text(''.join(hashlib.sha256((release/n).read_bytes()).hexdigest()+'  '+n+'\n' for n in assets),encoding='ascii')
    if args.prepare_only:return
    assert not subprocess.check_output(['git','-C',str(ROOT),'status','--porcelain']).strip(),'Commit reviewed source before publishing'
    previous=json.loads(subprocess.check_output(['gh','api',f'repos/{REPO}/releases?per_page=100']))
    for item in previous:
        m=re.fullmatch(r'build-(\d{8})\.(\d{1,2})',item['tag_name'])
        if m and not item['draft']:assert report['build']>int(m[1]+m[2].zfill(2)),'Build number was already published'
    commit=subprocess.check_output(['git','-C',str(ROOT),'rev-parse','HEAD']).decode().strip();assert re.fullmatch('[a-f0-9]{40}',commit)
    run(['gh','release','create',args.tag,'--repo',REPO,'--target',commit,'--latest','--title',f'Server build {report["build"]}','--notes-file',args.notes_file,*[release/n for n in assets],release/'SHA256SUMS.txt'])
if __name__=='__main__':main()
