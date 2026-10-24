package dev.mihile.tridentback;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

/** No item copies: only ownership proof and a pending rescue flag are persisted. */
public final class RescueData {
    public UUID playerOwner;
    public boolean rescuing;
    public boolean creativeThrow;
    public boolean synthetic;
    private boolean originKnown;

    public void recordPlayer(UUID owner, boolean creative) {
        if (!originKnown) { creativeThrow = creative; originKnown = true; }
        playerOwner = owner;
    }

    public void reconcileOwner(UUID actualOwner) {
        if (playerOwner != null && !playerOwner.equals(actualOwner)) {
            playerOwner = null;
            rescuing = false;
        }
    }

    public void write(CompoundTag root) {
        if (playerOwner == null) { root.remove("GiveMyTridentBack"); return; }
        CompoundTag tag = new CompoundTag();
        tag.putUUID("owner", playerOwner);
        tag.putBoolean("rescuing", rescuing);
        tag.putBoolean("creativeThrow", creativeThrow);
        tag.putBoolean("synthetic", synthetic);
        root.put("GiveMyTridentBack", tag);
    }

    public void read(CompoundTag root) {
        playerOwner = null;
        rescuing = false;
        creativeThrow = false;
        synthetic = false;
        originKnown = false;
        CompoundTag tag = root.getCompound("GiveMyTridentBack");
        if (!tag.hasUUID("owner")) return;
        playerOwner = tag.getUUID("owner");
        originKnown = true;
        rescuing = tag.getBoolean("rescuing");
        creativeThrow = tag.getBoolean("creativeThrow");
        synthetic = tag.getBoolean("synthetic");
    }
}
