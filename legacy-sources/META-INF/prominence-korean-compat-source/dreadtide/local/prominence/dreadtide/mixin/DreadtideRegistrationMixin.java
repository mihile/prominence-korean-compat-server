package local.prominence.dreadtide.mixin;

import local.prominence.dreadtide.DreadtideRestore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.sweenus.simplyswords.SimplySwords", remap = false)
public abstract class DreadtideRegistrationMixin {
    @Inject(method = "init()V", at = @At(value = "INVOKE",
            target = "Lnet/sweenus/simplyswords/config/Config;init()V", shift = At.Shift.AFTER), require = 1)
    private static void prominence$restoreDreadtide(CallbackInfo ci) {
        DreadtideRestore.register();
    }
}
