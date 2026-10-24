package dev.mihile.skillpresets.mixin;

import dev.mihile.skillpresets.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.puffish.skillsmod.SkillsMod;
import net.puffish.skillsmod.config.CategoryConfig;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Optional;

@Mixin(value = SkillsMod.class, remap = false)
public abstract class SkillsModMixin implements SkillsInternals {
    @Shadow private Optional<CategoryConfig> getCategory(ResourceLocation id) { throw new AssertionError(); }
    @Override public Optional<CategoryConfig> presets$category(ResourceLocation id) { return getCategory(id); }

    @Inject(method = "resetSkills", at = @At("TAIL"))
    private void presets$reset(ServerPlayer player, ResourceLocation categoryId, CallbackInfo ci) {
        if (!dev.mihile.skillpresets.protocol.PresetScope.includes(categoryId.toString())) return;
        // Scrolls and native reset commands already preserve XP/points. Capture only the active slot.
        try {
            PresetPlayerData data = PresetController.data(player);
            PresetBook book = data.presets$book();
            book.capture(data.presets$snapshot());
            PresetNetwork.sync(player);
        } catch (RuntimeException e) { SkillPresets.LOGGER.error("Native reset completed; could not update preset feedback", e); }
    }

    @Inject(method = "eraseCategory", at = @At("TAIL"))
    private void presets$erase(ServerPlayer player, ResourceLocation categoryId, CallbackInfo ci) {
        try { PresetController.data(player).presets$book().eraseCategory(categoryId.toString()); }
        catch (RuntimeException e) { SkillPresets.LOGGER.error("Native category erase completed; preset data needs inspection", e); }
    }

    @Inject(target = @Desc(value = "setExperience", args = {ServerPlayer.class, ResourceLocation.class, int.class}), at = @At("TAIL"))
    private void presets$fullProgressReset(ServerPlayer player, ResourceLocation categoryId, int amount, CallbackInfo ci) {
        // Prominent's artifact rebinding explicitly sets XP to zero as well as resetting skills.
        // This is a shared progression wipe, unlike either respec scroll (which never sets XP).
        if (amount != 0 || SkillsMod.getInstance().getExperience(player, categoryId).orElse(-1) != 0) return;
        try { PresetController.data(player).presets$book().eraseCategory(categoryId.toString()); }
        catch (RuntimeException e) { SkillPresets.LOGGER.error("Native XP wipe completed; preset data needs inspection", e); }
    }
}
