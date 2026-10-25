"""Run production HammerBreaks against a small deterministic API fixture, without Minecraft/world startup."""
from pathlib import Path
import argparse,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
STUBS={
'net/minecraft/class_1922.java': 'public interface class_1922 {}',
'net/minecraft/class_239.java': 'public class class_239 {}',
'net/minecraft/class_2350.java': 'public enum class_2350 {UP,DOWN,NORTH,SOUTH,EAST,WEST}',
'net/minecraft/class_3965.java': 'public class class_3965 extends class_239 {public class_2350 face=class_2350.UP; public class_2350 method_17780(){return face;}}',
'net/minecraft/class_2791.java': 'public class class_2791 {}',
'net/minecraft/class_2806.java': 'public class class_2806 {public static final class_2806 field_12803=new class_2806();}',
'net/minecraft/class_1792.java': 'public class class_1792 {}',
'net/minecraft/class_124.java': 'public class class_124 {public static final class_124 field_1061=new class_124();}',
'net/minecraft/class_2561.java': 'public interface class_2561 {public static class_5250 method_43471(String s){return new class_5250();}}',
'net/minecraft/class_5250.java': 'public class class_5250 implements class_2561 {public class_5250 method_27692(class_124 c){return this;}}',
'net/minecraft/class_2487.java': 'public class class_2487 {public boolean warned;public boolean method_10545(String s){return warned;}public void method_10556(String s,boolean b){warned=b;}public void method_10551(String s){warned=false;}}',
'net/minecraft/class_1799.java': '''public class class_1799 {public class_1792 item; public int damage,max=1000;public boolean empty;private class_2487 tag=new class_2487();public class_1799(class_1792 i){item=i;}public boolean method_7960(){return empty;}public class_1792 method_7909(){return item;}public int method_7919(){return damage;}public int method_7936(){return max;}public class_2487 method_7948(){return tag;}}''',
'net/minecraft/class_2680.java': '''public class class_2680 {public boolean air,allowed=true,correct=true;public float hardness=1;public class_2680(){} public class_2680(boolean a){air=a;} public float method_26214(class_1922 w,class_2338 p){return hardness;}}''',
'net/minecraft/class_2338.java': '''public class class_2338 {public int x,y,z; public class_2338(int a,int b,int c){x=a;y=b;z=c;}public int method_10263(){return x;}public int method_10260(){return z;}public class_2338 method_10062(){return new class_2338(x,y,z);}public boolean equals(Object o){return o instanceof class_2338 p&&x==p.x&&y==p.y&&z==p.z;}public int hashCode(){return java.util.Objects.hash(x,y,z);}public static java.util.stream.Stream<class_2338> method_23627(class_3341 b){return b.positions.stream();}}''',
'net/minecraft/class_3341.java': 'public class class_3341 {public java.util.List<class_2338> positions=new java.util.ArrayList<>();}',
'net/minecraft/class_1937.java': '''public class class_1937 implements class_1922 {public boolean field_9236;public int reads,loads;public java.util.Set<Integer> unloaded=new java.util.HashSet<>();public java.util.Map<class_2338,class_2680> blocks=new java.util.HashMap<>();public class_2791 method_8402(int x,int z,class_2806 s,boolean create){if(create)loads++;return unloaded.contains(x)?null:new class_2791();}public class_2680 method_8320(class_2338 p){reads++;return blocks.getOrDefault(p,new class_2680(true));}}''',
'net/minecraft/class_3218.java': 'public class class_3218 extends class_1937 {}',
'net/minecraft/class_1309.java': '''public class class_1309 {public boolean crouch;public class_3965 hit=new class_3965();public boolean method_18276(){return crouch;}public class_239 method_5745(double a,float b,boolean c){return hit;}}''',
'net/minecraft/class_3222.java': '''public class class_3222 extends class_1309 {public java.util.UUID id=java.util.UUID.randomUUID();public class_1799 stack;public class_3218 world=new class_3218();public class_3225 field_13974=new class_3225(this);public boolean creative;public int warnings;public java.util.UUID method_5667(){return id;}public class_1799 method_6047(){return stack;}public class_3218 method_51469(){return world;}public boolean method_7337(){return creative;}public void method_43502(class_2561 text,boolean overlay){warnings++;}}''',
'net/minecraft/class_3225.java': '''public class class_3225 {public class_3222 player;public int before,after,drops;public java.util.Set<class_2338> denied=new java.util.HashSet<>(),broken=new java.util.HashSet<>();public boolean throwNext;public Runnable onAfter=()->{};public class_3225(class_3222 p){player=p;}public boolean method_14266(class_2338 pos){local.prominence.hammercompat.HammerBreaks.begin();local.prominence.hammercompat.HammerBreaks.capture(player.world,pos,player.world.method_8320(pos),player.stack,player);if(throwNext){throwNext=false;throw new IllegalStateException("fixture callback");}before++;if(denied.contains(pos))return false;class_2680 s=player.world.method_8320(pos);if(s.air)return false;player.world.blocks.put(pos.method_10062(),new class_2680(true));if(!broken.add(pos.method_10062()))throw new AssertionError("Duplicate break");after++;if(!player.creative){drops++;player.stack.damage++;}onAfter.run();local.prominence.hammercompat.HammerBreaks.finish(player,pos,true);return true;}}''',
'pro/mikey/justhammers/Config.java': '''public enum Config {INSTANCE;public ConfigData config=new ConfigData(false,false);public static class ConfigData {public boolean breaking;public ConfigData(boolean b,boolean d){breaking=b;}public boolean allowBreaking(){return breaking;}}}''',
'pro/mikey/justhammers/HammerItem.java': '''public class HammerItem extends net.minecraft.class_1792 implements local.prominence.hammercompat.mixin.HammerAccess {public int radius=3,depth=1;public int getRadius(){return radius;}public int getDepth(){return depth;}public boolean actualIsCorrectToolForDrops(net.minecraft.class_2680 s){return s.correct;}public boolean prominence$canDestroy(net.minecraft.class_2680 s,net.minecraft.class_1937 w,net.minecraft.class_2338 p){return !s.air&&s.allowed&&s.hardness>0;}public static net.minecraft.class_3341 getAreaOfEffect(net.minecraft.class_2338 p,net.minecraft.class_2350 f,int r,int d){var box=new net.minecraft.class_3341();int n=r/2;for(int a=-n;a<=n;a++)for(int b=-n;b<=n;b++)for(int c=0;c<d;c++){int x=p.x,y=p.y,z=p.z;switch(f){case UP:y+=c;x+=a;z+=b;break;case DOWN:y-=c;x+=a;z+=b;break;case NORTH:z-=c;x+=a;y+=b;break;case SOUTH:z+=c;x+=a;y+=b;break;case EAST:x+=c;y+=a;z+=b;break;case WEST:x-=c;y+=a;z+=b;break;}box.positions.add(new net.minecraft.class_2338(x,y,z));}return box;}}'''
}
TEST=r'''
import net.minecraft.*;
import pro.mikey.justhammers.*;
import local.prominence.hammercompat.HammerBreaks;
public class HammerBehaviorTests {
 static int cases;
 static class_2338 origin=new class_2338(0,64,0);
 static class_3222 fixture(){HammerBreaks.begin();Config.INSTANCE.config.breaking=false;var p=new class_3222();p.stack=new class_1799(new HammerItem());populate(p);return p;}
 static void populate(class_3222 p){var h=(HammerItem)p.stack.item;for(var pos:HammerItem.getAreaOfEffect(origin,p.hit.face,h.radius,h.depth).positions)p.world.blocks.put(pos,new class_2680());}
 static void queue(class_3222 p){HammerBreaks.begin();HammerBreaks.capture(p.world,origin,new class_2680(),p.stack,p);}
 static void complete(class_3222 p){queue(p);p.world.blocks.put(origin,new class_2680(true));HammerBreaks.finish(p,origin,true);}
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static void test(String n,Runnable r){r.run();cases++;System.out.println("PASS "+n);}
 public static void main(String[] args){
  test("each of eight neighbors receives one normal break and drop",()->{var p=fixture();complete(p);check(p.field_13974.before==8&&p.field_13974.after==8&&p.field_13974.drops==8,"events/drop count");check(p.stack.damage==8,"per-block wear");check(!p.field_13974.broken.contains(origin),"origin duplicated");});
  test("canceled source has no area side effects",()->{var p=fixture();queue(p);HammerBreaks.finish(p,origin,false);check(p.field_13974.before==0,"source cancellation");});
  test("source absent queue does nothing",()->{var p=fixture();HammerBreaks.finish(p,origin,true);check(p.field_13974.before==0,"unrelated tool");});
  test("protected neighbors do not drop or complete events",()->{var p=fixture();p.field_13974.denied.add(new class_2338(-1,64,-1));complete(p);check(p.field_13974.before==8&&p.field_13974.after==7&&p.field_13974.drops==7,"protected target");});
  test("recursive source capture and nested completion do not expand area",()->{var p=fixture();p.field_13974.onAfter=()->{check(HammerBreaks.active(),"not scoped");HammerBreaks.capture(p.world,origin,new class_2680(),p.stack,p);HammerBreaks.finish(p,origin,true);};complete(p);check(p.field_13974.after==8&&!HammerBreaks.active(),"recursive expansion");});
  test("exception clears active and pending scopes",()->{var p=fixture();p.field_13974.throwNext=true;boolean caught=false;try{complete(p);}catch(IllegalStateException e){caught=true;}check(caught&&!HammerBreaks.active(),"scope leaked");var q=fixture();complete(q);check(q.field_13974.after==8,"subsequent break poisoned");});
  test("tool switch from callback stops further area",()->{var p=fixture();p.field_13974.onAfter=()->p.stack=new class_1799(new class_1792());complete(p);check(p.field_13974.after==1,"continued with wrong tool");});
  test("dimension switch from callback stops further area",()->{var p=fixture();p.field_13974.onAfter=()->p.world=new class_3218();complete(p);check(p.field_13974.after==1,"cross-world mining");});
  test("empty stack stops area",()->{var p=fixture();p.field_13974.onAfter=()->p.stack.empty=true;complete(p);check(p.field_13974.after==1,"empty tool continued");});
  test("reserve durability respected and original warning retained",()->{var p=fixture();p.stack.damage=996;complete(p);check(p.field_13974.after==2&&p.stack.damage==998&&p.warnings==1,"durability guard/warning");});
  test("creative area has events without drops or wear",()->{var p=fixture();p.creative=true;p.stack.damage=999;complete(p);check(p.field_13974.after==8&&p.field_13974.drops==0&&p.stack.damage==999,"creative behavior");});
  test("unloaded neighbor chunks never load or mine",()->{var p=fixture();p.world.unloaded.add(-1);complete(p);check(p.field_13974.after==5&&p.world.loads==0,"unloaded neighbor");});
  test("unsafe and wrong-tier targets remain untouched",()->{var p=fixture();p.world.blocks.get(new class_2338(-1,64,-1)).allowed=false;p.world.blocks.get(new class_2338(-1,64,0)).correct=false;complete(p);check(p.field_13974.after==6,"filters");});
  test("sneaking leaves area disabled",()->{var p=fixture();p.crouch=true;complete(p);check(p.field_13974.before==0,"crouch");});
  test("client world cannot queue",()->{var p=fixture();p.world.field_9236=true;complete(p);check(p.field_13974.before==0,"client");});
  test("zero hardness source cannot queue",()->{var p=fixture();var s=new class_2680();s.hardness=0;HammerBreaks.capture(p.world,origin,s,p.stack,p);HammerBreaks.finish(p,origin,true);check(p.field_13974.before==0,"zero source");});
  test("different player cannot consume another player's request",()->{var p=fixture();queue(p);var q=new class_3222();q.stack=p.stack;HammerBreaks.finish(q,origin,true);check(q.field_13974.before==0,"identity");});
  test("different origin cannot consume request",()->{var p=fixture();queue(p);HammerBreaks.finish(p,new class_2338(1,64,0),true);check(p.field_13974.before==0,"origin");});
  test("next unrelated attempt clears stale request",()->{var p=fixture();queue(p);HammerBreaks.begin();HammerBreaks.finish(p,origin,true);check(p.field_13974.before==0,"stale request");});
  test("other hammer sizes and all six faces",()->{for(var face:class_2350.values()){var p=fixture();var h=(HammerItem)p.stack.item;h.radius=5;h.depth=2;p.hit.face=face;populate(p);complete(p);check(p.field_13974.after==49,"size/face "+face);}});
  System.out.println("PASS: "+cases+" behavior scenarios using production helper, isolated API fixture; no game/world launched");
 }
}
'''
def main():
    a=argparse.ArgumentParser()
    for n in ['jar','java-home','output']:a.add_argument('--'+n,type=Path,required=True)
    args=a.parse_args();out=args.output.resolve();src=out/'src';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
    for name,body in STUBS.items():
        p=src/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text('package '+name.rsplit('/',1)[0].replace('/','.')+';\n'+body+'\n','utf8')
    (src/'HammerBehaviorTests.java').write_text(TEST,'utf8')
    params=['--release','17','-proc:none','-classpath',str(args.jar.resolve()),'-d',str(classes),*map(str,src.rglob('*.java'))]
    argfile=out/'compile.args';argfile.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in params),'utf8')
    subprocess.run([str(args.java_home/'bin/javac.exe'),'-J-Xmx256m','@'+str(argfile)],check=True)
    result=subprocess.run([str(args.java_home/'bin/java.exe'),'-Xmx128m','-ea','-cp',str(classes)+';'+str(args.jar.resolve()),'HammerBehaviorTests'],capture_output=True,text=True,encoding='utf8',errors='replace')
    (out/'behavior.log').write_text(result.stdout+result.stderr,'utf8');assert result.returncode==0,result.stdout+result.stderr
    print(result.stdout)
if __name__=='__main__':main()
