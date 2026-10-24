package local.prominence.smithing;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Keep this adapter limited to the inspected helper contract. */
public final class SortilegeCachePlugin implements IMixinConfigPlugin {
    private boolean enabled;
    @Override public void onLoad(String pkg) {
        FabricLoader loader = FabricLoader.getInstance();
        String version = loader.getModContainer("sortilege")
            .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
        String minecraft = loader.getModContainer("minecraft")
            .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
        enabled = version.equals("9.0") && minecraft.equals("1.20.1");
        LoggerFactory.getLogger("ProminenceSmithingCompat").info(
            "Sortilege live enchantment slot reads: {} (sortilege={}, minecraft={})", enabled, version, minecraft);
    }
    @Override public boolean shouldApplyMixin(String target, String name) { return enabled; }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> own, Set<String> other) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String name, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String name, IMixinInfo info) {}
}
