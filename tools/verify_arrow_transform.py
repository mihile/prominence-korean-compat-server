from pathlib import Path
import argparse,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
def main():
 a=argparse.ArgumentParser()
 for n in ['jar','classpath','java-home','output','mods']:a.add_argument('--'+n,type=Path,required=True)
 args=a.parse_args();out=args.output.resolve();classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
 services=classes/'META-INF/services';services.mkdir(parents=True,exist_ok=True)
 for iface,impl in [('IMixinService','verification.VerifyMixinTransforms'),('IMixinServiceBootstrap','verification.VerifyMixinTransforms$Bootstrap'),('IGlobalPropertyService','verification.VerifyMixinTransforms$Props')]:
  (services/('org.spongepowered.asm.service.'+iface)).write_text(impl+'\n')
 cp=';'.join([str(classes),str(args.jar.resolve()),*[str(p.resolve()) for p in args.mods.glob('*.jar')],*json.loads(args.classpath.read_text('utf8'))])
 params=['--release','17','-proc:none','-classpath',cp,'-d',str(classes),str(ROOT/'tests/arrow/verification/VerifyMixinTransforms.java')]
 f=out/'compile.args';f.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in params),'utf8');subprocess.run([str(args.java_home/'bin/javac.exe'),'-J-Xmx256m','@'+str(f)],check=True)
 runtime=['-Xmx384m','-cp',cp,'verification.VerifyMixinTransforms',str(out/'transformed'),str(out/'resources')]
 rf=out/'runtime.args';rf.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in runtime),'utf8')
 p=subprocess.run([str(args.java_home/'bin/java.exe'),'@'+str(rf)],cwd=out,capture_output=True,text=True,encoding='utf8',errors='replace')
 (out/'transform.log').write_text(p.stdout+p.stderr,'utf8');assert p.returncode==0,p.stdout+p.stderr;print(p.stdout)
if __name__=='__main__':main()
