package net.earthcomputer.antixray;

import net.minecraft.util.PackedIntegerArray;
import net.minecraft.world.chunk.Palette;

public interface IPalettedContainer<T> {

    Palette<T> getPalette();

    PackedIntegerArray getData();

    PackedIntegerArray getMixedData();

}
