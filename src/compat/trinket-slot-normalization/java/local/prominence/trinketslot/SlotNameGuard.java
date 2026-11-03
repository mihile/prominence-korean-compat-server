package local.prominence.trinketslot;

import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class SlotNameGuard implements IMixinConfigPlugin {
    private boolean enabled;

    public void onLoad(String pkg) {
        try {
            var loader = FabricLoader.getInstance();
            var mod = loader.getModContainer("tclayer").orElseThrow();
            enabled = loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion()
                    .getFriendlyString().equals("1.20.1")
                    && mod.getMetadata().getVersion().getFriendlyString().equals("3.8.1+beta.14")
                    && HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(
                            mod.findPath("dev/emi/trinkets/compat/WrappingTrinketsUtils.class").orElseThrow())))
                    .equals("c7a652ff05bbe155e3ff0be5df718261e60d17382f0fddcc9dc8cdea50c3625e");
        } catch (Exception e) {
            enabled = false;
        }
        org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info(
                "Trinket slot-name pattern reuse compatibility: {}", enabled ? "enabled" : "disabled (class mismatch)");
    }

    public boolean shouldApplyMixin(String target, String mixin) { return enabled; }
    public String getRefMapperConfig() { return null; }
    public List<String> getMixins() { return null; }
    public void acceptTargets(Set<String> ours, Set<String> others) {}
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
