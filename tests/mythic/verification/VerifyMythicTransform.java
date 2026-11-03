package verification;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import local.prominence.mythicrefresh.RefreshRewriter;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.extensibility.*;
import org.spongepowered.asm.mixin.transformer.*;

public final class VerifyMythicTransform {
    static byte[] before;
    public static class Plugin implements IMixinConfigPlugin {
        public void onLoad(String s) {} public String getRefMapperConfig(){return null;}
        public boolean shouldApplyMixin(String a,String b){return true;} public void acceptTargets(Set<String>a,Set<String>b){}
        public List<String> getMixins(){return null;} public void preApply(String a,ClassNode b,String c,IMixinInfo d){}
        public void postApply(String a,ClassNode b,String c,IMixinInfo d){
            ClassWriter writer=new ClassWriter(0);b.accept(writer);before=writer.toByteArray();
            if(!RefreshRewriter.apply(b))throw new AssertionError("native callback not wrapped");
        }
    }
    static MethodNode callback(ClassNode node){return node.methods.stream().filter(m->m.name.endsWith("$updateByConfigs")).findFirst().orElseThrow();}
    static byte[] oneMethod(ClassNode node,MethodNode method){ClassWriter writer=new ClassWriter(0);writer.visit(node.version,node.access,node.name,null,node.superName,null);method.accept(writer);writer.visitEnd();return writer.toByteArray();}
    static List<String> instructions(MethodNode method){var result=new ArrayList<String>();var labels=new IdentityHashMap<LabelNode,Integer>();int index=0;
        for(var i:method.instructions){if(i instanceof LabelNode label)labels.put(label,index);if(i.getOpcode()>=0)index++;}
        for(var i:method.instructions){
        if(i.getOpcode()<0)continue;String s=i.getClass().getSimpleName()+":"+i.getOpcode();
        if(i instanceof VarInsnNode n)s+=":"+n.var;else if(i instanceof MethodInsnNode n)s+=":"+n.owner+":"+n.name+":"+n.desc;
        else if(i instanceof FieldInsnNode n)s+=":"+n.owner+":"+n.name+":"+n.desc;else if(i instanceof TypeInsnNode n)s+=":"+n.desc;
        else if(i instanceof IntInsnNode n)s+=":"+n.operand;else if(i instanceof LdcInsnNode n)s+=":"+n.cst;
        else if(i instanceof JumpInsnNode n)s+=":"+labels.get(n.label);
        else if(i instanceof IincInsnNode n)s+=":"+n.var+":"+n.incr;
        else if(i instanceof InvokeDynamicInsnNode n)s+=":"+n.name+":"+n.desc+":"+n.bsm+":"+Arrays.toString(n.bsmArgs);
        else if(i instanceof TableSwitchInsnNode n)s+=":"+n.min+":"+n.max+":"+labels.get(n.dflt)+":"+n.labels.stream().map(labels::get).toList();
        else if(i instanceof LookupSwitchInsnNode n)s+=":"+n.keys+":"+labels.get(n.dflt)+":"+n.labels.stream().map(labels::get).toList();
        else if(i instanceof MultiANewArrayInsnNode n)s+=":"+n.desc+":"+n.dims;
        result.add(s);
    }
    for(var block:method.tryCatchBlocks)result.add("catch:"+block.type+":"+labels.get(block.start)+":"+labels.get(block.end)+":"+labels.get(block.handler));
    return result;}
    public static void main(String[] args)throws Exception {
        Path work=Path.of(args[0]),configs=work.resolve("configs");Files.createDirectories(configs);
        String config;try(var in=VerifyMythicTransform.class.getResourceAsStream("/prominence-mythic-refresh.mixins.json")){config=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
        Files.writeString(configs.resolve("offline-mythic-refresh.mixins.json"),config.replace("local.prominence.mythicrefresh.RefreshGuard",Plugin.class.getName()));
        Files.writeString(configs.resolve("offline-mythic-native.mixins.json"),"""
        {"required":true,"package":"net.trique.mythicupgrades.mixin","compatibilityLevel":"JAVA_17","mixins":["ServerWorldMixin"],"refmap":"mythicupgrades-refmap.json","injectors":{"defaultRequire":1}}
        """);
        var urls=new ArrayList<URL>();urls.add(configs.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
        var loader=new URLClassLoader(urls.toArray(URL[]::new),VerifyMythicTransform.class.getClassLoader());
        var field=VerifyMixinTransforms.class.getDeclaredField("LOADER");field.setAccessible(true);field.set(null,loader);
        MixinBootstrap.init();Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);
        phase.invoke(null,MixinEnvironment.Phase.INIT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        Mixins.addConfiguration("offline-mythic-native.mixins.json");Mixins.addConfiguration("offline-mythic-refresh.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);field=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");field.setAccessible(true);var transformer=(IMixinTransformer)field.get(null);
        byte[] raw;try(var in=loader.getResourceAsStream("net/minecraft/class_3218.class")){raw=in.readAllBytes();}
        byte[] fixed=transformer.transformClassBytes("net.minecraft.class_3218","net.minecraft.class_3218",raw);
        ClassNode original=new ClassNode(),changed=new ClassNode();new ClassReader(before).accept(original,0);new ClassReader(fixed).accept(changed,0);
        var old=callback(original);var now=callback(changed);var oldOps=instructions(old);var newOps=instructions(now);
        var stripped=new ArrayList<>(newOps.subList(5,newOps.size()-4));stripped.add(newOps.get(newOps.size()-1));
        if(!oldOps.equals(stripped))throw new AssertionError("native callback body changed");
        int setters=0,builders=0;for(var i:old.instructions){
            if(i instanceof VarInsnNode n&&n.var==0)throw new AssertionError("native callback reads world");
            if(i instanceof MethodInsnNode n&&n.name.equals("setNewEffects"))setters++;
            if(i instanceof TypeInsnNode n&&n.getOpcode()==Opcodes.NEW&&n.desc.endsWith("ItemEffectsListBuilder"))builders++;
        }
        if(setters!=53||builders!=12)throw new AssertionError("unexpected native refresh outputs");
        int unchanged=0,verified=0;for(var method:changed.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(changed.name,method);verified++;}
        for(var method:original.methods)if(method!=old){var after=changed.methods.stream().filter(m->m.name.equals(method.name)&&m.desc.equals(method.desc)).findFirst().orElseThrow();if(!instructions(method).equals(instructions(after)))throw new AssertionError("native instruction/control flow changed "+method.name);unchanged++;}
        if(RefreshRewriter.apply(changed))throw new AssertionError("double wrapping accepted");
        var missing=new ClassNode();if(RefreshRewriter.apply(missing))throw new AssertionError("missing callback modified");
        var unsafe=new ClassNode();new ClassReader(before).accept(unsafe,0);callback(unsafe).instructions.insert(new VarInsnNode(Opcodes.ALOAD,0));
        if(RefreshRewriter.apply(unsafe))throw new AssertionError("world-dependent callback modified");
        var signature=new ClassNode();new ClassReader(before).accept(signature,0);callback(signature).desc="()V";
        if(RefreshRewriter.apply(signature))throw new AssertionError("unknown callback signature modified");
        Files.write(work.resolve("before-world.class"),before);Files.write(work.resolve("fixed-world.class"),fixed);
        // Describe only referenced native API shapes for the tiny fixture generator.
        JsonObject model=new JsonObject();JsonArray fields=new JsonArray(),calls=new JsonArray();Set<String> seen=new HashSet<>();
        for(var i:old.instructions){
            if(i instanceof FieldInsnNode n&&seen.add("F"+n.owner+n.name+n.desc)){JsonObject o=new JsonObject();o.addProperty("owner",n.owner);o.addProperty("name",n.name);o.addProperty("desc",n.desc);o.addProperty("opcode",n.getOpcode());fields.add(o);}
            if(i instanceof MethodInsnNode n&&seen.add("M"+n.owner+n.name+n.desc)){JsonObject o=new JsonObject();o.addProperty("owner",n.owner);o.addProperty("name",n.name);o.addProperty("desc",n.desc);calls.add(o);}
        }
        model.add("fields",fields);model.add("calls",calls);JsonObject parents=new JsonObject();
        for(var item:fields){String desc=item.getAsJsonObject().get("desc").getAsString();if(desc.startsWith("Lnet/trique/mythicupgrades/item/")){String name=desc.substring(1,desc.length()-1);try(var in=loader.getResourceAsStream(name+".class")){parents.addProperty(name,new ClassReader(in.readAllBytes()).getSuperName());}}}
        model.add("parents",parents);Files.writeString(work.resolve("fixture-model.json"),new GsonBuilder().setPrettyPrinting().create().toJson(model));
        JsonObject pins;try(var in=loader.getResourceAsStream("prominence-mythic-refresh-pins.json")){pins=JsonParser.parseReader(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
        for(var pin:pins.entrySet()){
            byte[] data;try(var in=loader.getResourceAsStream(pin.getKey())){data=in.readAllBytes();}
            if(!HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data)).equals(pin.getValue().getAsString()))throw new AssertionError("wrong native pin "+pin.getKey());
            Path file=work.resolve("pins").resolve(pin.getKey());Files.createDirectories(file.getParent());Files.write(file,data);
        }
        try(var in=loader.getResourceAsStream("net/trique/mythicupgrades/mixin/ServerWorldMixin.class")){Files.write(work.resolve("native-source.class"),in.readAllBytes());}
        System.out.println("PASS: actual native Mythic world Mixin + compatibility post-apply, "+verified+" ASM methods, "+unchanged+" unchanged methods; full 12-builder/53-setter body preserved");
    }
}
