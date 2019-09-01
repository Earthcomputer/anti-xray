package net.earthcomputer.antixray.mixin;

import net.earthcomputer.antixray.XrayUtil;
import net.minecraft.client.network.packet.ChunkDataS2CPacket;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkDataS2CPacket.class)
public class MixinChunkDataPacket {

    @Inject(method = "writeData", at = @At("HEAD"))
    public void onStartWriteData(PacketByteBuf buf, WorldChunk chunk, int flags, CallbackInfoReturnable<Integer> ci) {
        XrayUtil.setSendingChunk(chunk);
    }

    @Inject(method = "writeData", at = @At("RETURN"))
    public void onEndWriteData(PacketByteBuf buf, WorldChunk chunk, int flags, CallbackInfoReturnable<Integer> ci) {
        XrayUtil.setSendingChunk(null);
    }

}
