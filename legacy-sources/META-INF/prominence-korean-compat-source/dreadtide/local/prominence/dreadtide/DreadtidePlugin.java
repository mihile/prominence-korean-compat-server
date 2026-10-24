package local.prominence.dreadtide;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class DreadtidePlugin implements IMixinConfigPlugin {
    private boolean version(String id, String expected) {
        return FabricLoader.getInstance().getModContainer(id)
                .map(m -> expected.equals(m.getMetadata().getVersion().getFriendlyString())).orElse(false);
    }
    public boolean shouldApplyMixin(String target, String mixin) {
        return version("minecraft", "1.20.1") && version("simplyswords", "1.70.2-1.20.1")
                && version("eldritch_end", "0.3.4");
    }
    public void onLoad(String p) {}
    public String getRefMapperConfig() { return null; }
    public void acceptTargets(Set<String> a, Set<String> b) {}
    public List<String> getMixins() { return null; }
    public void preApply(String t, ClassNode c, String m, IMixinInfo i) {}
    public void postApply(String t, ClassNode c, String m, IMixinInfo i) {}
}
