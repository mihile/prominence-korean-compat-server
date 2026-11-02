package local.prominence.voxyservercalm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class VoxyServerCalmGuard implements IMixinConfigPlugin {
    private boolean active;

    public static boolean matches(JsonObject pins,
                                  Function<String, String> versions,
                                  BiFunction<String, String, byte[]> bytes) {
        try {
            for (var entry : pins.entrySet()) {
                String modId = entry.getKey();
                JsonObject expected = entry.getValue().getAsJsonObject();
                String actualVer = versions.apply(modId);
                if (actualVer == null) return false;
                if (expected.has("version") && !actualVer.equals(expected.get("version").getAsString())) {
                    return false;
                }
                if (expected.has("versionPrefix") && !actualVer.startsWith(expected.get("versionPrefix").getAsString())) {
                    return false;
                }
                for (var cls : expected.getAsJsonObject("classes").entrySet()) {
                    byte[] raw = bytes.apply(modId, cls.getKey());
                    if (raw == null) return false;
                    String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
                    if (!digest.equals(cls.getValue().getAsString())) return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
        try (var in = VoxyServerCalmGuard.class.getResourceAsStream("/prominence-voxy-server-calm-pins.json")) {
            if (in == null) {
                active = false;
                return;
            }
            var pins = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            active = !"false".equalsIgnoreCase(System.getProperty("prominence.voxyServerCalm", "true"))
                && matches(
                    pins,
                    id -> FabricLoader.getInstance().getModContainer(id)
                        .map(m -> m.getMetadata().getVersion().getFriendlyString())
                        .orElse(null),
                    (id, path) -> {
                        try {
                            var file = FabricLoader.getInstance().getModContainer(id).orElseThrow().findPath(path);
                            return file.isPresent() ? Files.readAllBytes(file.get()) : null;
                        } catch (Exception | LinkageError e) {
                            return null;
                        }
                    });
        } catch (Exception | LinkageError e) {
            active = false;
        }
        LoggerFactory.getLogger("Prominence compatibility").info(
            "VoxyServer world-engine retention & join chunk-load pacing: {}",
            active ? "enabled" : "disabled");
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return active;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
