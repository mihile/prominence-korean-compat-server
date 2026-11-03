"""Transform four native equipment effect classes with the real Mixin engine offline."""
from pathlib import Path
import argparse,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
MAIN=r'''    public static void main(String[] args) throws Exception {
        Path configs=Path.of(args[0]);Files.createDirectories(configs);
        List<URL> urls=new ArrayList<>();urls.add(configs.toUri().toURL());
        for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
        LOADER=new URLClassLoader(urls.toArray(URL[]::new),VerifyMixinTransforms.class.getClassLoader());
        String config;try(InputStream in=LOADER.getResourceAsStream("prominence-equipment-balance.mixins.json")){config=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        config=config.replace("local.prominence.equipment.EquipmentGuard",OfflinePlugin.class.getName());
        Files.writeString(configs.resolve("offline-vitality.mixins.json"),config);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);
        phase.invoke(null,MixinEnvironment.Phase.INIT);Mixins.addConfiguration("offline-vitality.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);
        int checked=0;
        for(String target:List.of("elocindev.prominent.primordial.set.FrostMageArmorSetBonus","elocindev.prominent.primordial.set.FrostDkArmorSetBonus","elocindev.prominent.spells.frostmourne.RemorselessWinter","elocindev.prominent.spells.frostmourne.AgonizingBreath")){
            byte[] raw;try(InputStream in=LOADER.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
            byte[] changed=TRANSFORMER.transformClassBytes(target,target,raw);if(Arrays.equals(raw,changed))throw new AssertionError("Mixin not applied "+target);
            ClassNode cn=new ClassNode();new ClassReader(changed).accept(cn,0);int helper=0,cancel=0;
            for(MethodNode m:cn.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(cn.name,m);
                for(AbstractInsnNode i=m.instructions.getFirst();i!=null;i=i.getNext())if(i instanceof MethodInsnNode n){
                    if(n.owner.equals("local/prominence/equipment/CriticalStats")&&n.name.equals("attributeValue"))helper++;
                    if(n.owner.contains("CallbackInfo")&&(n.name.equals("cancel")||n.name.equals("setReturnValue")))cancel++;
                }
            }
            if(helper!=1)throw new AssertionError(target+" helper="+helper+" cancel="+cancel);checked++;
        }
        if(OfflinePlugin.applied!=4)throw new AssertionError("expected two mixins: "+OfflinePlugin.applied);
        if(local.prominence.equipment.CriticalStats.percentUnits(0.05)!=5.0)throw new AssertionError("base crit conversion");
        if(local.prominence.equipment.CriticalStats.percentUnits(0.25)!=25.0)throw new AssertionError("increased crit conversion");
        if(local.prominence.equipment.CriticalStats.percentUnits(Double.NaN)!=0.0)throw new AssertionError("nonfinite");
        System.out.println("PASS: four native Mixin transforms, ASM verification and critical chance conversions");
    }
}
'''
def main():
 a=argparse.ArgumentParser()
 for n in ['jar','classpath','java-home','output','mods']:a.add_argument('--'+n,type=Path,required=True)
 args=a.parse_args();out=args.output.resolve();classes=out/'classes';services=classes/'META-INF/services';services.mkdir(parents=True,exist_ok=True)
 for iface,impl in [('IMixinService','verification.VerifyMixinTransforms'),('IMixinServiceBootstrap','verification.VerifyMixinTransforms$Bootstrap'),('IGlobalPropertyService','verification.VerifyMixinTransforms$Props')]:
  (services/('org.spongepowered.asm.service.'+iface)).write_text(impl+'\n')
 template=(ROOT/'tests/arrow/verification/VerifyMixinTransforms.java').read_text('utf8')
 src=template[:template.index('    public static void main(')]+MAIN
 java=out/'src/verification/VerifyMixinTransforms.java';java.parent.mkdir(parents=True,exist_ok=True);java.write_text(src,'utf8')
 cp=';'.join([str(classes),str(args.jar.resolve()),*[str(p.resolve()) for p in args.mods.glob('*.jar')],*json.loads(args.classpath.read_text('utf8'))])
 options=['--release','17','-proc:none','-classpath',cp,'-d',str(classes),str(java)]
 f=out/'compile.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in options),'utf8')
 subprocess.run([str(args.java_home/'bin/javac.exe'),'-J-Xmx256m','@'+str(f)],check=True)
 f=out/'runtime.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in ['-Xmx384m','-cp',cp,'verification.VerifyMixinTransforms',str(out/'resources')]),'utf8')
 result=subprocess.run([str(args.java_home/'bin/java.exe'),'@'+str(f)],cwd=out,capture_output=True,text=True,encoding='utf8',errors='replace')
 (out/'transform.log').write_text(result.stdout+result.stderr,'utf8');assert result.returncode==0,result.stdout+result.stderr;print(result.stdout)
if __name__=='__main__':main()
