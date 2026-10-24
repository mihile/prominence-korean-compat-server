package local.prominence.artifactmemory.mixin;
import net.minecraft.class_3222;
import net.puffish.skillsmod.server.data.PlayerData;
import net.puffish.skillsmod.config.CategoryConfig;
import net.minecraft.class_2960;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(targets="net.puffish.skillsmod.SkillsMod",remap=false)
public interface SkillsDataAccess {
    @Invoker("getPlayerData") PlayerData prominence$getPlayerData(class_3222 player);
    @Invoker("getCategory") java.util.Optional<CategoryConfig> prominence$getCategory(class_2960 category);
}
