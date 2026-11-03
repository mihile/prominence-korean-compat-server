"""Transform both installed Zenith classes with the real Mixin engine offline."""
from pathlib import Path
import argparse,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
MAIN=r'''    public static void main(String[] args) throws Exception {
        Path configs=Path.of(args[0]);Files.createDirectories(configs);
        List<URL> urls=new ArrayList<>();urls.add(configs.toUri().toURL());
        for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
        LOADER=new URLClassLoader(urls.toArray(URL[]::new),VerifyMixinTransforms.class.getClassLoader());
        String config;try(InputStream in=LOADER.getResourceAsStream("prominence-vitality-balance.mixins.json")){config=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        config=config.replace("local.prominence.vitalitybalance.VitalityBalanceGuard",OfflinePlugin.class.getName());
        Files.writeString(configs.resolve("offline-vitality.mixins.json"),config);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);
        phase.invoke(null,MixinEnvironment.Phase.INIT);Mixins.addConfiguration("offline-vitality.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);
        int checked=0;
        for(String target:List.of("dev.shadowsoffire.apotheosis.potion.PotionCharmItem","dev.shadowsoffire.apotheosis.potion.PotionModule$CharmMatcher")){
            byte[] raw;try(InputStream in=LOADER.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
            byte[] changed=TRANSFORMER.transformClassBytes(target,target,raw);if(Arrays.equals(raw,changed))throw new AssertionError("Mixin not applied "+target);
            ClassNode cn=new ClassNode();new ClassReader(changed).accept(cn,0);int helper=0,cancel=0;
            for(MethodNode m:cn.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(cn.name,m);
                for(AbstractInsnNode i=m.instructions.getFirst();i!=null;i=i.getNext())if(i instanceof MethodInsnNode n){
                    if(n.owner.equals("local/prominence/vitalitybalance/VitalityLimit")&&n.name.equals("restricted"))helper++;
                    if(n.owner.contains("CallbackInfo")&&(n.name.equals("cancel")||n.name.equals("setReturnValue")))cancel++;
                }
            }
            if(helper!=1||cancel!=1)throw new AssertionError(target+" helper="+helper+" cancel="+cancel);checked++;
        }
        if(OfflinePlugin.applied!=2)throw new AssertionError("expected two mixins: "+OfflinePlugin.applied);
        var good=new net.minecraft.class_2960("zenith_attributes","vitality");
        var other=new net.minecraft.class_2960("minecraft","speed");
        if(!local.prominence.vitalitybalance.VitalityLimit.restricted(good,2))throw new AssertionError("III not restricted");
        for(int amp:new int[]{0,1,3,4})if(local.prominence.vitalitybalance.VitalityLimit.restricted(good,amp))throw new AssertionError("unexpected amplifier "+amp);
        if(local.prominence.vitalitybalance.VitalityLimit.restricted(other,2)||local.prominence.vitalitybalance.VitalityLimit.restricted((net.minecraft.class_2960)null,2))throw new AssertionError("unrelated effect restricted");
        String ch="47b076a6737825244798186b87de3711332a3f29cbf627970a212af20d2071dd",ma="3d7dd17def373ae9cc3760ddd06189fea9aea86518f71c5c64d4a0f8f189f0ef",ef="402b880c34dce4ae4b39b3765b04147caf34c6ab7d87878a59ddef5d949e0ac6";
        if(!local.prominence.vitalitybalance.VitalityBalanceGuard.matches("1.20.1","2.0.2","1.0.3",ch,ma,ef))throw new AssertionError("valid guard rejected");
        for(String[] x:List.of(new String[]{"1.20.2","2.0.2","1.0.3",ch,ma,ef},new String[]{"1.20.1","2.0.3","1.0.3",ch,ma,ef},new String[]{"1.20.1","2.0.2","1.0.4",ch,ma,ef},new String[]{"1.20.1","2.0.2","1.0.3","bad",ma,ef},new String[]{"1.20.1","2.0.2","1.0.3",ch,"bad",ef},new String[]{"1.20.1","2.0.2","1.0.3",ch,ma,"bad"}))
            if(local.prominence.vitalitybalance.VitalityBalanceGuard.matches(x[0],x[1],x[2],x[3],x[4],x[5]))throw new AssertionError("guard accepted mismatch");
        System.out.println("PASS: two native Mixin transforms, ASM verification, vitality-only limits and six guard mismatch cases");
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
