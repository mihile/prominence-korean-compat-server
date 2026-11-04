package local.prominence.hourglass;

import java.util.*;
import net.minecraft.*;
import elocindev.prominentbase.mythicchallenge.MythicChallenge;

/** Per-challenge rescue for players embedded in arena blocks or moved beyond its outer wall. */
public final class ArenaRecovery {
    private static final double CENTER_X=25.5, CENTER_Z=25.5, OUTER_RADIUS=25.75, SAFE_RADIUS=24.0;
    private static final int[] Y_OFFSETS={0,1,-1,2,-2,3};
    private final Map<UUID, State> states=new HashMap<>();
    private static final class State { class_243 safe; int embedded; }

    static boolean outside(double x,double y,double z) {
        double dx=x-CENTER_X,dz=z-CENTER_Z;
        return dx*dx+dz*dz>OUTER_RADIUS*OUTER_RADIUS || y<55;
    }
    static boolean inner(double x,double y,double z) {
        double dx=x-CENTER_X,dz=z-CENTER_Z;
        return y>=60 && y<=110 && dx*dx+dz*dz<=SAFE_RADIUS*SAFE_RADIUS;
    }
    private static class_238 boxAt(class_3222 p,class_243 v) {
        var from=p.method_19538();
        return p.method_5829().method_989(v.field_1352-from.field_1352,v.field_1351-from.field_1351,v.field_1350-from.field_1350).method_1011(.001);
    }
    private static boolean clear(class_3218 w,class_3222 p,class_243 v) {
        var box=boxAt(p,v);
        return !w.method_20812(p,box).iterator().hasNext()
            && w.method_8316(class_2338.method_49637(v.field_1352,v.field_1351,v.field_1350)).method_15769()
            && w.method_8316(class_2338.method_49637(v.field_1352,v.field_1351+1,v.field_1350)).method_15769();
    }
    private static boolean supported(class_3218 w,class_3222 p,class_243 v) {
        return w.method_20812(p,boxAt(p,v).method_989(0,-.16,0)).iterator().hasNext();
    }
    private static boolean safe(class_3218 w,class_3222 p,class_243 v) {
        return inner(v.field_1352,v.field_1351,v.field_1350) && clear(w,p,v) && supported(w,p,v);
    }
    private static class_243 findSafe(class_3218 w,class_3222 p,State state) {
        if(state.safe!=null && safe(w,p,state.safe))return state.safe;
        var entry=MythicChallenge.ARENA_ENTRY;
        if(safe(w,p,entry))return entry;
        // The known entry can be occupied or altered by a boss. Search nearby, checking
        // the player's real pose, collision shapes, fluid and a solid supporting surface.
        for(int radius=1;radius<=12;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++) {
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            for(int dy:Y_OFFSETS) {
                var candidate=new class_243(entry.field_1352+dx,entry.field_1351+dy,entry.field_1350+dz);
                if(safe(w,p,candidate))return candidate;
            }
        }
        // A collision-free entry still permits a short fall when the original floor changed.
        return clear(w,p,entry)?entry:null;
    }
    public void tick(class_3218 world) {
        Set<UUID> present=new HashSet<>();
        for(class_3222 p:world.method_18456()) {
            if(p.method_7325() || !p.method_5805())continue;
            UUID id=p.method_5667(); present.add(id);
            State s=states.computeIfAbsent(id,k->new State());
            class_243 here=p.method_19538();
            boolean embedded=world.method_20812(p,boxAt(p,here)).iterator().hasNext();
            s.embedded=embedded?s.embedded+1:0;
            if(outside(here.field_1352,here.field_1351,here.field_1350)||s.embedded>=3) {
                class_243 destination=findSafe(world,p,s);
                if(destination!=null) {
                    p.method_14251(world,destination.field_1352,destination.field_1351,destination.field_1350,p.method_36454(),p.method_36455());
                    p.method_18799(class_243.field_1353);p.field_6017=0;s.embedded=0;
                }
            } else if(!embedded && safe(world,p,here))s.safe=here;
        }
        states.keySet().retainAll(present);
    }
}
