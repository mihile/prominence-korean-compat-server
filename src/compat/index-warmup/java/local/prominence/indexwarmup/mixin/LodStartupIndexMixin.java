package local.prominence.indexwarmup.mixin;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import com.dripps.voxyserver.server.ServerLodEngine;
import local.prominence.indexwarmup.*;
import me.cortex.voxy.commonImpl.WorldIdentifier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.*;
@Mixin(targets="com.dripps.voxyserver.server.LodStreamingService",remap=false)
public abstract class LodStartupIndexMixin implements LodWarmupAccess {
 @Shadow @Final private ServerLodEngine engine;
 @Shadow private volatile ExecutorService streamExecutor;
 @Shadow private volatile long lastStreamHeartbeat;
 @Unique private boolean prominence$startupIndexQueued;
 @Override public void prominence$warmStartup(MinecraftServer server){
  if(prominence$startupIndexQueued||engine==null||!server.method_18854())return;
  var level=server.method_30002();if(level==null)return;
  var id=WorldIdentifier.of(level);if(id==null)return;
  var dimension=level.method_27983().method_29177();
  try{
   streamExecutor.execute(new IndexWarmupTask(engine,id,dimension,()->lastStreamHeartbeat=System.nanoTime()));
   prominence$startupIndexQueued=true;
  }catch(RejectedExecutionException ignored){/* Native shutdown owns this executor; do not resurrect it. */}
 }
}
