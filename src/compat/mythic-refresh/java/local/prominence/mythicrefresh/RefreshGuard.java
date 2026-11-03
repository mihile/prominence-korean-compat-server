package local.prominence.mythicrefresh;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class RefreshGuard implements IMixinConfigPlugin {
    private boolean enabled;
    private static boolean compatible() {
        if ("false".equalsIgnoreCase(System.getProperty("prominence.mythic.configRefresh", "true"))) return false;
        try (var in = RefreshGuard.class.getResourceAsStream("/prominence-mythic-refresh-pins.json")) {
            var loader = FabricLoader.getInstance();
            if (!loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.20.1")) return false;
            var mod = loader.getModContainer("mythicupgrades").orElseThrow();
            if (!mod.getMetadata().getVersion().getFriendlyString().equals("4.2.0+mc1.20.1")) return false;
            var pins = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : pins.entrySet()) {
                byte[] bytes = Files.readAllBytes(mod.findPath(entry.getKey()).orElseThrow());
                if (!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(entry.getValue().getAsString())) return false;
            }
            return true;
        } catch (Exception | LinkageError unavailable) { return false; }
    }
    public void onLoad(String pkg) {
        enabled = compatible();
        org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Mythic Upgrades global config refresh coalescing: {}", enabled ? "enabled" : "disabled (version/class mismatch or opt-out)");
    }
    public boolean shouldApplyMixin(String target, String mixin) { return enabled; }
    public String getRefMapperConfig() { return null; }
    public List<String> getMixins() { return null; }
    public void acceptTargets(Set<String> ours, Set<String> others) {}
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (enabled) org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info(
                "Mythic Upgrades native config callback wrapping: {}", RefreshRewriter.apply(node) ? "active" : "unchanged (unexpected merged callback shape)");
    }
}
