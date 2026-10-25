/* SPDX-License-Identifier: GPL-3.0-only */
package local.prominence.hammercompat.mixin;
import local.prominence.hammercompat.HammerBreaks;
import net.minecraft.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=class_3225.class, remap=false)
public class HammerPlayerBreakMixin {
    @Shadow @Final protected class_3222 field_14008;
    @Inject(method="method_14266(Lnet/minecraft/class_2338;)Z", at=@At("HEAD"), remap=false)
    private void prominence$begin(class_2338 pos, CallbackInfoReturnable<Boolean> ci) {
        HammerBreaks.begin();
    }
    @Inject(method="method_14266(Lnet/minecraft/class_2338;)Z", at=@At("RETURN"), remap=false)
    private void prominence$finish(class_2338 pos, CallbackInfoReturnable<Boolean> ci) {
        HammerBreaks.finish(field_14008, pos, Boolean.TRUE.equals(ci.getReturnValue()));
    }
}
