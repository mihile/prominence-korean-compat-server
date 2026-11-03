package local.prominence.joinallocation;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
public final class JoinAllocationGuard implements IMixinConfigPlugin {
 private boolean packet,invoke;
 private String hash(String id,String path)throws Exception {
  var mod=FabricLoader.getInstance().getModContainer(id).orElseThrow();
  return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(mod.findPath(path).orElseThrow())));
 }
 public void onLoad(String pkg){
  boolean mc=false;try{mc=FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.20.1");}catch(Exception e){}
  try{packet=mc && java.util.Set.of("c179c6088c6ed5071a844422f081deb956660c8084e390eeabfb637f95ee87ce").contains(hash("voxyserver","com/dripps/voxyserver/network/PreSerializedLodPayload.class")) && java.util.Set.of("b0b8ad96626eabfdc8f01cb27b4a8f2fdc569ae4be29bc0a5b35046fad60f3e4").contains(hash("voxyserver","com/dripps/voxyserver/network/LODBulkPayload.class")) && java.util.Set.of("a5282116e7ff9baf040f0e54703bfc82dd26846bc7f1df7d52e711826893e80e").contains(hash("voxyserver","com/dripps/voxyserver/network/LODSectionPayload.class")) && java.util.Set.of("334996a77be0f48ae8c4c5b044f287ae965f1ef574bd2b582c1072d1c789017d").contains(hash("voxyserver","com/dripps/voxyserver/network/PacketCodec.class"));}catch(Exception e){packet=false;}
  try{invoke=mc && hash("invoke","com/invoke/InvokeMod.class").equals("bed3813f651b6136fdb4c652963144aedf0f8239cb5ac7ee330c8c517d04ea61") && hash("spell_engine","net/spell_engine/internals/SpellHelper.class").equals("669afdc0bead19d69f85c9d2d7cf9f9934056958b4b8c8d21077d9e6bcdac2f5");}catch(Exception e){invoke=false;}
  org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Join allocation compatibility: LOD packet sizing={}, Invoke unused cooldown={}",packet,invoke);
 }
 public boolean shouldApplyMixin(String target,String mixin){return mixin.endsWith("InvokeUnusedCooldownMixin")?invoke:packet;}
 public String getRefMapperConfig(){return null;}
 public List<String> getMixins(){return null;}
 public void acceptTargets(Set<String>a,Set<String>b){}
 public void preApply(String a,ClassNode b,String c,IMixinInfo d){}
 public void postApply(String a,ClassNode b,String c,IMixinInfo d){}
}
