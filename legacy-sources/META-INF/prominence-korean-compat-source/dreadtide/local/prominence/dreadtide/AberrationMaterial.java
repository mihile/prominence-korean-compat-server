package local.prominence.dreadtide;

import net.minecraft.class_1832;
import net.minecraft.class_1856;
import net.minecraft.class_1935;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

/** Original 1.56.0 material values; resolve the repair item only when requested. */
public final class AberrationMaterial implements class_1832 {
    public int method_8025() { return 3270; }
    public float method_8027() { return 15.0f; }
    public float method_8028() { return 5.0f; }
    public int method_8024() { return 4; }
    public int method_8026() { return 30; }
    public class_1856 method_8023() {
        return class_1856.method_8091(new class_1935[]{
                class_7923.field_41178.method_10223(new class_2960("eldritch_end", "aberration_heart"))});
    }
}
