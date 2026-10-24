package local.prominence.structurememory.mixin;

import com.chaosthedude.explorerscompass.util.StructureUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_2960;
import net.minecraft.class_3218;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.chaosthedude.explorerscompass.util.StructureUtils", remap = false)
public abstract class CompassAllowedStructuresMixin {
    @Inject(method = "getAllowedStructureIDs(Lnet/minecraft/class_3218;)Ljava/util/List;", at = @At("RETURN"), cancellable = true, require = 1)
    private static void prominence$onlyPlacedStructures(class_3218 world, CallbackInfoReturnable<List<class_2960>> cir) {
        List<class_2960> allowed = new ArrayList<>();
        for (var id : cir.getReturnValue()) {
            boolean placed = false;
            for (var dimension : world.method_8503().method_3738()) {
                var structure = StructureUtils.getStructureForID(dimension, id);
                if (structure == null) continue;
                var entry = StructureUtils.getEntryForStructure(dimension, structure);
                if (entry != null && !dimension.method_14178().method_46642().method_46708(entry).isEmpty()) {
                    placed = true;
                    break;
                }
            }
            if (placed) allowed.add(id);
        }
        cir.setReturnValue(allowed);
    }
}
