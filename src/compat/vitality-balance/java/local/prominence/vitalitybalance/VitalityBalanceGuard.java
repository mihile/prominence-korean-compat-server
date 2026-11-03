package local.prominence.vitalitybalance;

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

public final class VitalityBalanceGuard implements IMixinConfigPlugin {
    private boolean enabled;
    public static boolean matches(String minecraft,String zenith,String attributes,String charmHash,String matcherHash,String effectHash) {
        return "1.20.1".equals(minecraft) && "2.0.2".equals(zenith) && "1.0.3".equals(attributes)
          && "47b076a6737825244798186b87de3711332a3f29cbf627970a212af20d2071dd".equals(charmHash)
          && "3d7dd17def373ae9cc3760ddd06189fea9aea86518f71c5c64d4a0f8f189f0ef".equals(matcherHash)
          && "402b880c34dce4ae4b39b3765b04147caf34c6ab7d87878a59ddef5d949e0ac6".equals(effectHash);
    }
    private static String digest(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
    @Override public void onLoad(String pkg) {
        try {
            var l=FabricLoader.getInstance();var z=l.getModContainer("zenith").orElseThrow();var a=l.getModContainer("zenith_attributes").orElseThrow();
            enabled=matches(l.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString(),z.getMetadata().getVersion().getFriendlyString(),a.getMetadata().getVersion().getFriendlyString(),
                digest(Files.readAllBytes(z.findPath("dev/shadowsoffire/apotheosis/potion/PotionCharmItem.class").orElseThrow())),
                digest(Files.readAllBytes(z.findPath("dev/shadowsoffire/apotheosis/potion/PotionModule$CharmMatcher.class").orElseThrow())),
                digest(Files.readAllBytes(a.findPath("dev/shadowsoffire/attributeslib/mobfx/VitalityEffect.class").orElseThrow())));
        } catch(Exception ignored) {enabled=false;}
        LoggerFactory.getLogger("Prominence compatibility").info("Vitality charm balance and item preservation: {}",enabled?"enabled":"disabled (version or class mismatch)");
    }
    @Override public boolean shouldApplyMixin(String target,String mixin){return enabled;}
    @Override public String getRefMapperConfig(){return null;}
    @Override public List<String> getMixins(){return null;}
    @Override public void acceptTargets(Set<String>a,Set<String>b){}
    @Override public void preApply(String a,ClassNode b,String c,IMixinInfo d){}
    @Override public void postApply(String a,ClassNode b,String c,IMixinInfo d){}
}
