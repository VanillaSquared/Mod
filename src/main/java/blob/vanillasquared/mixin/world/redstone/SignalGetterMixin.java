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
    // Mirror redstone blocks: entity power contributes weak power only, not strong/direct power.
    @Inject(method = "getSignal", at = @At("RETURN"), cancellable = true)
    private void vsq$getEntityRedstoneSignal(BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
        SignalGetter signalGetter = (SignalGetter) this;
        if (signalGetter instanceof ServerLevel level) {
            cir.setReturnValue(Math.max(cir.getReturnValue(), VSQEntityRedstonePower.getSignal(level, pos)));
        }
    }
}
