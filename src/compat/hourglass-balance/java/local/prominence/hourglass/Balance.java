package local.prominence.hourglass;
import java.util.UUID;
import net.minecraft.*;
import elocindev.prominentbase.mythicchallenge.*;
public final class Balance {
 public static final UUID MOB_HP=UUID.fromString("5e720d68-550f-4193-9cc0-c048d72f4736");
 private static final ThreadLocal<Integer> PARTY=new ThreadLocal<>();
 public static double factor(int n){return 1.0+0.8*(Math.max(1,n)-1);}
 public static class_1309 spawn(MythicBossEntry b,class_3218 w,class_2338 p,int level,int oldParty,int actual){Integer prior=PARTY.get(); PARTY.set(Math.max(1,actual));try{return b.spawn(w,p,level,oldParty);}finally{if(prior==null)PARTY.remove();else PARTY.set(prior);}}
 public static double bossHp(MythicScalingConfig c,int level,double time,int party,double budget){Integer n=PARTY.get();return n==null?c.targetBossHp(level,time,party,budget):c.targetBossHp(level,time,1,budget)*0.8*factor(n);}
 public static boolean inBossSpawn(){return PARTY.get()!=null;}
 public static void reduceMob(class_1309 e){var a=e.method_6127().method_26842(class_5134.field_23716);if(a==null||a.method_6199(MOB_HP)!=null)return;float max=e.method_6063(),health=e.method_6032();a.method_26837(new class_1322(MOB_HP,"hourglass_mob_hp",-0.2,class_1322.class_1323.field_6331));if(max>0)e.method_6033(Math.min(e.method_6063(),health*e.method_6063()/max));}
 public static void removeMobModifier(class_1309 e){var a=e.method_6127().method_26842(class_5134.field_23716);if(a!=null)a.method_6200(MOB_HP);}
}
