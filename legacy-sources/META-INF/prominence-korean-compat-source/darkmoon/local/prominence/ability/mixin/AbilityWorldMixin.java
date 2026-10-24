package local.prominence.ability.mixin;

import local.prominence.ability.AbilityProjectile;
import net.minecraft.class_1297;
import net.minecraft.class_3218;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = class_3218.class, priority = 1100, remap = false)
public abstract class AbilityWorldMixin {
    @Inject(method = "method_8649(Lnet/minecraft/class_1297;)Z", at = @At("HEAD"), require = 1)
    private void prominence$scaleMarkedExtraArrow(class_1297 entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof AbilityProjectile projectile) projectile.prominence$applyMcdwGrowth();
    }
}
