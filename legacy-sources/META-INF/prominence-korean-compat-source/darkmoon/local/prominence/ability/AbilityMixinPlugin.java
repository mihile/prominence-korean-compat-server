package local.prominence.ability;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class AbilityMixinPlugin implements IMixinConfigPlugin {
    private boolean mcdw;
    private boolean simplyBows;
    private boolean phantom;

    private boolean version(String id, String expected) {
        return FabricLoader.getInstance().getModContainer(id)
            .map(m -> m.getMetadata().getVersion().getFriendlyString().equals(expected)).orElse(false);
    }
    @Override public void onLoad(String pkg) {
        mcdw = version("mcdw", "9.0.4") && version("ranged_weapon_api", "1.1.4+1.20.1");
        simplyBows = version("simplybows", "0.1.4") && version("ranged_weapon_api", "1.1.4+1.20.1");
        phantom = version("soulsweapons", "1.3.1-1.20.1-fabric")
            && version("gun-scaling", "2.1.0") && version("ranged_weapon_api", "1.1.4+1.20.1")
            && version("archers", "1.3.0+1.20.1");
        LoggerFactory.getLogger("ProminenceAbilityCompat").info(
            "Ability growth adapters: MCDW extra arrows={}, Simply Bows companions={}, Phantom Trace={}",
            mcdw, simplyBows, phantom);
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.endsWith("SimplyBowsCompanionMixin")) return simplyBows;
        if (mixin.endsWith("GunPhantomMixin")) return phantom;
        if (mixin.endsWith("McdwProjectileFactoryMixin") || mixin.endsWith("AbilityWorldMixin")) return mcdw;
        return mcdw || simplyBows;
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> own, Set<String> other) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
