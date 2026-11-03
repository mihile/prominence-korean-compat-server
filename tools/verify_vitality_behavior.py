"""Run the actual packaged Mixin handlers against tiny API fixtures; no game launch."""
from pathlib import Path
import argparse,json,subprocess,zipfile
ROOT=Path(__file__).resolve().parents[1]
STUBS={
'net/minecraft/class_2960.java':'''package net.minecraft; public class class_2960 {private final String a,b;public class_2960(String a,String b){this.a=a;this.b=b;}public String method_12836(){return a;}public String method_12832(){return b;}}''',
'net/minecraft/class_1291.java':'''package net.minecraft; public class class_1291 {public final class_2960 id;public class_1291(class_2960 id){this.id=id;}}''',
'net/minecraft/class_1293.java':'''package net.minecraft; public class class_1293 {private final class_1291 e;private final int a;public class_1293(class_1291 e,int a){this.e=e;this.a=a;}public class_1291 method_5579(){return e;}public int method_5578(){return a;}}''',
'net/minecraft/class_2378.java':'''package net.minecraft; public interface class_2378 {class_2960 method_10221(Object e);}''',
'net/minecraft/class_7923.java':'''package net.minecraft; public class class_7923 {public static final class_2378 field_41174=e->((class_1291)e).id;}''',
'net/minecraft/class_1297.java':'''package net.minecraft; public class class_1297 {}''',
'net/minecraft/class_3222.java':'''package net.minecraft; public class class_3222 extends class_1297 {}''',
'net/minecraft/class_1937.java':'''package net.minecraft; public class class_1937 {}''',
'net/minecraft/class_1799.java':'''package net.minecraft; public class class_1799 {public class_1293 effect;public int count=1;public class_1799(class_1293 e){effect=e;}}''',
'dev/shadowsoffire/apotheosis/potion/PotionCharmItem.java':'''package dev.shadowsoffire.apotheosis.potion; import net.minecraft.*;public class PotionCharmItem {public static boolean hasEffect(class_1799 s){return s.effect!=null;}public static class_1293 getEffect(class_1799 s){return s.effect;}}''',
'VitalityBehavior.java':r'''import java.lang.reflect.*;
import net.minecraft.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import local.prominence.vitalitybalance.mixin.*;
public class VitalityBehavior {
 static int count;
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);count++;}
 static class Dummy extends VitalityCharmPreserveMixin {}
 public static void main(String[] a)throws Exception {
  Method preserve=VitalityCharmPreserveMixin.class.getDeclaredMethod("prominence$preserveRestrictedCharm",class_1799.class,class_1937.class,class_1297.class,int.class,boolean.class,CallbackInfo.class);preserve.setAccessible(true);
  Method limit=VitalityCharmMatcherMixin.class.getDeclaredMethod("prominence$limitVitality",class_2960.class,int.class,CallbackInfoReturnable.class);limit.setAccessible(true);
  var vitality=new class_2960("zenith_attributes","vitality");var other=new class_2960("minecraft","speed");
  for(var id:new class_2960[]{vitality,other})for(int amp=0;amp<4;amp++) {
   boolean restricted=id==vitality&&amp==2;
   var stack=new class_1799(new class_1293(new class_1291(id),amp));var ci=new CallbackInfo("charmLogic",true);
   preserve.invoke(new Dummy(),stack,new class_1937(),new class_3222(),0,false,ci);
   check(ci.isCancelled()==restricted,"preserve matcher "+amp);check(stack.count==1,"item count changed");
   var cir=new CallbackInfoReturnable<Boolean>("isDisabled",true);limit.invoke(null,id,amp,cir);
   check(cir.isCancelled()==restricted,"recipe matcher "+amp);
   if(restricted)check(Boolean.TRUE.equals(cir.getReturnValue()),"restricted craft accepted");
  }
  var noEffect=new class_1799(null);var ci=new CallbackInfo("charmLogic",true);
  preserve.invoke(new Dummy(),noEffect,new class_1937(),new class_3222(),0,false,ci);check(!ci.isCancelled(),"empty charm modified");
  var npc=new class_1799(new class_1293(new class_1291(vitality),2));ci=new CallbackInfo("charmLogic",true);
  preserve.invoke(new Dummy(),npc,new class_1937(),new class_1297(),0,false,ci);check(!ci.isCancelled()&&npc.count==1,"nonplayer changed");
  System.out.println("PASS: "+count+" actual Mixin-handler checks; restricted player charm preserved and made inactive; other charms unchanged");
 }
}'''}
def main():
 p=argparse.ArgumentParser()
 for n in ['jar','java-home','output','mixin']:p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();out=a.output.resolve();src=out/'src';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
 for n,s in STUBS.items():
  f=src/n;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(s,encoding='utf8')
 cp=str(a.jar.resolve())+';'+str(a.mixin.resolve());options=['--release','17','-proc:none','-cp',cp,'-d',str(classes),*[str(src/n) for n in STUBS]]
 f=out/'javac.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in options),'utf8')
 subprocess.run([str(a.java_home/'bin/javac.exe'),'@'+str(f)],check=True)
 r=subprocess.run([str(a.java_home/'bin/java.exe'),'-Xmx128m','-Xverify:all','-cp',str(classes)+';'+cp,'VitalityBehavior'],cwd=out,capture_output=True,text=True,encoding='utf8',errors='replace')
 (out/'behavior.log').write_text(r.stdout+r.stderr,'utf8');assert r.returncode==0,r.stdout+r.stderr;print(r.stdout)
if __name__=='__main__':main()
