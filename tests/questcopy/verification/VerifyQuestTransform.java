package verification;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import local.prominence.questcopy.CopyRewriter;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.extensibility.*;
import org.spongepowered.asm.mixin.transformer.*;

public final class VerifyQuestTransform {
    static ClassNode reviewed;
    static ClassNode read(byte[] bytes) {var n=new ClassNode();new ClassReader(bytes).accept(n,0);return n;}
    public static class Plugin implements IMixinConfigPlugin {
        public void onLoad(String s) {} public String getRefMapperConfig(){return null;}
        public boolean shouldApplyMixin(String a,String b){return true;} public void acceptTargets(Set<String>a,Set<String>b){}
        public List<String> getMixins(){return null;} public void preApply(String a,ClassNode b,String c,IMixinInfo d){}
        public void postApply(String a,ClassNode b,String c,IMixinInfo d){if(!CopyRewriter.apply(b,reviewed))throw new AssertionError("copy not removed");}
    }
    public static void main(String[] args)throws Exception {
        Path work=Path.of(args[0]),configs=work.resolve("configs");Files.createDirectories(configs);
        String config;try(var in=VerifyQuestTransform.class.getResourceAsStream("/prominence-quest-copy.mixins.json")){config=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
        Files.writeString(configs.resolve("offline-quest-copy.mixins.json"),config.replace("local.prominence.questcopy.CopyGuard",Plugin.class.getName()));
        var urls=new ArrayList<URL>();urls.add(configs.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
        var loader=new URLClassLoader(urls.toArray(URL[]::new),VerifyQuestTransform.class.getClassLoader());
        byte[] raw;try(var in=loader.getResourceAsStream("elocindev/prominent/api/questing/QuestProgress.class")){raw=in.readAllBytes();}reviewed=read(raw);
        var field=VerifyMixinTransforms.class.getDeclaredField("LOADER");field.setAccessible(true);field.set(null,loader);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);
        phase.invoke(null,MixinEnvironment.Phase.INIT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("intermediary");
        Mixins.addConfiguration("offline-quest-copy.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);field=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");field.setAccessible(true);var transformer=(IMixinTransformer)field.get(null);
        String name="elocindev.prominent.api.questing.QuestProgress";
        byte[] fixed=transformer.transformClassBytes(name,name,raw);ClassNode changed=read(fixed);
        int verified=0,unchanged=0;
        for(var m:changed.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(changed.name,m);verified++;}
        for(var m:reviewed.methods){var other=changed.methods.stream().filter(x->x.name.equals(m.name)&&x.desc.equals(m.desc)).findFirst().orElseThrow();
            var oldOps=VerifyMythicTransform.instructions(m);var newOps=VerifyMythicTransform.instructions(other);
            if(m.name.equals("open")){var expected=new ArrayList<>(oldOps);String copy="MethodInsnNode:182:net/minecraft/class_2487:method_10553:()Lnet/minecraft/class_2487;";int index=expected.indexOf(copy);if(index<0)throw new AssertionError("no first copy");expected.remove(index);
                // Independently adjust branch destinations after the removed instruction.
                for(int j=0;j<expected.size();j++)if(expected.get(j).startsWith("JumpInsnNode:")){String[] pieces=expected.get(j).split(":");int dest=Integer.parseInt(pieces[2]);if(dest>index)expected.set(j,pieces[0]+":"+pieces[1]+":"+(dest-1));}
                if(!expected.equals(newOps))throw new AssertionError("unexpected transformed body");
                if(oldOps.size()!=newOps.size()+1||newOps.stream().filter(x->x.equals(copy)).count()!=1)throw new AssertionError("selected quest copy removed");
            }else {if(!oldOps.equals(newOps))throw new AssertionError("other method changed "+m.name);unchanged++;}}
        if(CopyRewriter.apply(changed,reviewed))throw new AssertionError("double rewrite");
        var unknown=read(raw);unknown.methods.stream().filter(x->x.name.equals("open")).findFirst().orElseThrow().instructions.insert(new InsnNode(Opcodes.NOP));
        if(CopyRewriter.apply(unknown,reviewed))throw new AssertionError("other transform ignored");
        var signature=read(raw);signature.methods.stream().filter(x->x.name.equals("open")).findFirst().orElseThrow().desc="()V";
        if(CopyRewriter.apply(signature,reviewed))throw new AssertionError("signature ignored");
        if(CopyRewriter.apply(new ClassNode(),reviewed))throw new AssertionError("missing target ignored");
        Path path=Path.of("elocindev/prominent/api/questing/QuestProgress.class");
        for(String mode:new String[]{"baseline","fixed"}){Path out=work.resolve(mode).resolve(path);Files.createDirectories(out.getParent());Files.write(out,mode.equals("baseline")?raw:fixed);}
        System.out.println("PASS: real QuestProgress Mixin, "+verified+" ASM methods, "+unchanged+" unchanged native methods; one intermediate copy removed, four shape rejection checks");
    }
}
