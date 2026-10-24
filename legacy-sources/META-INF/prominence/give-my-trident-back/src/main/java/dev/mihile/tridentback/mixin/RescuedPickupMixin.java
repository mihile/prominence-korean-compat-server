package dev.mihile.tridentback.mixin;

import dev.mihile.tridentback.RescueData;
import dev.mihile.tridentback.RescueState;
import dev.mihile.tridentback.VoidPolicy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class RescuedPickupMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void gmtb$onlyOriginalOwner(Player player, CallbackInfo ci) {
        RescueData data = ((RescueState)this).gmtb$data();
        if (!VoidPolicy.allowPickup(data.rescuing, data.playerOwner, player.getUUID())) ci.cancel();
    }
}
