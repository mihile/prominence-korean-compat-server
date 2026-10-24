package local.prominence.serverupdate;

import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Does no networking. Confirms this build only after real startup and 60 seconds of server ticking. */
public final class ServerHealth implements DedicatedServerModInitializer {
    private long started;
    private boolean written;
    @Override public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> started = System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (written || started == 0 || System.nanoTime() - started < TimeUnit.SECONDS.toNanos(60)) return;
            written = true;
            try {
                Properties p;
                try (var in = ServerHealth.class.getResourceAsStream("/" + Release.IDENTITY)) {
                    if (in == null) throw new java.io.IOException("Missing running server build");
                    p = FilesEx.properties(in.readAllBytes());
                }
                Path root = FabricLoader.getInstance().getGameDir();
                UpdateEngine.markHealthy(root, System.getenv("PROMINENCE_SERVER_UPDATE_TOKEN"), Long.parseLong(p.getProperty("build")));
                System.out.println("[Server patch] Server compatibility build " + p.getProperty("build") + " startup confirmed.");
            } catch (Exception e) {
                System.err.println("[Server patch] Could not confirm update health; next start will recover: " + e);
            }
        });
    }
}
