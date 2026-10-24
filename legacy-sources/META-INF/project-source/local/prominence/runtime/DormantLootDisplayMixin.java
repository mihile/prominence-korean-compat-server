package local.prominence.runtime;
import fzzyhmstrs.emi_loot.parser.function.SetNbtFunctionParser;
import fzzyhmstrs.emi_loot.parser.LootTableParser;
import fzzyhmstrs.emi_loot.util.TextKey;
import java.util.List;
import net.minecraft.class_117;
import net.minecraft.class_1799;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Apply the context-free dormant transformation only to EMI's display stack. */
@Mixin(value=SetNbtFunctionParser.class,remap=false)
public abstract class DormantLootDisplayMixin {
  @Inject(method="parseFunction",at=@At("HEAD"),cancellable=true)
  private void prominence$describeDormantWeapon(class_117 function,class_1799 stack,
      boolean alternative,List<TextKey> conditions,
      CallbackInfoReturnable<LootTableParser.LootFunctionResult> cir) {
    if(function.getClass().getName().equals("net.sweenus.simplyswords.util.ModLootTableModifiers$DormantAwakeningLootFunction")) {
      // This exact function only calls initializeNaturalDrop(stack); context is unused.
      function.apply(stack,null);
      cir.setReturnValue(new LootTableParser.LootFunctionResult(
        TextKey.of("emi_loot.function.set_nbt"),class_1799.field_8037,conditions));
    }
  }
}
