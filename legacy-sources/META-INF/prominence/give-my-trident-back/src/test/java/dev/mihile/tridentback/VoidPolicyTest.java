package dev.mihile.tridentback;

import org.junit.Test;
import static org.junit.Assert.*;

public class VoidPolicyTest {
    private boolean start(WeaponFamily family, boolean real, boolean owner, int count, boolean pickup, boolean require, int loyalty) {
        return VoidPolicy.mayStart(true, family, real, owner, count, pickup, require, loyalty);
    }

    @Test public void endThresholdIsBeforeVanillaVoidRemoval() {
        assertFalse(VoidPolicy.belowThreshold(-8, 0, 8));
        assertTrue(VoidPolicy.belowThreshold(-8.001, 0, 8));
        assertTrue(VoidPolicy.belowThreshold(-65, 0, 8));
    }
    @Test public void overworldThresholdUsesItsOwnBuildHeight() {
        assertFalse(VoidPolicy.belowThreshold(-70, -64, 8));
        assertTrue(VoidPolicy.belowThreshold(-73, -64, 8));
    }
    @Test public void unusualWorldHeightsAndHighSpeedOvershootStillTrigger() {
        assertTrue(VoidPolicy.belowThreshold(-5000, -128, 48));
        assertTrue(VoidPolicy.belowThreshold(90, 100, 8));
        assertFalse(VoidPolicy.belowThreshold(99, 100, 8));
    }
    @Test public void invalidCoordinatesNeverTriggerTeleport() {
        assertFalse(VoidPolicy.belowThreshold(Double.NaN, 0, 8));
        assertFalse(VoidPolicy.belowThreshold(Double.NEGATIVE_INFINITY, 0, 8));
    }
    @Test public void normalPlayerThrownTridentIsProtectedWithoutLoyaltyByDefault() {
        assertTrue(start(WeaponFamily.TRIDENT, true, true, 1, true, false, 0));
    }
    @Test public void optionalLoyaltyRequirementOnlyAppliesToTridents() {
        assertFalse(start(WeaponFamily.TRIDENT, true, true, 1, true, true, 0));
        assertTrue(start(WeaponFamily.TRIDENT, true, true, 1, true, true, 1));
        assertTrue(start(WeaponFamily.SIMPLY_SWORDS, true, true, 1, true, true, 0));
        assertTrue(start(WeaponFamily.SOULS_WEAPONRY, true, true, 1, true, true, 0));
    }
    @Test public void syntheticAttacksAndUnknownOwnersCannotProduceItems() {
        assertFalse(start(WeaponFamily.SIMPLY_SWORDS, false, true, 1, true, false, 3));
        assertFalse(start(WeaponFamily.TRIDENT, true, false, 1, true, false, 3));
        assertFalse(start(WeaponFamily.NONE, true, true, 1, true, false, 3));
    }
    @Test public void emptyOrStackedProjectileItemsAreNotRefunded() {
        assertFalse(start(WeaponFamily.TRIDENT, true, true, 0, true, false, 3));
        assertFalse(start(WeaponFamily.TRIDENT, true, true, 2, true, false, 3));
        assertFalse(start(WeaponFamily.TRIDENT, true, true, 1, false, false, 3));
    }
    @Test public void disablingStopsNewRescues() {
        assertFalse(VoidPolicy.mayStart(false, WeaponFamily.TRIDENT, true, true, 1, true, false, 3));
    }
    @Test public void offlineDeadAndSpectatorOwnersMustWait() {
        assertFalse(VoidPolicy.safeOwner(false, true, false, 70, 0));
        assertFalse(VoidPolicy.safeOwner(true, false, false, 70, 0));
        assertFalse(VoidPolicy.safeOwner(true, true, true, 70, 0));
    }
    @Test public void fallingOwnerCannotPullWeaponBackIntoVoid() {
        assertFalse(VoidPolicy.safeOwner(true, true, false, -1, 0));
        assertFalse(VoidPolicy.safeOwner(true, true, false, Double.NaN, 0));
        assertTrue(VoidPolicy.safeOwner(true, true, false, 0, 0));
        assertTrue(VoidPolicy.safeOwner(true, true, false, 80, -64));
    }
    @Test public void protectedWeaponCannotBePickedUpByAnotherPlayerWhileInventoryIsFull() {
        var owner = java.util.UUID.randomUUID(); var stranger = java.util.UUID.randomUUID();
        assertTrue(VoidPolicy.allowPickup(true, owner, owner));
        assertFalse(VoidPolicy.allowPickup(true, owner, stranger));
        assertFalse(VoidPolicy.allowPickup(true, null, stranger));
        assertTrue(VoidPolicy.allowPickup(false, owner, stranger));
    }
}
