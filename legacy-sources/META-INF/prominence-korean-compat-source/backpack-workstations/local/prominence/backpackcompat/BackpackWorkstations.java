package local.prominence.backpackcompat;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_747;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeContainer;

public final class BackpackWorkstations implements ModInitializer {
    public static final class_2960 OPEN_REFORGE=new class_2960("prominence_korean_compat","backpack_reforge");
    @Override public void onInitialize() {
        if(!BackpackCompatGuard.enabled())return;
        ServerPlayNetworking.registerGlobalReceiver(OPEN_REFORGE,(server,player,network,buffer,sender)->{
            if(buffer.readableBytes()!=8)return;
            int sync=buffer.readInt(),upgradeId=buffer.readInt();
            server.execute(()->{
                if(!(player.field_7512 instanceof BackpackContainer bag) || bag.field_7763!=sync || !bag.method_7597(player))return;
                var upgrade=bag.getUpgradeContainers().get(upgradeId);
                if(!(upgrade instanceof AnvilUpgradeContainer) || !upgrade.isOpen())return;
                var context=bag.getBackpackContext();
                var uuid=context.getBackpackWrapper(player).getContentsUuid();
                if(uuid.isEmpty())return;
                player.method_17355(new class_747((id,inventory,p)->new PortableReforgeMenu(id,inventory,context,uuid.get(),upgradeId),class_2561.method_43471("container.reforge")));
            });
        });
    }
}
