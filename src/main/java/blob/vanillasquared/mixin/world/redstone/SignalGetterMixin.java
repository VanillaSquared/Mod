package blob.vanillasquared.mixin.world.redstone;

import blob.vanillasquared.main.world.redstone.VSQEntityRedstonePower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.SignalGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignalGetter.class)
public interface SignalGetterMixin {
    // A swallowed redstone block should mirror the weak-power behavior of a real redstone block.
    // Augment getSignal only; getDirectSignal is strong power and would power through solid blocks.
    @Inject(method = "getSignal", at = @At("RETURN"), cancellable = true)
    private void vsq$getEntityRedstoneSignal(BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof ServerLevel level) {
            cir.setReturnValue(Math.max(cir.getReturnValue(), VSQEntityRedstonePower.getSignal(level, pos)));
        }
    }
}
