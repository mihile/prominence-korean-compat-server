package local.prominence.ability.mixin;

import local.prominence.ability.AbilityProjectile;
import net.fabric_extras.ranged_weapon.api.CustomRangedWeapon;
import net.fabric_extras.ranged_weapon.api.EntityAttributes_RangedWeapon;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1665;
import net.minecraft.class_1799;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = class_1665.class, remap = false)
public abstract class AbilityProjectileMixin implements AbilityProjectile {
    @Unique private boolean prominence$mcdwArrow;
    @Unique private boolean prominence$mcdwScaled;
    @Unique private boolean prominence$companionScaled;

    @Shadow public abstract double method_7448();
    @Shadow public abstract void method_7438(double damage);

    @Override public void prominence$markMcdwArrow() { prominence$mcdwArrow = true; }

    @Override public void prominence$applyMcdwGrowth() {
        if (!prominence$mcdwArrow || prominence$mcdwScaled) return;
        prominence$mcdwScaled = true;
        // Secondary arrows can be constructed at the victim, then reassigned to the shooter.
        // Read their final owner at spawn, never their temporary constructor owner.
        class_1297 owner = ((class_1665) (Object) this).method_24921();
        if (!(owner instanceof class_1657 player)) return;
        class_1799 stack = player.method_6030();
        if (!(stack.method_7909() instanceof CustomRangedWeapon weaponInUse)
                || weaponInUse.getRangedWeaponConfig().damage() <= 0) stack = player.method_6047();
        if (!(stack.method_7909() instanceof CustomRangedWeapon mainWeapon)
                || mainWeapon.getRangedWeaponConfig().damage() <= 0) stack = player.method_6079();
        if (!(stack.method_7909() instanceof CustomRangedWeapon weapon)) return;
        double baseline = weapon.getRangedWeaponConfig().damage();
        double current = player.method_26825(EntityAttributes_RangedWeapon.DAMAGE.attribute);
        if (!Double.isFinite(baseline) || baseline <= 0 || !Double.isFinite(current) || current <= 0) return;
        double ratio = current / baseline;
        double damage = method_7448() * ratio;
        if (Double.isFinite(ratio) && Double.isFinite(damage) && damage >= 0) method_7438(damage);
    }

    @Override public void prominence$addCompanionBonus(double bonus) {
        if (prominence$companionScaled) return;
        prominence$companionScaled = true;
        double damage = method_7448() + bonus;
        if (Double.isFinite(bonus) && bonus > 0 && Double.isFinite(damage) && damage >= 0) method_7438(damage);
    }
}
