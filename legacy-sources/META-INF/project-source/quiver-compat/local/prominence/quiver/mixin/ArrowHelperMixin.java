package local.prominence.quiver.mixin;

import local.prominence.quiver.QuiverAmmo;
import net.minecraft.*;
import net.spell_engine.api.spell.SpellInfo;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.arrow.ArrowHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ArrowHelper.class, remap = false)
public abstract class ArrowHelperMixin {
    private static final String SHOOT = "shootArrow(Lnet/minecraft/class_1937;Lnet/minecraft/class_1309;Lnet/spell_engine/api/spell/SpellInfo;Lnet/spell_engine/internals/SpellHelper$ImpactContext;I)V";

    @ModifyVariable(method = SHOOT, at = @At("STORE"), ordinal = 0, require = 1, allow = 1)
    private static class_1799 prominence$selectedArrow(class_1799 fallback,
            class_1937 world, class_1309 shooter, SpellInfo spell,
            SpellHelper.ImpactContext context, int sequence) {
        if (shooter instanceof class_1657 player) {
            var selected = QuiverAmmo.selected(player);
            if (selected != null) return selected.arrow();
        }
        return fallback;
    }

    @Redirect(method = SHOOT, at = @At(value = "INVOKE", target = "Lnet/spell_engine/internals/arrow/ArrowHelper;tryConsumeItem(Lnet/minecraft/class_1657;Lnet/minecraft/class_1792;)Z"), require = 1, allow = 1)
    private static boolean prominence$consumeQuiver(class_1657 player, class_1792 item) {
        if (item == class_1802.field_8107) {
            var selected = QuiverAmmo.selected(player);
            if (selected != null) return QuiverAmmo.consume(player, selected);
        }
        return ArrowHelper.tryConsumeItem(player, item);
    }

    @Redirect(method = SHOOT, at = @At(value = "INVOKE", target = "Lnet/minecraft/class_1890;method_8225(Lnet/minecraft/class_1887;Lnet/minecraft/class_1799;)I"), require = 1, allow = 1)
    private static int prominence$normalArrowInfinity(class_1887 enchantment, class_1799 weapon,
            class_1937 world, class_1309 shooter, SpellInfo spell,
            SpellHelper.ImpactContext context, int sequence) {
        if (enchantment == class_1893.field_9125 && shooter instanceof class_1657 player) {
            var selected = QuiverAmmo.selected(player);
            // Vanilla Infinity does not make tipped/spectral/modded arrows free.
            if (selected != null && !selected.arrow().method_31574(class_1802.field_8107)) return 0;
        }
        return class_1890.method_8225(enchantment, weapon);
    }
}
