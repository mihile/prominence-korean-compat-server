package dev.mihile.skillpresets;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.config.CategoryConfig;
import net.puffish.skillsmod.server.data.CategoryData;
import net.puffish.skillsmod.server.data.ServerData;
import java.util.*;

public final class PresetController {
    private PresetController() { }

    public static PresetPlayerData data(ServerPlayer player) {
        return (PresetPlayerData) ServerData.getOrCreate(player.server).getPlayerData(player);
    }

    public static boolean select(ServerPlayer player, int target) {
        if (!player.isAlive() || player.isSpectator()) throw new IllegalArgumentException("살아 있는 상태에서 프리셋을 전환하세요.");
        PresetPlayerData data = data(player);
        PresetBook book = data.presets$book();
        Map<ResourceLocation, CategoryData> live = data.presets$categories();
        Map<ResourceLocation, CompoundTag> backup = new LinkedHashMap<>();
        live.forEach((id, category) -> backup.put(id, category.writeNbt(new CompoundTag())));
        Map<ResourceLocation, CategoryConfig> configs = new LinkedHashMap<>();
        SkillsInternals internals = (SkillsInternals) SkillsMod.getInstance();
        SkillsAPI.streamCategories().forEach(category -> configs.put(category.getId(), internals.presets$category(category.getId()).orElseThrow()));
        if (configs.isEmpty()) throw new IllegalArgumentException("스킬 트리 설정이 아직 로드되지 않았거나 잘못되었습니다.");

        return PresetSwitch.run(book, data.presets$snapshot(), target, desired -> {
            for (String id : desired.keySet()) {
                ResourceLocation key = new ResourceLocation(id);
                if (!configs.containsKey(key) || !live.containsKey(key))
                    throw new IllegalArgumentException("현재 사용할 수 없는 스킬 트리가 프리셋에 있습니다: " + id);
            }
            live.forEach((id, category) -> {
                if (!dev.mihile.skillpresets.protocol.PresetScope.includes(id.toString())) return;
                Set<String> wanted = desired.getOrDefault(id.toString(), Set.of());
                if (wanted.equals(category.getUnlockedSkillIds())) return;
                CategoryConfig config = configs.get(id);
                if (config == null) throw new IllegalArgumentException("현재 설정에서 사라진 스킬 트리: " + id);
                AllocationValidator.validate(config, category, wanted);
                RewardSafety.validate(config, category.getUnlockedSkillIds(), wanted);
            });
        }, desired -> {
            AllocationValidator.applyAllocations(live, desired);
            // Refresh native attribute rewards and the original client's skill/points packets.
            // No unlock actions, reset API, XP setter or point setter are used for switching.
            SkillsMod.getInstance().updateAllCategories(player);
            for (var entry : backup.entrySet()) {
                CategoryData category = live.get(entry.getKey());
                if (category == null || !AllocationValidator.sharedProgress(CategoryData.read(entry.getValue())).equals(AllocationValidator.sharedProgress(category)))
                    throw new IllegalStateException("경험치/포인트 변경을 감지해 전환을 취소했습니다.");
            }
            syncSimplySkills(player);
        }, () -> {
            live.clear();
            backup.forEach((id, nbt) -> live.put(id, CategoryData.read(nbt)));
            SkillsMod.getInstance().updateAllCategories(player);
            syncSimplySkills(player);
        });
    }

    private static void syncSimplySkills(ServerPlayer player) {
        if (FabricLoader.getInstance().isModLoaded("simplyskills")) SimplyBridge.sync(player);
    }

    private static final class SimplyBridge {
        static void sync(ServerPlayer player) { net.sweenus.simplyskills.network.ModPacketHandler.sendSignatureAbility(player); }
    }
}
