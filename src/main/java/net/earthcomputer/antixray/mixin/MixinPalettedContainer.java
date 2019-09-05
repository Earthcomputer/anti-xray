package net.earthcomputer.antixray.mixin;

import net.earthcomputer.antixray.IPalettedContainer;
import net.earthcomputer.antixray.XrayUtil;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.PackedIntegerArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;
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

    @Shadow private static int toIndex(int int_1, int int_2, int int_3) {return 0;}

    @Unique private PackedIntegerArray mixedData;
    @Unique private Palette<T> oldPalette;
    @Unique private WorldChunk parentChunk;
    @Unique private ChunkSection parentChunkSection;

    @Redirect(method = "toPacket", at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/PalettedContainer;data:Lnet/minecraft/util/PackedIntegerArray;"))
    public PackedIntegerArray transformData(PalettedContainer<T> _this) {
        if (XrayUtil.getSendingChunk() == null || XrayUtil.getSendingChunkSection() == null) {
            return data;
        } else if (mixedData != null) {
            return mixedData;
        } else {
            parentChunk = XrayUtil.getSendingChunk();
            parentChunkSection = XrayUtil.getSendingChunkSection();
            mixedData = XrayUtil.transformChunkSectionData(parentChunk, parentChunkSection, data);
            return mixedData;
        }
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

    @Inject(method = "setAndGetOldValue", at = @At("TAIL"))
    private void onChange(int index, T value, CallbackInfoReturnable<T> ci) {
        if (mixedData != null) {
            BlockPos pos = new BlockPos(parentChunk.getPos().x * 16 + (index & 0xf),
                    parentChunkSection.getYOffset() + ((index >> 8) & 0xf),
                    parentChunk.getPos().z * 16 + ((index >> 4) & 0xf));
            for (Direction dir : Direction.values()) {
                BlockPos offsetPos1 = pos.offset(dir);
                for (Direction dir2 : Direction.values()) {
                    BlockPos offsetPos = offsetPos1.offset(dir2);
                    if (parentChunk.getWorld().isBlockLoaded(offsetPos)) {
                        WorldChunk chunk = parentChunk.getWorld().getWorldChunk(offsetPos);
                        int x = offsetPos.getX() & 15;
                        int y = offsetPos.getY() & 15;
                        int z = offsetPos.getZ() & 15;
                        int offsetIndex = toIndex(x, y, z);
                        int sectionIndex = offsetPos.getY() >> 4;
                        if (sectionIndex >= 0 && sectionIndex < chunk.getSectionArray().length) {
                            ChunkSection section = chunk.getSectionArray()[sectionIndex];
                            if (section != null) {
                                @SuppressWarnings("unchecked") IPalettedContainer<BlockState> palettedContainer = (IPalettedContainer<BlockState>) section.getContainer();
                                if (palettedContainer.getMixedData() != null) {
                                    if (palettedContainer.getData().get(offsetIndex) != palettedContainer.getMixedData().get(offsetIndex)) {
                                        palettedContainer.getMixedData().set(offsetIndex, palettedContainer.getData().get(offsetIndex));
                                        ((ServerWorld) chunk.getWorld()).method_14178().markForUpdate(offsetPos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Accessor
    @Override
    public abstract Palette<T> getPalette();

    @Accessor
    @Override
    public abstract PackedIntegerArray getData();

    @Override
    public PackedIntegerArray getMixedData() {
        return mixedData;
    }
}
