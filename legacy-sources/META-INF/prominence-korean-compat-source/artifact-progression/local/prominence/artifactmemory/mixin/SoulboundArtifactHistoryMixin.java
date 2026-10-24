package local.prominence.artifactmemory.mixin;
import local.prominence.artifactmemory.ArtifactHistory;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="elocindev.prominent.soulbinding.Soulbound",remap=false)
public interface SoulboundArtifactHistoryMixin {
    @Inject(method="soulbind(Lnet/minecraft/class_1799;Lnet/minecraft/class_1657;)V",at=@At("HEAD"),require=1)
    private static void prominence$capture(class_1799 stack,class_1657 player,CallbackInfo ci){
        if(player instanceof class_3222 server)ArtifactHistory.capture(server,stack);
    }
    @Inject(method="soulbind(Lnet/minecraft/class_1799;Lnet/minecraft/class_1657;)V",at=@At("RETURN"),require=1)
    private static void prominence$restore(class_1799 stack,class_1657 player,CallbackInfo ci){
        if(player instanceof class_3222 server)ArtifactHistory.restore(server,stack);
    }
}
