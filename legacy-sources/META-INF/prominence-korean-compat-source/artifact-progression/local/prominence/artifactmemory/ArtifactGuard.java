package local.prominence.artifactmemory;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
public final class ArtifactGuard implements IMixinConfigPlugin {
    public static boolean supported(){
        var l=FabricLoader.getInstance();
        return l.getModContainer("minecraft").map(m->m.getMetadata().getVersion().getFriendlyString().equals("1.20.1")).orElse(false)
            && l.getModContainer("prominent").map(m->m.getMetadata().getVersion().getFriendlyString().equals("4.1.1")).orElse(false)
            && l.getModContainer("puffish_skills").map(m->m.getMetadata().getVersion().getFriendlyString().equals("0.17.3")).orElse(false);
    }
    public boolean shouldApplyMixin(String t,String m){return supported();}
    public void onLoad(String p){}
    public String getRefMapperConfig(){return null;}
    public void acceptTargets(Set<String>a,Set<String>b){}
    public List<String> getMixins(){return null;}
    public void preApply(String t,ClassNode c,String m,IMixinInfo i){}
    public void postApply(String t,ClassNode c,String m,IMixinInfo i){}
}
