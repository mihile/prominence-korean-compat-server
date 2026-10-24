from pathlib import Path
import argparse,json,os,subprocess
ROOT=Path(__file__).resolve().parents[1]
def main():
    a=argparse.ArgumentParser()
    for name in ['build-dir','classpath','java-home']:a.add_argument('--'+name,type=Path,required=True)
    args=a.parse_args();build=args.build_dir.resolve();classes=build/'work/classes';tests=build/'tests';tests.mkdir(exist_ok=True)
    jar=build/'prominence-korean-compat-server-1.0.0.jar'
    cp=os.pathsep.join([str(jar),*json.loads(args.classpath.read_text('utf8'))])
    params=['--release','17','-proc:none','-encoding','UTF-8','-cp',cp,'-d',str(tests),*[str(p) for p in sorted((ROOT/'tests').glob('*.java'))]]
    argfile=build/'test-compile.args';argfile.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in params),encoding='utf8')
    subprocess.run([str(args.java_home/'bin/javac.exe'),'@'+str(argfile)],check=True)
    results=[]
    for name,params in [('UpdaterTests',[]),('VerifyBytecode',[str(classes)])]:
        p=subprocess.run([str(args.java_home/'bin/java.exe'),'-Xmx128m','-ea','-cp',str(tests)+os.pathsep+cp,name,*params],capture_output=True,text=True,encoding='utf8',errors='replace')
        (build/(name+'.log')).write_text(p.stdout+p.stderr,encoding='utf8');assert p.returncode==0,p.stdout+p.stderr
        line=p.stdout.strip().splitlines()[-1];print(line);results.append(line)
    (build/'verification.json').write_text(json.dumps({'status':'passed','results':results,'gameStarted':False,'serverStarted':False},indent=2),encoding='utf8')
if __name__=='__main__':main()
