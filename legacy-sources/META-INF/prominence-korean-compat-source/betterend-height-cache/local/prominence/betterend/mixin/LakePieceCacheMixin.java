package local.prominence.betterend.mixin;

import java.util.Map;
import local.prominence.betterend.HeightCaches;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "org.betterx.betterend.world.structures.piece.LakePiece", remap = false)
public abstract class LakePieceCacheMixin {
    @Shadow(remap = false) @Final @Mutable private Map<Integer, Byte> heightmap;

    // Explicit constructors keep worldgen and NBT reload coverage independently required.
    @Inject(method = "<init>(Lnet/minecraft/class_2338;FFLnet/minecraft/class_5819;Lnet/minecraft/class_6880;)V", at = @At("RETURN"), require = 1, remap = false)
    private void prominence$makeHeightCacheConcurrentNew(CallbackInfo ci) {
        heightmap = HeightCaches.concurrentCopy(heightmap);
    }
    @Inject(method = "<init>(Lnet/minecraft/class_6625;Lnet/minecraft/class_2487;)V", at = @At("RETURN"), require = 1, remap = false)
    private void prominence$makeHeightCacheConcurrentNbt(CallbackInfo ci) {
        heightmap = HeightCaches.concurrentCopy(heightmap);
    }
}
