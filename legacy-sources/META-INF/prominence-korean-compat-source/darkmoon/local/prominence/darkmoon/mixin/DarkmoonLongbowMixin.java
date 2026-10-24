package local.prominence.darkmoon.mixin;

import local.prominence.darkmoon.StormDamage;
import local.prominence.darkmoon.StormScaling;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_3218;
import net.soulsweaponry.config.ConfigConstructor;
import net.soulsweaponry.entity.projectile.noclip.ArrowStormEntity;
import net.soulsweaponry.items.bow.DarkmoonLongbow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DarkmoonLongbow.class, remap = false)
public abstract class DarkmoonLongbowMixin {
    @Redirect(method = "useKeybindAbilityServer", at = @At(value = "INVOKE",
        target = "Lnet/soulsweaponry/entity/projectile/noclip/ArrowStormEntity;method_7438(D)V"), require = 1)
    private void prominence$useRangedGrowth(ArrowStormEntity cloud, double oldAmount,
            class_3218 world, class_1799 stack, class_1657 player) {
        // Restore just the base division; retain upstream's exact Power bonus.
        double configured = ConfigConstructor.darkmoon_longbow_ability_damage;
        double nominal = oldAmount - configured / (double) 2.6F + configured;
        ((StormDamage) cloud).prominence$setCloudDamage(nominal * StormScaling.growth(player, stack));
    }
}
