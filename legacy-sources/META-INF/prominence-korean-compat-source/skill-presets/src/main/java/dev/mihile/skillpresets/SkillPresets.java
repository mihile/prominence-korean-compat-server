package dev.mihile.skillpresets;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SkillPresets implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("skill_tree_presets");
    @Override public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() != net.fabricmc.api.EnvType.SERVER) return;
        if (!supportedVersion()) {
            if (FabricLoader.getInstance().isModLoaded("puffish_skills"))
                throw new IllegalStateException("Skill Tree Presets requires Pufferfish Skills 0.17.3 and (if present) SimplySkills 1.7.2+1.20.1. "
                        + "Startup stopped before loading the world to preserve saved preset data. Review compatibility before changing these versions.");
            LOGGER.warn("Skill presets disabled: Pufferfish Skills is not installed");
            return;
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> PresetCommands.register(dispatcher));
        PresetNetwork.register();
        LOGGER.info("Three skill allocation presets enabled; native level and point progress remain shared");
    }

    public static boolean supportedVersion() {
        return version("puffish_skills", "0.17.3", false) && version("simplyskills", "1.7.2+1.20.1", true);
    }
    private static boolean version(String id, String expected, boolean optional) {
        return FabricLoader.getInstance().getModContainer(id).map(mod -> mod.getMetadata().getVersion().getFriendlyString().equals(expected)).orElse(optional);
    }
}
