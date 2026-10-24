package dev.mihile.tridentback.mixin;

import dev.mihile.tridentback.RescueController;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void gmtb$beforeWeaponTick(Entity entity, CallbackInfo ci) {
        if (RescueController.handle((ServerLevel)(Object)this, entity)) ci.cancel();
    }
}
