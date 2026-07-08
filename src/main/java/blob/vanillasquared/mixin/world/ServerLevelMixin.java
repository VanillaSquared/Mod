package blob.vanillasquared.mixin.world;

import blob.vanillasquared.main.world.effect.ChannelingState;
import blob.vanillasquared.main.world.effect.LungingState;
import blob.vanillasquared.main.world.effect.SwirlingState;
import blob.vanillasquared.main.world.redstone.VSQEntityRedstonePower;
import blob.vanillasquared.main.world.redstone.VSQEntityRedstonePowerAccess;
import blob.vanillasquared.main.world.redstone.VSQEntityRedstonePowerLevelAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements VSQEntityRedstonePowerLevelAccess {
    @Unique
    private int vsq$poweredEntityCount;

    @Inject(method = "tick", at = @At("TAIL"))
    private void vsq$tickChanneling(BooleanSupplier haveTime, CallbackInfo ci) {
        ChannelingState.tick((ServerLevel) (Object) this);
        LungingState.tick((ServerLevel) (Object) this);
        SwirlingState.tick((ServerLevel) (Object) this);
    }

    @Inject(method = "addEntity", at = @At("RETURN"))
    private void vsq$reconcileLoadedEntityRedstonePower(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && entity instanceof VSQEntityRedstonePowerAccess access) {
            access.vsq$reconcileRedstonePowerCount();
            if (VSQEntityRedstonePower.getPower(entity) > 0) {
                VSQEntityRedstonePower.updateNeighbors((ServerLevel) (Object) this, entity.getBoundingBox());
            }
        }
    }

    @Override
    public int vsq$getPoweredEntityCount() {
        return this.vsq$poweredEntityCount;
    }

    @Override
    public void vsq$incrementPoweredEntityCount() {
        this.vsq$poweredEntityCount++;
    }

    @Override
    public void vsq$decrementPoweredEntityCount() {
        if (this.vsq$poweredEntityCount > 0) {
            this.vsq$poweredEntityCount--;
        }
    }
}
