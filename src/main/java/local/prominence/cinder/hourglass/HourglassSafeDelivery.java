package local.prominence.cinder.hourglass;

import net.minecraft.*;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HourglassSafeDelivery {
    private static final Logger LOGGER = LoggerFactory.getLogger("ProminenceHourglassGuard");

    private HourglassSafeDelivery() {}

    public static boolean deliver(MinecraftServer server, class_3222 player, class_1799 stack, class_2338 fallbackPos, String reason) {
        if (stack == null || stack.method_7960()) {
            return true;
        }

        if (player != null && !player.method_31481()) {
            // 1. Try regular inventory
            boolean inserted = player.method_31548().method_7394(stack);
            if (stack.method_7960()) {
                player.method_43496(class_2561.method_43470("§a[모래시계] §f" + reason + " (인벤토리 지급 완료)"));
                LOGGER.info("Hourglass safely delivered to inventory of {} ({})", player.method_5477().getString(), reason);
                return true;
            }

            // 2. Try Ender Chest if inventory was full
            stack = player.method_7274().method_5491(stack);
            if (stack == null || stack.method_7960()) {
                player.method_43496(class_2561.method_43470("§e[모래시계] §f인벤토리가 가득 차 §6엔더 상자§f로 안전하게 지급되었습니다. (" + reason + ")"));
                LOGGER.info("Hourglass safely delivered to Ender Chest of {} ({})", player.method_5477().getString(), reason);
                return true;
            }
        }

        // 3. Fallback: Spawn in Overworld (NEVER in temporary arena dimension!)
        if (server != null) {
            class_3218 overworld = server.method_30002();
            if (overworld != null) {
                class_2338 targetPos = fallbackPos;
                if (player != null && player.method_37908().method_27983().equals(class_1937.field_25179)) {
                    targetPos = player.method_24515();
                }
                if (targetPos == null) {
                    targetPos = overworld.method_43126();
                }

                class_1542 entity = new class_1542(overworld, targetPos.method_10263() + 0.5, targetPos.method_10264() + 0.5, targetPos.method_10260() + 0.5, stack.method_7972());
                entity.method_6975(); // resetPickupDelay()
                if (player != null) {
                    entity.method_48349(player.method_5667()); // setOwner(UUID)
                }
                entity.method_6976(); // setCovetedItem()
                overworld.method_8649(entity); // spawnEntity()

                if (player != null) {
                    player.method_43496(class_2561.method_43470("§c[모래시계] §f인벤토리/엔더상자가 가득 차 오버월드(" + targetPos.method_10263() + ", " + targetPos.method_10264() + ", " + targetPos.method_10260() + ") 바닥에 생성되었습니다. (" + reason + ")"));
                }
                LOGGER.warn("Hourglass dropped in Overworld at {} for {} ({})", targetPos, player != null ? player.method_5477().getString() : "offline", reason);
                return true;
            }
        }
        return false;
    }
}
