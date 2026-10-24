package dev.mihile.tridentback;

public final class VoidPolicy {
    private VoidPolicy() { }

    public static boolean belowThreshold(double y, int minBuildHeight, int depth) {
        return Double.isFinite(y) && y < (double)minBuildHeight - depth;
    }

    public static boolean mayStart(boolean enabled, WeaponFamily family, boolean realWeapon, boolean knownPlayer,
                                   int count, boolean pickupEnabled, boolean requireLoyalty, int loyalty) {
        return enabled && family != WeaponFamily.NONE && realWeapon && knownPlayer && count == 1 && pickupEnabled
                && (family != WeaponFamily.TRIDENT || !requireLoyalty || loyalty > 0);
    }

    public static boolean safeOwner(boolean online, boolean alive, boolean spectator, double y, int minBuildHeight) {
        return online && alive && !spectator && Double.isFinite(y) && y >= minBuildHeight;
    }

    public static boolean allowPickup(boolean rescuing, java.util.UUID owner, java.util.UUID player) {
        return !rescuing || (owner != null && owner.equals(player));
    }
}
