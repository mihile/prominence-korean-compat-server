package local.prominence.quiver.mixin;

import local.prominence.quiver.QuiverAmmo;
import net.minecraft.*;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpellHelper.class, remap = false)
public abstract class SpellHelperMixin {
    @Inject(method = "ammoForSpell", at = @At("RETURN"), cancellable = true, require = 1)
    private static void prominence$quiverPrecheck(class_1657 player, Spell spell, class_1799 weapon,
            CallbackInfoReturnable<SpellHelper.AmmoResult> cir) {
        var result = cir.getReturnValue();
        // Only the generic arrow prerequisite for arrow skills. Never waive other spell materials.
        if (!result.satisfied() && result.ammo() != null
                && result.ammo().method_31574(class_1802.field_8107)
                && spell.cost != null && !spell.cost.consume_item
                && spell.release != null && spell.release.target != null
                && spell.release.target.type == Spell.Release.Target.Type.SHOOT_ARROW
                && QuiverAmmo.selected(player) != null) {
            cir.setReturnValue(new SpellHelper.AmmoResult(true, result.ammo()));
        }
    }
}
