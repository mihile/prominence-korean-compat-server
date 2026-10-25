"""Real offline Mixin transformation of the original hammer + original player hook; no game main."""
from pathlib import Path
import argparse,json,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]
def main():
    a=argparse.ArgumentParser()
    for n in ['jar','classpath','java-home','hammers','output']:a.add_argument('--'+n,type=Path,required=True)
    args=a.parse_args();out=args.output.resolve();classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
    services=classes/'META-INF/services';services.mkdir(parents=True,exist_ok=True)
    for iface,impl in [('IMixinService','verification.VerifyMixinTransforms'),('IMixinServiceBootstrap','verification.VerifyMixinTransforms$Bootstrap'),('IGlobalPropertyService','verification.VerifyMixinTransforms$Props')]:
        (services/('org.spongepowered.asm.service.'+iface)).write_text(impl+'\n')
    cp=';'.join([str(classes),str(args.jar.resolve()),str(args.hammers.resolve()),*json.loads(args.classpath.read_text('utf8'))])
    params=['--release','17','-proc:none','-classpath',cp,'-d',str(classes),str(ROOT/'tests/hammers/verification/VerifyMixinTransforms.java')]
    argfile=out/'compile.args';argfile.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in params),'utf8')
    subprocess.run([str(args.java_home/'bin/javac.exe'),'-J-Xmx256m','@'+str(argfile)],check=True)
    result=subprocess.run([str(args.java_home/'bin/java.exe'),'-Xmx384m','-cp',cp,'verification.VerifyMixinTransforms',str(out/'transformed'),str(out/'resources')],cwd=out,capture_output=True,text=True,encoding='utf8',errors='replace')
    (out/'transform.log').write_text(result.stdout+result.stderr,'utf8');assert result.returncode==0,result.stdout+result.stderr
    print(result.stdout)
if __name__=='__main__':main()
