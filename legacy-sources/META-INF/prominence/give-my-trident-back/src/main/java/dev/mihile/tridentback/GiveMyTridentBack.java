package dev.mihile.tridentback;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class GiveMyTridentBack implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("give_my_trident_back");
    public static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("give-my-trident-back.json");
    private static RescueConfig config = new RescueConfig();
    public static RescueConfig config() { return config; }

    @Override public void onInitialize() {
        try {
            if (Files.exists(CONFIG_PATH)) config = RescueConfig.read(CONFIG_PATH);
            else config.write(CONFIG_PATH);
        } catch (IOException e) {
            config.enabled = false;
            LOGGER.error("Invalid configuration: new rescues disabled; fix the file and /gmtb reload", e);
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> RescueCommands.register(dispatcher));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> RescueController.resetStatistics());
        LOGGER.info("give my trident back enabled={}, voidDepth={}, requireLoyalty={}", config.enabled, config.voidDepth, config.requireLoyalty);
    }

    public static void update(Consumer<RescueConfig> edit) throws IOException {
        RescueConfig next = config.edited(edit);
        next.write(CONFIG_PATH); config = next;
    }
    public static void reload() throws IOException { config = RescueConfig.read(CONFIG_PATH); }
}
