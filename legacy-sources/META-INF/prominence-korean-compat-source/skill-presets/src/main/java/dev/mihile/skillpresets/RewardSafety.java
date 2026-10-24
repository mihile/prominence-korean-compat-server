package dev.mihile.skillpresets;

import dev.mihile.skillpresets.protocol.PresetScope;
import net.puffish.skillsmod.config.CategoryConfig;
import java.util.Set;

/** Inspect only rewards whose counts actually change in the character skill tree. */
final class RewardSafety {
    private static final Set<String> SAFE = Set.of("puffish_skills:attribute", "puffish_skills:passive_skill", "puffish_skills:dummy");
    private RewardSafety() { }
    static void validate(CategoryConfig config, Set<String> before, Set<String> after) {
        if (!PresetScope.includes(config.id().toString())) return;
        config.definitions().getAll().forEach(definition -> {
            long oldCount = config.skills().getAll().stream().filter(skill -> skill.definitionId().equals(definition.id()) && before.contains(skill.id())).count();
            long newCount = config.skills().getAll().stream().filter(skill -> skill.definitionId().equals(definition.id()) && after.contains(skill.id())).count();
            if (oldCount == newCount) return;
            definition.rewards().forEach(reward -> {
                if (!SAFE.contains(reward.type().toString()))
                    throw new IllegalArgumentException("프리셋 전환 검토가 필요한 보상 유형: " + reward.type());
            });
        });
    }
}
