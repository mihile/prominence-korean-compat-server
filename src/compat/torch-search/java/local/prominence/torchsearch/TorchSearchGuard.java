package local.prominence.torchsearch;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
public final class TorchSearchGuard implements IMixinConfigPlugin {
 private boolean enabled;
 private String hash(String id,String path)throws Exception {
  var mod=FabricLoader.getInstance().getModContainer(id).orElseThrow();
  return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(mod.findPath(path).orElseThrow())));
 }
 public void onLoad(String pkg){try{
  enabled=FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.20.1")
   && hash("magnumtorch","fuzs/magnumtorch/handler/MobSpawningHandler.class").equals("d360212e956fd0bfd6430bd3e714860f5cdfa0e08cc4b9b413efcdb999e17f5e")
   && hash("magnumtorch","fuzs/magnumtorch/config/ServerConfig$MagnumTorchConfig.class").equals("7f542a8e3641dbd7bf896e852e7314496167ba7c83ceb6440118a6ed849d5983")
   && hash("magnumtorch","fuzs/magnumtorch/config/SpawnShapeType.class").equals("e81211de0115d42295b0364614995aa68359d85b9cc5cddb0f98e2d4b3115635")
   && hash("magnumtorch","fuzs/magnumtorch/config/SpawnShapeType$1.class").equals("2eb05dfc0f60a619a354a43cff86750fa3dd6e26b48109562c6358cdb4681662")
   && hash("magnumtorch","fuzs/magnumtorch/config/SpawnShapeType$2.class").equals("d7a3578fa9941a97cb1375f515a8ec84ee07166cd1b607bfb3f0528e7891ee77")
   && hash("magnumtorch","fuzs/magnumtorch/config/SpawnShapeType$3.class").equals("803f840a6b2e53dad51dcd05b9d804f6e25ced87332deabbc79d02cc56a7934c");
 }catch(Exception e){enabled=false;}
 org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Magnum Torch conservative POI search compatibility: {}",enabled?"enabled":"disabled (class mismatch)");
 }
 public boolean shouldApplyMixin(String t,String m){return enabled;}
 public String getRefMapperConfig(){return null;}
 public List<String> getMixins(){return null;}
 public void acceptTargets(Set<String>a,Set<String>b){}
 public void preApply(String a,ClassNode b,String c,IMixinInfo d){}
 public void postApply(String a,ClassNode b,String c,IMixinInfo d){}
}
