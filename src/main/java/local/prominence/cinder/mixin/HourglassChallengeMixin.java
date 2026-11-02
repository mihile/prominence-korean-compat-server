package local.prominence.cinder.mixin;

import local.prominence.cinder.hourglass.HourglassSafeDelivery;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "elocindev.prominentbase.mythicchallenge.MythicChallenge", remap = false)
public abstract class HourglassChallengeMixin {
    @Shadow @Final public elocindev.prominentbase.mythicchallenge.MythicChallengeData data;
    @Shadow @Final public net.minecraft.class_2338 spawnPos;
    @Shadow @Final public xyz.nucleoid.fantasy.RuntimeWorldHandle worldHandle;

    @Inject(method = "giveReward", at = @At("HEAD"), cancellable = true)
    private void onGiveReward(net.minecraft.class_3218 world, CallbackInfoReturnable<Boolean> cir) {
        if (this.data.endState != elocindev.prominentbase.mythicchallenge.MythicChallengeData.EndState.WIN) {
            cir.setReturnValue(true);
            return;
        }
        net.minecraft.server.MinecraftServer server = world.method_8503();
        net.minecraft.class_3222 recipient = null;
        if (this.data.initializingPlayer != null) {
            recipient = server.method_3760().method_14602(this.data.initializingPlayer);
        }
        if (recipient == null && !this.data.players.isEmpty()) {
            for (java.util.UUID u : this.data.players) {
                recipient = server.method_3760().method_14602(u);
                if (recipient != null) break;
            }
        }
        if (recipient == null) {
            cir.setReturnValue(false);
            return;
        }

        net.minecraft.class_1799 reward = ((MythicChallengeAccessor)this).invokeBuildWinReward(world, recipient.method_5667());
        boolean delivered = HourglassSafeDelivery.deliver(server, recipient, reward, this.spawnPos, HourglassSafeDelivery.DeliveryType.VICTORY);
        cir.setReturnValue(delivered);
    }

    @Inject(method = "refundHourglass", at = @At("HEAD"), cancellable = true)
    private void onRefundHourglass(int level, CallbackInfo ci) {
        if (((MythicChallengeAccessor)this).getHourglassRefunded()) {
            ci.cancel();
            return;
        }
        net.minecraft.class_3218 world = this.worldHandle.asWorld();
        net.minecraft.server.MinecraftServer server = world.method_8503();
        net.minecraft.class_3222 recipient = null;
        if (this.data.initializingPlayer != null) {
            recipient = server.method_3760().method_14602(this.data.initializingPlayer);
        }
        if (recipient == null && !this.data.players.isEmpty()) {
            for (java.util.UUID u : this.data.players) {
                recipient = server.method_3760().method_14602(u);
                if (recipient != null) break;
            }
        }
        if (recipient == null) {
            ((MythicChallengeAccessor)this).setPendingRefundLevel(level);
            ci.cancel();
            return;
        }

        net.minecraft.class_1799 refund = ((MythicChallengeAccessor)this).invokeBuildRefund(level);
        HourglassSafeDelivery.deliver(server, recipient, refund, this.spawnPos, HourglassSafeDelivery.DeliveryType.REFUND);
        ((MythicChallengeAccessor)this).setHourglassRefunded(true);
        ((MythicChallengeAccessor)this).setPendingRefundLevel(-1);
        ci.cancel();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        net.minecraft.class_3218 arenaWorld = this.worldHandle.asWorld();
        net.minecraft.server.MinecraftServer server = arenaWorld.method_8503();
        if (server == null) return;

        MythicChallengeAccessor acc = (MythicChallengeAccessor)this;
        if (acc.getPendingRefundLevel() >= 0) {
            net.minecraft.class_3222 recipient = this.data.initializingPlayer != null ? server.method_3760().method_14602(this.data.initializingPlayer) : null;
            if (recipient != null) {
                net.minecraft.class_1799 refund = acc.invokeBuildRefund(acc.getPendingRefundLevel());
                HourglassSafeDelivery.deliver(server, recipient, refund, this.spawnPos, HourglassSafeDelivery.DeliveryType.REFUND);
                acc.setHourglassRefunded(true);
                acc.setPendingRefundLevel(-1);
            }
        }
        if (this.data.endState == elocindev.prominentbase.mythicchallenge.MythicChallengeData.EndState.WIN && !acc.getRewardGiven()) {
            net.minecraft.class_3222 recipient = this.data.initializingPlayer != null ? server.method_3760().method_14602(this.data.initializingPlayer) : null;
            if (recipient != null) {
                net.minecraft.class_1799 reward = acc.invokeBuildWinReward(arenaWorld, recipient.method_5667());
                HourglassSafeDelivery.deliver(server, recipient, reward, this.spawnPos, HourglassSafeDelivery.DeliveryType.VICTORY);
                acc.setRewardGiven(true);
            }
        }
    }
}
