package dev.mihile.tridentback;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import java.util.UUID;
import static org.junit.Assert.*;

public class RescueDataTest {
    @Test public void pendingOwnerAndStateSurviveSaveAndCrossDimensionCopy() {
        var data = new RescueData(); data.recordPlayer(UUID.randomUUID(), false); data.rescuing = true;
        var tag = new CompoundTag(); data.write(tag);
        var restored = new RescueData(); restored.read(tag.copy());
        assertEquals(data.playerOwner, restored.playerOwner);
        assertTrue(restored.rescuing);
        assertFalse(restored.creativeThrow);
    }
    @Test public void creativeOriginSurvivesGameModeChangeAndReload() {
        var data = new RescueData(); UUID player = UUID.randomUUID();
        data.recordPlayer(player, true); data.recordPlayer(player, false);
        assertTrue(data.creativeThrow);
        var tag = new CompoundTag(); data.write(tag);
        var restored = new RescueData(); restored.read(tag);
        assertTrue(restored.creativeThrow);
    }
    @Test public void survivalOriginRemainsRealAfterChangingToCreative() {
        var data = new RescueData(); UUID player = UUID.randomUUID();
        data.recordPlayer(player, false); data.recordPlayer(player, true);
        assertFalse(data.creativeThrow);
    }
    @Test public void originalItemNbtIsUntouched() {
        var root = new CompoundTag();
        var item = new CompoundTag(); item.putString("id", "simplyswords:livyatan");
        item.putInt("Damage", 123); item.putString("CustomName", "My unique weapon");
        var enchantments = new CompoundTag(); enchantments.putInt("loyalty", 3); item.put("Enchantments", enchantments);
        root.put("item", item.copy());
        var data = new RescueData(); data.recordPlayer(UUID.randomUUID(), false); data.rescuing = true; data.write(root);
        assertEquals(item, root.getCompound("item"));
        assertFalse(root.getCompound("GiveMyTridentBack").contains("item"));
    }
    @Test public void syntheticFlagSurvivesReloadEvenIfOriginalModForgetsItsFlag() {
        var data = new RescueData(); data.recordPlayer(UUID.randomUUID(), false); data.synthetic = true;
        var tag = new CompoundTag(); data.write(tag);
        var restored = new RescueData(); restored.read(tag);
        assertTrue(restored.synthetic);
    }
    @Test public void missingOrInvalidOwnerCannotBecomePendingRescue() {
        var root = new CompoundTag(); var tag = new CompoundTag(); tag.putBoolean("rescuing", true); root.put("GiveMyTridentBack", tag);
        var data = new RescueData(); data.read(root);
        assertNull(data.playerOwner); assertFalse(data.rescuing);
    }
    @Test public void ownershipChangeCannotReturnToPreviousPlayerOrResetCreativeOrigin() {
        var data = new RescueData(); UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        data.recordPlayer(first, true); data.rescuing = true;
        data.reconcileOwner(second);
        assertNull(data.playerOwner); assertFalse(data.rescuing);
        data.recordPlayer(second, false);
        assertEquals(second, data.playerOwner); assertTrue(data.creativeThrow);
    }
}
