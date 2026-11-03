package local.prominence.trinketslot.mixin;

import local.prominence.trinketslot.SlotNameNormalizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.emi.trinkets.compat.WrappingTrinketsUtils", remap = false)
public abstract class SlotNamePatternMixin {
    @Redirect(method = "filterGroupInfo(Ljava/lang/String;)Ljava/lang/String;", at = @At(value = "INVOKE",
            target = "Ljava/lang/String;replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"), require = 1)
    private static String prominence$reuseSlotNamePattern(String name, String expression, String replacement) {
        if ("(trinket_group_).*-".equals(expression) && "".equals(replacement)) {
            return SlotNameNormalizer.normalize(name);
        }
        return name.replaceAll(expression, replacement);
    }
}
