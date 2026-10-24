package local.prominence.smithing.mixin;

import net.lyof.sortilege.setup.ModConfig;
import net.lyof.sortilege.util.EnchantHelper;
import net.minecraft.class_1799;
import net.minecraft.class_1887;
import net.minecraft.class_1890;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sortilege 9.0 caches by ItemStack identity even while that stack's NBT changes.
 * Read current state at both accessors; keep its slot rules and smithing filters.
 * No shared last-stack state, no global changes to enchantment application.
 */
@Pseudo
@Mixin(targets = "net.lyof.sortilege.util.EnchantHelper", remap = false)
public abstract class SortilegeSlotCacheMixin {
    @Inject(method = "getUsedEnchantSlots(Lnet/minecraft/class_1799;)I",
        at = @At("HEAD"), cancellable = true, remap = false)
    private static void pkCurrentUsed(class_1799 stack, CallbackInfoReturnable<Integer> cir) {
        boolean cursesAddSlots = (Boolean) ModConfig.cursesAddSlots.get();
        int used = 0;
        for (class_1887 enchant : class_1890.method_8222(stack).keySet()) {
            if (!cursesAddSlots || !enchant.method_8195()) used++;
        }
        cir.setReturnValue(used);
    }

    @Inject(method = "getTotalEnchantSlots(Lnet/minecraft/class_1799;)I",
        at = @At("HEAD"), cancellable = true, remap = false)
    private static void pkCurrentTotal(class_1799 stack, CallbackInfoReturnable<Integer> cir) {
        int total = EnchantHelper.getBaseEnchantSlots(stack);
        if (total >= 0) total += EnchantHelper.getExtraEnchantSlots(stack)
            + EnchantHelper.getCurseEnchantSlots(stack);
        cir.setReturnValue(total);
    }
}
