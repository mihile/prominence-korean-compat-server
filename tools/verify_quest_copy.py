"""Real native quest Mixin, NBT behavior, state equivalence and allocation checks."""
from pathlib import Path
import argparse,json,os,subprocess,zipfile,re
ROOT=Path(__file__).resolve().parents[1]
def main():
 p=argparse.ArgumentParser()
 for name in ['jar','classpath','java-home','mods','output']:p.add_argument('--'+name,type=Path,required=True)
 p.add_argument('--upgrade-base',type=Path);p.add_argument('--upgrade-manifest',type=Path)
 a=p.parse_args();out=a.output.resolve();classes=out/'transform-classes';classes.mkdir(parents=True,exist_ok=True)
 cp=[str(a.jar.resolve()),*[str(x.resolve()) for x in a.mods.glob('*.jar')],*json.loads(a.classpath.read_text())]
 def run(name,exe,options):
  f=out/(name+'.args');f.write_text('\n'.join('"'+str(v).replace('\\','/')+'"' for v in options),encoding='utf8')
  r=subprocess.run([str(a.java_home/('bin/'+exe+'.exe')),'@'+str(f)],cwd=out,capture_output=True,text=True,encoding='utf8',errors='replace')
  (out/(name+'.log')).write_text(r.stdout+r.stderr,encoding='utf8');assert r.returncode==0,r.stdout+r.stderr
  print(r.stdout.strip());return r.stdout
 sources=[ROOT/'tests/arrow/verification/VerifyMixinTransforms.java',ROOT/'tests/mythic/verification/VerifyMythicTransform.java',ROOT/'tests/questcopy/verification/VerifyQuestTransform.java']
 run('compile-transform','javac',['--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',classes,*sources])
 services=classes/'META-INF/services';services.mkdir(parents=True,exist_ok=True)
 for iface,impl in [('IMixinService','verification.VerifyMixinTransforms'),('IMixinServiceBootstrap','verification.VerifyMixinTransforms$Bootstrap'),('IGlobalPropertyService','verification.VerifyMixinTransforms$Props')]:
  (services/('org.spongepowered.asm.service.'+iface)).write_text(impl+'\n')
 run('transform','java',['-Xmx384m','-Xverify:all','-cp',os.pathsep.join([str(classes),*cp]),'verification.VerifyQuestTransform',out])
 fixture=out/'fixture-classes';fixture.mkdir(exist_ok=True)
 stubs={
  'net/minecraft/class_1297.java':'package net.minecraft;public class class_1297 implements com.faux.customentitydata.api.ICustomDataHolder {private class_2487 custom=new class_2487();public class_2487 faux$getCustomData(){return custom;}public void faux$setCustomData(class_2487 value){custom=value;}}',
  'net/minecraft/class_3222.java':'package net.minecraft;public class class_3222 extends class_1297 implements com.faux.customentitydata.api.IPersistentDataHolder {private class_2487 persistent=new class_2487();public class_2487 faux$getPersistentData(){return persistent;}public void faux$setPersistentData(class_2487 value){persistent=value;}}',
  'net/fabricmc/loader/api/Version.java':'package net.fabricmc.loader.api;public interface Version {String getFriendlyString();}',
  'net/fabricmc/loader/api/metadata/ModMetadata.java':'package net.fabricmc.loader.api.metadata;public interface ModMetadata {net.fabricmc.loader.api.Version getVersion();}',
  'net/fabricmc/loader/api/ModContainer.java':'package net.fabricmc.loader.api;public interface ModContainer {net.fabricmc.loader.api.metadata.ModMetadata getMetadata();java.util.Optional<java.nio.file.Path> findPath(String path);}',
  'net/fabricmc/loader/api/FabricLoader.java':'package net.fabricmc.loader.api;public interface FabricLoader {static FabricLoader getInstance(){return Holder.INSTANCE;}java.util.Optional<ModContainer> getModContainer(String id);class Holder {public static FabricLoader INSTANCE=id->java.util.Optional.empty();}}'
 }
 sources=[]
 for name,value in stubs.items():
  file=out/'stubs'/name;file.parent.mkdir(parents=True,exist_ok=True);file.write_text(value);sources.append(file)
 sources.extend([ROOT/'tests/questcopy/verification/QuestBehaviorChecks.java',ROOT/'tests/questcopy/verification/QuestGuardChecks.java'])
 run('compile-fixture','javac',['--release','17','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(cp),'-d',fixture,*sources])
 results={}
 for mode in ['baseline','fixed']:
  text=run(mode,'java',['-Xmx256m','-Xverify:all','-cp',os.pathsep.join([str(out/mode),str(fixture),*cp]),'verification.QuestBehaviorChecks'])
  results[mode]={key:int(value) if key!='STATE_SHA256' else value for key,value in re.findall(r'(STATE_SHA256|CHECKS|ALLOCATION_BYTES)=(\w+)',text)}
 assert results['baseline']['STATE_SHA256']==results['fixed']['STATE_SHA256'],'native state semantics differ'
 assert results['fixed']['ALLOCATION_BYTES']<results['baseline']['ALLOCATION_BYTES']*0.7,'allocation not reduced'
 pins=out/'pins'
 with zipfile.ZipFile(a.mods/'Prominent-GLOBAL-MC1.20.1-4.1.1.jar') as jar:
  for name in ['elocindev/prominent/api/questing/QuestProgress.class','elocindev/prominent/utils/PersistentPlayerData.class']:
   file=pins/name;file.parent.mkdir(parents=True,exist_ok=True);file.write_bytes(jar.read(name))
 run('guard','java',['-Xmx128m','-Xverify:all','-cp',os.pathsep.join([str(fixture),*cp]),'verification.QuestGuardChecks',pins])
 if a.upgrade_base or a.upgrade_manifest:
  assert a.upgrade_base and a.upgrade_manifest,'Both upgrade inputs are required'
  run('compile-upgrade','javac',['--release','17','-proc:none','-cp',os.pathsep.join(cp),'-d',classes,ROOT/'tests/questcopy/verification/VerifyQuestUpgrade.java'])
  run('upgrade','java',['-Xmx128m','-cp',os.pathsep.join([str(classes),*cp]),'verification.VerifyQuestUpgrade',out/'upgrade-smoke',a.upgrade_base.resolve(),a.jar.resolve(),a.upgrade_manifest.resolve()])
  results['signedOldToNewAndRepeatPreparationPassed']=True
 results.update({'nativeQuestProgressExecuted':True,'nativeNbtExecuted':True,'nativePersistentPlayerDataExecuted':True,'nativeCustomDataHelperExecuted':True,'entityShells':'API doubles','serverStarted':False,'status':'passed'})
 (out/'report.json').write_text(json.dumps(results,indent=2))
if __name__=='__main__':main()
