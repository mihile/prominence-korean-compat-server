/* SPDX-License-Identifier: GPL-3.0-only */
package local.prominence.hammercompat.mixin;
import local.prominence.hammercompat.HammerBreaks;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="pro.mikey.justhammers.HammerItem", remap=false)
public class HammerAreaMixin {
    @Inject(method="causeAoe", at=@At("HEAD"), cancellable=true, remap=false)
    private void prominence$deferArea(class_1937 world, class_2338 pos, class_2680 state,
                                     class_1799 stack, class_1309 entity, CallbackInfo ci) {
        HammerBreaks.capture(world, pos, state, stack, entity);
        ci.cancel();
    }
}
