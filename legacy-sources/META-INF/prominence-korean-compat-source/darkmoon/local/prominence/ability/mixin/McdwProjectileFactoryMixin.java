package local.prominence.ability.mixin;

import chronosacaria.mcdw.api.util.ProjectileEffectHelper;
import local.prominence.ability.AbilityProjectile;
import net.minecraft.class_1309;
import net.minecraft.class_1665;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ProjectileEffectHelper.class, remap = false)
public abstract class McdwProjectileFactoryMixin {
    @Inject(method = "mcdw$createAbstractArrow", at = @At("RETURN"), require = 1)
    private static void prominence$markExtraArrow(class_1309 attacker, CallbackInfoReturnable<class_1665> cir) {
        if (cir.getReturnValue() instanceof AbilityProjectile projectile) projectile.prominence$markMcdwArrow();
    }
}
