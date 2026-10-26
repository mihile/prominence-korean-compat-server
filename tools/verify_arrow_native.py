from pathlib import Path
import argparse,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
def main():
 a=argparse.ArgumentParser()
 for n in ['jar','classpath','java-home','output']:a.add_argument('--'+n,type=Path,required=True)
 args=a.parse_args();out=args.output.resolve();classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
 cp=';'.join([str(args.jar.resolve()),*json.loads(args.classpath.read_text('utf8'))])
 params=['--release','17','-proc:none','-cp',cp,'-d',str(classes),str(ROOT/'tests/ArrowNativeTests.java')]
 f=out/'compile.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in params),'utf8');subprocess.run([str(args.java_home/'bin/javac.exe'),'-J-Xmx256m','@'+str(f)],check=True)
 p=subprocess.run([str(args.java_home/'bin/java.exe'),'-Xmx256m','-ea','-cp',str(classes)+';'+cp,'ArrowNativeTests'],capture_output=True,text=True,encoding='utf8',errors='replace')
 (out/'native.log').write_text(p.stdout+p.stderr,'utf8');assert p.returncode==0,p.stdout+p.stderr;print(p.stdout)
if __name__=='__main__':main()
