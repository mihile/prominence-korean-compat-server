package dev.mihile.skillpresets;

import java.util.*;

final class PresetNames {
    record Rename(int slot, String name) { }
    private PresetNames() { }
    static String display(PresetBook book, int slot) {
        String name = book.name(slot);
        return name.equals("프리셋 " + (slot + 1)) ? "프리셋" + (slot + 1) : name;
    }
    static int resolve(PresetBook book, String input) {
        String name = input.strip();
        if (name.matches("[1-3]")) return Integer.parseInt(name) - 1;
        int found = -1;
        for (int slot = 0; slot < 3; slot++) {
            if (!display(book, slot).equalsIgnoreCase(name)) continue;
            if (found >= 0) throw new IllegalArgumentException("이름이 중복됩니다. 1~3 번호로 선택하세요.");
            found = slot;
        }
        if (found >= 0) return found;
        throw new IllegalArgumentException("없는 프리셋: " + name);
    }
    static List<String> suggestions(PresetBook book, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return java.util.stream.IntStream.range(0, 3).mapToObj(slot -> display(book, slot)).distinct()
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }

    static Rename parseRename(PresetBook book, String input) {
        String text = input.strip();
        String target, name;
        if (text.startsWith("\"")) {
            var reader = new com.mojang.brigadier.StringReader(text);
            try { target = reader.readQuotedString(); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException invalid) { throw new IllegalArgumentException("대상 이름의 따옴표를 확인하세요."); }
            if (!reader.canRead() || !Character.isWhitespace(reader.peek())) throw renameUsage();
            name = reader.getRemaining().strip();
        } else {
            // Stable slot selectors remain usable even after the display name has changed.
            var fixed = java.util.regex.Pattern.compile("^(프리셋[1-3]|[1-3])\\s+(.+)$", java.util.regex.Pattern.DOTALL).matcher(text);
            if (fixed.matches()) return checkedRename(Integer.parseInt(fixed.group(1).substring(fixed.group(1).length() - 1)) - 1, fixed.group(2));
            target = java.util.stream.IntStream.range(0, 3).mapToObj(slot -> display(book, slot))
                    .filter(candidate -> text.length() > candidate.length() && text.regionMatches(true, 0, candidate, 0, candidate.length())
                            && Character.isWhitespace(text.charAt(candidate.length())))
                    .max(Comparator.comparingInt(String::length)).orElseThrow(PresetNames::renameUsage);
            name = text.substring(target.length()).strip();
        }
        int slot = target.matches("프리셋[1-3]") ? target.charAt(target.length() - 1) - '1' : resolve(book, target);
        return checkedRename(slot, name);
    }
    private static Rename checkedRename(int slot, String name) {
        name = name.strip();
        if (name.startsWith("\"")) {
            var reader = new com.mojang.brigadier.StringReader(name);
            try { name = reader.readQuotedString(); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException invalid) { throw new IllegalArgumentException("새 이름의 따옴표를 확인하세요."); }
            if (!reader.getRemaining().isBlank()) throw renameUsage();
        }
        var validated = new dev.mihile.skillpresets.protocol.PresetProtocol.Rename(slot, name);
        return new Rename(validated.slot(), validated.name());
    }
    private static IllegalArgumentException renameUsage() { return new IllegalArgumentException("이름변경 <프리셋> <새 이름>"); }
    static List<String> renameTargets(PresetBook book, String input) {
        Set<String> targets = new LinkedHashSet<>();
        for (int slot = 0; slot < 3; slot++) {
            String name = display(book, slot);
            targets.add(name);
        }
        String prefix = input.toLowerCase(Locale.ROOT);
        return targets.stream().filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)
                        || com.mojang.brigadier.arguments.StringArgumentType.escapeIfRequired(name).toLowerCase(Locale.ROOT).startsWith(prefix))
                .map(name -> name.codePoints().anyMatch(Character::isWhitespace) || name.contains("\"")
                        ? com.mojang.brigadier.arguments.StringArgumentType.escapeIfRequired(name) : name).distinct().toList();
    }
}
