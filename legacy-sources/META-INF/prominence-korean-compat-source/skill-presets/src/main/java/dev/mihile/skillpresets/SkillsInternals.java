package dev.mihile.skillpresets;

import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.config.CategoryConfig;
import java.util.Optional;

public interface SkillsInternals {
    Optional<CategoryConfig> presets$category(ResourceLocation id);
}
