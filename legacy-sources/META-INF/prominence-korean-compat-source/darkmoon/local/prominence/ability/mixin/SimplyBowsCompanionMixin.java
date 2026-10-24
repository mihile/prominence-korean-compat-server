package local.prominence.ability.mixin;

import local.prominence.ability.AbilityProjectile;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1676;
import net.minecraft.class_1799;
import net.minecraft.class_243;
import net.minecraft.class_3218;
import net.sweenus.simplybows.entity.ShoulderBowEntity;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShoulderBowEntity.class, remap = false)
public abstract class SimplyBowsCompanionMixin {
    @Unique private static void prominence$addNativeBonus(class_1309 owner, class_1676 arrow) {
        if (owner instanceof class_1657 && arrow instanceof AbilityProjectile projectile)
            projectile.prominence$addCompanionBonus(CombatTargeting.getRangedWeaponDamageBonus(owner));
    }

    @Inject(method = "createCompanionArrow", at = @At("RETURN"), require = 1)
    private void prominence$scaleCompanion(class_3218 world, class_1309 owner, class_243 direction,
            CallbackInfoReturnable<class_1676> cir) {
        prominence$addNativeBonus(owner, cir.getReturnValue());
    }

    @Inject(method = "createOffhandIceProjectile", at = @At("RETURN"), require = 1)
    private void prominence$scaleEveryIceArrow(class_3218 world, class_1309 owner, class_1799 bow,
            class_1799 ammo, double multiplier, RuneEtching rune, CallbackInfoReturnable<class_1676> cir) {
        // Includes arrows spawned inside the volley. The returned first arrow reaches both hooks.
        prominence$addNativeBonus(owner, cir.getReturnValue());
    }
}
