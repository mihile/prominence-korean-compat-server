package local.prominence.hourglass.mixin;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="elocindev.eldritch_end.entity.faceless.FacelessEntity",remap=false) public abstract class FacelessMixin {
 @ModifyArg(method="shadowSurgeAttack",at=@At(value="INVOKE",target="Lnet/minecraft/class_1657;method_5643(Lnet/minecraft/class_1282;F)Z"),index=1)
 private float prominence$surge(float damage){return damage*0.2f;}
}
