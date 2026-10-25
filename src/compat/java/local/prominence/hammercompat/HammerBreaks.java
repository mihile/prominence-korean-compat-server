/* SPDX-License-Identifier: GPL-3.0-only
 * Compatibility with Just Hammers by ErrorMikey. Original mod remains unchanged. */
package local.prominence.hammercompat;

import java.util.UUID;
import net.minecraft.*;
import pro.mikey.justhammers.Config;
import pro.mikey.justhammers.HammerItem;
import local.prominence.hammercompat.mixin.HammerAccess;

/** Defer the area until the source break succeeds; use the normal player break pipeline. */
public final class HammerBreaks {
    private record Pending(UUID player, class_2338 origin, class_2350 face, class_1799 stack) {}
    // Pending data deliberately contains no player/world references, even on an exceptional return.
    private static final ThreadLocal<Pending> PENDING = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();
    private HammerBreaks() {}
    public static boolean active() { return Boolean.TRUE.equals(ACTIVE.get()); }
    public static void begin() { if (!active()) PENDING.remove(); }

    public static void capture(class_1937 world, class_2338 pos, class_2680 state,
                               class_1799 stack, class_1309 entity) {
        if (active() || !(entity instanceof class_3222 player) || world.field_9236
                || state.method_26214(world, pos) == 0.0f || entity.method_18276()) return;
        if (entity.method_5745(20.0, 0.0f, false) instanceof class_3965 hit) {
            PENDING.set(new Pending(player.method_5667(), pos.method_10062(), hit.method_17780(), stack));
        }
    }

    public static void finish(class_3222 player, class_2338 origin, boolean success) {
        if (active()) return;
        Pending request = PENDING.get();
        PENDING.remove();
        if (!success || request == null || !request.player.equals(player.method_5667())
                || !request.origin.equals(origin) || player.method_6047() != request.stack
                || request.stack.method_7960() || !(request.stack.method_7909() instanceof HammerItem hammer)) return;
        ACTIVE.set(true);
        try {
            class_3218 world = player.method_51469();
            var bounds = HammerItem.getAreaOfEffect(origin, request.face, hammer.getRadius(), hammer.getDepth());
            var positions = class_2338.method_23627(bounds).iterator();
            while (positions.hasNext()) {
                // The iterator reuses a mutable BlockPos. Never retain or compare it by identity.
                class_2338 pos = positions.next().method_10062();
                if (pos.equals(origin)) continue;
                if (player.method_51469() != world || player.method_6047() != request.stack
                        || request.stack.method_7960()) break;
                if (!player.method_7337() && request.stack.method_7919() + 1 >= request.stack.method_7936()
                        - (Config.INSTANCE.config.allowBreaking() ? 0 : 1)) break;
                // Area mining must never load a new chunk solely to inspect a neighbor.
                if (world.method_8402(pos.method_10263() >> 4, pos.method_10260() >> 4,
                        class_2806.field_12803, false) == null) continue;
                class_2680 state = world.method_8320(pos);
                if (!((HammerAccess) hammer).prominence$canDestroy(state, world, pos)
                        || !hammer.actualIsCorrectToolForDrops(state)) continue;
                player.field_13974.method_14266(pos);
            }
            if ((double) request.stack.method_7919() >= (double) request.stack.method_7936() * 0.95) {
                if (!request.stack.method_7948().method_10545("has_been_warned")) {
                    player.method_43502(class_2561.method_43471("justhammers.tooltip.durability_warning")
                            .method_27692(class_124.field_1061), true);
                    request.stack.method_7948().method_10556("has_been_warned", true);
                }
            } else if (request.stack.method_7948().method_10545("has_been_warned")) {
                request.stack.method_7948().method_10551("has_been_warned");
            }
        } finally {
            ACTIVE.remove();
            PENDING.remove();
        }
    }
}
