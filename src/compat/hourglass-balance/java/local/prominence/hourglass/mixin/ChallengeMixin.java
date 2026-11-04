package local.prominence.hourglass.mixin;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;
import elocindev.prominentbase.mythicchallenge.*;import net.minecraft.*;import local.prominence.hourglass.Balance;
@Mixin(value=MythicChallenge.class,remap=false) public abstract class ChallengeMixin {
 @Shadow @Final public MythicChallengeData data;
 @Redirect(method="spawnBoss",at=@At(value="INVOKE",target="Lelocindev/prominentbase/mythicchallenge/MythicBossEntry;spawn(Lnet/minecraft/class_3218;Lnet/minecraft/class_2338;II)Lnet/minecraft/class_1309;"))
 private class_1309 prominence$boss(MythicBossEntry b,class_3218 w,class_2338 pos,int level,int party){return Balance.spawn(b,w,pos,level,party,data.players.size());}
}
