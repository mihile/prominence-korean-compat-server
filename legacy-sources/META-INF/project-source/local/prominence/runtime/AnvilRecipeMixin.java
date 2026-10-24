package local.prominence.runtime;

import net.minecraft.class_1792;
import net.minecraft.class_6880;
import net.minecraft.class_7923;
import org.betterx.bclib.recipes.AnvilRecipe;
import org.betterx.worlds.together.tag.v3.CommonItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Item tags are synchronized on the static item registry in multiplayer. */
@Mixin(value = AnvilRecipe.class, remap = false)
public abstract class AnvilRecipeMixin {
    @Inject(method = "getAllHammers", at = @At("HEAD"), cancellable = true)
    private static void prominence$useSyncedItemRegistry(
            CallbackInfoReturnable<Iterable<class_6880<class_1792>>> cir) {
        cir.setReturnValue(class_7923.field_41178.method_40286(CommonItemTags.HAMMERS));
    }
}
