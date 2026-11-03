package local.prominence.vitalitybalance;

import net.minecraft.class_1293;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

public final class VitalityLimit {
    private VitalityLimit() {}
    public static boolean restricted(class_2960 id, int amplifier) {
        return amplifier == 2 && id != null && "zenith_attributes".equals(id.method_12836()) && "vitality".equals(id.method_12832());
    }
    public static boolean restricted(class_1293 effect) {
        return effect != null && restricted(class_7923.field_41174.method_10221(effect.method_5579()), effect.method_5578());
    }
}
