package dev.mihile.skillpresets;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.Tag;
import net.puffish.skillsmod.server.data.CategoryData;
import java.util.*;

/** Implemented on Pufferfish PlayerData, so native progress and presets share one save operation. */
public interface PresetPlayerData {
    PresetBook presets$book();
    void presets$restore(Tag raw);
    Map<ResourceLocation, CategoryData> presets$categories();
    default Map<String, Set<String>> presets$snapshot() {
        Map<String, Set<String>> result = new TreeMap<>();
        presets$categories().forEach((id, data) -> {
            if (dev.mihile.skillpresets.protocol.PresetScope.includes(id.toString()))
                result.put(id.toString(), Set.copyOf(data.getUnlockedSkillIds()));
        });
        return result;
    }
}
