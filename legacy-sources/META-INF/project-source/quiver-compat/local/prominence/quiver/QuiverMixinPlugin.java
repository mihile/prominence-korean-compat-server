package local.prominence.quiver;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Never inject the legacy method contract into an unreviewed engine update. */
public final class QuiverMixinPlugin implements IMixinConfigPlugin {
    private boolean enabled;
    @Override public void onLoad(String pkg) {
        FabricLoader loader = FabricLoader.getInstance();
        String engine = loader.getModContainer("spell_engine")
            .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
        String quiver = loader.getModContainer("supplementaries")
            .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
        enabled = engine.equals("0.15.12+1.20.1") && quiver.equals("1.20-3.1.43");
        var log = LoggerFactory.getLogger("ProminenceQuiverCompat");
        if (enabled) log.info("Enabling Supplementaries quiver support for Spell Engine 0.15.12");
        else log.warn("Quiver adapter disabled for unreviewed/absent versions: spell_engine={}, supplementaries={}", engine, quiver);
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) { return enabled; }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> own, Set<String> other) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
