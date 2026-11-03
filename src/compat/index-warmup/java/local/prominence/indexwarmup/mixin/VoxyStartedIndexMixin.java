package local.prominence.indexwarmup.mixin;
import com.dripps.voxyserver.server.LodStreamingService;
import local.prominence.indexwarmup.LodWarmupAccess;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.dripps.voxyserver.Voxyserver",remap=false)
public abstract class VoxyStartedIndexMixin {
 @Shadow private LodStreamingService streamingService;
 @Inject(method="lambda$onInitialize$2",at=@At("TAIL"),remap=false,require=1)
 private void prominence$queueDefaultIndex(MinecraftServer server,CallbackInfo ci){
  if(streamingService!=null)((LodWarmupAccess)streamingService).prominence$warmStartup(server);
 }
}
