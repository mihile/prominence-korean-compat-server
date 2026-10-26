"""Reproduce the native area-radius NPE; execute production repair and native power/context/radius code in an API fixture."""
from pathlib import Path
import argparse,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
STUBS={
'net/minecraft/class_1937.java':'public class class_1937 {public boolean field_9236;}',
'net/minecraft/class_1297.java':'public class class_1297 {public class_1937 world=new class_1937();public class_1937 method_37908(){return world;}}',
'net/minecraft/class_1309.java':'public class class_1309 extends class_1297 {}',
'net/minecraft/class_1665.java':'public class class_1665 extends class_1297 {}',
'net/minecraft/class_243.java':'public class class_243 {}',
'net/spell_power/api/SpellSchool.java':'''public class SpellSchool {public enum Trait {POWER,CRIT_CHANCE,CRIT_DAMAGE,HASTE} public record QueryArgs(net.minecraft.class_1309 entity){} public int queries;public double getValue(Trait trait,QueryArgs args){queries++;return switch(trait){case POWER->12.0;case CRIT_CHANCE->0.25;case CRIT_DAMAGE->1.8;case HASTE->1.0;};}}'''
}
TEST='''
import net.minecraft.*;
import net.spell_engine.api.spell.*;
import net.spell_engine.internals.SpellHelper.ImpactContext;
import net.spell_engine.utils.TargetHelper;
import net.spell_power.api.*;
import local.prominence.arrowtick.ArrowImpactPower;
public final class ArrowPowerTests {
 static int checks;static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
 public static void main(String[] args){
  var school=new SpellSchool();var spell=new Spell();spell.school=school;var info=new SpellInfo(spell,null);
  var caster=new class_1309();var arrow=new class_1665();var pos=new class_243();
  var context=new ImpactContext(0.3f,0.4f,pos,null,TargetHelper.TargetingMode.AREA);
  var area=new Spell.AreaImpact();area.radius=2;area.extra_radius.power_coefficient=0.5f;area.extra_radius.power_cap=10;spell.area_impact=area;
  boolean failed=false;try{area.combinedRadius(context.power());}catch(NullPointerException e){failed=true;}
  check(failed,"original native area crash not reproduced");
  var fixed=ArrowImpactPower.complete(context,caster,arrow,info);
  check(fixed!=context&&context.power()==null,"immutable original context changed");
  check(fixed.power()!=null&&fixed.power().baseValue()==12.0&&school.queries==3,"native power resolver traits not used");
  check(fixed.power().school()==school&&fixed.power().criticalChance()==0.25&&fixed.power().criticalDamage()==1.8,"native power data lost");
  check(fixed.position()==pos&&fixed.channel()==0.3f&&fixed.distance()==0.4f&&fixed.targetingMode()==TargetHelper.TargetingMode.AREA,"context components changed");
  check(area.combinedRadius(fixed.power())==7.0f,"native radius coefficient/cap changed");
  check(ArrowImpactPower.complete(fixed,caster,arrow,info)==fixed&&school.queries==3,"existing power recomputed");
  check(ArrowImpactPower.complete(null,caster,arrow,info)==null,"null context");
  check(ArrowImpactPower.complete(context,null,arrow,info)==context,"absent caster");
  check(ArrowImpactPower.complete(context,caster,new class_1297(),info)==context,"non-arrow projectile changed");
  check(ArrowImpactPower.complete(context,caster,null,info)==context,"null projectile");
  arrow.world.field_9236=true;check(ArrowImpactPower.complete(context,caster,arrow,info)==context,"client changed");arrow.world.field_9236=false;
  check(ArrowImpactPower.complete(context,caster,arrow,null)==context,"absent info");
  check(ArrowImpactPower.complete(context,caster,arrow,new SpellInfo(null,null))==context,"absent spell");
  spell.area_impact=null;check(ArrowImpactPower.complete(context,caster,arrow,info)==context,"unaffected non-area spell changed");spell.area_impact=area;
  spell.school=null;check(ArrowImpactPower.complete(context,caster,arrow,info)==context,"absent school");
  System.out.println("PASS: "+checks+" arrow checks; native original radius NPE reproduced; native SpellPower.getSpellPower, Result, ImpactContext and AreaImpact executed; remaining world/school APIs are fixture stubs");
 }
}
'''
def main():
 a=argparse.ArgumentParser()
 for n in ['jar','classpath','java-home','output','mods']:a.add_argument('--'+n,type=Path,required=True)
 args=a.parse_args();out=args.output.resolve();src=out/'src';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
 for name,body in STUBS.items():
  p=src/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text('package '+name.rsplit('/',1)[0].replace('/','.')+';\n'+body+'\n','utf8')
 (src/'ArrowPowerTests.java').write_text(TEST,'utf8')
 cp=';'.join([str(args.jar.resolve()),str((args.mods/'spell_engine-0.15.12+1.20.1.jar').resolve()),str((args.mods/'spell_power-0.12.0+1.20.1.jar').resolve()),*json.loads(args.classpath.read_text('utf8'))])
 params=['--release','17','-proc:none','-cp',cp,'-d',str(classes),*map(str,src.rglob('*.java'))]
 f=out/'compile.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in params),'utf8');subprocess.run([str(args.java_home/'bin/javac.exe'),'-J-Xmx256m','@'+str(f)],check=True)
 runtime=['-Xmx256m','-ea','-cp',str(classes)+';'+cp,'ArrowPowerTests'];rf=out/'runtime.args';rf.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in runtime),'utf8')
 p=subprocess.run([str(args.java_home/'bin/java.exe'),'@'+str(rf)],capture_output=True,text=True,encoding='utf8',errors='replace');(out/'power.log').write_text(p.stdout+p.stderr,'utf8');assert p.returncode==0,p.stdout+p.stderr;print(p.stdout)
if __name__=='__main__':main()
