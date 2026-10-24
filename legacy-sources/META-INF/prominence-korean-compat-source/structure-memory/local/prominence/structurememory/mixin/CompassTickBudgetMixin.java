package local.prominence.structurememory.mixin;

import local.prominence.structurememory.SearchBudget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.chaosthedude.explorerscompass.workers.WorldWorkerManager", remap = false)
public abstract class CompassTickBudgetMixin {
    @Inject(method = "tick(Z)V", at = @At("HEAD"), require = 1)
    private static void prominence$startBudget(boolean start, CallbackInfo ci) {
        // Start this budget when search work actually begins, after the normal server tick.
        if (!start) SearchBudget.beginTick();
    }
}
