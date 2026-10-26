/* SPDX-License-Identifier: GPL-3.0-only */
package local.prominence.arrowtick.mixin;
import local.prominence.arrowtick.ArrowImpactPower;
import net.minecraft.*;
import net.spell_engine.api.spell.SpellInfo;
import net.spell_engine.internals.SpellHelper.ImpactContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="net.spell_engine.internals.SpellHelper",remap=false)
public class ArrowImpactMixin {
    @ModifyVariable(method="projectileImpact",at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false)
    private static ImpactContext prominence$arrowPower(ImpactContext context, class_1309 caster, class_1297 projectile,
            class_1297 target, SpellInfo info, ImpactContext original) {
        return ArrowImpactPower.complete(context,caster,projectile,info);
    }
}
