package local.prominence.questcopy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "elocindev.prominent.api.questing.QuestProgress", priority = 500, remap = false)
public abstract class CopyMarkerMixin {
    @Unique private static boolean prominence$questSnapshotMarker;
}
