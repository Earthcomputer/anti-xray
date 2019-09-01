package net.earthcomputer.antixray.mixin;

import net.earthcomputer.antixray.XrayUtil;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSection.class)
public class MixinChunkSection {

    @Inject(method = "toPacket", at = @At("HEAD"))
    public void onStartToPacket(PacketByteBuf buf, CallbackInfo ci) {
        if (XrayUtil.getSendingChunk() != null)
            XrayUtil.setSendingChunkSection((ChunkSection) (Object) this);
    }

    @Inject(method = "toPacket", at = @At("RETURN"))
    public void onEndToPacket(PacketByteBuf buf, CallbackInfo ci) {
        if (XrayUtil.getSendingChunk() != null)
            XrayUtil.setSendingChunkSection(null);
    }

}
