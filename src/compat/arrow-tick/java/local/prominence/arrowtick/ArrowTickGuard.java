/* SPDX-License-Identifier: MIT */
package local.prominence.arrowtick;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.*;
public final class ArrowTickGuard implements IMixinConfigPlugin {
    private static final Map<String,String> ENGINE=Map.of(
        "net/spell_engine/internals/SpellHelper.class","669afdc0bead19d69f85c9d2d7cf9f9934056958b4b8c8d21077d9e6bcdac2f5",
        "net/spell_engine/internals/SpellHelper$ImpactContext.class","d7adafc3ad6afe61ad537068de7d498a25271d9acb43f9fd897fab14adf3d1fb",
        "net/spell_engine/mixin/arrow/PersistentProjectileEntityMixin.class","bde1b36133e0dae7e4e8ffc9c05f9e2e99160a59a9ef02234b1edfb44f24421f",
        "net/spell_engine/api/spell/Spell$AreaImpact.class","ba07f3b9e0ad4c777c37c1f48f18e3ced256eb12f9982b1575ef77e272fe25ba");
    private static final Map<String,String> POWER=Map.of("net/spell_power/api/SpellPower.class","25756a34c807ccaf217cbfe5986f01861a4ee404247a3987bcf9cbbf43d90d15");
    private static final Map<String,String> NERUINA=Map.of("com/bawnorton/neruina/util/TickingEntry.class","a73c3f1a25c45b788f3f8437043d7e454b88a0d9c51489a0603e2cf73ee78c6e");
    private boolean arrow,neruina;
    public static boolean matches(Map<String,String> pins,Map<String,byte[]> originals) {
        if (originals==null) return false;
        try {for(var p:pins.entrySet()) {
            byte[] raw=originals.get(p.getKey());
            if(raw==null||!p.getValue().equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw))))return false;
        }return true;}catch(Exception e){return false;}
    }
    public static Map<String,String> pins(String mod) { return switch(mod) {case "spell_engine"->ENGINE;case "spell_power"->POWER;case "neruina"->NERUINA;default->Map.of();}; }
    private boolean known(String id,String version,Map<String,String> pins) {
        var mod=FabricLoader.getInstance().getModContainer(id);
        if(mod.isEmpty()||!version.equals(mod.get().getMetadata().getVersion().getFriendlyString()))return false;
        try {Map<String,byte[]> raw=new HashMap<>();for(String name:pins.keySet()) {
            var p=mod.get().findPath(name);if(p.isPresent())raw.put(name,Files.readAllBytes(p.get()));
        }return matches(pins,raw);}catch(Exception e){return false;}
    }
    public void onLoad(String pkg) {
        boolean mc=FabricLoader.getInstance().getModContainer("minecraft").map(m->m.getMetadata().getVersion().getFriendlyString().equals("1.20.1")).orElse(false);
        arrow=mc&&known("spell_engine","0.15.12+1.20.1",ENGINE)&&known("spell_power","0.12.0+1.20.1",POWER);
        neruina=mc&&known("neruina","2.3.1-beta.1",NERUINA);
        LoggerFactory.getLogger("Prominence compatibility").info("Arrow tick compatibility: power={}, neruinaUuid={}",arrow?"enabled":"skipped",neruina?"enabled":"skipped");
    }
    public boolean shouldApplyMixin(String target,String mixin){return mixin.endsWith("NeruinaEntryMixin")?neruina:arrow;}
    public String getRefMapperConfig(){return null;}public List<String> getMixins(){return null;}
    public void acceptTargets(Set<String>a,Set<String>b){}public void preApply(String a,ClassNode b,String c,IMixinInfo d){}public void postApply(String a,ClassNode b,String c,IMixinInfo d){}
}
