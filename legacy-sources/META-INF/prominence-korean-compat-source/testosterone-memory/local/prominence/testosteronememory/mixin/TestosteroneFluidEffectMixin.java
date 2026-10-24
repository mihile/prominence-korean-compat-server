package local.prominence.testosteronememory.mixin;

import local.prominence.testosteronememory.TestosteroneFluidChecks;
import net.minecraft.class_1309;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mifort.testosterone.events.fluidEffectHandler", remap = false)
public abstract class TestosteroneFluidEffectMixin {
    @Inject(method = "applyPotionEffect(Lnet/minecraft/class_1309;)V",
            at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void prominence$checkFluidWithoutLoading(class_1309 entity, CallbackInfo ci) {
        TestosteroneFluidChecks.apply(entity);
        ci.cancel();
    }
}
