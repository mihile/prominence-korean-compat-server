/* SPDX-License-Identifier: MIT */
package local.prominence.arrowtick;
import java.util.UUID;
import net.minecraft.class_2487;
public final class NeruinaUuid {
    private NeruinaUuid() {}
    private static UUID legacy(class_2487 nbt, String name) {
        if (!nbt.method_10573(name, 8)) return null;
        String text = nbt.method_10558(name);
        try {
            UUID value = UUID.fromString(text);
            return value.toString().equalsIgnoreCase(text) ? value : null;
        } catch (IllegalArgumentException ignored) { return null; }
    }
    public static class_2487 read(class_2487 nbt) {
        if (nbt == null) return null;
        if (legacy(nbt,"uuid") == null && legacy(nbt,"entityUuid") == null) return nbt;
        class_2487 copy = nbt.method_10553();
        write(copy);
        return copy;
    }
    public static void write(class_2487 nbt) {
        if (nbt == null) return;
        for (String name : new String[]{"uuid", "entityUuid"}) {
            UUID value = legacy(nbt,name);
            if (value != null) nbt.method_25927(name,value);
        }
    }
}
