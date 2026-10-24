package local.prominence.backpackcompat.mixin;
import elocindev.tierify.screen.ReforgeScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=ReforgeScreenHandler.class,remap=false)
public interface ReforgeReadyAccess {
    @Accessor("reforgeReady") boolean prominence$reforgeReady();
}
