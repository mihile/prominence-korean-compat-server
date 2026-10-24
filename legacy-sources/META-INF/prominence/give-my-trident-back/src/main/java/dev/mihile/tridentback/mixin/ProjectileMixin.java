package dev.mihile.tridentback.mixin;

import dev.mihile.tridentback.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import java.util.UUID;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileMixin implements RescueState {
    @Shadow private UUID ownerUUID;
    @Unique private RescueData gmtb$data = new RescueData();
    @Override public RescueData gmtb$data() { return gmtb$data; }
    @Override public UUID gmtb$actualOwner() { return ownerUUID; }

    @Inject(method = "setOwner", at = @At("TAIL"))
    private void gmtb$recordOwner(Entity owner, CallbackInfo ci) {
        if (!((Object)this instanceof AbstractArrow arrow) || RescueController.family(arrow) == WeaponFamily.NONE) return;
        if (owner instanceof ServerPlayer player) gmtb$data.recordPlayer(player.getUUID(), player.getAbilities().instabuild);
        else gmtb$data.playerOwner = null;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void gmtb$save(CompoundTag tag, CallbackInfo ci) {
        if ((Object)this instanceof SimplyRealWeapon real && !real.gmtb$isRealWeapon()) gmtb$data.synthetic = true;
        gmtb$data.write(tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void gmtb$load(CompoundTag tag, CallbackInfo ci) { gmtb$data.read(tag); }
}
