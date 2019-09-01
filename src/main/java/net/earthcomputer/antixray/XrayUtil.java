package net.earthcomputer.antixray;

import net.minecraft.util.PackedIntegerArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

public class XrayUtil {

    private static ThreadLocal<WorldChunk> currentSendingChunk = new ThreadLocal<>();
    private static ThreadLocal<ChunkSection> currentSendingChunkSection = new ThreadLocal<>();

    public static void setSendingChunk(WorldChunk chunk) {
        currentSendingChunk.set(chunk);
    }

    public static WorldChunk getSendingChunk() {
        return currentSendingChunk.get();
    }

    public static void setSendingChunkSection(ChunkSection chunkSection) {
        currentSendingChunkSection.set(chunkSection);
    }

    public static ChunkSection getSendingChunkSection() {
        return currentSendingChunkSection.get();
    }

    // ---

    public static PackedIntegerArray transformChunkSectionData(WorldChunk chunk, ChunkSection chunkSection, PackedIntegerArray data) {
        ChunkMixer mixer = new ChunkMixer(chunk, chunkSection, data);
        mixer.process();
        return mixer.getOutput();
    }

}
