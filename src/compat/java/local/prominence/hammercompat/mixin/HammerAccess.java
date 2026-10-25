/* SPDX-License-Identifier: GPL-3.0-only */
package local.prominence.hammercompat.mixin;
import net.minecraft.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(targets="pro.mikey.justhammers.HammerItem", remap=false)
public interface HammerAccess {
    @Invoker("canDestroy") boolean prominence$canDestroy(class_2680 state, class_1937 world, class_2338 pos);
}
