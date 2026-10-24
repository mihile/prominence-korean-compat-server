package local.prominence.backpackcompat;
import java.nio.file.Files;import java.security.MessageDigest;import java.util.HexFormat;import java.util.List;import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;import org.objectweb.asm.tree.ClassNode;import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
public final class BackpackCompatGuard implements IMixinConfigPlugin {
 private static boolean version(String id,String v){return FabricLoader.getInstance().getModContainer(id).map(m->m.getMetadata().getVersion().getFriendlyString().equals(v)).orElse(false);}
 private static boolean known(String id,String name,String hash){try{var m=FabricLoader.getInstance().getModContainer(id);if(m.isEmpty())return false;var p=m.get().findPath(name);return p.isPresent() && HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p.get()))).equals(hash);}catch(Exception e){return false;}}
 public static boolean enabled(){return version("minecraft","1.20.1") && version("mixinextras","0.5.4") && version("sophisticatedcore","1.20.1-1.2.7.15.166") && version("sophisticatedbackpacks","1.20.1-3.23.4.5.110") && version("tiered","1.2.0") && version("sortilege","9.0") && !System.getProperty("prominence.backpackWorkstations","true").equalsIgnoreCase("false") && known("sophisticatedcore","net/p3pp3rf1y/sophisticatedcore/common/gui/StorageContainerMenuBase.class","424154d8f2c1c621c44f58cc49298fb8668f569e430c62c64dfc24d8b7bb3550") &&
            known("sophisticatedbackpacks","net/p3pp3rf1y/sophisticatedbackpacks/upgrades/anvil/AnvilUpgradeTab.class","b2ae90ab6520fe20af13715824963200f8ddfc4cb0b9811d9a01f71c884c9a8a") &&
            known("sophisticatedbackpacks","net/p3pp3rf1y/sophisticatedbackpacks/upgrades/anvil/AnvilUpgradeContainer$PersistableAnvilMenu.class","794494a61d52f519e7a4c2e5059d5b693c460c9b3c34713dab67e7b7ebcf6e44") &&
            known("sophisticatedbackpacks","net/p3pp3rf1y/sophisticatedbackpacks/upgrades/smithing/SmithingUpgradeContainer.class","3586206f756f1ef39cea1bec6b9daf7418ab2786703ef155e27bce01f7960ec2") &&
            known("sophisticatedbackpacks","net/p3pp3rf1y/sophisticatedbackpacks/network/BackpackOpenMessage.class","be8b3fe75e3fab6886f30f92c88b9148704e68a5cbf1bc250f732720834c34b4") &&
            known("sophisticatedbackpacks","net/p3pp3rf1y/sophisticatedbackpacks/common/gui/BackpackContext.class","1335ddebf9cff2dc3360116f7da7e8252283946a43eb5a4555693c07fdca5940") &&
            known("tiered","elocindev/tierify/screen/ReforgeScreenHandler.class","f1ae71265c2db4cba87038b5612da6a9da57cd5e5c915351874206051a9f3c2f") &&
            known("tiered","elocindev/tierify/network/TieredServerPacket.class","7931c7c5f90b159991dbb5e495fefcead963a30dc45f6e2e87a19c690cb3c860") &&
            known("sortilege","net/lyof/sortilege/mixin/AnvilMenuMixin.class","71eeb9dc8727bb458ebc823a8afdda0cac54a39b3d42d76e8071ee196dc0c920");}
 private static boolean client(){return false;}
 public void onLoad(String pkg){LoggerFactory.getLogger("Prominence compatibility").info("Backpack workstation compatibility: common={}, client={}",enabled(),client());}
 public boolean shouldApplyMixin(String target,String mixin){return mixin.endsWith("BackpackSmithingEmiMixin") || mixin.endsWith("BackpackAnvilCostMixin") || mixin.endsWith("BackpackReforgeTabMixin") ? client() : enabled();}
 public String getRefMapperConfig(){return null;}public List<String> getMixins(){return null;}public void acceptTargets(Set<String>a,Set<String>b){}public void preApply(String a,ClassNode b,String c,IMixinInfo d){}public void postApply(String a,ClassNode b,String c,IMixinInfo d){}
}