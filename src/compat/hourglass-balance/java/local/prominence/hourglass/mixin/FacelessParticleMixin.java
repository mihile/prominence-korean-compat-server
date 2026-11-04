package local.prominence.hourglass.mixin;
import mod.chloeprime.aaaparticles.api.common.AAALevel;
import mod.chloeprime.aaaparticles.api.common.ParticleEmitterInfo;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(targets="elocindev.eldritch_end.entity.faceless.FacelessEntity",remap=false)
public abstract class FacelessParticleMixin {
 @Redirect(method="shadowSurge",at=@At(value="INVOKE",target="Lelocindev/eldritch_end/client/particle/EldritchParticles;playEffek(Ljava/lang/String;Lnet/minecraft/class_1937;Lnet/minecraft/class_243;ZF)Lmod/chloeprime/aaaparticles/api/common/ParticleEmitterInfo;"))
 private ParticleEmitterInfo prominence$bindBeforeSend(String name,class_1937 world,class_243 pos,boolean distant,float scale){
  var emitter=new ParticleEmitterInfo(new class_2960("eldritch_end",name)).position(pos).scale(scale).bindOnEntity((class_1297)(Object)this);
  AAALevel.addParticle(world,distant,emitter);
  return emitter;
 }
}
