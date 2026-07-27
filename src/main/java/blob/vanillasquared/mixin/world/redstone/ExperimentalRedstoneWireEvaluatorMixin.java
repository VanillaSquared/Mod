package blob.vanillasquared.mixin.world.redstone;

import blob.vanillasquared.main.world.redstone.VSQDirectionalRedstoneTransmission;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.ExperimentalRedstoneWireEvaluator;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ExperimentalRedstoneWireEvaluator.class)
public abstract class ExperimentalRedstoneWireEvaluatorMixin {
    @WrapOperation(
            method = "propagateChangeToNeighbors",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/redstone/ExperimentalRedstoneWireEvaluator;enqueueNeighborWire(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/level/redstone/Orientation;Z)V",
                    ordinal = 1
            )
    )
    private void vsq$enqueueUpwardWire(
            ExperimentalRedstoneWireEvaluator evaluator,
            Level level,
            BlockPos wirePos,
            int power,
            Orientation orientation,
            boolean allowTurningOff,
            Operation<Void> original
    ) {
        BlockPos supportPos = wirePos.below();
        BlockState supportState = level.getBlockState(supportPos);
        if (!VSQDirectionalRedstoneTransmission.hasDirectionalTransmission(supportState)
                || supportState.is(VSQDirectionalRedstoneTransmission.TRANSMITS_UPWARD)) {
            original.call(evaluator, level, wirePos, power, orientation, allowTurningOff);
        }
    }

    @Redirect(
            method = "propagateChangeToNeighbors",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;isRedstoneConductor(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z",
                    ordinal = 1
            )
    )
    private boolean vsq$blocksDownwardTransmission(BlockState state, BlockGetter level, BlockPos pos) {
        return !VSQDirectionalRedstoneTransmission.transmitsDownward(state, level, pos);
    }
}
