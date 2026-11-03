package local.prominence.equipment.mixin;
import net.minecraft.class_3222;
import net.minecraft.class_1320;
import local.prominence.equipment.CriticalStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(targets="elocindev.prominent.primordial.set.FrostDkArmorSetBonus",remap=false)
public abstract class FrostDkCritMixin {
 @Redirect(method="applyWinterDamage",at=@At(value="INVOKE",target="Lnet/minecraft/class_3222;method_26825(Lnet/minecraft/class_1320;)D"),require=1)
 private double prominence$correctChance(class_3222 entity,class_1320 attribute) {
  return CriticalStats.attributeValue(entity,attribute);
 }
}
