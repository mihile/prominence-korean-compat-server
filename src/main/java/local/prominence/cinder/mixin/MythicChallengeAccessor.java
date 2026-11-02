package local.prominence.cinder.mixin;

import net.minecraft.class_1799;
import net.minecraft.class_3218;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.UUID;

@Mixin(targets = "elocindev.prominentbase.mythicchallenge.MythicChallenge", remap = false)
public interface MythicChallengeAccessor {
    @Accessor("rewardGiven")
    boolean getRewardGiven();

    @Accessor("rewardGiven")
    void setRewardGiven(boolean value);

    @Accessor("hourglassRefunded")
    boolean getHourglassRefunded();

    @Accessor("hourglassRefunded")
    void setHourglassRefunded(boolean value);

    @Accessor("pendingRefundLevel")
    int getPendingRefundLevel();

    @Accessor("pendingRefundLevel")
    void setPendingRefundLevel(int level);

    @Invoker("buildWinReward")
    class_1799 invokeBuildWinReward(class_3218 world, UUID uuid);

    @Invoker("buildRefund")
    class_1799 invokeBuildRefund(int level);
}
