package local.prominence.mythicrefresh;

import java.lang.ref.WeakReference;
import net.minecraft.server.MinecraftServer;

/** Successful global refreshes are coalesced, never world ticks or item effects. */
public final class RefreshGate {
    private static WeakReference<MinecraftServer> owner = new WeakReference<>(null);
    private static int completedTick;
    private RefreshGate() {}

    public static synchronized boolean shouldRun(MinecraftServer server) {
        if (server == null || !server.method_18854()) return true;
        return owner.get() != server || completedTick != server.method_3780();
    }

    public static synchronized void completed(MinecraftServer server) {
        if (server == null || !server.method_18854()) return;
        if (owner.get() != server) owner = new WeakReference<>(server);
        completedTick = server.method_3780();
    }
}
