package dev.mihile.skillpresets;

import java.util.*;

/** Slots deliberately contain allocations only. XP, level and point sources belong to Pufferfish. */
public final class PresetBook {
    public static final int SLOT_COUNT = 3;
    private int active;
    private final List<Map<String, Set<String>>> slots = new ArrayList<>();
    private final List<String> names = new ArrayList<>();

    public PresetBook(Map<String, Set<String>> existing) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            slots.add(Map.of());
            names.add(defaultName(slot));
        }
        capture(existing);
    }

    public int active() { return active; }
    public String name(int slot) { checkSlot(slot); return names.get(slot); }
    public static String defaultName(int slot) { checkSlot(slot); return "프리셋 " + (slot + 1); }
    public Map<String, Set<String>> allocation(int slot) { checkSlot(slot); return slots.get(slot); }
    public void select(int slot) { checkSlot(slot); active = slot; }
    public void capture(Map<String, Set<String>> live) { put(active, live); }

    public void retainCategories(java.util.function.Predicate<String> supported) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            Map<String, Set<String>> copy = new TreeMap<>(slots.get(i));
            copy.keySet().removeIf(id -> !supported.test(id));
            put(i, copy);
        }
    }

    void put(int slot, Map<String, Set<String>> allocation) {
        checkSlot(slot);
        if (allocation.size() > 1024) throw new IllegalArgumentException("스킬 트리가 너무 많습니다.");
        Map<String, Set<String>> copy = new TreeMap<>();
        allocation.forEach((category, skills) -> {
            if (category == null || !category.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || skills == null || skills.size() > 16384)
                throw new IllegalArgumentException("잘못된 스킬 트리 데이터입니다.");
            if (skills.stream().anyMatch(s -> s == null || s.isBlank() || s.length() > 512))
                throw new IllegalArgumentException("잘못된 스킬 ID입니다.");
            if (!skills.isEmpty()) copy.put(category, Set.copyOf(skills));
        });
        slots.set(slot, Collections.unmodifiableMap(copy));
    }

    public void rename(int slot, String name) {
        checkSlot(slot);
        name = name.strip();
        if (name.isBlank() || name.length() > 32 || name.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("이름은 제어 문자가 없는 1~32자여야 합니다.");
        names.set(slot, name);
    }

    /** Explicit native category erasure must not be reversible through an inactive slot. */
    public void eraseCategory(String category) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            Map<String, Set<String>> copy = new TreeMap<>(slots.get(i));
            copy.remove(category);
            put(i, copy);
        }
    }

    public static void checkSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) throw new IllegalArgumentException("프리셋은 1~3번만 사용할 수 있습니다.");
    }
}
