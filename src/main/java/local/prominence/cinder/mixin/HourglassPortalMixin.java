package local.prominence.cinder.mixin;

import local.prominence.cinder.hourglass.HourglassSafeDelivery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(targets = "elocindev.prominentbase.entity.nonliving.ChallengePortalEntity", remap = false)
public abstract class HourglassPortalMixin {
    @Shadow private UUID challengeUuid;
    @Shadow private UUID initializingPlayer;
    @Shadow private net.minecraft.class_2338 overworldSpawnPos;
    @Shadow public abstract boolean isExitPortal();

    @Inject(method = "handleExitCollision", at = @At("HEAD"))
    private void onHandleExitCollision(net.minecraft.class_3222 player, CallbackInfo ci) {
        try {
            if (player == null || player.method_37908() == null) return;
            net.minecraft.server.MinecraftServer server = player.method_5682();
            if (server == null) return;

            UUID cUuid = elocindev.prominentbase.mythicchallenge.MythicChallenge.uuidFromWorld(player.method_37908());
            if (cUuid == null) return;

            elocindev.prominentbase.mythicchallenge.MythicChallengesComponent comp =
                elocindev.prominentbase.mythicchallenge.MythicChallengesComponent.KEY.getNullable(player.method_37908().method_8428());
            if (comp == null) return;

            elocindev.prominentbase.mythicchallenge.MythicChallenge challenge = comp.getChallenge(cUuid);
            if (challenge == null) return;

            MythicChallengeAccessor acc = (MythicChallengeAccessor)challenge;
            if (challenge.data.endState == elocindev.prominentbase.mythicchallenge.MythicChallengeData.EndState.WIN && !acc.getRewardGiven()) {
                net.minecraft.class_1799 reward = acc.invokeBuildWinReward((net.minecraft.class_3218)player.method_37908(), player.method_5667());
                HourglassSafeDelivery.deliver(server, player, reward, this.overworldSpawnPos, HourglassSafeDelivery.DeliveryType.VICTORY);
                acc.setRewardGiven(true);
            } else if (!acc.getHourglassRefunded() && (challenge.data.endState == elocindev.prominentbase.mythicchallenge.MythicChallengeData.EndState.LOSE || challenge.data.endState == elocindev.prominentbase.mythicchallenge.MythicChallengeData.EndState.TIMEOUT)) {
                int level = acc.getPendingRefundLevel() >= 0 ? acc.getPendingRefundLevel() : challenge.data.challengeLevel;
                net.minecraft.class_1799 refund = acc.invokeBuildRefund(level);
                HourglassSafeDelivery.deliver(server, player, refund, this.overworldSpawnPos, HourglassSafeDelivery.DeliveryType.REFUND);
                acc.setHourglassRefunded(true);
                acc.setPendingRefundLevel(-1);
            }
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger("ProminenceHourglassGuard").error("Failed in onHandleExitCollision", t);
        }
    }

    @Inject(method = "method_5773", at = @At(value = "INVOKE", target = "Lelocindev/prominentbase/entity/nonliving/ChallengePortalEntity;method_31472()V"))
    private void onPortalDiscard(CallbackInfo ci) {
        prominence$refundIfEmpty();
    }

    @Inject(method = "method_5773", at = @At("RETURN"))
    private void onTickEnd(CallbackInfo ci) {
        if (!this.isExitPortal() && this.challengeUuid != null) {
            net.minecraft.class_1297 self = (net.minecraft.class_1297)(Object)this;
            if (self.method_31481()) {
                prominence$refundIfEmpty();
            }
        }
    }

    @Unique
    private void prominence$refundIfEmpty() {
        try {
            if (this.isExitPortal() || this.challengeUuid == null) return;
            net.minecraft.class_1297 self = (net.minecraft.class_1297)(Object)this;
            net.minecraft.class_1937 world = self.method_37908();
            if (world == null || world.method_8608()) return;

            net.minecraft.server.MinecraftServer server = self.method_5682();
            if (server == null) return;

            elocindev.prominentbase.mythicchallenge.MythicChallengesComponent comp =
                elocindev.prominentbase.mythicchallenge.MythicChallengesComponent.KEY.getNullable(world.method_8428());
            if (comp == null) return;

            elocindev.prominentbase.mythicchallenge.MythicChallenge challenge = comp.getChallenge(this.challengeUuid);
            if (challenge != null && challenge.data.players.isEmpty() && !((MythicChallengeAccessor)challenge).getHourglassRefunded()) {
                net.minecraft.class_3222 player = this.initializingPlayer != null ? server.method_3760().method_14602(this.initializingPlayer) : null;
                net.minecraft.class_1799 refund = ((MythicChallengeAccessor)challenge).invokeBuildRefund(challenge.data.challengeLevel);

                HourglassSafeDelivery.deliver(server, player, refund, self.method_24515(), HourglassSafeDelivery.DeliveryType.TIMEOUT);
                ((MythicChallengeAccessor)challenge).setHourglassRefunded(true);

                comp.closeChallenge(this.challengeUuid);
                this.challengeUuid = null;
            }
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger("ProminenceHourglassGuard").error("Failed in refundIfEmpty", t);
        }
    }
}
