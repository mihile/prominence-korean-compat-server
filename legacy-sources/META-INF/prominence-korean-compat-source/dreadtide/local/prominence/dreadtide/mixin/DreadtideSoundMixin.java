package local.prominence.dreadtide.mixin;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.sweenus.simplyswords.item.custom.DreadtideSwordItem", remap = false)
public abstract class DreadtideSoundMixin {
    // Corruption from equipment can exceed the seven sound variations in the original ability.
    @Redirect(method = "method_7836(Lnet/minecraft/class_1937;Lnet/minecraft/class_1657;Lnet/minecraft/class_1268;)Lnet/minecraft/class_1271;",
            at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"), require = 2)
    private Object prominence$boundSoundVariation(List<?> sounds, int index) {
        return sounds.get(Math.max(0, Math.min(index, sounds.size() - 1)));
    }
}
