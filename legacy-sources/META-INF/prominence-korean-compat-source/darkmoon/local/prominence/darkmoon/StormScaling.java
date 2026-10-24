package local.prominence.darkmoon;

import net.fabric_extras.ranged_weapon.api.CustomRangedWeapon;
import net.fabric_extras.ranged_weapon.api.EntityAttributes_RangedWeapon;
import net.minecraft.class_1657;
import net.minecraft.class_1799;

public final class StormScaling {
    private StormScaling() {}

    /** Preserve the native ability at base gear; apply only the player's growth. */
    public static double growth(class_1657 player, class_1799 stack) {
        if (!(stack.method_7909() instanceof CustomRangedWeapon weapon)) return 1.0D;
        double baseline = weapon.getRangedWeaponConfig().damage();
        double current = player.method_26825(EntityAttributes_RangedWeapon.DAMAGE.attribute);
        if (!Double.isFinite(baseline) || baseline <= 0.0D
                || !Double.isFinite(current) || current <= 0.0D) return 1.0D;
        double ratio = current / baseline;
        return Double.isFinite(ratio) ? ratio : 1.0D;
    }
}
