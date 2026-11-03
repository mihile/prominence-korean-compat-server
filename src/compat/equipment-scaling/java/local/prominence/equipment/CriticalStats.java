package local.prominence.equipment;
import net.minecraft.class_1309;
import net.minecraft.class_1320;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellPowerMechanics;
import net.spell_power.api.SpellSchools;
public final class CriticalStats {
 private CriticalStats() {}
 public static double percentUnits(double chance) {
  return Double.isFinite(chance) ? Math.max(0.0,chance)*100.0 : 0.0;
 }
 public static double attributeValue(class_1309 caster,class_1320 attribute) {
  if(attribute==SpellPowerMechanics.CRITICAL_CHANCE.attribute)
   return percentUnits(SpellPower.getSpellPower(SpellSchools.FROST,caster).criticalChance());
  return caster.method_26825(attribute);
 }
}
