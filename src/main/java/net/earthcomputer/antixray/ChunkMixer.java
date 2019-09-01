package net.earthcomputer.antixray;

import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.BooleanBiFunction;
import net.minecraft.util.PackedIntegerArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.lang.reflect.Field;
import java.util.*;

public class ChunkMixer implements BlockView {

    private static final int CHANGE_DEPTH = 2;

    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<Block.NeighborGroup>> FACE_CULL_MAP;
    static {
        try {
            Field field = Arrays.stream(Block.class.getDeclaredFields())
                    .filter(it -> it.getType() == ThreadLocal.class)
                    .findFirst().orElseThrow(NoSuchFieldException::new);
            field.setAccessible(true);
            //noinspection unchecked
            FACE_CULL_MAP = (ThreadLocal<Object2ByteLinkedOpenHashMap<Block.NeighborGroup>>) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private final WorldChunk chunk;
    private final ChunkSection chunkSection;
    private final PackedIntegerArray input;
    private PackedIntegerArray output;

    private int[][][] depth = new int[16 + CHANGE_DEPTH * 2][16 + CHANGE_DEPTH * 2][16 + CHANGE_DEPTH * 2];

    public ChunkMixer(WorldChunk chunk, ChunkSection chunkSection, PackedIntegerArray input) {
        this.chunk = chunk;
        this.chunkSection = chunkSection;
        this.input = input;
    }

    public void process() {
        System.out.println("Processing chunk at (" + chunk.getPos().x + ", " + chunk.getPos().z + ")");
        computeDepth();
        replaceConcealedBlocks();
    }

    private void computeDepth() {
        for (int[][] yz : depth)
            for (int[] z : yz)
                Arrays.fill(z, Integer.MAX_VALUE);

        LinkedHashSet<BlockPos> toCompute = new LinkedHashSet<>();
        toCompute.add(new BlockPos(-CHANGE_DEPTH, -CHANGE_DEPTH, -CHANGE_DEPTH));
        while (!toCompute.isEmpty()) {
            Iterator<BlockPos> itr = toCompute.iterator();
            BlockPos pos = itr.next();
            itr.remove();

            int computedDepth = Integer.MAX_VALUE;
            for (Direction dir : Direction.values()) {
                if (shouldDrawSide(pos, dir)) {
                    computedDepth = 0;
                    break;
                } else {
                    int depthX = pos.getX() + CHANGE_DEPTH + dir.getOffsetX();
                    int depthY = pos.getY() + CHANGE_DEPTH + dir.getOffsetY();
                    int depthZ = pos.getZ() + CHANGE_DEPTH + dir.getOffsetZ();
                    int otherDepth;
                    if (depthX < 0 || depthY < 0 || depthZ < 0 || depthX >= depth.length || depthY >= depth.length || depthZ >= depth.length)
                        otherDepth = -1;
                    else
                        otherDepth = depth[depthX][depthY][depthZ];
                    if (otherDepth == Integer.MAX_VALUE)
                        continue;
                    computedDepth = Math.min(computedDepth, otherDepth + 1);
                }
            }

            if (computedDepth != depth[pos.getX() + CHANGE_DEPTH][pos.getY() + CHANGE_DEPTH][pos.getZ() + CHANGE_DEPTH]) {
                depth[pos.getX() + CHANGE_DEPTH][pos.getY() + CHANGE_DEPTH][pos.getZ() + CHANGE_DEPTH] = computedDepth;
                for (Direction dir : Direction.values()) {
                    BlockPos offsetPos = pos.offset(dir);
                    if (offsetPos.getX() >= -CHANGE_DEPTH && offsetPos.getY() >= -CHANGE_DEPTH && offsetPos.getZ() >= -CHANGE_DEPTH
                            && offsetPos.getX() < 16 + CHANGE_DEPTH && offsetPos.getY() < 16 + CHANGE_DEPTH && offsetPos.getZ() < 16 + CHANGE_DEPTH)
                        toCompute.add(offsetPos);
                }
            }
        }
    }

    private void replaceConcealedBlocks() {
        List<BlockState> replacements = new ArrayList<>();
        for (BlockState state : Block.STATE_IDS) {
            if (state.isFullOpaque(this, BlockPos.ORIGIN) && chunkSection.getContainer().method_19526(state)) {
                replacements.add(state);
            }
        }
        if (replacements.isEmpty())
            return;

        Random rand = new Random();

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    if (depth[x + CHANGE_DEPTH][y + CHANGE_DEPTH][z + CHANGE_DEPTH] >= CHANGE_DEPTH) {
                        if (output == null) {
                            output = clonePackedIntegerArray(input);
                        }
                        BlockState replacement = replacements.get(rand.nextInt(replacements.size()));
                        @SuppressWarnings("unchecked") int replacementId = ((IPalettedContainer<BlockState>) chunkSection.getContainer()).getPalette().getIndex(replacement);
                        output.set((y << 8) | (z << 4) | x, replacementId);
                    }
                }
            }
        }
    }

    public PackedIntegerArray getOutput() {
        return output == null ? input : output;
    }


    // Helper methods

    // copied client-side method from Block.java
    private boolean shouldDrawSide(BlockPos pos, Direction dir) {
        BlockState state = getBlockState(pos);
        BlockPos otherPos = pos.offset(dir);
        BlockState otherState = getBlockState(otherPos);
        //noinspection ConstantConditions
        if (/*state.isSideInvisible(otherState, dir)*/ false) { // client-only method
            return false;
        } else if (otherState.isOpaque()) {
            Block.NeighborGroup neighborGroup = new Block.NeighborGroup(state, otherState, dir);
            Object2ByteLinkedOpenHashMap<Block.NeighborGroup> faceCullMap = FACE_CULL_MAP.get();
            byte flags = faceCullMap.getAndMoveToFirst(neighborGroup);
            if (flags != 127) {
                return flags != 0;
            } else {
                VoxelShape cullShape = state.getCullShape(this, pos, dir);
                VoxelShape otherCullShape = otherState.getCullShape(this, otherPos, dir.getOpposite());
                boolean shouldDraw = VoxelShapes.matchesAnywhere(cullShape, otherCullShape, BooleanBiFunction.ONLY_FIRST);
                if (faceCullMap.size() == 200) {
                    faceCullMap.removeLastByte();
                }

                faceCullMap.putAndMoveToFirst(neighborGroup, (byte)(shouldDraw ? 1 : 0));
                return shouldDraw;
            }
        } else {
            return true;
        }
    }

    private final BlockPos.Mutable getBlockStatePos = new BlockPos.Mutable();
    private BlockState getBlockState(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16) {
            getBlockStatePos.set(chunk.getPos().x * 16 + x, chunkSection.getYOffset() + y, chunk.getPos().z * 16 + z);
            if (chunk.getWorld().isBlockLoaded(getBlockStatePos)) {
                return chunk.getWorld().getBlockState(getBlockStatePos);
            } else {
                return Blocks.STONE.getDefaultState();
            }
        } else {
            return chunkSection.getBlockState(x, y, z);
        }
    }

    private static PackedIntegerArray clonePackedIntegerArray(PackedIntegerArray input) {
        return new PackedIntegerArray(input.getElementBits(), input.getSize(), input.getStorage().clone());
    }


    // BlockView methods

    @Override
    public BlockEntity getBlockEntity(BlockPos blockPos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos blockPos) {
        return getBlockState(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    @Override
    public FluidState getFluidState(BlockPos blockPos) {
        return Fluids.EMPTY.getDefaultState();
    }
}
