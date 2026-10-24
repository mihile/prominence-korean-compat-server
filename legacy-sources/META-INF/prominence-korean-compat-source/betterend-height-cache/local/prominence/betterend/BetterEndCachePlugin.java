package local.prominence.betterend;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Exact reviewed BetterEnd field/constructor contract, independent of C2ME presence. */
public final class BetterEndCachePlugin implements IMixinConfigPlugin {
    private boolean enabled;
    @Override public void onLoad(String pkg) {
        FabricLoader loader = FabricLoader.getInstance();
        String end = loader.getModContainer("betterend")
            .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
        String minecraft = loader.getModContainer("minecraft")
            .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
        enabled = end.equals("4.0.11") && minecraft.equals("1.20.1");
        LoggerFactory.getLogger("ProminenceBetterEndCompat").info(
            "Concurrent lake/mountain height caches: {} (betterend={}, minecraft={})", enabled, end, minecraft);
    }
    @Override public boolean shouldApplyMixin(String target, String name) { return enabled; }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> own, Set<String> other) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String name, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String name, IMixinInfo info) {}
}
