package local.prominence.backpackcompat;
import elocindev.tierify.screen.ReforgeScreenHandler;
import java.util.UUID;
import local.prominence.backpackcompat.mixin.ReforgeReadyAccess;
import net.minecraft.class_1657;
import net.minecraft.class_1661;
import net.minecraft.class_3914;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeWrapper;

public final class PortableReforgeMenu extends ReforgeScreenHandler {
    private final BackpackContext backpack;
    private final UUID contents;
    private final int upgradeId;
    private final class_1657 owner;
    public PortableReforgeMenu(int id,class_1661 inventory,BackpackContext backpack,UUID contents,int upgradeId) {
        super(id,inventory,class_3914.method_17392(inventory.field_7546.method_37908(),inventory.field_7546.method_24515()));
        this.backpack=backpack;this.contents=contents;this.upgradeId=upgradeId;this.owner=inventory.field_7546;
    }
    public BackpackContext backpackContext(){return backpack;}
    @Override public boolean method_7597(class_1657 player) {
        if(backpack==null || !backpack.canInteractWith(player))return false;
        var wrapper=backpack.getBackpackWrapper(player);
        return wrapper.getContentsUuid().filter(contents::equals).isPresent()
            && wrapper.getUpgradeHandler().getSlotWrappers().get(upgradeId) instanceof AnvilUpgradeWrapper;
    }
    @Override public void reforge() {
        if(owner.field_7512==this && method_7597(owner) && ((ReforgeReadyAccess)(Object)this).prominence$reforgeReady())super.reforge();
    }
}
