/* SPDX-License-Identifier: GPL-3.0-only */
package local.prominence.arrowtick;
import net.minecraft.*;
import net.spell_engine.api.spell.SpellInfo;
import net.spell_engine.internals.SpellHelper.ImpactContext;
import net.spell_power.api.SpellPower;
public final class ArrowImpactPower {
    private ArrowImpactPower() {}
    public static ImpactContext complete(ImpactContext context, class_1309 caster, class_1297 projectile, SpellInfo info) {
        if (context == null || context.power() != null || caster == null || !(projectile instanceof class_1665)
                || projectile.method_37908().field_9236 || info == null || info.spell() == null
                || info.spell().area_impact == null || info.spell().school == null) return context;
        // Same native resolver used by SpellHelper.performImpact; preserve every other context component.
        return context.power(SpellPower.getSpellPower(info.spell().school, caster));
    }
}
