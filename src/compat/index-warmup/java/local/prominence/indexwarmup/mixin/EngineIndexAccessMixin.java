package local.prominence.indexwarmup.mixin;
import local.prominence.indexwarmup.IndexPreparationAccess;
import me.cortex.voxy.commonImpl.WorldIdentifier;
import me.cortex.voxy.common.world.WorldEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(targets="com.dripps.voxyserver.server.ServerLodEngine",remap=false)
public abstract class EngineIndexAccessMixin implements IndexPreparationAccess {
 @Invoker(value="prepareStoredSectionPresence",remap=false)
 public abstract boolean prominence$prepareIndex(WorldIdentifier id,WorldEngine world,Runnable heartbeat);
}
