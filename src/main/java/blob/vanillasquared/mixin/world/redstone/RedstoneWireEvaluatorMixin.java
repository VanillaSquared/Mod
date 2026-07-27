package blob.vanillasquared.mixin.world.redstone;

import blob.vanillasquared.main.world.redstone.VSQDirectionalRedstoneTransmission;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.RedstoneWireEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RedstoneWireEvaluator.class)
public abstract class RedstoneWireEvaluatorMixin {
    @Shadow
    protected abstract int getWireSignal(BlockPos pos, BlockState state);

    @Redirect(
            method = "getIncomingWireSignal",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;isRedstoneConductor(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z",
                    ordinal = 0
            )
    )
    private boolean vsq$transmitsDownward(BlockState state, BlockGetter level, BlockPos pos) {
        return VSQDirectionalRedstoneTransmission.transmitsDownward(state, level, pos);
    }

    @Redirect(
            method = "getIncomingWireSignal",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/redstone/RedstoneWireEvaluator;getWireSignal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)I",
                    ordinal = 1
            )
    )
    private int vsq$getBidirectionalWireSignal(
            RedstoneWireEvaluator ignoredEvaluator,
            BlockPos upperWirePos,
            BlockState upperWireState,
            Level level,
            BlockPos ignoredWirePos
    ) {
        int signal = this.getWireSignal(upperWirePos, upperWireState);
        BlockPos neighborPos = upperWirePos.below();
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.is(VSQDirectionalRedstoneTransmission.TRANSMITS_UPWARD)
                && neighborState.is(VSQDirectionalRedstoneTransmission.TRANSMITS_DOWNWARD)) {
            BlockPos lowerWirePos = neighborPos.below();
            signal = Math.max(signal, this.getWireSignal(lowerWirePos, level.getBlockState(lowerWirePos)));
        }

        return signal;
    }

    @Redirect(
            method = "getIncomingWireSignal",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;isRedstoneConductor(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z",
                    ordinal = 2
            )
    )
    private boolean vsq$blocksUpwardTransmission(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Level wireLevel,
            BlockPos wirePos
    ) {
        BlockPos supportPos = wirePos.below();
        BlockState supportState = wireLevel.getBlockState(supportPos);
        if (VSQDirectionalRedstoneTransmission.hasDirectionalTransmission(supportState)) {
            return !supportState.is(VSQDirectionalRedstoneTransmission.TRANSMITS_UPWARD);
        }

        return state.isRedstoneConductor(level, pos);
    }
}
