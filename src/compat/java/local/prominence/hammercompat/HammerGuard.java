/* SPDX-License-Identifier: GPL-3.0-only */
package local.prominence.hammercompat;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.*;
public final class HammerGuard implements IMixinConfigPlugin {
    private static final Map<String,String> TARGETS = Map.of(
        "pro/mikey/justhammers/HammerItem.class", "11b594605bcee91808f380378f1d433d5fb79d0dc4818d91f4675ab2359e4444",
        "pro/mikey/justhammers/fabric/mixin/ServerPlayerGameModeMixinFabric.class", "639871b2e7e5edeb362c15d4d413f8679a1ec346220a34acd4f6ac9c6ec11f29");
    private boolean enabled;
    public static boolean supports(String minecraft, String version, Map<String,byte[]> originals) {
        if (!"1.20.1".equals(minecraft) || !"20.1.5+mc1.20.1".equals(version) || originals == null) return false;
        try {
            for (var entry : TARGETS.entrySet()) {
                byte[] raw = originals.get(entry.getKey());
                if (raw == null || !entry.getValue().equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw)))) return false;
            }
            return true;
        } catch (Exception e) { return false; }
    }
    public void onLoad(String pkg) {
        var loader = FabricLoader.getInstance();
        var mod = loader.getModContainer("justhammers");
        if (mod.isPresent()) {
            try {
                Map<String,byte[]> originals = new HashMap<>();
                for (String name : TARGETS.keySet()) {
                    var path = mod.get().findPath(name);
                    if (path.isPresent()) originals.put(name, Files.readAllBytes(path.get()));
                }
                enabled = supports(loader.getModContainer("minecraft").map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse(""),
                        mod.get().getMetadata().getVersion().getFriendlyString(), originals);
            } catch (Exception ignored) { enabled = false; }
            LoggerFactory.getLogger("Prominence compatibility").info("Just Hammers enchantment compatibility: {}", enabled ? "enabled" : "skipped (unrecognized version/code)");
        }
    }
    public boolean shouldApplyMixin(String target, String mixin) { return enabled; }
    public String getRefMapperConfig() { return null; }
    public List<String> getMixins() { return null; }
    public void acceptTargets(Set<String> a, Set<String> b) {}
    public void preApply(String a, ClassNode b, String c, IMixinInfo d) {}
    public void postApply(String a, ClassNode b, String c, IMixinInfo d) {}
}
