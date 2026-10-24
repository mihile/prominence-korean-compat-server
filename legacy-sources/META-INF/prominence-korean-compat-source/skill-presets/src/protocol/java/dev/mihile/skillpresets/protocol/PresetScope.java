package dev.mihile.skillpresets.protocol;

/** Only the character skill tree has builds. Artifact upgrades remain native shared progress. */
public final class PresetScope {
    private PresetScope() { }
    public static boolean includes(String category) { return "puffish_skills:prom".equals(category); }
}
