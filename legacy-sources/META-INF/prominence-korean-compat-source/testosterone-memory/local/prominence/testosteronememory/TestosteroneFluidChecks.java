/* SPDX-License-Identifier: GPL-3.0-only
 * Targeted repair of Create: Testosterone 2.0.3's fluidEffectHandler.
 * Original mod authors: mifort, JonMyDude. */
package local.prominence.testosteronememory;

import net.mifort.testosterone.effects.testosteroneModEffects;
import net.mifort.testosterone.fluids.testosteroneFluids;
import net.minecraft.class_1293;
import net.minecraft.class_1309;
import net.minecraft.class_2338;
import net.minecraft.class_2791;
import net.minecraft.class_2806;
import net.minecraft.class_3218;

public final class TestosteroneFluidChecks {
    private TestosteroneFluidChecks() {}

    public static void apply(class_1309 entity) {
        if (!(entity.method_37908() instanceof class_3218 world)) return;
        class_2338 pos = entity.method_24515();
        if (!world.method_37118(pos)) return;
        class_2791 chunk = world.method_8402(pos.method_10263() >> 4,
                pos.method_10260() >> 4, class_2806.field_12803, false);
        if (chunk == null) return;
        var fluid = chunk.method_8316(pos).method_15772();
        if (fluid == testosteroneFluids.TESTOSTERONE_FLUID.get()
                || fluid == testosteroneFluids.TESTOSTERONE_FLUID.getSource()) {
            entity.method_6092(new class_1293(testosteroneModEffects.TESTOSTERONE_EFFECT, 20, 0));
        }
        if (fluid == testosteroneFluids.TRENBOLONE_FLUID.get()
                || fluid == testosteroneFluids.TRENBOLONE_FLUID.getSource()) {
            entity.method_6092(new class_1293(testosteroneModEffects.ROID_RAGE_EFFECT, 20, 0));
        }
    }
}
