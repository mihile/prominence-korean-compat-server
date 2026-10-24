package local.prominence.zenithcompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.shadowsoffire.apotheosis.ench.Ench;
import dev.shadowsoffire.apotheosis.Apotheosis;
import net.minecraft.class_1799;
import net.minecraft.class_1890;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_3218;
import net.minecraft.class_3222;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "ht.treechop.common.FabricCommon", remap = false)
public abstract class TreeChopChainsawMixin {
    @WrapOperation(method = "onBreakEvent(Lnet/minecraft/class_1937;Lnet/minecraft/class_1657;Lnet/minecraft/class_2338;Lnet/minecraft/class_2680;Lnet/minecraft/class_2586;)Z",
        at = @At(value = "INVOKE", target = "Lht/treechop/common/chop/ChopUtil;chop(Lnet/minecraft/class_3222;Lnet/minecraft/class_3218;Lnet/minecraft/class_2338;Lnet/minecraft/class_2680;Lnet/minecraft/class_1799;Ljava/lang/Object;)Z"), require = 1)
    private static boolean prominence$letChainsawBreakLog(class_3222 player, class_3218 world, class_2338 pos,
            class_2680 state, class_1799 tool, Object trigger, Operation<Boolean> original) {
        if (Apotheosis.enableEnch && player.getClass() == class_3222.class && !player.method_31549().field_7477
                && class_1890.method_8225(Ench.Enchantments.CHAINSAW, tool) > 0
                && ((ChainsawTreeAccess)Ench.Enchantments.CHAINSAW).prominence$isChainsawTree(world, pos, state)) {
            // False means TreeChop did not intercept; Fabric BEFORE continues normally.
            return false;
        }
        return original.call(player, world, pos, state, tool, trigger);
    }
}
