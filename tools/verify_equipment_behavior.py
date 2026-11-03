"""Execute production CriticalStats with bounded attribute/API doubles, no game."""
from pathlib import Path
import argparse,subprocess
ROOT=Path(__file__).resolve().parents[1]
def main():
 p=argparse.ArgumentParser()
 for n in ('jar','java-home','output'):p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();out=a.output.resolve();out.mkdir(parents=True,exist_ok=True)
 sources={
 'net/minecraft/class_1320.java':'package net.minecraft; public class class_1320 {}',
 'net/minecraft/class_1309.java':'package net.minecraft; public class class_1309 {public double raw=100; public double chance=.05; public int reads; public double method_26825(class_1320 a){reads++;return raw;}}',
 'net/spell_power/internals/CustomEntityAttribute.java':'package net.spell_power.internals; public class CustomEntityAttribute extends net.minecraft.class_1320 {}',
 'net/spell_power/api/SpellSchool.java':'package net.spell_power.api; public class SpellSchool {}',
 'net/spell_power/api/SpellPowerMechanics.java':'package net.spell_power.api; public class SpellPowerMechanics {public static final Entry CRITICAL_CHANCE=new Entry(); public static class Entry{public final net.spell_power.internals.CustomEntityAttribute attribute=new net.spell_power.internals.CustomEntityAttribute();}}',
 'net/spell_power/api/SpellSchools.java':'package net.spell_power.api; public class SpellSchools {public static final SpellSchool FROST=new SpellSchool();}',
 'net/spell_power/api/SpellPower.java':'package net.spell_power.api; public class SpellPower {public record Result(double criticalChance){} public static Result getSpellPower(SpellSchool school,net.minecraft.class_1309 e){if(school!=SpellSchools.FROST)throw new AssertionError();return new Result(e.chance);}}',
 'Test.java':'''import local.prominence.equipment.CriticalStats;
 public class Test {
 static int checks;static void eq(double a,double b){if(Math.abs(a-b)>1e-9)throw new AssertionError(a+" != "+b);checks++;}
 public static void main(String[] args){var e=new net.minecraft.class_1309();var crit=net.spell_power.api.SpellPowerMechanics.CRITICAL_CHANCE.attribute;
 // Old native code mistakes the raw 100 baseline for probability 1.
 eq(e.method_26825(crit)/100,1);eq(CriticalStats.attributeValue(e,crit)/100,.05);
 for(double c:new double[]{0,.05,.25,.50,1,1.5}){e.chance=c;eq(CriticalStats.attributeValue(e,crit)/100,c);eq(CriticalStats.attributeValue(e,crit)/100/2,c/2);}
 e.raw=175;eq(CriticalStats.attributeValue(e,new net.minecraft.class_1320()),175);
 e.chance=.25;int reads=e.reads;eq(CriticalStats.attributeValue(e,crit),25);eq(e.reads,reads);
 eq(CriticalStats.percentUnits(-.2),0);eq(CriticalStats.percentUnits(Double.NaN),0);eq(CriticalStats.percentUnits(Double.POSITIVE_INFINITY),0);
 System.out.println("PASS: "+checks+" production critical-stat checks; native baseline bug, stat changes, half chance and unrelated attributes");}}
 '''}
 files=[]
 for n,s in sources.items():
  f=out/'src'/n;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(s);files.append(str(f))
 options=['--release','17','-proc:none','-cp',str(a.jar.resolve()),'-d',str(out/'classes'),*files]
 arg=out/'compile.args';arg.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in options))
 subprocess.run([str(a.java_home/'bin/javac.exe'),'@'+str(arg)],check=True)
 run=subprocess.run([str(a.java_home/'bin/java.exe'),'-Xverify:all','-cp',str(out/'classes')+';'+str(a.jar.resolve()),'Test'],capture_output=True,text=True)
 (out/'behavior.log').write_text(run.stdout+run.stderr);assert run.returncode==0,run.stdout+run.stderr;print(run.stdout)
if __name__=='__main__':main()
