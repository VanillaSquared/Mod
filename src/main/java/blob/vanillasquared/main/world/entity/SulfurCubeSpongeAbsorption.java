package blob.vanillasquared.main.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public final class SulfurCubeSpongeAbsorption {
    private static final Direction[] DIRECTIONS = Direction.values();

    private SulfurCubeSpongeAbsorption() {
    }

    public static int absorb(Level level, BlockPos startPos, int capacity) {
        if (capacity <= 0) {
            return 0;
        }

        int accepted = BlockPos.breadthFirstTraversal(startPos, 6, capacity + 1, (pos, consumer) -> {
            for (Direction direction : DIRECTIONS) {
                consumer.accept(pos.relative(direction));
            }
        }, pos -> {
            if (pos.equals(startPos)) {
                return BlockPos.TraversalNodeStatus.ACCEPT;
            }

            BlockState state = level.getBlockState(pos);
            FluidState fluidState = level.getFluidState(pos);
            if (!fluidState.is(FluidTags.WATER)) {
                return BlockPos.TraversalNodeStatus.SKIP;
            }

            if (state.getBlock() instanceof BucketPickup bucketPickup && !bucketPickup.pickupBlock(null, level, pos, state).isEmpty()) {
                return BlockPos.TraversalNodeStatus.ACCEPT;
            }

            if (state.getBlock() instanceof LiquidBlock) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return BlockPos.TraversalNodeStatus.ACCEPT;
            }

            if (!state.is(Blocks.KELP) && !state.is(Blocks.KELP_PLANT) && !state.is(Blocks.SEAGRASS) && !state.is(Blocks.TALL_SEAGRASS)) {
                return BlockPos.TraversalNodeStatus.SKIP;
            }

            BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
            Block.dropResources(state, level, pos, blockEntity);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return BlockPos.TraversalNodeStatus.ACCEPT;
        });
        return accepted - 1;
    }
}
