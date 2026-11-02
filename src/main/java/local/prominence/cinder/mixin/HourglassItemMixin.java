package local.prominence.cinder.mixin;

import local.prominence.cinder.hourglass.HourglassSafeDelivery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "elocindev.prominentbase.item.HourglassItem", remap = false)
public abstract class HourglassItemMixin {
    @Inject(method = "lambda$useOnBlock$0", at = @At("HEAD"))
    private static void onChallengeCreated(
        net.minecraft.class_1657 player,
        net.minecraft.class_1937 world,
        java.util.UUID challengeUuid,
        java.util.UUID initializingPlayer,
        net.minecraft.class_2338 pos,
        elocindev.prominentbase.mythicchallenge.MythicChallenge challenge,
        CallbackInfo ci
    ) {
        if (challenge == null && player instanceof net.minecraft.class_3222 serverPlayer) {
            net.minecraft.class_1799 refund = new net.minecraft.class_1799(elocindev.prominentbase.registry.ProminentBaseItems.HOURGLASS);
            HourglassSafeDelivery.deliver(serverPlayer.method_5682(), serverPlayer, refund, pos, HourglassSafeDelivery.DeliveryType.CREATION_FAILED);
        }
    }
}
