package local.prominence.hourglass.mixin;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import elocindev.prominentbase.mythicchallenge.*;import local.prominence.hourglass.Balance;
@Mixin(value=MythicBossEntry.class,remap=false) public abstract class BossMixin {
 @Redirect(method="spawn",at=@At(value="INVOKE",target="Lelocindev/prominentbase/mythicchallenge/MythicScalingConfig;targetBossHp(IDID)D"))
 private double prominence$hp(MythicScalingConfig c,int level,double time,int party,double budget){return Balance.bossHp(c,level,time,party,budget);}
}
