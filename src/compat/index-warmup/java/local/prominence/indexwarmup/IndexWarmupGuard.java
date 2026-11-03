package local.prominence.indexwarmup;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
public final class IndexWarmupGuard implements IMixinConfigPlugin {
 private boolean enabled;
 private String hash(String id,String path)throws Exception {
  var mod=FabricLoader.getInstance().getModContainer(id).orElseThrow();
  return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(mod.findPath(path).orElseThrow())));
 }
 public void onLoad(String pkg){try{
  enabled=!"false".equalsIgnoreCase(System.getProperty("prominence.voxy.startupIndex","true")) && FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.20.1")
   && java.util.Set.of("ecb7504ca0703511d323292c35645506f0a9422a9558cf46d06c863017e2e2b6").contains(hash("voxyserver","com/dripps/voxyserver/Voxyserver.class"))
   && java.util.Set.of("9a71cf041599ce6c96eb9dd6004e525fcc15adf8d61adc3152cd1ecc19302a92","9deeaaef3702783a931da76602ba2819212f0eb1facf5da5e45376644290835e").contains(hash("voxyserver","com/dripps/voxyserver/server/LodStreamingService.class"))
   && java.util.Set.of("481835c74d9411a2d00300bbd18881c62fbd2d66502d17c159eae3eec45ddec6").contains(hash("voxyserver","com/dripps/voxyserver/server/ServerLodEngine.class"))
   && hash("voxy","me/cortex/voxy/commonImpl/WorldIdentifier.class").equals("38e4e60684aa96e7142f4ffae8c2e3cdfb91108b8e1ad123c2df93393de1a92c")
   && hash("voxy","me/cortex/voxy/common/world/WorldEngine.class").equals("ef05cfb9450a81fdf8abbfd5cdf7ee9b8d2af7fccead5c3db15cf31bd0715bbe");
 }catch(Exception e){enabled=false;}
 org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Default-world LOD startup preparation compatibility: {}",enabled?"enabled":"disabled (class mismatch)");
 }
 public boolean shouldApplyMixin(String t,String m){return enabled;}
 public String getRefMapperConfig(){return null;}
 public List<String> getMixins(){return null;}
 public void acceptTargets(Set<String>a,Set<String>b){}
 public void preApply(String a,ClassNode b,String c,IMixinInfo d){}
 public void postApply(String a,ClassNode b,String c,IMixinInfo d){}
}
