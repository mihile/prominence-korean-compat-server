package dev.mihile.tridentback;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class CommandFeedbackTest {
    @Test public void depthReportsPreviousAppliedAndFactoryValues() {
        RescueConfig before = new RescueConfig().edited(c -> c.voidDepth = 12);
        var lines = CommandFeedback.changes(before, before.edited(c -> c.voidDepth = 16), "voidDepth");
        assertEquals(List.of("회수 기준 깊이 (건축 최저 높이 아래): 이전 12블록 → 적용 16블록 (기본: 8블록)"), lines);
        assertEquals(12, before.voidDepth);
    }

    @Test public void booleanSettingsNameTheirFunctionsAndDefaults() {
        RescueConfig before = new RescueConfig();
        assertEquals(List.of("공허 무기 회수 활성화: 이전 true → 적용 false (기본: true)",
                        "삼지창 충성 인챈트 필수: 이전 false → 적용 true (기본: false)"),
                CommandFeedback.changes(before, before.edited(c -> { c.enabled = false; c.requireLoyalty = true; })));
    }

    @Test public void repeatedInputStillReportsSetting() {
        RescueConfig config = new RescueConfig();
        assertEquals(List.of("삼지창 충성 인챈트 필수: 이전 false → 적용 false (기본: false)"),
                CommandFeedback.changes(config, config, "requireLoyalty"));
    }

    @Test public void reloadReportsAllChangedFieldsWithoutUnrelatedSettings() {
        RescueConfig before = new RescueConfig();
        var lines = CommandFeedback.changes(before, before.edited(c -> { c.enabled = false; c.voidDepth = 0; }));
        assertEquals(2, lines.size());
        assertTrue(lines.contains("회수 기준 깊이 (건축 최저 높이 아래): 이전 8블록 → 적용 0블록 (기본: 8블록)"));
        assertFalse(lines.stream().anyMatch(s -> s.contains("충성")));
    }

    @Test public void unchangedReloadShowsCurrentValuesAndFactoryDefaults() {
        RescueConfig config = new RescueConfig().edited(c -> c.voidDepth = 48);
        var lines = CommandFeedback.changes(config, config);
        assertEquals("설정 변경 없음.", lines.get(0));
        assertEquals(CommandFeedback.status(config), lines.subList(1, lines.size()));
        assertTrue(lines.contains("회수 기준 깊이 (건축 최저 높이 아래): 현재 48블록 (기본: 8블록)"));
    }
}
