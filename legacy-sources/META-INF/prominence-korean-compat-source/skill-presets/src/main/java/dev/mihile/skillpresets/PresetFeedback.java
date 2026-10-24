package dev.mihile.skillpresets;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;

final class PresetFeedback {
    private PresetFeedback() { }
    static MutableComponent prefix() { return Component.literal("[스킬] ").withStyle(ChatFormatting.GOLD); }
    static Component selection(PresetBook book, int before) {
        return prefix().append(Component.literal(PresetNames.display(book, before)).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" → ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(PresetNames.display(book, book.active())).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" (기본 1)").withStyle(ChatFormatting.DARK_GRAY));
    }
    static Component renamed(int slot, String before, String after) {
        return prefix().append(Component.literal(before).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" → ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(after).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" (기본 프리셋" + (slot + 1) + ")").withStyle(ChatFormatting.DARK_GRAY));
    }
    static void confirm(ServerPlayer player, Component message) {
        if (!PresetNetwork.hasUi(player)) player.sendSystemMessage(message);
    }
}
