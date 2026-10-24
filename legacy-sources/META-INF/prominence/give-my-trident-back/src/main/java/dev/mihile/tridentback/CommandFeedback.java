package dev.mihile.tridentback;

import java.util.*;

final class CommandFeedback {
    private record Setting(String label, Object value, Object defaultValue, String unit) {
        String current() { return label + ": 현재 " + value + unit + " (기본: " + defaultValue + unit + ")"; }
        String change(Setting before) {
            return label + ": 이전 " + before.value + unit + " → 적용 " + value + unit
                    + " (기본: " + defaultValue + unit + ")";
        }
    }

    static List<String> changes(RescueConfig before, RescueConfig after, String... requested) {
        var oldSettings = settings(before);
        Set<String> forced = new HashSet<>(Arrays.asList(requested));
        List<String> lines = new ArrayList<>();
        settings(after).forEach((key, setting) -> {
            Setting old = oldSettings.get(key);
            if (forced.contains(key) || !Objects.equals(old.value, setting.value)) lines.add(setting.change(old));
        });
        if (lines.isEmpty()) {
            lines.add("설정 변경 없음.");
            lines.addAll(status(after));
        }
        return lines;
    }

    static List<String> status(RescueConfig config) {
        return settings(config).values().stream().map(Setting::current).toList();
    }

    private static Map<String, Setting> settings(RescueConfig config) {
        RescueConfig defaults = new RescueConfig();
        Map<String, Setting> values = new LinkedHashMap<>();
        values.put("enabled", new Setting("공허 무기 회수 활성화", config.enabled, defaults.enabled, ""));
        values.put("requireLoyalty", new Setting("삼지창 충성 인챈트 필수", config.requireLoyalty, defaults.requireLoyalty, ""));
        values.put("voidDepth", new Setting("회수 기준 깊이 (건축 최저 높이 아래)", config.voidDepth, defaults.voidDepth, "블록"));
        return values;
    }
}
