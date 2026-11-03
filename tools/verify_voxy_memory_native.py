"""Exercise native Voxy copies and eviction with synthetic section data, no world."""
from pathlib import Path
import argparse,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
JAVA=r'''import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.lang.management.ManagementFactory;
import me.cortex.voxy.common.world.*;
import local.prominence.voxymemory.SectionScratch;
public class VerifyScratchNative {
 static volatile Object sink;static int checks;
 static void check(boolean x){if(!x)throw new AssertionError("check "+checks);checks++;}
 public static void main(String[]args)throws Exception{
  check(SectionScratch.cacheLimit(2048)==512);check(SectionScratch.cacheLimit(128)==128);
  System.setProperty("prominence.voxy.secondaryCacheSections","256");check(SectionScratch.cacheLimit(2048)==256);
  System.setProperty("prominence.voxy.secondaryCacheSections","0");check(SectionScratch.cacheLimit(2048)==2048);
  System.clearProperty("prominence.voxy.secondaryCacheSections");
  AtomicInteger loads=new AtomicInteger();
  ActiveSectionTracker tracker=new ActiveSectionTracker(6,s->{loads.incrementAndGet();Arrays.fill(s._unsafeGetRawDataArray(),s.key);return 0;},SectionScratch.cacheLimit(2048));
  WorldSection active=tracker.acquire(0,0,0,1,false);
  for(int i=0;i<100;i++){long[] raw=active._unsafeGetRawDataArray();Arrays.fill(raw,i*987654321L);long[] original=active.copyData();long[] scratch=SectionScratch.copy(active);check(Arrays.equals(original,scratch));check(scratch!=raw);check(scratch==SectionScratch.copy(active));}
  long[] first=SectionScratch.copy(active);long[] unchanged=first.clone();
  WorldSection second=tracker.acquire(0,0,0,2,false);Arrays.fill(second._unsafeGetRawDataArray(),17);
  AtomicReference<long[]> other=new AtomicReference<>();Thread t=new Thread(()->other.set(SectionScratch.copy(second)));t.start();t.join();
  check(first!=other.get());check(Arrays.equals(first,unchanged));check(other.get()[0]==17);second.release();
  var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long tid=Thread.currentThread().getId();
  for(int i=0;i<100;i++){sink=active.copyData();sink=SectionScratch.copy(active);}
  long before=bean.getThreadAllocatedBytes(tid);for(int i=0;i<1000;i++)sink=active.copyData();long baseline=bean.getThreadAllocatedBytes(tid)-before;
  before=bean.getThreadAllocatedBytes(tid);for(int i=0;i<1000;i++)sink=SectionScratch.copy(active);long patched=bean.getThreadAllocatedBytes(tid)-before;
  check(baseline>=1000L*32768*8);check(patched<baseline/100);
  for(int i=3;i<800;i++)tracker.acquire(0,0,0,i,false).release();
  check(tracker.getSecondaryCacheSize()<=512);check(tracker.getLoadedCacheCount()==1);check(active.getRefCount()>0);check(active._unsafeGetRawDataArray()[0]==unchanged[0]);
  int old=loads.get();WorldSection reread=tracker.acquire(0,0,0,2,false);check(loads.get()>old);check(reread._unsafeGetRawDataArray()[0]==reread.key);reread.release();active.release();
  System.out.println("PASS: "+checks+" native copy/thread-isolation/cache-eviction/reload checks; baseline allocated="+baseline+" bytes, scratch="+patched+" bytes /1000 copies; no Minecraft or world launched");
 }
}'''
def main():
 a=argparse.ArgumentParser()
 for n in ('jar','classpath','java-home','mods','output'):a.add_argument('--'+n,type=Path,required=True)
 args=a.parse_args();out=args.output.resolve();out.mkdir(parents=True,exist_ok=True)
 java=out/'VerifyScratchNative.java';java.write_text(JAVA,encoding='utf-8')
 cp=';'.join([str(out),str(args.jar.resolve()),*[str(p.resolve()) for p in args.mods.glob('*.jar')],*json.loads(args.classpath.read_text())])
 opts=['--release','17','-proc:none','-cp',cp,'-d',str(out),str(java)]
 f=out/'compile.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in opts))
 subprocess.run([str(args.java_home/'bin/javac.exe'),'@'+str(f)],check=True)
 f=out/'run.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in ['-Xmx512m','-Xverify:all','-cp',cp,'VerifyScratchNative']))
 p=subprocess.run([str(args.java_home/'bin/java.exe'),'@'+str(f)],capture_output=True,text=True)
 (out/'native.log').write_text(p.stdout+p.stderr);assert p.returncode==0,p.stdout+p.stderr;print(p.stdout)
if __name__=='__main__':main()
