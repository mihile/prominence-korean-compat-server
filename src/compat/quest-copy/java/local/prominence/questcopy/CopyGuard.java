package local.prominence.questcopy;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class CopyGuard implements IMixinConfigPlugin {
    private byte[] reviewed;
    private static final String QUEST = "elocindev/prominent/api/questing/QuestProgress.class";
    private static final Map<String, String> PINS = Map.of(
        QUEST, "4e8986834837b4f4df1d6be942e85fa729c5155444cf44b47eff64fb0e832890",
        "elocindev/prominent/utils/PersistentPlayerData.class", "2fbaf86dad52164b5d910581075984091c3bf3a8161e6d958404a73830a42efb");
    private byte[] compatible() {
        if ("false".equalsIgnoreCase(System.getProperty("prominence.quest.snapshotCopy", "true"))) return null;
        try {
            var loader = FabricLoader.getInstance();
            if (!loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.20.1")) return null;
            var mod = loader.getModContainer("prominent").orElseThrow();
            if (!mod.getMetadata().getVersion().getFriendlyString().equals("4.1.1")) return null;
            byte[] result = null;
            for (var entry : PINS.entrySet()) {
                byte[] bytes = Files.readAllBytes(mod.findPath(entry.getKey()).orElseThrow());
                if (!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(entry.getValue())) return null;
                if (entry.getKey().equals(QUEST)) result = bytes;
            }
            return result;
        } catch (Exception | LinkageError unavailable) { return null; }
    }
    public void onLoad(String pkg) {
        reviewed = compatible();
        org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Prominent redundant quest snapshot copy: {}", reviewed != null ? "enabled" : "unchanged (version/class mismatch or opt-out)");
    }
    public boolean shouldApplyMixin(String target, String mixin) { return reviewed != null; }
    public String getRefMapperConfig() { return null; }
    public List<String> getMixins() { return null; }
    public void acceptTargets(Set<String> ours, Set<String> others) {}
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (reviewed == null) return;
        var original = new ClassNode();
        new ClassReader(reviewed).accept(original, 0);
        boolean changed = CopyRewriter.apply(node, original);
        org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Prominent quest snapshot copy removal: {}", changed ? "active" : "unchanged (unexpected method shape)");
    }
}
