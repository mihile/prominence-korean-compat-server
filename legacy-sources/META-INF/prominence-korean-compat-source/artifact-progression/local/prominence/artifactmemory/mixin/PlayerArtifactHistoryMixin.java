package local.prominence.artifactmemory.mixin;
import local.prominence.artifactmemory.ArtifactHistory;
import local.prominence.artifactmemory.ArtifactHistoryStore;
import net.minecraft.class_2487;
import net.puffish.skillsmod.server.data.PlayerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="net.puffish.skillsmod.server.data.PlayerData",remap=false)
public abstract class PlayerArtifactHistoryMixin implements ArtifactHistoryStore {
    @Unique private class_2487 prominence$history=new class_2487();
    public class_2487 prominence$artifactHistory(){return prominence$history;}
    public void prominence$setArtifactHistory(class_2487 value){prominence$history=ArtifactHistory.sanitize(value);}
    @Inject(method="writeNbt(Lnet/minecraft/class_2487;)Lnet/minecraft/class_2487;",at=@At("RETURN"),require=1)
    private void prominence$write(class_2487 input,CallbackInfoReturnable<class_2487> ci){
        ci.getReturnValue().method_10566(ArtifactHistory.TAG,prominence$history.method_10553());
    }
    @Inject(method="read(Lnet/minecraft/class_2487;)Lnet/puffish/skillsmod/server/data/PlayerData;",at=@At("RETURN"),require=1)
    private static void prominence$read(class_2487 input,CallbackInfoReturnable<PlayerData> ci){
        ((ArtifactHistoryStore)(Object)ci.getReturnValue()).prominence$setArtifactHistory(input.method_10562(ArtifactHistory.TAG));
    }
}
