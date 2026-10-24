package dev.mihile.skillpresets;

import net.minecraft.nbt.CompoundTag;
import net.puffish.skillsmod.config.CategoryConfig;
import net.puffish.skillsmod.server.data.CategoryData;
import java.util.*;

public final class AllocationValidator {
    private AllocationValidator() { }

    static void applyAllocations(Map<net.minecraft.resources.ResourceLocation, CategoryData> live, Map<String, Set<String>> desired) {
        live.forEach((id, category) -> {
            if (!dev.mihile.skillpresets.protocol.PresetScope.includes(id.toString())) return;
            Set<String> wanted = desired.getOrDefault(id.toString(), Set.of());
            if (category.getUnlockedSkillIds().equals(wanted)) return;
            category.resetSkills(); wanted.forEach(category::unlockSkill);
        });
    }

    public static void validate(CategoryConfig category, CategoryData current, Set<String> desired) {
        long cost = 0;
        for (String id : desired) {
            var skill = category.skills().getById(id).orElseThrow(() -> new IllegalArgumentException("사라진 스킬: " + category.id() + "/" + id));
            var definition = category.definitions().getById(skill.definitionId()).orElseThrow();
            cost += definition.cost();
        }
        int budget = Math.min(current.getPointsTotal(), category.general().spentPointsLimit());
        if (cost > budget) throw new IllegalArgumentException(category.id() + ": 배분에 " + cost + "포인트가 필요하지만 현재 사용 가능 총량은 " + budget + "입니다.");
        // These sets were captured from native server state, never supplied by a client.
        // Do not replay manual purchase prerequisites: SimplySkills can legitimately auto-unlock
        // distant junction nodes with force=true. New purchases still use the original skill UI.
    }

    /** Only allocations may differ. XP, point sources and category visibility must remain shared. */
    static CompoundTag sharedProgress(CategoryData data) {
        CompoundTag nbt = data.writeNbt(new CompoundTag());
        nbt.remove("unlocked_skills");
        return nbt;
    }
}
