package local.prominence.sparkbackground.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Applies only through the exact-version Windows guard. Manual samplers are untouched. */
@Mixin(targets = "me.lucko.spark.common.sampler.BackgroundSamplerManager", remap = false)
public abstract class WindowsBackgroundSamplerMixin {
    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE",
            target = "Lme/lucko/spark/common/util/Configuration;getBoolean(Ljava/lang/String;Z)Z"), require = 1)
    private boolean prominence$disableContinuousJavaSampling(boolean configured) {
        return false;
    }
}
