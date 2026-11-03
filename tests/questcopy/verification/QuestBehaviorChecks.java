package verification;

import elocindev.prominent.api.questing.QuestProgress;
import net.minecraft.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.lang.management.ManagementFactory;

/** Real native QuestProgress/PersistentPlayerData, NBT and CustomDataHelper; only entity shells are doubles. */
public final class QuestBehaviorChecks {
    static int checks;static final MessageDigest digest;
    static {try{digest=MessageDigest.getInstance("SHA-256");}catch(Exception e){throw new RuntimeException(e);}}
    static void check(boolean result,String why){if(!result)throw new AssertionError(why);checks++;}
    static void record(class_3222 p){digest.update((p.faux$getPersistentData().toString()+"|"+p.faux$getCustomData().toString()+"\n").getBytes(StandardCharsets.UTF_8));}
    public static void main(String[] args){
        var p=new class_3222();var a=new class_2960("test:a");var b=new class_2960("test:b");
        p.faux$getPersistentData().method_10582("unrelated","preserve");var external=p.faux$getPersistentData();
        var q=QuestProgress.open(p,a);q.start();q.setPhase(2);q.setCounter("kills",8);q.setFlag("key",true);q.setObjectiveCompleted("objective",true);
        check(!QuestProgress.open(p,a).isActive(),"unsaved state escaped");check(external==p.faux$getPersistentData(),"open replaced persistent data");q.save();
        check(!external.method_10545("prominent_quests"),"save mutated previous persistent snapshot");record(p);
        var old=QuestProgress.open(p,a);check(old.getCounter("kills")==8&&old.getFlag("key")&&old.getPhase()==2&&old.isObjectiveCompleted("objective"),"save/read state");
        var other=QuestProgress.open(p,b);other.start();other.setCounter("other",7);other.save();old.setCounter("kills",9);old.save();record(p); // Preserve native stale-snapshot semantics.
        q=QuestProgress.open(p,a);q.setTimer("t",2);check(!q.tickTimer("t")&&q.tickTimer("t"),"timer behavior");q.setLong("long",Long.MAX_VALUE);check(q.hasLong("long")&&q.getLong("long")==Long.MAX_VALUE,"long state");q.clearLong("long");
        q.setSequenceStep("s",3);q.setSequenceDelay("s",2);q.setSequenceReminder("s",1);check(q.getSequenceStep("s")==3&&q.getSequenceDelay("s")==2&&q.getSequenceReminder("s")==1,"sequence state");q.save();record(p);
        q=QuestProgress.open(p,a);q.finish();q.save();check(QuestProgress.open(p,a).isCompleted(),"finish");record(p);q.reset();q.save();check(!QuestProgress.open(p,a).isCompleted(),"reset");record(p);q.clear();q.save();record(p);
        check(!p.faux$getPersistentData().method_10545("prominent_quests"),"empty root cleanup");check(p.faux$getPersistentData().method_10558("unrelated").equals("preserve"),"unrelated data");
        // Legacy root migration, existing persistent value wins, legacy unrelated data remains.
        var legacy=new class_2487();var qa=new class_2487();qa.method_10556("active",true);legacy.method_10566("test:a",qa);p.faux$getCustomData().method_10566("prominent_quests",legacy);p.faux$getCustomData().method_10582("legacy-other","keep");
        check(QuestProgress.open(p,a).isActive(),"legacy migration");check(!p.faux$getCustomData().method_10545("prominent_quests"),"legacy cleanup");record(p);
        q=QuestProgress.open(p,a);check(q.getPlayer()==p&&q.getQuestId()==a,"identity");
        check(q.markWaypointCompleted("route",2)&&!q.markWaypointCompleted("route",2),"waypoint deduplication");q.markWaypointCompleted("route",1);
        check(q.getCompletedWaypoints("route").equals(Set.of(1,2)),"waypoint state");q.save();record(p);q.clearWaypoints("route");q.clearSequence("s");q.completeObjective("next");
        q.startTimerIfAbsent("wait",3);q.startTimerIfAbsent("wait",8);check(q.getTimer("wait")==3,"existing timer preserved");q.clearTimer("wait");check(!q.hasTimer("wait")&&!q.tickTimer("wait"),"absent timer");q.save();record(p);
        p.faux$getCustomData().method_10566("prominent_quests",new class_2487());check(QuestProgress.open(p,a).isActive(),"persistent root wins migration");record(p);
        var malformed=new class_3222();malformed.faux$getPersistentData().method_10582("prominent_quests","wrong type");
        var missing=QuestProgress.open(malformed,a);check(!missing.isActive(),"wrong root type");missing.start();missing.save();record(malformed);
        malformed.faux$getPersistentData().method_10562("prominent_quests").method_10582("test:a","wrong quest type");missing=QuestProgress.open(malformed,a);check(!missing.isActive(),"wrong quest type");missing.start();missing.save();record(malformed);
        var random=new Random(4713);
        for(int i=0;i<400;i++){
            var id=new class_2960("test:q"+random.nextInt(12));var progress=QuestProgress.open(p,id);
            switch(random.nextInt(9)){
                case 0 -> progress.start();case 1 -> progress.finish();case 2 -> progress.reset();case 3 -> progress.clear();
                case 4 -> progress.incrementCounter("counter");case 5 -> progress.setFlag("f",random.nextBoolean());
                case 6 -> progress.setTimer("t",random.nextInt(5));case 7 -> progress.setObjectiveCompleted("o",true);case 8 -> progress.setLong("l",random.nextLong());
            }
            if(i%4!=0)progress.save(); // Unsaved objects must remain private, even when discarded.
            record(p);check(p.faux$getPersistentData().method_10558("unrelated").equals("preserve"),"random unrelated data");
        }
        System.out.println("STATE_SHA256="+HexFormat.of().formatHex(digest.digest()));
        System.out.println("CHECKS="+checks);
        // Allocation fixture uses identical real NBT trees in each process.
        var tree=new class_2487();for(int i=0;i<80;i++){var item=new class_2487();item.method_10556("active",true);for(int j=0;j<10;j++)item.method_10582("flag"+j,"value-"+i+"-"+j);tree.method_10566("test:q"+i,item);}
        p.faux$getPersistentData().method_10566("prominent_quests",tree);var id=new class_2960("test:q4");
        for(int i=0;i<1500;i++)check(QuestProgress.open(p,id).isActive(),"fixture active");
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();long tid=Thread.currentThread().getId();long before=bean.getThreadAllocatedBytes(tid);
        for(int i=0;i<1000;i++)if(!QuestProgress.open(p,id).isActive())throw new AssertionError();
        System.out.println("ALLOCATION_BYTES="+(bean.getThreadAllocatedBytes(tid)-before));
    }
}
