package local.prominence.hourglass.mixin;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;import net.minecraft.*;import elocindev.prominentbase.mythicchallenge.MythicChallenge;import local.prominence.hourglass.Balance;
@Mixin(value=class_3218.class,remap=false) public abstract class WorldMixin {
 @Inject(method="method_14175",at=@At("RETURN")) private void prominence$mob(class_1297 e,CallbackInfoReturnable<Boolean> ci){if(ci.getReturnValueZ() && !Balance.inBossSpawn() && e instanceof class_1308 mob && MythicChallenge.isChallengeWorld((class_3218)(Object)this)) Balance.reduceMob(mob);}
}
