package dev.mihile.skillpresets.protocol;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Optional play channels. Neither side sends to a peer which has not advertised support. */
public final class PresetProtocol {
    public static final ResourceLocation REQUEST = new ResourceLocation("skill_tree_presets", "request_v1");
    public static final ResourceLocation SELECT = new ResourceLocation("skill_tree_presets", "select_v1");
    public static final ResourceLocation STATE = new ResourceLocation("skill_tree_presets", "state_v1");
    public static final ResourceLocation RENAME = new ResourceLocation("skill_tree_presets", "rename_v1");
    public record Rename(int slot, String name) {
        public Rename {
            if (slot < 0 || slot > 2 || name == null) throw new IllegalArgumentException("Invalid preset name");
            name = name.strip();
            if (name.isEmpty() || name.length() > 32 || name.codePoints().anyMatch(Character::isISOControl))
                throw new IllegalArgumentException("이름은 제어 문자가 없는 1~32자여야 합니다.");
        }
    }
    public record State(int active, List<String> names, String error) {
        public State {
            if (active < 0 || active > 2 || names.size() != 3 || names.stream().anyMatch(s -> s == null || s.length() > 32)
                    || error == null || error.length() > 512) throw new IllegalArgumentException("Invalid preset state");
            names = List.copyOf(names);
        }
    }
    private PresetProtocol() { }
    public static void write(FriendlyByteBuf buffer, State state) {
        buffer.writeVarInt(state.active());
        state.names().forEach(name -> buffer.writeUtf(name, 32));
        buffer.writeUtf(state.error(), 512);
    }
    public static State read(FriendlyByteBuf buffer) {
        State state = new State(buffer.readVarInt(), List.of(buffer.readUtf(32), buffer.readUtf(32), buffer.readUtf(32)), buffer.readUtf(512));
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing preset packet data");
        return state;
    }
    public static int readSelection(FriendlyByteBuf buffer) {
        int slot = buffer.readVarInt();
        if (slot < 0 || slot > 2 || buffer.isReadable()) throw new IllegalArgumentException("Invalid preset selection");
        return slot;
    }
    public static void writeRename(FriendlyByteBuf buffer, Rename rename) {
        buffer.writeVarInt(rename.slot()); buffer.writeUtf(rename.name(), 32);
    }
    public static Rename readRename(FriendlyByteBuf buffer) {
        Rename rename = new Rename(buffer.readVarInt(), buffer.readUtf(32));
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing rename packet data");
        return rename;
    }
}
