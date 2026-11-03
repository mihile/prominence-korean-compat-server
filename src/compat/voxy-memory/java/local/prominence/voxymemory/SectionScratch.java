package local.prominence.voxymemory;
import me.cortex.voxy.common.world.WorldSection;
public final class SectionScratch {
 private static final ThreadLocal<long[]> COPY=ThreadLocal.withInitial(()->new long[WorldSection.SECTION_VOLUME]);
 private SectionScratch(){}
 public static long[] copy(WorldSection section){long[] data=COPY.get();section.copyDataTo(data);return data;}
 public static int cacheLimit(int original){
  int cap=Integer.getInteger("prominence.voxy.secondaryCacheSections",512);
  if(cap<=0)return original;
  return Math.min(original,Math.max(64,Math.min(cap,2048)));
 }
}
