package local.prominence.joinallocation.mixin;
import net.minecraft.class_1309;
import net.spell_engine.api.spell.Spell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(targets="com.invoke.InvokeMod",remap=false)
public abstract class InvokeUnusedCooldownMixin {
 @Redirect(method="lambda$onInitialize$5",at=@At(value="INVOKE",target="Lnet/spell_engine/internals/SpellHelper;getCooldownDuration(Lnet/minecraft/class_1309;Lnet/spell_engine/api/spell/Spell;)F"),require=1)
 private static float prominence$omitUnusedValue(class_1309 player,Spell spell){return 0.0F;}
}
