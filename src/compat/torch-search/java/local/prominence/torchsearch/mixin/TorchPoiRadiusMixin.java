package local.prominence.torchsearch.mixin;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.*;
import fuzs.magnumtorch.config.ServerConfig;
import local.prominence.torchsearch.SearchRadius;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(targets="fuzs.magnumtorch.handler.MobSpawningHandler",remap=false)
public abstract class TorchPoiRadiusMixin {
 @Redirect(method="isSpawnCancelled",at=@At(value="INVOKE",target="Lnet/minecraft/class_4153;method_21647(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/class_2338;ILnet/minecraft/class_4153$class_4155;)Ljava/util/stream/Stream;"),require=1)
 private static Stream<class_2338> prominence$tightSearch(class_4153 manager,Predicate<class_6880<class_4158>> typeFilter,Predicate<class_2338> positionFilter,class_2338 center,int radius,class_4153.class_4155 occupation,
   class_4153 originalManager,class_1299<?> entity,class_2338 toCheck,class_3730 reason,class_5321<class_4158> poiKey,ServerConfig.MagnumTorchConfig config){
  return manager.method_21647(typeFilter,positionFilter,center,SearchRadius.forConfig(radius,config),occupation);
 }
}
