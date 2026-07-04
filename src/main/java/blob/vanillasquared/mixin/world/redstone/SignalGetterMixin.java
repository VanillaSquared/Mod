package blob.vanillasquared.mixin.world.redstone;

import blob.vanillasquared.main.world.redstone.VSQEntityRedstonePower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.SignalGetter;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerLevel.class)
public abstract class SignalGetterMixin implements SignalGetter {
    @Override
    public int getSignal(BlockPos pos, Direction direction) {
        int signal = SignalGetter.super.getSignal(pos, direction);
        return Math.max(signal, VSQEntityRedstonePower.getSignal((ServerLevel) (Object) this, pos));
    }

    @Override
    public int getDirectSignal(BlockPos pos, Direction direction) {
        int signal = SignalGetter.super.getDirectSignal(pos, direction);
        return Math.max(signal, VSQEntityRedstonePower.getSignal((ServerLevel) (Object) this, pos));
    }
}
