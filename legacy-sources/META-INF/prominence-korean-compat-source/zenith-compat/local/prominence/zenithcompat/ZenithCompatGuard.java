package local.prominence.zenithcompat;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
public final class ZenithCompatGuard implements IMixinConfigPlugin {
    private boolean tree, charm, text;
    private static boolean version(String id,String v) {return FabricLoader.getInstance().getModContainer(id).map(m->m.getMetadata().getVersion().getFriendlyString().equals(v)).orElse(false);}
    private static boolean known(String id,String name,String hash) {
        try {var m=FabricLoader.getInstance().getModContainer(id);if(m.isEmpty())return false;var p=m.get().findPath(name);return p.isPresent() && HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p.get()))).equals(hash);}
        catch(Exception e) {LoggerFactory.getLogger("Prominence compatibility").warn("Cannot verify Zenith compatibility target; retaining original behavior",e);return false;}
    }
    public void onLoad(String pkg) {
        boolean base=version("minecraft","1.20.1") && version("mixinextras","0.5.4") && !System.getProperty("prominence.zenithCompat","true").equalsIgnoreCase("false");
        boolean zenith=base && version("zenith","2.0.2");
        tree=zenith && version("treechop","0.19.0") && !System.getProperty("prominence.chainsawTreeChop","true").equalsIgnoreCase("false") && known("zenith", "dev/shadowsoffire/apotheosis/Apotheosis.class", "bd1ca87af6ec9229c3b2d394afc12b9998533d99328af4fb79f9a7a3431c298f") &&
            known("zenith", "dev/shadowsoffire/apotheosis/ench/Ench$Enchantments.class", "62682159e51ec523bf7fb372392159317d1711fda9e6386c521bc4204f0d21c5") &&
            known("zenith", "dev/shadowsoffire/apotheosis/ench/enchantments/masterwork/ChainsawEnchant.class", "0cd39ed53bf02fcd9bb6a84c63100ff1a5dd78d28f3c4c636ae5f56ac4ea3888") &&
            known("treechop", "ht/treechop/common/FabricCommon.class", "a26ae0b71062227620a28e7e6147d03581cea8d4c3d778305506e533416a35ee") &&
            known("treechop", "ht/treechop/common/chop/ChopUtil.class", "a29a5f1484f6b4c3f126fa98046a4cbd0c20297c226663ac5a6b85118dc9c99b");
        charm=false;
        text=false;
        LoggerFactory.getLogger("Prominence compatibility").info("Zenith compatibility: chainsawTreeChop={}, charmTooltip={}, nonItemTooltip={}",tree,charm,text);
    }
    public boolean shouldApplyMixin(String target,String mixin) {
        if(mixin.endsWith("TreeChopChainsawMixin") || mixin.endsWith("ChainsawTreeAccess"))return tree;
        return mixin.endsWith("PotionCharmTooltipMixin") ? charm : text;
    }
    public String getRefMapperConfig(){return null;}
    public List<String> getMixins(){return null;}
    public void acceptTargets(Set<String>a,Set<String>b){}
    public void preApply(String a,ClassNode b,String c,IMixinInfo d){}
    public void postApply(String a,ClassNode b,String c,IMixinInfo d){}
}
