"""Compare native LOD packet encoding bytes and allocations, no world."""
from pathlib import Path
import argparse,json,subprocess
ROOT=Path(__file__).resolve().parents[1]
JAVA=r'''import java.util.*;
import java.lang.management.ManagementFactory;
import io.netty.buffer.*;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import com.dripps.voxyserver.network.*;
import local.prominence.joinallocation.EncodedSize;
public class VerifyCapacityNative {
 static int checks;static volatile Object sink;
 static void check(boolean v){if(!v)throw new AssertionError("check "+checks);checks++;}
 static byte[] encode(LODBulkPayload p,int capacity){var raw=Unpooled.buffer(capacity,PreSerializedLodPayload.MAX_ENCODED_BYTES);try{var buf=new class_2540(raw);LODBulkPayload.CODEC.encode(buf,p);byte[] b=new byte[buf.readableBytes()];buf.readBytes(b);return b;}finally{raw.release();}}
 static LODBulkPayload payload(int lut,int count,int length){
  var dim=new class_2960("minecraft","overworld");List<LODSectionPayload> list=new ArrayList<>();Random random=new Random(lut);
  for(int c=0;c<count;c++){
   int[] b=new int[lut],bio=new int[lut];byte[] light=new byte[lut];short[] indices=new short[length];
   for(int i=0;i<lut;i++){b[i]=i%3==0?-1:i*128;bio[i]=i*127;light[i]=(byte)random.nextInt();}
   for(int i=0;i<length;i++)indices[i]=(short)random.nextInt(lut);
   list.add(new LODSectionPayload(dim,c,b,bio,light,indices,123456L+c));
  }return new LODBulkPayload(dim,list);
 }
 public static void main(String[] args)throws Exception{
  for(int lut:new int[]{1,2,3,15,16,17,255,256,257,1024,32768})for(int count:new int[]{1,2,16})for(int len:new int[]{1,65,32768}){
   var p=payload(lut,count,len);int cap=EncodedSize.capacity(256,PreSerializedLodPayload.MAX_ENCODED_BYTES,p);check(cap>=256&&cap<=PreSerializedLodPayload.MAX_ENCODED_BYTES);
   byte[] original=null;try{original=encode(p,256);}catch(IndexOutOfBoundsException invalid){check(cap==256);try{encode(p,cap);throw new AssertionError("expected native size failure");}catch(IndexOutOfBoundsException ok){checks++;}continue;}
   byte[] patched=encode(p,cap);check(Arrays.equals(original,patched));check(original.length<=cap);
   var nativePayload=PreSerializedLodPayload.fromBulk(p,null);check(Arrays.equals(original,nativePayload.data()));
   var decoded=new PreSerializedLodPayload(0,0,p.sections().size(),patched).decodeBulk(null);
   check(decoded.sections().size()==count);for(int i=0;i<count;i++){var x=p.sections().get(i);var y=decoded.sections().get(i);check(Arrays.equals(x.indexArray(),y.indexArray()));check(Arrays.equals(x.lutBlockStateIds(),y.lutBlockStateIds()));check(Arrays.equals(x.lutBiomeIds(),y.lutBiomeIds()));check(Arrays.equals(x.lutLight(),y.lutLight()));check(x.contentHash()==y.contentHash());}
  }
  var bench=payload(64,4,32768);int cap=EncodedSize.capacity(256,PreSerializedLodPayload.MAX_ENCODED_BYTES,bench);
  for(int i=0;i<20;i++){sink=encode(bench,256);sink=encode(bench,EncodedSize.capacity(256,PreSerializedLodPayload.MAX_ENCODED_BYTES,bench));}
  var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();bean.setThreadAllocatedMemoryEnabled(true);long tid=Thread.currentThread().getId();
  long start=bean.getThreadAllocatedBytes(tid);for(int i=0;i<100;i++)sink=encode(bench,256);long base=bean.getThreadAllocatedBytes(tid)-start;
  start=bean.getThreadAllocatedBytes(tid);for(int i=0;i<100;i++)sink=encode(bench,EncodedSize.capacity(256,PreSerializedLodPayload.MAX_ENCODED_BYTES,bench));long better=bean.getThreadAllocatedBytes(tid)-start;check(better<base);
  System.out.println("PASS: "+checks+" native packet bytes/roundtrip/capacity/oversize checks; allocated baseline="+base+", pre-sized="+better+" bytes /100 packets; no Minecraft/world launched");
 }
}'''
def main():
 a=argparse.ArgumentParser()
 for n in ('jar','classpath','java-home','mods','output'):a.add_argument('--'+n,type=Path,required=True)
 args=a.parse_args();out=args.output.resolve();out.mkdir(parents=True,exist_ok=True)
 java=out/'VerifyCapacityNative.java';java.write_text(JAVA,encoding='utf-8')
 cp=';'.join([str(out),str(args.jar.resolve()),*[str(p.resolve()) for p in args.mods.glob('*.jar')],*json.loads(args.classpath.read_text())])
 opts=['--release','17','-proc:none','-cp',cp,'-d',str(out),str(java)]
 f=out/'compile.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in opts))
 subprocess.run([str(args.java_home/'bin/javac.exe'),'@'+str(f)],check=True)
 f=out/'run.args';f.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in ['-Xmx512m','-Xverify:all','-cp',cp,'VerifyCapacityNative']))
 p=subprocess.run([str(args.java_home/'bin/java.exe'),'@'+str(f)],capture_output=True,text=True)
 (out/'native.log').write_text(p.stdout+p.stderr);assert p.returncode==0,p.stdout+p.stderr;print(p.stdout)
if __name__=='__main__':main()
