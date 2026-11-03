package local.prominence.trinketslot;

import java.util.regex.Pattern;

/** Reuses immutable parsing machinery, never equipment or slot lookup results. */
public final class SlotNameNormalizer {
    private static final String PREFIX = "trinket_group_";
    private static final Pattern GROUP_INFO = Pattern.compile("(trinket_group_).*-");

    private SlotNameNormalizer() {}

    public static String normalize(String name) {
        if (name.indexOf(PREFIX) < 0) return name;
        return GROUP_INFO.matcher(name).replaceAll("");
    }
}
