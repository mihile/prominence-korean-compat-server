package local.prominence.structurememory;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class StructureMemoryPlugin implements IMixinConfigPlugin {
    private String version(String id) {
        return FabricLoader.getInstance().getModContainer(id).map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (!version("minecraft").equals("1.20.1")) return false;
        if (mixin.endsWith("CachedStructureReadMixin")) return version("modernfix").equals("5.24.3+mc1.20.1");
        return version("explorerscompass").equals("1.20.1-2.6.0-fabric");
    }
    @Override public void onLoad(String pkg) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> other) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
