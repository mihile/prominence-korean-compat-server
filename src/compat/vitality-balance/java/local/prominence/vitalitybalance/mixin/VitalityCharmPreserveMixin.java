package local.prominence.vitalitybalance.mixin;

import dev.shadowsoffire.apotheosis.potion.PotionCharmItem;
import local.prominence.vitalitybalance.VitalityLimit;
import net.minecraft.class_1297;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="dev.shadowsoffire.apotheosis.potion.PotionCharmItem",remap=false)
public abstract class VitalityCharmPreserveMixin {
    @Inject(method="charmLogic(Lnet/minecraft/class_1799;Lnet/minecraft/class_1937;Lnet/minecraft/class_1297;IZ)V",
            at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private void prominence$preserveRestrictedCharm(class_1799 stack,class_1937 world,class_1297 entity,int slot,boolean selected,CallbackInfo ci) {
        if (entity instanceof class_3222 && PotionCharmItem.hasEffect(stack) && VitalityLimit.restricted(PotionCharmItem.getEffect(stack))) {
            ci.cancel();
        }
    }
}
