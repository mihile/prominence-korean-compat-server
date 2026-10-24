package local.prominence.ability.mixin;

import net.archers.ArchersMod;
import net.fabric_extras.ranged_weapon.api.EntityAttributes_RangedWeapon;
import net.mervyn.gunscaling.GunScalingMod;
import net.minecraft.class_1309;
import net.minecraft.class_1324;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1890;
import net.minecraft.class_1893;
import net.minecraft.class_1937;
import net.soulsweaponry.config.ConfigConstructor;
import net.soulsweaponry.entity.projectile.SilverBulletEntity;
import net.soulsweaponry.items.gun.GunItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GunItem.class, remap = false)
public abstract class GunPhantomMixin {
    @Redirect(method = "createSilverBulletEntity", at = @At(value = "INVOKE",
        target = "Lnet/soulsweaponry/entity/projectile/SilverBulletEntity;method_7438(D)V", ordinal = 1), require = 1)
    private void prominence$scalePhantomOnly(SilverBulletEntity copy, double nativeDamage,
            class_1937 world, class_1309 shooter, class_1799 stack) {
        double damage = nativeDamage;
        class_1324 ranged = shooter.method_5996(EntityAttributes_RangedWeapon.DAMAGE.attribute);
        if (shooter instanceof class_1657 && copy.isEchoCopy() && ranged != null) {
            GunItem gun = (GunItem) stack.method_7909();
            double configuredSpeed = gun.getBulletVelocity(stack);
            double actualSpeed = copy.method_18798().method_1033();
            double factor = GunScalingMod.configManager.value.gun.damageScalingFactor;
            if (factor == 0) factor = 1;
            int power = class_1890.method_8225(class_1893.field_9103, stack);
            double powerScale = ArchersMod.tweaksConfig.value.power_enchantment_multiplier_per_level;
            // Gun Scaling 2.1.0's formula, limited to the copies omitted by its RETURN hook.
            // Read both mods' live configuration. Never multiply an already scaled main bullet.
            if (configuredSpeed > 0 && actualSpeed > 0) {
                double base = gun.getBulletDamage(stack)
                    * ConfigConstructor.phantom_trace_enchant_phantom_projectile_damage_mod;
                double scaled = base * (1 + power * powerScale) * (1 + ranged.method_6194() / factor)
                    * (1 + (actualSpeed - configuredSpeed) / configuredSpeed) / actualSpeed;
                scaled = (double) (float) scaled; // Match Gun Scaling's final float storage.
                if (Double.isFinite(scaled) && scaled >= 0) damage = scaled;
            }
        }
        copy.method_7438(damage);
    }
}
