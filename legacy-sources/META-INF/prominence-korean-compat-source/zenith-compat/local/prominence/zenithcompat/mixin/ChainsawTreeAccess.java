package local.prominence.zenithcompat.mixin;

import dev.shadowsoffire.apotheosis.ench.enchantments.masterwork.ChainsawEnchant;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ChainsawEnchant.class, remap = false)
public interface ChainsawTreeAccess {
    @Invoker("isTree") boolean prominence$isChainsawTree(class_1937 world, class_2338 pos, class_2680 state);
}
