"""Exhaustively compare reduced POI bounds with native Magnum Torch predicates."""
from pathlib import Path
import argparse,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
JAVA=r'''import java.util.*;
import fuzs.magnumtorch.config.SpawnShapeType;
import local.prominence.torchsearch.SearchRadius;
public class VerifyTorchNative {
 static long points,protectedPoints;static int checks;
 static void check(boolean b){if(!b)throw new AssertionError("check "+checks);checks++;}
 static int original(int h,int v){return (int)Math.ceil(Math.max(h,v)*1.7320508076);}
 static void verify(SpawnShapeType shape,int h,int v,int x,int y,int z){
  int old=original(h,v),now=SearchRadius.tightened(old,shape,h,v);long distance=(long)x*x+(long)y*y+(long)z*z;points++;
  checkRange(now,old);
  if(distance<=(long)old*old&&shape.isPositionContained(x,y,z,h,v)){
   protectedPoints++;if(distance>(long)now*now)throw new AssertionError("protection lost "+shape+" "+h+"/"+v+" at "+x+","+y+","+z);
  }
 }
 static void checkRange(int now,int old){if(now<0||now>old)throw new AssertionError("search expanded");}
 public static void main(String[] args){
  check(SearchRadius.tightened(111,SpawnShapeType.ELLIPSOID,64,32)==65);
  check(SearchRadius.tightened(222,SpawnShapeType.CUBOID,128,64)==193);
  check(SearchRadius.tightened(111,SpawnShapeType.CYLINDER,64,32)==73);
  // Exhaust every nonnegative point in the old query cube. Native callers use absolute deltas.
  for(var shape:SpawnShapeType.values()){
   int h=shape==SpawnShapeType.CUBOID?128:64,v=shape==SpawnShapeType.CUBOID?64:32,old=original(h,v);
   for(int x=0;x<=old;x++)for(int y=0;y<=old;y++)for(int z=0;z<=old;z++)verify(shape,h,v,x,y,z);
  }
  Random random=new Random(20261103);
  for(var shape:SpawnShapeType.values())for(int h:new int[]{1,2,3,16,32,64,128,256,4096})for(int v:new int[]{1,2,3,16,32,64,128,256,4096}){
   int old=original(h,v);
   for(int i=0;i<3000;i++)verify(shape,h,v,random.nextInt(old+1),random.nextInt(old+1),random.nextInt(old+1));
   for(int x:new int[]{0,h-1,h,h+1})for(int y:new int[]{0,v-1,v,v+1})for(int z:new int[]{0,h-1,h,h+1})verify(shape,h,v,x,y,z);
  }
  for(var shape:SpawnShapeType.values()){
   for(int[] pair:new int[][]{{0,0},{0,64},{64,0},{-1,64},{64,-1},{4097,64},{Integer.MAX_VALUE,64}})
    check(SearchRadius.tightened(123,shape,pair[0],pair[1])==123);
   check(SearchRadius.tightened(0,shape,64,32)==0);
  }
  check(SearchRadius.tightened(123,null,64,32)==123);check(SearchRadius.forConfig(123,null)==123);
  // No cached config: alternating ranges and shapes returns current bounds each time.
  for(int i=0;i<100;i++){check(SearchRadius.tightened(111,SpawnShapeType.ELLIPSOID,64,32)==65);check(SearchRadius.tightened(222,SpawnShapeType.CUBOID,128,64)==193);}
  System.out.println("PASS: "+points+" native shape points ("+protectedPoints+" protected) and "+checks+" bounds/fallback/live-config checks; no protection false negatives; no Minecraft/world launched");
 }
}'''
def main():
 a=argparse.ArgumentParser()
 for n in ('jar','classpath','java-home','mods','output'):a.add_argument('--'+n,type=Path,required=True)
 args=a.parse_args();out=args.output.resolve();out.mkdir(parents=True,exist_ok=True)
 java=out/'VerifyTorchNative.java';java.write_text(JAVA,encoding='utf-8')
 cp=';'.join([str(out),str(args.jar.resolve()),*[str(p.resolve()) for p in args.mods.glob('*.jar')],*json.loads(args.classpath.read_text())])
 opts=['--release','17','-proc:none','-cp',cp,'-d',str(out),str(java)]
 f=out/'compile.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in opts))
 subprocess.run([str(args.java_home/'bin/javac.exe'),'@'+str(f)],check=True)
 f=out/'run.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in ['-Xmx512m','-Xverify:all','-cp',cp,'VerifyTorchNative']))
 p=subprocess.run([str(args.java_home/'bin/java.exe'),'@'+str(f)],capture_output=True,text=True)
 (out/'geometry.log').write_text(p.stdout+p.stderr);assert p.returncode==0,p.stdout+p.stderr;print(p.stdout)
if __name__=='__main__':main()
