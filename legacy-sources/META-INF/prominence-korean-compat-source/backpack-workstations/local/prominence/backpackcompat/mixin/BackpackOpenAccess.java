package local.prominence.backpackcompat.mixin;
import net.minecraft.class_3222;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value=BackpackOpenMessage.class,remap=false)
public interface BackpackOpenAccess {
    @Invoker("openBackpack") static void prominence$openBackpack(class_3222 player,BackpackContext context){throw new AssertionError();}
}
