package net.earthcomputer.antixray.mixin;

import net.earthcomputer.antixray.IPalettedContainer;
import net.earthcomputer.antixray.XrayUtil;
import net.minecraft.util.PackedIntegerArray;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PalettedContainer.class)
public abstract class MixinPalettedContainer<T> implements IPalettedContainer<T> {

    @Shadow protected PackedIntegerArray data;

    @Redirect(method = "toPacket", at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/PalettedContainer;data:Lnet/minecraft/util/PackedIntegerArray;"))
    public PackedIntegerArray transformData(PalettedContainer<T> _this) {
        if (XrayUtil.getSendingChunk() == null || XrayUtil.getSendingChunkSection() == null)
            return data;
        else
            return XrayUtil.transformChunkSectionData(XrayUtil.getSendingChunk(), XrayUtil.getSendingChunkSection(), data);
    }

    @Accessor
    @Override
    public abstract Palette<T> getPalette();
}
