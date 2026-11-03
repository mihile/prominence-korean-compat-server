package local.prominence.rewardwarmup;

import java.util.Map;
import java.util.WeakHashMap;
import local.prominence.rewards.RewardRegistry;
import local.prominence.rewards.RewardWire;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.LoggerFactory;

/** Move lazy display preparation to a safe tick after all SERVER_STARTED callbacks. */
public final class RewardWarmupInit implements ModInitializer {
    private static final Map<MinecraftServer,Boolean> STARTUP = new WeakHashMap<>();

    private static void request(MinecraftServer server) {
        synchronized (STARTUP) { STARTUP.putIfAbsent(server, Boolean.TRUE); }
    }

    private static void forget(MinecraftServer server) {
        synchronized (STARTUP) { STARTUP.remove(server); }
    }

    private static void tick(MinecraftServer server) {
        if (!server.method_18854()) return;
        synchronized (STARTUP) {
            if (!Boolean.TRUE.equals(STARTUP.get(server))) return;
            STARTUP.put(server, Boolean.FALSE);
        }
        long started = System.nanoTime();
        try {
            var plans = RewardRegistry.snapshot(server.method_3772());
            // Both negotiated schemas use the existing bounded, reload-aware cache.
            RewardWire.payload(plans);
            RewardWire.legacyPayload(plans);
            LoggerFactory.getLogger("Prominence compatibility").info(
                "Reward display metadata prepared at startup: {} pools in {}ms",
                plans.size(), (System.nanoTime() - started) / 1_000_000);
        } catch (RuntimeException e) {
            // Optional display preparation must never reject startup or retry every tick.
            LoggerFactory.getLogger("Prominence compatibility").warn(
                "Startup reward display preparation skipped; normal login fallback remains available", e);
        }
    }

    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(RewardWarmupInit::request);
        ServerTickEvents.END_SERVER_TICK.register(RewardWarmupInit::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(RewardWarmupInit::forget);
    }
}
