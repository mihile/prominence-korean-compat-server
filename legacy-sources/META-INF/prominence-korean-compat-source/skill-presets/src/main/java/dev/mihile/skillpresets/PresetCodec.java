package dev.mihile.skillpresets;

import net.minecraft.nbt.*;
import java.util.*;

public final class PresetCodec {
    public static final String KEY = "prominence_skill_presets";
    private PresetCodec() { }

    public static CompoundTag write(PresetBook book) {
        CompoundTag root = new CompoundTag();
        root.putInt("version", 1);
        root.putInt("active", book.active());
        ListTag slots = new ListTag();
        for (int i = 0; i < PresetBook.SLOT_COUNT; i++) {
            CompoundTag slot = new CompoundTag();
            slot.putString("name", book.name(i));
            CompoundTag trees = new CompoundTag();
            book.allocation(i).forEach((id, skills) -> {
                ListTag list = new ListTag();
                new TreeSet<>(skills).forEach(skill -> list.add(StringTag.valueOf(skill)));
                trees.put(id, list);
            });
            slot.put("trees", trees);
            slots.add(slot);
        }
        root.put("slots", slots);
        return root;
    }

    public static PresetBook read(Tag raw) {
        if (!(raw instanceof CompoundTag root) || !root.contains("version", Tag.TAG_INT) || root.getInt("version") != 1
                || !root.contains("active", Tag.TAG_INT) || !(root.get("slots") instanceof ListTag slots)
                || slots.size() != PresetBook.SLOT_COUNT || slots.getElementType() != Tag.TAG_COMPOUND)
            throw new IllegalArgumentException("프리셋 저장 형식/버전을 확인하세요. 원본 데이터는 보존됩니다.");
        PresetBook book = new PresetBook(Map.of());
        book.select(root.getInt("active"));
        for (int i = 0; i < PresetBook.SLOT_COUNT; i++) {
            CompoundTag slot = slots.getCompound(i);
            if (!slot.contains("name", Tag.TAG_STRING) || !(slot.get("trees") instanceof CompoundTag trees))
                throw new IllegalArgumentException("프리셋 이름/스킬 트리 데이터가 손상되었습니다.");
            book.rename(i, slot.getString("name"));
            Map<String, Set<String>> allocation = new TreeMap<>();
            for (String id : trees.getAllKeys()) {
                if (!(trees.get(id) instanceof ListTag skills) || (!skills.isEmpty() && skills.getElementType() != Tag.TAG_STRING))
                    throw new IllegalArgumentException("프리셋 스킬 목록이 손상되었습니다.");
                Set<String> nodes = new TreeSet<>();
                for (Tag skill : skills) nodes.add(skill.getAsString());
                allocation.put(id, nodes);
            }
            book.put(i, allocation);
        }
        return book;
    }
}
