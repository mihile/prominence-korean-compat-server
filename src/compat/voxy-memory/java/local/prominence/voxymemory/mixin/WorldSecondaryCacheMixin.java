package local.prominence.voxymemory.mixin;
import local.prominence.voxymemory.SectionScratch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
@Mixin(targets="me.cortex.voxy.common.world.WorldEngine",remap=false)
public abstract class WorldSecondaryCacheMixin {
 @ModifyArg(method="<init>(Lme/cortex/voxy/common/config/section/SectionStorage;Lme/cortex/voxy/commonImpl/VoxyInstance;)V",at=@At(value="INVOKE",target="Lme/cortex/voxy/common/world/ActiveSectionTracker;<init>(ILme/cortex/voxy/common/world/ActiveSectionTracker$SectionLoader;ILme/cortex/voxy/common/world/WorldEngine;)V"),index=2,require=1)
 private int prominence$boundSecondaryCache(int original){return SectionScratch.cacheLimit(original);}
}
