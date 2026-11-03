package local.prominence.indexwarmup;
import com.dripps.voxyserver.server.ServerLodEngine;
import me.cortex.voxy.commonImpl.WorldIdentifier;
import net.minecraft.class_2960;
import org.slf4j.LoggerFactory;
public final class IndexWarmupTask implements Runnable {
 private final ServerLodEngine engine;private final WorldIdentifier identifier;private final class_2960 dimension;private final Runnable heartbeat;
 public IndexWarmupTask(ServerLodEngine engine,WorldIdentifier identifier,class_2960 dimension,Runnable heartbeat){this.engine=engine;this.identifier=identifier;this.dimension=dimension;this.heartbeat=heartbeat;}
 @Override public void run(){
  if(Thread.currentThread().isInterrupted())return;
  long start=System.nanoTime();
  try{
   var world=engine.getOrCreate(identifier,dimension);
   if(world==null||Thread.currentThread().isInterrupted())return;
   boolean completed=((IndexPreparationAccess)engine).prominence$prepareIndex(identifier,world,heartbeat);
   if(completed)LoggerFactory.getLogger("Prominence compatibility").info("Default-world LOD startup preparation attempt finished in {}ms",(System.nanoTime()-start)/1_000_000);
  }catch(RuntimeException failure){
   if(!Thread.currentThread().isInterrupted())LoggerFactory.getLogger("Prominence compatibility").warn("Startup LOD index preparation skipped; normal streaming fallback remains available",failure);
  }
 }
}
