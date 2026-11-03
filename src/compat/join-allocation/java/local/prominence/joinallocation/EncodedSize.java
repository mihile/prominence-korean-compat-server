package local.prominence.joinallocation;
import java.nio.charset.StandardCharsets;
import com.dripps.voxyserver.network.LODBulkPayload;
public final class EncodedSize {
 private EncodedSize(){}
 private static int varIntBytes(int value){int n=1;while((value&~127)!=0){value>>>=7;n++;}return n;}
 public static int capacity(int initial,int maximum,LODBulkPayload bulk){
  try{
   byte[] name=bulk.dimension().toString().getBytes(StandardCharsets.UTF_8);
   long size=varIntBytes(name.length)+name.length+varIntBytes(bulk.sections().size());
   if(bulk.sections().isEmpty()||bulk.sections().size()>16)return initial;
   for(var section:bulk.sections()){
    int[] blocks=section.lutBlockStateIds(),biomes=section.lutBiomeIds();byte[] light=section.lutLight();
    int lut=blocks.length;if(biomes.length!=lut||light.length!=lut)return initial;
    size+=16+varIntBytes(lut);
    for(int i=0;i<lut;i++)size+=varIntBytes(blocks[i])+varIntBytes(biomes[i])+1;
    int indexLen=section.indexArray().length;
    int bits=Math.max(1,32-Integer.numberOfLeadingZeros(Math.max(lut-1,0)));
    int perLong=64/bits;if(perLong==0)return initial;
    size+=varIntBytes(indexLen)+1+((indexLen+(long)perLong-1)/perLong)*8;
    if(size>maximum)return initial;
   }
   return size>=0&&size<=maximum ? Math.max(initial,(int)size):initial;
  }catch(RuntimeException invalid){return initial;}
 }
}
