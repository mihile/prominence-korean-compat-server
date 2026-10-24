package dev.mihile.skillpresets.mixin;

import dev.mihile.skillpresets.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.server.data.CategoryData;
import net.puffish.skillsmod.server.data.PlayerData;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;

@Mixin(value = PlayerData.class, remap = false)
public abstract class PlayerDataMixin implements PresetPlayerData {
    @Shadow @Final private Map<ResourceLocation, CategoryData> categories;
    @Unique private PresetBook presets$state;
    @Unique private Tag presets$unreadable;

    @Override public Map<ResourceLocation, CategoryData> presets$categories() { return categories; }
    @Override public PresetBook presets$book() {
        if (presets$unreadable != null) throw new IllegalStateException("프리셋 데이터 형식 오류: 저장된 원본을 보존하고 전환을 중단했습니다.");
        if (presets$state == null) presets$state = new PresetBook(presets$snapshot());
        return presets$state;
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void presets$read(CompoundTag nbt, CallbackInfoReturnable<PlayerData> cir) {
        if (!nbt.contains(PresetCodec.KEY)) return;
        ((PresetPlayerData) cir.getReturnValue()).presets$restore(nbt.get(PresetCodec.KEY));
    }

    @Override public void presets$restore(Tag raw) {
        try {
            presets$state = PresetCodec.read(raw);
            presets$state.retainCategories(dev.mihile.skillpresets.protocol.PresetScope::includes);
        }
        catch (RuntimeException e) {
            presets$unreadable = raw.copy();
            SkillPresets.LOGGER.error("Preserving unreadable skill preset data; preset commands disabled for this player", e);
        }
    }

    @Inject(method = "writeNbt", at = @At("RETURN"))
    private void presets$write(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
        if (presets$unreadable != null) cir.getReturnValue().put(PresetCodec.KEY, presets$unreadable.copy());
        else {
            PresetBook book = presets$book();
            book.capture(presets$snapshot());
            cir.getReturnValue().put(PresetCodec.KEY, PresetCodec.write(book));
        }
    }
}
