package local.prominence.vitalitybalance.mixin;

import local.prominence.vitalitybalance.VitalityLimit;
import net.minecraft.class_2960;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="dev.shadowsoffire.apotheosis.potion.PotionModule$CharmMatcher",remap=false)
public abstract class VitalityCharmMatcherMixin {
    @Inject(method="isDisabled(Lnet/minecraft/class_2960;I)Z",at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private static void prominence$limitVitality(class_2960 id,int amplifier,CallbackInfoReturnable<Boolean> cir) {
        if (VitalityLimit.restricted(id,amplifier)) cir.setReturnValue(true);
    }
}
