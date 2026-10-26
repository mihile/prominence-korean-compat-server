/* SPDX-License-Identifier: MIT */
package local.prominence.arrowtick.mixin;
import local.prominence.arrowtick.NeruinaUuid;
import net.minecraft.class_2487;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="com.bawnorton.neruina.util.TickingEntry",remap=false)
public class NeruinaEntryMixin {
    @ModifyVariable(method="fromNbt",at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false)
    private static class_2487 prominence$legacyUuid(class_2487 nbt) { return NeruinaUuid.read(nbt); }
    @Inject(method="writeNbt",at=@At("RETURN"),remap=false)
    private void prominence$writeUuid(CallbackInfoReturnable<class_2487> ci) { NeruinaUuid.write(ci.getReturnValue()); }
}
