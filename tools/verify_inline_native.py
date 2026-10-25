"""Verify dedicated-server Mixin application and Java17 bytecode without starting the game."""
from pathlib import Path
import argparse,json,os,subprocess
ROOT=Path(__file__).resolve().parents[1]
def main():
    p=argparse.ArgumentParser()
    for n in ['jar','classpath','java-home','output']:p.add_argument('--'+n,type=Path,required=True)
    a=p.parse_args();out=a.output.resolve();classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
    services=classes/'META-INF/services';services.mkdir(parents=True,exist_ok=True)
    for interface,impl in [('IMixinService','verification.VerifyMixinTransforms'),('IMixinServiceBootstrap','verification.VerifyMixinTransforms$Bootstrap'),('IGlobalPropertyService','verification.VerifyMixinTransforms$Props')]:
        (services/('org.spongepowered.asm.service.'+interface)).write_text(impl+'\n',encoding='utf-8')
    cp=os.pathsep.join([str(classes),str(a.jar.resolve()),*json.loads(a.classpath.read_text('utf-8'))])
    sources=[ROOT/'tests/hammers/verification/VerifyMixinTransforms.java',ROOT/'tests/inline/verification/VerifyServerInline.java']
    params=['--release','17','-proc:none','-encoding','UTF-8','-cp',cp,'-d',str(classes),*map(str,sources)]
    af=out/'compile.args';af.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in params),encoding='utf-8')
    subprocess.run([str(a.java_home/'bin/javac.exe'),'-J-Xmx256m','@'+str(af)],check=True)
    result=subprocess.run([str(a.java_home/'bin/java.exe'),'-Xmx384m','-ea','-cp',cp,'verification.VerifyServerInline',str(out/'transformed')],cwd=out,capture_output=True,text=True,encoding='utf-8',errors='replace')
    (out/'transform.log').write_text(result.stdout+result.stderr,encoding='utf-8')
    assert result.returncode==0,(result.stdout+result.stderr)[-7000:]
    print(result.stdout.strip().splitlines()[-1])
if __name__=='__main__':main()
