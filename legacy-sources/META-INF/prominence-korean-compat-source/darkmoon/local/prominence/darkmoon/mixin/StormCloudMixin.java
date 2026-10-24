package local.prominence.darkmoon.mixin;

import local.prominence.darkmoon.StormDamage;
import net.soulsweaponry.entity.projectile.arrow.MoonlightArrow;
import net.soulsweaponry.entity.projectile.noclip.ArrowStormEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ArrowStormEntity.class, remap = false)
public abstract class StormCloudMixin {
    @Redirect(method = "method_5773", at = @At(value = "INVOKE",
        target = "Lnet/soulsweaponry/entity/projectile/arrow/MoonlightArrow;method_7438(D)V"), require = 1)
    private void prominence$markStormArrow(MoonlightArrow arrow, double damage) {
        arrow.method_7438(damage);
        // The native setter applies Zenith's once-per-arrow modifier here.
        ((StormDamage) arrow).prominence$setStormDamage(arrow.method_7448());
    }
}
