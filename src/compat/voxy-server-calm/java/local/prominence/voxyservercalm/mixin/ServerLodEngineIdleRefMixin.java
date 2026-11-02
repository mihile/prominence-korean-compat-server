package local.prominence.voxyservercalm.mixin;

import com.dripps.voxyserver.server.ServerLodEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerLodEngine.class, remap = false)
public abstract class ServerLodEngineIdleRefMixin {
    /**
     * Keep WorldEngine references alive while the server is running so Active world cleaner
     * does not repeatedly close and reopen Overworld RocksDB every 10 seconds of idle ingest.
     * Full shutdown still calls releaseIngestReferences() -> releaseIngestReferencesLocked().
     */
    @Inject(method = "releaseIdleIngestReferences", at = @At("HEAD"), cancellable = true, remap = false)
    private void prominence$retainActiveWorldEngineDuringSession(CallbackInfo ci) {
        ci.cancel();
    }
}
