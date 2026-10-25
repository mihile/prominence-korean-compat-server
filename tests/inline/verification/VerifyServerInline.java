package verification;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import java.io.File;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.*;

/** Uses installed server class bytes, without starting Minecraft or loading a world. */
public final class VerifyServerInline {
    static List<String> calls(MethodNode m) {
        var result = new ArrayList<String>();
        for (var i:m.instructions) if (i instanceof MethodInsnNode c) result.add(c.owner+"."+c.name+c.desc);
        return result;
    }
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args[0]),configs=out.resolve("configs");Files.createDirectories(configs);
        var urls=new ArrayList<URL>();urls.add(configs.toUri().toURL());
        for(String p:System.getProperty("java.class.path").split(File.pathSeparator))if(!p.isBlank())urls.add(Path.of(p).toUri().toURL());
        var loader=new URLClassLoader(urls.toArray(URL[]::new),VerifyServerInline.class.getClassLoader());
        var field=VerifyMixinTransforms.class.getDeclaredField("LOADER");field.setAccessible(true);field.set(null,loader);
        String config;try(var in=loader.getResourceAsStream("prominence-inline-reforge.mixins.json")){config=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
        Files.writeString(configs.resolve("offline-inline.mixins.json"),config.replace("local.prominence.backpackinline.InlineGuard",VerifyMixinTransforms.OfflinePlugin.class.getName()));
        MixinBootstrap.init();var phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.INIT);
        MixinExtrasBootstrap.init();MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.SERVER);
        MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");Mixins.addConfiguration("offline-inline.mixins.json");
        for(Config c:Mixins.getConfigs()){c.getConfig().decorate(FabricUtil.KEY_MOD_ID,"prominence_korean_compat");c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY,FabricUtil.COMPATIBILITY_LATEST);}
        phase.invoke(null,MixinEnvironment.Phase.DEFAULT);MixinEnvironment.getCurrentEnvironment().setObfuscationContext("named:intermediary");
        field=VerifyMixinTransforms.class.getDeclaredField("TRANSFORMER");field.setAccessible(true);var transformer=(IMixinTransformer)field.get(null);
        int methods=0,bridges=0,preserved=0;var hooks=new HashMap<String,Integer>();
        var targets=List.of("net.minecraft.class_1735","net.minecraft.class_1703","net.minecraft.class_3244","elocindev.tierify.screen.ReforgeScreenHandler","net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeContainer","net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase");
        for(String target:targets){
            byte[] raw;try(var in=loader.getResourceAsStream(target.replace('.','/')+".class")){raw=in.readAllBytes();}
            byte[] fixed=transformer.transformClassBytes(target,target,raw);if(Arrays.equals(raw,fixed))throw new AssertionError("Untransformed "+target);
            var before=new ClassNode();var after=new ClassNode();new ClassReader(raw).accept(before,0);new ClassReader(fixed).accept(after,0);
            for(var method:after.methods){new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,method);methods++;for(String call:calls(method))if(call.startsWith("local/prominence/backpackinline/")){bridges++;hooks.merge(call.substring(0,call.indexOf('(')),1,Integer::sum);}}
            if(target.endsWith("class_3244")&&!after.interfaces.contains("local/prominence/backpackinline/HandshakeAccess"))throw new AssertionError("Handshake interface missing");
            if(target.endsWith("class_1735")&&(after.fields.stream().filter(f->f.name.equals("field_7871")).findFirst().orElseThrow().access&Opcodes.ACC_FINAL)!=0)throw new AssertionError("Slot inventory is still final");
            if(target.endsWith("ReforgeScreenHandler"))for(String name:List.of("reforge","updateResult","decrementStack","isValidAddition","method_7595")){
                var original=before.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();
                var changed=after.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(original.desc)).findFirst().orElseThrow();
                if(!calls(original).equals(calls(changed)))throw new AssertionError("Tierify mechanics changed: "+name);preserved++;
            }
            Files.write(out.resolve(target+".class"),fixed);
        }
        var expected=Map.of("local/prominence/backpackinline/InlineReforge.blocked",4,"local/prominence/backpackinline/InlineReforge.state",1,"local/prominence/backpackinline/InlineReforge.poll",1,"local/prominence/backpackinline/InlineState.create",1,"local/prominence/backpackinline/InlineState.activeSlots",1);
        if(!hooks.equals(expected)||preserved!=5)throw new AssertionError("Missing/extra hooks "+hooks);
        Files.writeString(out.resolve("result.json"),"{\"status\":\"passed\",\"side\":\"SERVER\",\"targets\":6,\"methods\":"+methods+",\"bridges\":"+bridges+",\"nativeBodiesPreserved\":5,\"gameStarted\":false}");
        System.out.println("PASS: SERVER inline reforge, 6 native targets / "+methods+" methods, "+bridges+" hooks; five Tierify mechanics preserved; no game/world launched.");
    }
}
