package local.prominence.artifactmemory;
import elocindev.prominent.item.artifacts.core.Artifact;
import elocindev.prominent.item.artifacts.core.IPartOfSet;
import elocindev.prominent.player.artifact.ArtifactAPI;
import local.prominence.artifactmemory.mixin.SkillsDataAccess;
import net.minecraft.class_1799;
import net.minecraft.class_2487;
import net.minecraft.class_2960;
import net.minecraft.class_3222;
import net.minecraft.class_7923;
import net.puffish.skillsmod.SkillsMod;
public final class ArtifactHistory {
    public static final String TAG="prominence_korean_compat:artifact_history_v1";
    public static class_2960 category(class_1799 stack){
        if(!(stack.method_7909() instanceof Artifact) || stack.method_7909() instanceof IPartOfSet)return null;
        var id=new class_2960("puffish_skills",class_7923.field_41178.method_10221(stack.method_7909()).method_12832());
        return ArtifactAPI.isArtifactCategory(id)?id:null;
    }
    private static ArtifactHistoryStore store(class_3222 player){
        var instance=SkillsMod.getInstance();
        return (ArtifactHistoryStore)((SkillsDataAccess)(Object)instance).prominence$getPlayerData(player);
    }
    public static void capture(class_3222 player,class_1799 target){
        if(category(target)==null)return;
        var inst=SkillsMod.getInstance();var history=store(player).prominence$artifactHistory();
        for(var tree:ArtifactAPI.ARTIFACT_TREES){
            if(!inst.isCategoryUnlocked(player,tree).orElse(false))continue;
            var xp=inst.getExperience(player,tree);var skills=inst.getUnlockedSkills(player,tree);
            if(xp.isEmpty() || skills.isEmpty())continue;
            var snapshot=new class_2487();snapshot.method_10569("experience",Math.max(0,xp.get()));
            var nodes=new class_2487();
            for(String id:skills.get())nodes.method_10556(id,true);
            snapshot.method_10566("skills",nodes);history.method_10566(tree.toString(),snapshot);
        }
    }
    public static void restore(class_3222 player,class_1799 target){
        var tree=category(target);if(tree==null)return;
        var inst=SkillsMod.getInstance();var history=store(player).prominence$artifactHistory();
        if(!inst.isCategoryUnlocked(player,tree).orElse(false) || !history.method_10573(tree.toString(),10))return;
        var snapshot=history.method_10562(tree.toString());
        // The original transition already cleared and hid every other artifact category.
        // Reapply only this artifact, using the native reward and synchronization paths.
        var access=(SkillsDataAccess)(Object)inst;
        var optional=access.prominence$getCategory(tree);if(optional.isEmpty())return;
        var config=optional.get();
        inst.resetSkills(player,tree);
        inst.setExperience(player,tree,Math.max(0,snapshot.method_10550("experience")));
        var categoryData=access.prominence$getPlayerData(player).getOrCreateCategoryData(config);
        var nodes=snapshot.method_10562("skills");var saved=new java.util.TreeSet<>(nodes.method_10541());
        int cost=0;boolean valid=true;
        for(String id:saved){
            var definition=config.skills().getById(id).flatMap(s->config.definitions().getById(s.definitionId()));
            if(!nodes.method_10577(id) || definition.isEmpty()){valid=false;break;}
            cost+=definition.get().cost();
        }
        if(valid && cost<=categoryData.getPointsLeft(config)){
            // Restore state directly; this is not a fresh skill purchase and must not replay unlock events.
            for(String id:saved)categoryData.unlockSkill(id);
        }else org.slf4j.LoggerFactory.getLogger("ProminenceArtifactHistory").warn("Artifact tree {} no longer matches saved selections; restored XP with unspent points",tree);
        inst.unlockCategory(player,tree); // Refresh native points, rewards and one category packet.
    }
    public static class_2487 sanitize(class_2487 input){
        var result=new class_2487();
        for(var tree:ArtifactAPI.ARTIFACT_TREES){
            String key=tree.toString();
            if(input.method_10573(key,10))result.method_10566(key,input.method_10562(key).method_10553());
        }
        return result;
    }
    private ArtifactHistory(){}
}
