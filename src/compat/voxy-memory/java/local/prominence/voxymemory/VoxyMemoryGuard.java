package local.prominence.voxymemory;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
public final class VoxyMemoryGuard implements IMixinConfigPlugin {
 private boolean enabled;
 private String hash(String id,String path)throws Exception {
  var mod=FabricLoader.getInstance().getModContainer(id).orElseThrow();
  return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(mod.findPath(path).orElseThrow())));
 }
 public void onLoad(String pkg){try{
  enabled=FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.20.1")
   && hash("voxy","me/cortex/voxy/common/world/WorldSection.class").equals("09c101914d0de3067b208e332281ab856c8b069648aa9a1687d5b7e192e11ba2")
   && hash("voxy","me/cortex/voxy/common/world/WorldEngine.class").equals("ef05cfb9450a81fdf8abbfd5cdf7ee9b8d2af7fccead5c3db15cf31bd0715bbe")
   && hash("voxy","me/cortex/voxy/common/world/ActiveSectionTracker.class").equals("d89d0a42a50410b3be557932f942236cbc9c969ac4032c86c54cc015b180a206")
   && java.util.Set.of("9deeaaef3702783a931da76602ba2819212f0eb1facf5da5e45376644290835e","9a71cf041599ce6c96eb9dd6004e525fcc15adf8d61adc3152cd1ecc19302a92").contains(hash("voxyserver","com/dripps/voxyserver/server/LodStreamingService.class"));
 }catch(Exception e){enabled=false;}
 org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Voxy streaming scratch and secondary cache compatibility: {}",enabled?"enabled":"disabled (class mismatch)");
 }
 public boolean shouldApplyMixin(String t,String m){return enabled;}
 public String getRefMapperConfig(){return null;}
 public List<String> getMixins(){return null;}
 public void acceptTargets(Set<String>a,Set<String>b){}
 public void preApply(String a,ClassNode b,String c,IMixinInfo d){}
 public void postApply(String a,ClassNode b,String c,IMixinInfo d){}
}
