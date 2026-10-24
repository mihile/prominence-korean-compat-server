package local.prominence.darkmoon;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class DarkmoonMixinPlugin implements IMixinConfigPlugin {
    private boolean enabled;
    private String version(String id) {
        return FabricLoader.getInstance().getModContainer(id)
            .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("");
    }
    private boolean absentOrVersion(String id, String expected) {
        return !FabricLoader.getInstance().isModLoaded(id) || version(id).equals(expected);
    }
    @Override public void onLoad(String pkg) {
        enabled = version("soulsweapons").equals("1.3.1-1.20.1-fabric")
            && version("ranged_weapon_api").equals("1.1.4+1.20.1")
            && absentOrVersion("zenith_attributes", "1.0.3")
            && absentOrVersion("projectile_damage", "3.2.3+1.20.1");
        LoggerFactory.getLogger("ProminenceDarkmoonCompat").info(
            "Arrow Storm and Galeforce growth adapters enabled={}; soulsweapons={}, ranged_weapon_api={}",
            enabled, version("soulsweapons"), version("ranged_weapon_api"));
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) { return enabled; }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> own, Set<String> other) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
