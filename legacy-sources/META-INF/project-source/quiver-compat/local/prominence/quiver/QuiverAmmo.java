package local.prominence.quiver;

import java.util.List;
import net.minecraft.*;
import net.mehvahdjukaar.supplementaries.common.items.QuiverItem;
import net.mehvahdjukaar.supplementaries.configs.CommonConfigs;

/** Uses Supplementaries' persisted container API; never shrinks its decoded stack copies. */
public final class QuiverAmmo {
    private QuiverAmmo() {}

    public record Selection(QuiverItem.Data data, int slot, class_1799 arrow) {}

    public static Selection selected(class_1657 player) {
        if (!CommonConfigs.Tools.QUIVER_ENABLED.get()) return null;
        class_1799 weapon = player.method_6047();
        if (!(weapon.method_7909() instanceof class_1811)) return null;
        // Respect vanilla/other mods' loose-arrow and offhand priorities.
        class_1799 projectile = player.method_18808(weapon);
        if (projectile.method_7960() || !(projectile.method_7909() instanceof class_1744)) return null;
        class_1661 inventory = player.method_31548();
        for (int i = 0; i < inventory.method_5439(); i++) {
            if (inventory.method_5438(i) == projectile) return null;
        }
        class_1799 quiver = QuiverItem.findActiveQuiver(player);
        if (quiver.method_7960()) return null;
        QuiverItem.Data data = QuiverItem.getQuiverData(quiver);
        if (data == null) return null;
        List<class_1799> contents = data.getContentView();
        if (contents.isEmpty()) return null;
        int start = Math.floorMod(data.getSelectedSlot(), contents.size());
        for (int i = 0; i < contents.size(); i++) {
            int slot = (start + i) % contents.size();
            class_1799 stack = contents.get(slot);
            if (!stack.method_7960() && class_1799.method_31577(stack, projectile)) {
                class_1799 copy = stack.method_7972();
                copy.method_7939(1);
                return new Selection(data, slot, copy);
            }
        }
        return null;
    }

    public static boolean consume(class_1657 player, Selection selected) {
        if (player.method_37908().field_9236) return false;
        selected.data().setSelectedSlot(selected.slot());
        selected.data().consumeSelected();
        player.method_31548().method_5431();
        // Standard container sync; Trinkets tracks its own stack NBT just as for normal bow shots.
        player.field_7512.method_7623();
        return true;
    }
}
