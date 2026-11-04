package local.prominence.hourglass.mixin;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
import elocindev.prominentbase.mythicchallenge.*;import elocindev.prominentbase.mythicchallenge.affix.*;import net.minecraft.*;import local.prominence.hourglass.Balance;
@Mixin(value=MythicAffixController.class,remap=false) public abstract class AffixMixin {
 @Shadow @Final private MythicChallenge challenge;
 @Redirect(method="onBossSpawned",at=@At(value="INVOKE",target="Lelocindev/prominentbase/mythicchallenge/MythicBossEntry;spawn(Lnet/minecraft/class_3218;Lnet/minecraft/class_2338;II)Lnet/minecraft/class_1309;"))
 private class_1309 prominence$twin(MythicBossEntry b,class_3218 w,class_2338 pos,int level,int party){return Balance.spawn(b,w,pos,level,party,challenge.data.players.size());}
 @Inject(method="tick",at=@At("HEAD")) private void prominence$clearObsidilithFatigue(CallbackInfo ci){var entry=challenge.bossEntry();if(entry == null || !"bosses_of_mass_destruction:obsidilith".equals(class_7923.field_41177.method_10221(entry.type()).toString()))return;for(var player:challenge.worldHandle.asWorld().method_18456())player.method_6016(class_1294.field_5901);}
 @ModifyVariable(method="applyAddMods",at=@At("HEAD"),argsOnly=true,ordinal=0)
 private float prominence$addBaseline(float hp){return (float)(hp/Balance.factor(challenge.data.players.size()));}
 @ModifyConstant(method="applyAddMods",constant=@Constant(doubleValue=1.0,ordinal=0)) private double prominence$minimumAddHp(double min){return min * 0.8;}
 @Inject(method="applyAddMods",at=@At("HEAD")) private void prominence$removeGeneric(class_1309 e,float hp,CallbackInfo ci){Balance.removeMobModifier(e);}
 @Redirect(method="tickFreezingCold",at=@At(value="INVOKE",target="Lnet/minecraft/class_3222;method_6092(Lnet/minecraft/class_1293;)Z",ordinal=1))
 private boolean prominence$fatigue(class_3222 p,class_1293 effect){var entry=challenge.bossEntry();return entry != null && "bosses_of_mass_destruction:obsidilith".equals(class_7923.field_41177.method_10221(entry.type()).toString()) ? false : p.method_6092(effect);}
}
