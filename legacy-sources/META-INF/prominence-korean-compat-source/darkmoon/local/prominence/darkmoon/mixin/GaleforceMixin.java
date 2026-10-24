package local.prominence.darkmoon.mixin;

import local.prominence.darkmoon.StormScaling;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_243;
import net.minecraft.class_3218;
import net.soulsweaponry.entity.projectile.arrow.ChargedArrow;
import net.soulsweaponry.items.bow.Galeforce;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Galeforce.class, remap = false)
public abstract class GaleforceMixin {
    @Redirect(method = "shootArrow", at = @At(value = "INVOKE",
        target = "Lnet/soulsweaponry/entity/projectile/arrow/ChargedArrow;method_7438(D)V"), require = 1)
    private void prominence$scaleAbility(ChargedArrow arrow, double damage, class_3218 world,
            class_1799 stack, class_1799 ammo, class_1657 player, class_243 targetPos) {
        // Keep native speed, critical hit and missing-health execution bonus.
        arrow.method_7438(damage * StormScaling.growth(player, stack));
    }
}
