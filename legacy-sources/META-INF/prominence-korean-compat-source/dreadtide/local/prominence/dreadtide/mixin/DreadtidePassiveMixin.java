package local.prominence.dreadtide.mixin;

import local.prominence.dreadtide.DreadtideRestore;
import net.minecraft.class_1309;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.sweenus.simplyswords.compat.eldritch_end.EldritchEndCompatMethods", remap = false)
public abstract class DreadtidePassiveMixin {
    @Inject(method = "generateVoidcloakStacks(Lnet/minecraft/class_1309;)V", at = @At("HEAD"), require = 1)
    private static void prominence$restorePassive(class_1309 entity, CallbackInfo ci) {
        DreadtideRestore.tick(entity);
    }
}
