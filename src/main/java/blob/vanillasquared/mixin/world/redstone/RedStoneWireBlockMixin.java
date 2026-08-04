package blob.vanillasquared.mixin.world.redstone;

import blob.vanillasquared.main.world.redstone.VSQDirectionalRedstoneTransmission;
import blob.vanillasquared.main.world.redstone.VSQEntityRedstonePower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RedStoneWireBlock.class)
public abstract class RedStoneWireBlockMixin {
    @Shadow
    private void checkCornerChangeAt(Level level, BlockPos pos) {
    }

    @Inject(method = "getBlockSignal", at = @At("RETURN"), cancellable = true)
    private void vsq$getEntitySignalAtWire(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (level instanceof ServerLevel serverLevel) {
            cir.setReturnValue(Math.max(cir.getReturnValue(), VSQEntityRedstonePower.getSignal(serverLevel, pos)));
        }
    }

    @Redirect(
            method = "updateNeighborsOfNeighboringWires",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;isRedstoneConductor(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean vsq$routeUpwardWireUpdate(BlockState state, BlockGetter level, BlockPos pos) {
        return VSQDirectionalRedstoneTransmission.routesUpdateUpward(state, level, pos);
    }

    @Inject(method = "updateNeighborsOfNeighboringWires", at = @At("TAIL"))
    private void vsq$routeBidirectionalWireUpdates(Level level, BlockPos pos, CallbackInfo ci) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos targetPos = pos.relative(direction);
            BlockState targetState = level.getBlockState(targetPos);
            if (VSQDirectionalRedstoneTransmission.transmitsUpward(targetState, level, targetPos)
                    && VSQDirectionalRedstoneTransmission.transmitsDownward(targetState, level, targetPos)) {
                this.checkCornerChangeAt(level, targetPos.below());
            }
        }
    }
}
