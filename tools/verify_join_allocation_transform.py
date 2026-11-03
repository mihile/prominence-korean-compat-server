"""Transform native LOD packet/Invoke classes with the real Mixin engine offline."""
from pathlib import Path
import argparse,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
MAIN=r'''    public static void main(String[] args) throws Exception {
        Path configs=Path.of(args[0]);Files.createDirectories(configs);
        List<URL> urls=new ArrayList<>();urls.add(configs.toUri().toURL());
        for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
        LOADER=new URLClassLoader(urls.toArray(URL[]::new),VerifyMixinTransforms.class.getClassLoader());
        String config;try(InputStream in=LOADER.getResourceAsStream("prominence-join-allocation-balance.mixins.json")){config=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        config=config.replace("local.prominence.joinallocation.JoinAllocationGuard",OfflinePlugin.class.getName());
        Files.writeString(configs.resolve("offline-join.mixins.json"),config);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);
        phase.invoke(null,MixinEnvironment.Phase.INIT);Mixins.addConfiguration("offline-join.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);
        int checked=0;
        for(String target:List.of("com.dripps.voxyserver.network.PreSerializedLodPayload","com.invoke.InvokeMod")){
            byte[] raw;try(InputStream in=LOADER.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
            if(target.endsWith("InvokeMod")){
                ClassNode original=new ClassNode();new ClassReader(raw).accept(original,0);int calls=0,stores=0,loads=0;
                for(MethodNode m:original.methods)if(m.name.equals("lambda$onInitialize$5"))for(AbstractInsnNode i:m.instructions){
                    if(i instanceof MethodInsnNode n&&n.name.equals("getCooldownDuration"))calls++;
                    if(i instanceof org.objectweb.asm.tree.VarInsnNode n&&n.var==5){if(n.getOpcode()==org.objectweb.asm.Opcodes.FSTORE)stores++;if(n.getOpcode()==org.objectweb.asm.Opcodes.FLOAD)loads++;}
                }
                if(calls!=1||stores!=1||loads!=0)throw new AssertionError("native cooldown result not demonstrably unused");
            }
            byte[] changed=TRANSFORMER.transformClassBytes(target,target,raw);if(Arrays.equals(raw,changed))throw new AssertionError("Mixin not applied "+target);
            ClassNode cn=new ClassNode();new ClassReader(changed).accept(cn,0);int helper=0,cancel=0;
            for(MethodNode m:cn.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(cn.name,m);
                for(AbstractInsnNode i=m.instructions.getFirst();i!=null;i=i.getNext())if(i instanceof MethodInsnNode n){
                    if(n.owner.equals("local/prominence/joinallocation/EncodedSize")&&n.name.equals("capacity"))helper++;
                    if(n.owner.contains("CallbackInfo")&&(n.name.equals("cancel")||n.name.equals("setReturnValue")))cancel++;
                }
            }
            if(target.endsWith("InvokeMod")){
                int oldCalls=0,redirectCalls=0;
                for(MethodNode m:cn.methods){for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode n&&m.name.equals("lambda$onInitialize$5")){
                    if(n.name.equals("getCooldownDuration"))oldCalls++;
                    if(n.name.contains("omitUnusedValue"))redirectCalls++;
                }}
                if(oldCalls!=0||redirectCalls!=1)throw new AssertionError("unused call was not narrowly redirected");
            }
            if(helper!=(target.endsWith("InvokeMod")?0:1))throw new AssertionError(target+" helper="+helper+" cancel="+cancel);checked++;
        }
        if(OfflinePlugin.applied!=2)throw new AssertionError("expected two mixins: "+OfflinePlugin.applied);
        byte[] patchedBytes;{
            String t="com.dripps.voxyserver.network.PreSerializedLodPayload";
            byte[] raw;try(InputStream in=LOADER.getResourceAsStream(t.replace('.','/')+".class")){raw=in.readAllBytes();}
            patchedBytes=TRANSFORMER.transformClassBytes(t,t,raw);
        }
        ClassLoader executing=new ClassLoader(VerifyMixinTransforms.class.getClassLoader()){
            protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{
                if(!n.equals("com.dripps.voxyserver.network.PreSerializedLodPayload"))return super.loadClass(n,resolve);
                synchronized(getClassLoadingLock(n)){Class<?> c=findLoadedClass(n);if(c==null)c=defineClass(n,patchedBytes,0,patchedBytes.length);if(resolve)resolveClass(c);return c;}
            }
        };
        Class<?> patchedClass=executing.loadClass("com.dripps.voxyserver.network.PreSerializedLodPayload");
        for(int lut:new int[]{1,16,257})for(int len:new int[]{65,32768}){
            var dim=new net.minecraft.class_2960("minecraft","overworld");int[] b=new int[lut],bio=new int[lut];byte[] light=new byte[lut];short[] idx=new short[len];
            for(int i=0;i<lut;i++){b[i]=i*127;bio[i]=i;light[i]=(byte)i;}for(int i=0;i<len;i++)idx[i]=(short)(i%lut);
            var sec=new com.dripps.voxyserver.network.LODSectionPayload(dim,42,b,bio,light,idx,9876);
            var bulk=new com.dripps.voxyserver.network.LODBulkPayload(dim,List.of(sec));
            byte[] before=com.dripps.voxyserver.network.PreSerializedLodPayload.fromBulk(bulk,null).data();
            Object after=patchedClass.getMethod("fromBulk",com.dripps.voxyserver.network.LODBulkPayload.class,net.minecraft.class_5455.class).invoke(null,bulk,null);
            if(!Arrays.equals(before,(byte[])patchedClass.getMethod("data").invoke(after)))throw new AssertionError("transformed packet bytes changed");
        }
        System.out.println("PASS: native LOD buffer and Invoke dead-result transforms, unused-local proof, ASM verification and six executed transformed packet comparisons");
    }
}
'''
def main():
 a=argparse.ArgumentParser()
 for n in ['jar','classpath','java-home','output','mods']:a.add_argument('--'+n,type=Path,required=True)
 a.add_argument('--server-jar',type=Path,required=True)
 args=a.parse_args();out=args.output.resolve();classes=out/'classes';services=classes/'META-INF/services';services.mkdir(parents=True,exist_ok=True)
 for iface,impl in [('IMixinService','verification.VerifyMixinTransforms'),('IMixinServiceBootstrap','verification.VerifyMixinTransforms$Bootstrap'),('IGlobalPropertyService','verification.VerifyMixinTransforms$Props')]:
  (services/('org.spongepowered.asm.service.'+iface)).write_text(impl+'\n')
 template=(ROOT/'tests/arrow/verification/VerifyMixinTransforms.java').read_text('utf8')
 src=template[:template.index('    public static void main(')]+MAIN
 java=out/'src/verification/VerifyMixinTransforms.java';java.parent.mkdir(parents=True,exist_ok=True);java.write_text(src,'utf8')
 cp=';'.join([str(classes),str(args.jar.resolve()),str(args.server_jar.resolve()),*[str(p.resolve()) for p in args.mods.glob('*.jar') if not p.name.startswith('VoxyServer-')],*json.loads(args.classpath.read_text('utf8'))])
 options=['--release','17','-proc:none','-classpath',cp,'-d',str(classes),str(java)]
 f=out/'compile.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in options),'utf8')
 subprocess.run([str(args.java_home/'bin/javac.exe'),'-J-Xmx256m','@'+str(f)],check=True)
 f=out/'runtime.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in ['-Xmx384m','-cp',cp,'verification.VerifyMixinTransforms',str(out/'resources')]),'utf8')
 result=subprocess.run([str(args.java_home/'bin/java.exe'),'@'+str(f)],cwd=out,capture_output=True,text=True,encoding='utf8',errors='replace')
 (out/'transform.log').write_text(result.stdout+result.stderr,'utf8');assert result.returncode==0,result.stdout+result.stderr;print(result.stdout)
if __name__=='__main__':main()
