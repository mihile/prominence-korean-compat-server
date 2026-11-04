"""Offline behavior and real native Mixin verification; no game/server startup."""
from pathlib import Path
import argparse,hashlib,json,os,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]
def main():
 a=argparse.ArgumentParser()
 for n in ['jar','classpath','java-home','prominent','eldritch','output']:a.add_argument('--'+n,type=Path,required=True)
 x=a.parse_args();work=x.output.resolve();work.mkdir(parents=True,exist_ok=True);classes=work/'classes';classes.mkdir(exist_ok=True)
 cp=[str(x.jar.resolve()),str(x.prominent.resolve()),str(x.eldritch.resolve()),*json.loads(x.classpath.read_text('utf8'))]
 def run(exe,args,name):
  f=work/(name+'.args');f.write_text('\n'.join('"'+str(s).replace('\\','/')+'"' for s in args),encoding='utf8')
  p=subprocess.run([str(x.java_home/'bin'/exe),'@'+str(f)],cwd=work,capture_output=True,text=True,encoding='utf8',errors='replace');(work/(name+'.log')).write_text(p.stdout+p.stderr,encoding='utf8');print(p.stdout,end='');assert p.returncode==0,p.stdout+p.stderr
 with zipfile.ZipFile(x.jar) as z:pins=json.loads(z.read('hourglass-pins.json'))
 for mod,path in [('prominentbase',x.prominent),('eldritch_end',x.eldritch)]:
  with zipfile.ZipFile(path) as z:
   for name,digest in pins[mod].items():assert hashlib.sha256(z.read(name)).hexdigest()==digest,(mod,name)
 print('PASS: all installed native class SHA256 pins match')
 transform=classes/'transform';transform.mkdir(exist_ok=True)
 run('javac.exe',['--release','17','-proc:none','-cp',os.pathsep.join(cp),'-d',transform,ROOT/'tests/arrow/verification/VerifyMixinTransforms.java',ROOT/'tests/hourglass/verification/VerifyHourglassTransform.java'],'compile-transform')
 s=transform/'META-INF/services';s.mkdir(parents=True,exist_ok=True)
 for a,b in [('IMixinService','verification.VerifyMixinTransforms'),('IMixinServiceBootstrap','verification.VerifyMixinTransforms$Bootstrap'),('IGlobalPropertyService','verification.VerifyMixinTransforms$Props')]: (s/('org.spongepowered.asm.service.'+a)).write_text(b+'\n')
 run('java.exe',['-Xmx512m','-Xverify:all','-cp',os.pathsep.join([str(transform),*cp]),'verification.VerifyHourglassTransform',work/'configs'],'transform')
 fixture=classes/'fixture';fixture.mkdir(exist_ok=True)
 run('javac.exe',['--release','17','-proc:none','-cp',os.pathsep.join(cp),'-d',fixture,*sorted((ROOT/'tests/hourglass/fixtures').rglob('*.java'))],'compile-fixture')
 run('java.exe',['-Xverify:all','-cp',os.pathsep.join([str(fixture),*cp]),'HourglassBehaviorChecks'],'behavior')
 print('PASS: no Minecraft world or server was started')
if __name__=='__main__':main()
