package local.prominence.hourglass.mixin;
import elocindev.prominentbase.mythicchallenge.MythicChallenge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(targets="elocindev.prominentbase.entity.nonliving.ChallengePortalEntity",remap=false)
public abstract class PortalMixin {
 @Redirect(method="handleEntryCollision",at=@At(value="INVOKE",target="Lelocindev/prominentbase/mythicchallenge/MythicChallenge;isFullPartyScaled()Z"))
 private boolean prominence$omitOutdatedMessage(MythicChallenge challenge){return false;}
}
