package local.prominence.artifactmemory.mixin;
import elocindev.prominent.item.artifacts.Vaazkar;
import elocindev.prominent.registry.AttributeRegistry;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.puffish.skillsmod.SkillsMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(targets="elocindev.prominent.item.artifacts.Vaazkar",remap=false)
public abstract class VaazkarUserPhaseMixin {
    @Unique private static int prominence$phase(class_1309 user){
        if(user instanceof class_3222 player && !SkillsMod.getInstance().isCategoryUnlocked(player,new class_2960("puffish_skills","vaazkar")).orElse(false))return 0;
        return Math.max(0,Math.min(3,(int)user.method_26825(AttributeRegistry.ARTIFACT_PHASE)));
    }
    @Redirect(method="method_7852",at=@At(value="FIELD",target="Lelocindev/prominent/item/artifacts/Vaazkar;abilityPhase:I",opcode=180),require=1)
    private int prominence$use(Vaazkar item,class_1937 world,class_1309 user,class_1799 stack,int ticks){return prominence$phase(user);}
    @Redirect(method="method_7840",at=@At(value="FIELD",target="Lelocindev/prominent/item/artifacts/Vaazkar;abilityPhase:I",opcode=180),require=1)
    private int prominence$release(Vaazkar item,class_1799 stack,class_1937 world,class_1309 user,int ticks){return prominence$phase(user);}
    @Redirect(method="addPassiveCooldown",at=@At(value="FIELD",target="Lelocindev/prominent/item/artifacts/Vaazkar;abilityPhase:I",opcode=180),require=1)
    private int prominence$cooldown(Vaazkar item,class_1657 player,class_1799 stack){return prominence$phase(player);}
    @Redirect(method="lambda$usageTick$2",at=@At(value="FIELD",target="Lelocindev/prominent/item/artifacts/Vaazkar;abilityPhase:I",opcode=180),require=1)
    private int prominence$damage(Vaazkar item,class_1309 user,int tick,class_1309 target){return prominence$phase(user);}
}
