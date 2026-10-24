package dev.mihile.tridentback.mixin;

import dev.mihile.tridentback.SimplyRealWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "net.sweenus.simplyswords.entity.ThrownSwordEntity", remap = false)
public abstract class SimplyThrownSwordMixin implements SimplyRealWeapon {
    @Shadow(remap = false) public boolean nonReturning;
    @Override public boolean gmtb$isRealWeapon() { return !nonReturning; }
}
