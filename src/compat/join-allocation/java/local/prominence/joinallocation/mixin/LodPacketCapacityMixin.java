package local.prominence.joinallocation.mixin;
import com.dripps.voxyserver.network.LODBulkPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.class_5455;
import local.prominence.joinallocation.EncodedSize;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(targets="com.dripps.voxyserver.network.PreSerializedLodPayload",remap=false)
public abstract class LodPacketCapacityMixin {
 @Redirect(method="fromBulk",at=@At(value="INVOKE",target="Lio/netty/buffer/Unpooled;buffer(II)Lio/netty/buffer/ByteBuf;"),require=1)
 private static ByteBuf prominence$sizeBuffer(int initial,int maximum,LODBulkPayload bulk,class_5455 registry){
  return Unpooled.buffer(EncodedSize.capacity(initial,maximum,bulk),maximum);
 }
}
