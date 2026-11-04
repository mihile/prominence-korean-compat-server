package local.prominence.hourglass;import net.minecraft.*;public class ArenaChecks {static void yes(boolean b,String s){if(!b)throw new AssertionError(s);}public static void main(String[]args){
 var a=new ArenaRecovery();var w=new class_3218();var p=new class_3222(25,67,25);w.players.add(p);a.tick(w);yes(p.teleports==0,"normal movement");
 p.set(48,67,25);a.tick(w);a.tick(w);yes(p.teleports==0,"brief overlap");a.tick(w);yes(p.teleports==1 && p.x==25 && p.z==25,"embedded restore");
 p.set(51.5,67,25);a.tick(w);yes(p.teleports==2 && p.x==25,"outside restore");
 p.set(25,52,25);a.tick(w);yes(p.teleports==3&&p.y==67,"under arena restore");
 p.spect=true;p.set(80,67,25);a.tick(w);yes(p.teleports==3,"spectator ignored");p.spect=false;
 w.lavaX=25;p.set(48,67,25);a.tick(w);a.tick(w);a.tick(w);yes(p.teleports==4 && ((int)p.x)!=25,"lava snapshot rejected and fallback searched");
 var b=new ArenaRecovery();var w2=new class_3218();var q=new class_3222(48,67,25);w2.players.add(q);b.tick(w2);b.tick(w2);b.tick(w2);yes(q.teleports==1 && q.x==25 && q.z==44,"fresh stuck player fallback");
 var c=new ArenaRecovery();var w3=new class_3218();var r=new class_3222(48,67,25);w3.players.add(r);w3.allBlocked=true;c.tick(w3);c.tick(w3);c.tick(w3);yes(r.teleports==0,"no unsafe fallback");w3.allBlocked=false;c.tick(w3);yes(r.teleports==1,"retry after obstruction");
 yes(!ArenaRecovery.outside(25.5,67,50.5),"valid perimeter");yes(ArenaRecovery.outside(0,67,0),"corner outside circular arena");
 System.out.println("PASS normal, transient collision, wall rescue, outside, floor escape, spectator, fluid, no history, blocked fallback, retry and arena bounds");}}