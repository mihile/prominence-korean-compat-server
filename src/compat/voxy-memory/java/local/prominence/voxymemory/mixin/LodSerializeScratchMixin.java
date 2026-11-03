package local.prominence.voxymemory.mixin;
import me.cortex.voxy.common.world.WorldSection;
import local.prominence.voxymemory.SectionScratch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(targets="com.dripps.voxyserver.server.LodStreamingService",remap=false)
public abstract class LodSerializeScratchMixin {
 @Redirect(method="serializeSection",at=@At(value="INVOKE",target="Lme/cortex/voxy/common/world/WorldSection;copyData()[J"),require=1)
 private long[] prominence$copyToScratch(WorldSection section){return SectionScratch.copy(section);}
}
