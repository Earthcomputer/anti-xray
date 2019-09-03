package net.earthcomputer.antixray.mixin;

import net.earthcomputer.antixray.IPalettedContainer;
import net.earthcomputer.antixray.XrayUtil;
import net.minecraft.util.PackedIntegerArray;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PalettedContainer.class)
public abstract class MixinPalettedContainer<T> implements IPalettedContainer<T> {

    @Shadow protected PackedIntegerArray data;
    @Shadow private Palette<T> palette;
    @Shadow private int paletteSize;

    @Unique private PackedIntegerArray mixedData;
    @Unique private Palette<T> oldPalette;

    @Redirect(method = "toPacket", at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/PalettedContainer;data:Lnet/minecraft/util/PackedIntegerArray;"))
    public PackedIntegerArray transformData(PalettedContainer<T> _this) {
        if (XrayUtil.getSendingChunk() == null || XrayUtil.getSendingChunkSection() == null)
            return data;
        else if (mixedData != null)
            return mixedData;
        else
            return mixedData = XrayUtil.transformChunkSectionData(XrayUtil.getSendingChunk(), XrayUtil.getSendingChunkSection(), data);
    }

    @Inject(method = "onResize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/PalettedContainer;setPaletteSize(I)V"))
    private void captureOldPalette(int newSize, T newObject, CallbackInfoReturnable<Integer> ci) {
        oldPalette = palette;
    }

    @Inject(method = "onResize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Palette;getIndex(Ljava/lang/Object;)I"))
    private void remapMixedData(int newSize, T newObject, CallbackInfoReturnable<Integer> ci) {
        if (mixedData != null) {
            PackedIntegerArray oldMixedData = mixedData;
            mixedData = new PackedIntegerArray(paletteSize, 4096);
            for (int i = 0; i < mixedData.getSize(); i++) {
                T obj = oldPalette.getByIndex(oldMixedData.get(i));
                if (obj != null)
                    mixedData.set(i, palette.getIndex(obj));
            }
        }
        oldPalette = null;
    }

    @Accessor
    @Override
    public abstract Palette<T> getPalette();
}
