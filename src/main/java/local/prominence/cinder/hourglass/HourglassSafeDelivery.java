package local.prominence.cinder.hourglass;

import elocindev.prominentbase.item.HourglassItem;
import net.minecraft.*;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HourglassSafeDelivery {
    private static final Logger LOGGER = LoggerFactory.getLogger("ProminenceHourglassGuard");

    public enum DeliveryType {
        TIMEOUT,
        VICTORY,
        REFUND,
        CREATION_FAILED
    }

    private HourglassSafeDelivery() {}

    public static boolean deliver(MinecraftServer server, class_3222 player, class_1799 stack, class_2338 fallbackPos, DeliveryType type) {
        if (stack == null || stack.method_7960()) {
            return true;
        }

        int level = HourglassItem.getLevel(stack);
        String levelText = level > 0 ? " (레벨 " + level + ")" : "";

        if (player != null && !player.method_31481()) {
            // 1. Try regular inventory
            boolean inserted = player.method_31548().method_7394(stack);
            if (stack.method_7960()) {
                String msg;
                switch (type) {
                    case TIMEOUT:
                        msg = "§6[불경한 모래시계] §f포탈 제한 시간이 만료되어 모래시계" + levelText + "가 인벤토리로 반환되었습니다.";
                        break;
                    case VICTORY:
                        msg = "§6[불경한 모래시계] §a도전 승리! §f승급된 모래시계" + levelText + "가 인벤토리로 지급되었습니다.";
                        break;
                    case CREATION_FAILED:
                        msg = "§6[불경한 모래시계] §f도전을 생성할 수 없어 소모된 모래시계가 인벤토리로 즉시 반환되었습니다.";
                        break;
                    case REFUND:
                    default:
                        msg = "§6[불경한 모래시계] §f도전이 종료되어 모래시계" + levelText + "가 인벤토리로 반환되었습니다.";
                        break;
                }
                player.method_43496(class_2561.method_43470(msg));
                LOGGER.info("Hourglass safely delivered to inventory of {} ({})", player.method_5477().getString(), type);
                return true;
            }

            // 2. Try Ender Chest if inventory was full
            stack = player.method_7274().method_5491(stack);
            if (stack == null || stack.method_7960()) {
                String msg;
                switch (type) {
                    case TIMEOUT:
                        msg = "§6[불경한 모래시계] §f포탈 제한 시간이 만료되었으나 인벤토리가 가득 차 §e엔더 상자§f로 모래시계" + levelText + "가 반환되었습니다.";
                        break;
                    case VICTORY:
                        msg = "§6[불경한 모래시계] §a도전 승리! §f인벤토리가 가득 차 승급된 모래시계" + levelText + "가 §e엔더 상자§f로 지급되었습니다.";
                        break;
                    case CREATION_FAILED:
                        msg = "§6[불경한 모래시계] §f도전을 생성할 수 없어 소모된 모래시계가 §e엔더 상자§f로 반환되었습니다.";
                        break;
                    case REFUND:
                    default:
                        msg = "§6[불경한 모래시계] §f도전이 종료되었으나 인벤토리가 가득 차 §e엔더 상자§f로 모래시계" + levelText + "가 반환되었습니다.";
                        break;
                }
                player.method_43496(class_2561.method_43470(msg));
                LOGGER.info("Hourglass safely delivered to Ender Chest of {} ({})", player.method_5477().getString(), type);
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
                    player.method_43496(class_2561.method_43470(
                        "§c[불경한 모래시계] §f인벤토리와 엔더 상자가 가득 차 오버월드(" +
                        targetPos.method_10263() + ", " + targetPos.method_10264() + ", " + targetPos.method_10260() +
                        ") 바닥에 모래시계" + levelText + "가 안전하게 보관되었습니다."
                    ));
                }
                LOGGER.warn("Hourglass dropped in Overworld at {} for {} ({})", targetPos, player != null ? player.method_5477().getString() : "offline", type);
                return true;
            }
        }
        return false;
    }
}
