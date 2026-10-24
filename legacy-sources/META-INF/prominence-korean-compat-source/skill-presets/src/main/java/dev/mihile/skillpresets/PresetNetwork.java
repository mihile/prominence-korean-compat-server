package dev.mihile.skillpresets;

import dev.mihile.skillpresets.protocol.PresetProtocol;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class PresetNetwork {
    private static final Map<UUID, Long> REQUESTS = new HashMap<>(), SELECTS = new HashMap<>(), RENAMES = new HashMap<>();
    private PresetNetwork() { }
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(PresetProtocol.REQUEST, (server, player, handler, buffer, sender) -> {
            if (buffer.isReadable()) return;
            server.execute(() -> { if (allow(REQUESTS, player, 5)) sync(player); });
        });
        ServerPlayNetworking.registerGlobalReceiver(PresetProtocol.SELECT, (server, player, handler, buffer, sender) -> {
            final int slot;
            try { slot = PresetProtocol.readSelection(buffer); }
            catch (RuntimeException invalid) { return; }
            server.execute(() -> {
                if (!allow(SELECTS, player, 10)) { send(player, "연속 전환은 잠시 기다린 후 다시 시도하세요."); return; }
                try {
                    PresetBook book = PresetController.data(player).presets$book();
                    PresetController.select(player, slot);
                    sync(player);
                } catch (RuntimeException failure) {
                    SkillPresets.LOGGER.warn("Preset UI switch failed for {}", player.getUUID(), failure);
                    send(player, failure.getMessage() == null ? "프리셋 전환에 실패했습니다." : failure.getMessage());
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(PresetProtocol.RENAME, (server, player, handler, buffer, sender) -> {
            final PresetProtocol.Rename rename;
            try { rename = PresetProtocol.readRename(buffer); }
            catch (RuntimeException invalid) { return; }
            server.execute(() -> {
                if (!allow(RENAMES, player, 5)) { send(player, "이름 변경은 잠시 기다린 후 다시 시도하세요."); return; }
                try {
                    PresetBook book = PresetController.data(player).presets$book();
                    if (book.active() != rename.slot()) throw new IllegalArgumentException("선택된 프리셋이 바뀌었습니다. 이름 변경 창을 다시 열어 주세요.");
                    book.rename(book.active(), rename.name());
                    sync(player);
                } catch (RuntimeException failure) { send(player, failure.getMessage() == null ? "이름 변경에 실패했습니다." : failure.getMessage()); }
            });
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            REQUESTS.remove(handler.player.getUUID()); SELECTS.remove(handler.player.getUUID()); RENAMES.remove(handler.player.getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { REQUESTS.clear(); SELECTS.clear(); RENAMES.clear(); });
    }
    private static boolean allow(Map<UUID, Long> limits, ServerPlayer player, int ticks) {
        if (player.hasDisconnected()) return false;
        long now = player.server.overworld().getGameTime();
        Long before = limits.get(player.getUUID());
        if (before != null && now - before < ticks) return false;
        limits.put(player.getUUID(), now); return true;
    }
    public static void sync(ServerPlayer player) { send(player, ""); }
    public static boolean hasUi(ServerPlayer player) { return ServerPlayNetworking.canSend(player, PresetProtocol.STATE); }
    public static void error(ServerPlayer player, String error) { send(player, error); }
    private static void send(ServerPlayer player, String error) {
        if (!ServerPlayNetworking.canSend(player, PresetProtocol.STATE)) return;
        try {
            PresetBook book = PresetController.data(player).presets$book();
            var buffer = PacketByteBufs.create();
            PresetProtocol.write(buffer, new PresetProtocol.State(book.active(), List.of(book.name(0), book.name(1), book.name(2)),
                    error.length() > 512 ? error.substring(0, 512) : error));
            ServerPlayNetworking.send(player, PresetProtocol.STATE, buffer);
        } catch (RuntimeException failure) {
            SkillPresets.LOGGER.warn("Could not sync preset UI for {}", player.getUUID(), failure);
        }
    }
}
