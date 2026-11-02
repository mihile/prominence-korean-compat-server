package local.prominence.voxyservercalm.mixin;

import com.dripps.voxyserver.server.ChunkVoxelizer;
import local.prominence.voxyservercalm.VoxyServerCalmState;
import net.minecraft.class_2818;
import net.minecraft.class_3218;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkVoxelizer.class, remap = false)
public abstract class ChunkVoxelizerLoadPaceMixin {
    @Shadow(remap = false)
    private long currentTick;

    @Shadow(remap = false)
    private void scheduleRetry(class_3218 level, class_2818 chunk) {}

    @Inject(
        method = "onChunkLoad(Lnet/minecraft/class_3218;Lnet/minecraft/class_2818;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/dripps/voxyserver/server/ChunkVoxelizer;ingestChunk(Lnet/minecraft/class_3218;Lnet/minecraft/class_2818;Z)Lcom/dripps/voxyserver/server/ChunkVoxelizer$IngestOutcome;"
        ),
        cancellable = true,
        remap = false
    )
    private void prominence$paceChunkLoadIngestBurst(class_3218 level, class_2818 chunk, CallbackInfo ci) {
        if (!VoxyServerCalmState.tryAcquireLoadIngest(this.currentTick)) {
            this.scheduleRetry(level, chunk);
            ci.cancel();
        }
    }
}
