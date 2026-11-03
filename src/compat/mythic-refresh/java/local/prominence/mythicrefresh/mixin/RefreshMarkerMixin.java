package local.prominence.mythicrefresh.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.minecraft.class_3218", priority = 500, remap = false)
public abstract class RefreshMarkerMixin {
    @Unique private static void prominence$mythicRefreshMarker() {}
}
