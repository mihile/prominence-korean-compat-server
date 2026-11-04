package local.prominence.hourglass;
import mod.chloeprime.aaaparticles.api.common.ParticleEmitterInfo;
import net.minecraft.class_1297;
import net.minecraft.class_2960;
/** Bound emitters use zero local offset. A world position would be added twice. */
public final class FacelessEffect {
 private FacelessEffect(){}
 public static ParticleEmitterInfo create(String name,float scale,class_1297 boss){
  return new ParticleEmitterInfo(new class_2960("eldritch_end",name)).scale(scale).bindOnEntity(boss);
 }
}
