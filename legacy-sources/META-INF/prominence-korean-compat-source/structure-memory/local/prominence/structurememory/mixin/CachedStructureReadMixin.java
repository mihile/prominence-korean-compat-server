package local.prominence.structurememory.mixin;

import com.mojang.datafixers.DataFixer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.Semaphore;
import net.minecraft.class_155;
import net.minecraft.class_2487;
import net.minecraft.class_2507;
import net.minecraft.class_2960;
import net.minecraft.class_4284;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "org.embeddedt.modernfix.structure.CachingStructureManager", remap = false)
public abstract class CachedStructureReadMixin {
    @Unique private static final Semaphore prominence$nbtReaders = new Semaphore(2, true);
    @Shadow private static byte[] toBytes(InputStream input) throws IOException { throw new AssertionError(); }
    @Shadow private static class_2487 getCachedUpgraded(class_2960 id, String hash) { throw new AssertionError(); }
    @Shadow private static void saveCachedUpgraded(class_2960 id, String hash, class_2487 tag) { throw new AssertionError(); }

    /**
     * @author Prominence compatibility maintenance
     * @reason Avoid retaining the original and upgraded giant structure NBT at once;
     *         bound concurrent NBT expansion while preserving ModernFix's DFU and disk cache.
     */
    @Overwrite
    public static class_2487 readStructureTag(class_2960 id, DataFixer fixer, InputStream input) throws IOException {
        prominence$nbtReaders.acquireUninterruptibly();
        try {
            byte[] bytes = toBytes(input);
            String hash;
            try { hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
            catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
            int expected = class_155.method_16673().method_37912().method_38494();
            class_2487 cached = getCachedUpgraded(id, hash.substring(0, 10));
            if (cached != null && cached.method_10550("DataVersion") == expected) return cached;
            // Do not retain an obsolete cached tree while reading and upgrading the source.
            cached = null;
            class_2487 tag = class_2507.method_10629(new ByteArrayInputStream(bytes));
            if (!tag.method_10573("DataVersion", 99)) tag.method_10569("DataVersion", 500);
            int version = tag.method_10550("DataVersion");
            if (version < expected) {
                tag = class_4284.field_19217.method_48131(fixer, tag, version, expected);
                tag.method_10569("DataVersion", expected);
                saveCachedUpgraded(id, hash, tag);
            }
            return tag;
        } finally { prominence$nbtReaders.release(); }
    }
}
