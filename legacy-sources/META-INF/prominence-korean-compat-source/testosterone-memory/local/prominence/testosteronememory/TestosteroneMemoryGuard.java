package local.prominence.testosteronememory;

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

public final class TestosteroneMemoryGuard implements IMixinConfigPlugin {
    private static final String CLASS = "net/mifort/testosterone/events/fluidEffectHandler.class";
    private static final String HASH = "d2f5316c85d38dabfd299707aa0e25e03e680082d788f6ccf3d8b41245e5d881";
    private boolean enabled;

    public static boolean supports(String minecraft, String version, byte[] original) {
        if (!"1.20.1".equals(minecraft) || !"2.0.3".equals(version) || original == null) return false;
        try {
            return HASH.equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original)));
        } catch (Exception e) { return false; }
    }

    @Override public void onLoad(String pkg) {
        var loader = FabricLoader.getInstance();
        var mod = loader.getModContainer("testosterone");
        if (mod.isPresent()) {
            try {
                var path = mod.get().findPath(CLASS);
                String minecraft = loader.getModContainer("minecraft").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
                enabled = supports(minecraft, mod.get().getMetadata().getVersion().getFriendlyString(),
                        path.isPresent() ? Files.readAllBytes(path.get()) : null);
            } catch (Exception ignored) { enabled = false; }
            LoggerFactory.getLogger("Prominence compatibility").info("Testosterone fluid memory repair: {}", enabled ? "enabled" : "skipped (unrecognized version/code)");
        }
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) { return enabled; }
    @Override public String getRefMapperConfig() { return null; }
    @Override public List<String> getMixins() { return null; }
    @Override public void acceptTargets(Set<String> a, Set<String> b) {}
    @Override public void preApply(String a, ClassNode b, String c, IMixinInfo d) {}
    @Override public void postApply(String a, ClassNode b, String c, IMixinInfo d) {}
}
