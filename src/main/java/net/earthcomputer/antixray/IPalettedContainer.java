package net.earthcomputer.antixray;

import net.minecraft.world.chunk.Palette;

public interface IPalettedContainer<T> {

    Palette<T> getPalette();

}
