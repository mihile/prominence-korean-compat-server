package local.prominence.runtime;

import java.io.InputStream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_2960;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.common.book.BookRegistry;
import vazkii.patchouli.xplat.XplatModContainer;

/** BetterNether 9.0.10 contains an empty pre-1.20 scaffold, not a guide. */
@Mixin(value = BookRegistry.class, remap = false)
public abstract class LegacyNetherBookMixin {
    @Inject(method = "loadBook", at = @At("HEAD"), cancellable = true)
    private void prominence$skipEmptyLegacyScaffold(XplatModContainer owner,
            class_2960 id, InputStream stream, boolean external, CallbackInfo ci) {
        if (!external && "betternether:betternether_book".equals(id.toString())
                && "betternether".equals(owner.getId())
                && FabricLoader.getInstance().getModContainer("betternether")
                    .map(mod -> "9.0.10".equals(mod.getMetadata().getVersion().getFriendlyString()))
                    .orElse(false)) {
            System.out.println("[Prominence Runtime Fixes] Skipped BetterNether 9.0.10 empty legacy book scaffold");
            ci.cancel();
        }
    }
}
