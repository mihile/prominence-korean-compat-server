package local.prominence.structurememory.mixin;

import local.prominence.structurememory.SearchBudget;
import com.chaosthedude.explorerscompass.workers.WorldWorkerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
    "com.chaosthedude.explorerscompass.workers.RandomSpreadSearchWorker",
    "com.chaosthedude.explorerscompass.workers.ConcentricRingsSearchWorker",
    "com.chaosthedude.explorerscompass.workers.GenericSearchWorker"
}, remap = false)
public abstract class CompassSampleBudgetMixin {
    @Inject(method = "doWork()Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void prominence$sampleWithinBudget(CallbackInfoReturnable<Boolean> cir) {
        // Let exhausted/stopped workers run their original failure/next-worker cleanup.
        if (((WorldWorkerManager.IWorker)(Object)this).hasWork() && !SearchBudget.trySample()) cir.setReturnValue(false);
    }
}
