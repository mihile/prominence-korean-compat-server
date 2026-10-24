package local.prominence.backpackcompat.mixin;
import elocindev.tierify.network.TieredServerPacket;
import local.prominence.backpackcompat.PortableReforgeMenu;
import net.minecraft.class_2338;
import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=TieredServerPacket.class,remap=false)
public abstract class PortableReforgeReturnMixin {
    @Inject(method="lambda$init$2(Ljava/lang/Boolean;Lnet/minecraft/class_3222;Lnet/minecraft/class_2338;II)V",at=@At("HEAD"),cancellable=true,require=1)
    private static void prominence$returnToBackpack(Boolean reforging,class_3222 player,class_2338 pos,int x,int y,CallbackInfo ci) {
        if(!reforging && player.field_7512 instanceof PortableReforgeMenu portable) {
            if(portable.method_7597(player))BackpackOpenAccess.prominence$openBackpack(player,portable.backpackContext());
            else player.method_7346();
            ci.cancel();
        }
    }
}
