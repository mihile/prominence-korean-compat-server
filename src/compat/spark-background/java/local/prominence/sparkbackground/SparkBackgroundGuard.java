package local.prominence.sparkbackground;

import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class SparkBackgroundGuard implements IMixinConfigPlugin {
    private boolean enabled;
    public static boolean matches(String os, String version, String backgroundHash, String accessHash, boolean optOut) {
        return !optOut && os.toLowerCase(Locale.ROOT).startsWith("windows") && version.equals("1.10.53")
                && backgroundHash.equals("88a8d60a1ff46901ed866997a8c933f44b8fa73bf0ed7d44b94b42e35f233fb2")
                && accessHash.equals("dc4550511b64147067e3ce26d6e1c70f1d60ed46720f7446e34fb4da310c41c1");
    }
    private String hash(String path) throws Exception {
        var mod = FabricLoader.getInstance().getModContainer("spark").orElseThrow();
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                Files.readAllBytes(mod.findPath(path).orElseThrow())));
    }
    public void onLoad(String pkg) {
        try {
            var loader = FabricLoader.getInstance();
            enabled = loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion()
                    .getFriendlyString().equals("1.20.1")
                    && matches(System.getProperty("os.name", ""), loader.getModContainer("spark").orElseThrow()
                            .getMetadata().getVersion().getFriendlyString(),
                            hash("me/lucko/spark/common/sampler/BackgroundSamplerManager.class"),
                            hash("me/lucko/spark/common/sampler/async/AsyncProfilerAccess.class"),
                            Boolean.getBoolean("prominence.spark.backgroundJava"));
        } catch (Exception e) { enabled = false; }
        org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info(
                "Windows spark continuous Java sampling suppression: {}", enabled ? "enabled; manual profiling available" : "disabled");
    }
    public boolean shouldApplyMixin(String target, String mixin) { return enabled; }
    public String getRefMapperConfig() { return null; }
    public List<String> getMixins() { return null; }
    public void acceptTargets(Set<String> ours, Set<String> others) {}
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
