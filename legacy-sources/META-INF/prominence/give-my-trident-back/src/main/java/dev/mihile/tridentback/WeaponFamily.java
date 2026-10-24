package dev.mihile.tridentback;

public enum WeaponFamily {
    NONE, TRIDENT, SIMPLY_SWORDS, SOULS_WEAPONRY;

    public static WeaponFamily fromModHierarchy(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            WeaponFamily family = fromClassName(current.getName());
            if (family != NONE) return family;
        }
        return NONE;
    }

    public static WeaponFamily fromClassName(String name) {
        return switch (name) {
            case "net.sweenus.simplyswords.entity.ThrownSwordEntity" -> SIMPLY_SWORDS;
            case "net.soulsweaponry.entity.projectile.ReturningProjectile" -> SOULS_WEAPONRY;
            default -> NONE;
        };
    }
}
