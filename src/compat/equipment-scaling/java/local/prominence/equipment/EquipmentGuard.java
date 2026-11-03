package local.prominence.equipment;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
public final class EquipmentGuard implements IMixinConfigPlugin {
 private boolean enabled;
 private String hash(String id,String path)throws Exception {
  var mod=FabricLoader.getInstance().getModContainer(id).orElseThrow();
  return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(mod.findPath(path).orElseThrow())));
 }
 public void onLoad(String pkg){try{
  enabled=FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.20.1")
   && hash("prominent","elocindev/prominent/primordial/set/FrostMageArmorSetBonus.class").equals("d341b7aef4648e0a569b6a786d940700e9411db2f18114fa5e16ab32a561a011")
   && hash("prominent","elocindev/prominent/primordial/set/FrostDkArmorSetBonus.class").equals("2eb251b10a7d484157e1d9f863d651fbb05ce58aeb3add0e679a7f41fa38b4ae")
   && hash("prominent","elocindev/prominent/spells/frostmourne/RemorselessWinter.class").equals("9094fb0934c687d2164102909eee11898cbfbc06ad354bd9ee933359fb29e802")
   && hash("prominent","elocindev/prominent/spells/frostmourne/AgonizingBreath.class").equals("0b3ae1a4ecff8d09120058ee94d4034bc75654a87b82c6fa3635809b6ee7947a")
   && hash("spell_power","net/spell_power/api/SpellSchools.class").equals("421fae4dd0a00cac9b4b2c0964156bba193cc7bd80ef6d1830729ebb65ecb4d5")
   && hash("spell_power","net/spell_power/api/SpellPowerMechanics.class").equals("9d38578ca332616af39e54e5786702a8ba44347574b2d453721f257b33ee9c34")
   && hash("spell_power","net/spell_power/api/SpellPower.class").equals("25756a34c807ccaf217cbfe5986f01861a4ee404247a3987bcf9cbbf43d90d15");
 }catch(Exception e){enabled=false;}
 org.slf4j.LoggerFactory.getLogger("Prominence compatibility").info("Equipment spell critical chance compatibility: {}",enabled?"enabled":"disabled (class mismatch)");
 }
 public boolean shouldApplyMixin(String t,String m){return enabled;}
 public String getRefMapperConfig(){return null;}
 public List<String> getMixins(){return null;}
 public void acceptTargets(Set<String>a,Set<String>b){}
 public void preApply(String a,ClassNode b,String c,IMixinInfo d){}
 public void postApply(String a,ClassNode b,String c,IMixinInfo d){}
}
