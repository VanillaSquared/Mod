package blob.vanillasquared.mixin.world.entity.entities;

import blob.vanillasquared.main.world.entity.SulfurCubeBreedingState;
import blob.vanillasquared.main.world.entity.SulfurCubeSpongeAbsorption;
import blob.vanillasquared.main.world.entity.SulfurCubeSpongeState;
import blob.vanillasquared.main.world.item.VSQItems;
import blob.vanillasquared.main.world.redstone.VSQContentRedstonePowerAccess;
import blob.vanillasquared.main.world.redstone.VSQEntityRedstonePower;
import blob.vanillasquared.main.world.redstone.VSQEntityRedstonePowerAccess;
import blob.vanillasquared.mixin.world.entity.CubeMobMoveControlAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;

@Mixin(SulfurCube.class)
public abstract class SulfurCubeMixin extends AgeableMob implements SulfurCubeBreedingState, SulfurCubeSpongeState, VSQContentRedstonePowerAccess {
    @Unique
    private static final int VSQ_IN_LOVE_TIME = 600;
    @Unique
    private static final int VSQ_PARENT_AGE_AFTER_BREEDING = 6000;
    @Unique
    private static final int VSQ_BREEDING_TIME = 60;
    @Unique
    private static final int VSQ_SPONGE_CAPACITY = 1280;
    @Unique
    private static final TagKey<Item> VSQ_DRIES_WATER = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("vsq", "sulfur_cube_archetype/dries_water")
    );
    @Unique
    private int vsq$inLove;
    @Unique
    private int vsq$breedTime;
    @Unique
    private int vsq$lastResolvedContentRedstonePower = -1;
    @Unique
    private int vsq$spongeAbsorbedWater;
    @Unique
    private boolean vsq$changingSpongeInternally;
    @Unique
    @Nullable
    private BlockPos vsq$lastSpongePosition;
    @Unique
    @Nullable
    private ServerPlayer vsq$loveCause;

    @Shadow
    protected abstract void playEatingSound();

    protected SulfurCubeMixin(EntityType<? extends AgeableMob> type, Level level) {
        super(type, level);
    }

    @Inject(method = "addBehaviourGoals", at = @At("TAIL"))
    private void vsq$addSulfurGooTemptGoal(CallbackInfo ci) {
        this.goalSelector.addGoal(2, new SulfurGooTemptGoal((SulfurCube) (Object) this));
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void vsq$breedWithSulfurGoo(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (!heldItem.is(VSQItems.SULFUR_GOO)) {
            return;
        }

        if (this.isBaby()) {
            if (this.canAgeUp()) {
                int age = this.getAge();
                this.usePlayerItem(player, hand, heldItem);
                this.ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(-age), true);
                this.playEatingSound();
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            return;
        }

        if (this.vsq$inLove <= 0 && this.getAge() == 0 && player instanceof ServerPlayer serverPlayer) {
            this.usePlayerItem(player, hand, heldItem);
            this.vsq$setInLove(serverPlayer);
            this.playEatingSound();
            cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
        } else if (((SulfurCube) (Object) this).level().isClientSide()) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void vsq$customServerAiStep(ServerLevel level, CallbackInfo ci) {
        this.vsq$setRedstonePowerForContent();
        this.vsq$tickSponge(level);

        if (this.getAge() != 0) {
            this.vsq$resetLove();
            return;
        }

        if (this.vsq$inLove > 0) {
            this.vsq$inLove--;
            SulfurCube partner = this.vsq$findBreedPartner(level);
            if (this.vsq$inLove % 10 == 0) {
                this.vsq$spawnLoveParticle();
            }

            if (partner == null) {
                this.vsq$breedTime = 0;
                return;
            }

            this.vsq$moveTowardBreedPartner(partner);
            if (selfHasBreedingPriority(partner)) {
                this.vsq$breedTime++;
            }

            if (this.vsq$breedTime >= VSQ_BREEDING_TIME && ((SulfurCube) (Object) this).distanceToSqr(partner) < 9.0) {
                this.vsq$spawnChildFromBreeding(level, partner);
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void vsq$saveBreedingState(ValueOutput output, CallbackInfo ci) {
        output.putInt("VSQInLove", this.vsq$inLove);
        output.putInt("VSQSpongeAbsorbedWater", this.vsq$spongeAbsorbedWater);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void vsq$loadBreedingState(ValueInput input, CallbackInfo ci) {
        this.vsq$inLove = input.getIntOr("VSQInLove", 0);
        this.vsq$spongeAbsorbedWater = Math.clamp(input.getIntOr("VSQSpongeAbsorbedWater", 0), 0, VSQ_SPONGE_CAPACITY);
        if (!vsq$isDrySponge(this.getItemBySlot(EquipmentSlot.BODY))) {
            this.vsq$spongeAbsorbedWater = 0;
        }
        this.vsq$lastSpongePosition = this.blockPosition();
        this.vsq$setRedstonePowerForContent();
    }

    @Inject(method = "saveToBucketTag", at = @At("TAIL"))
    private void vsq$saveSpongeStateToBucket(ItemStack bucket, CallbackInfo ci) {
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA, bucket,
                tag -> tag.putInt("VSQSpongeAbsorbedWater", this.vsq$spongeAbsorbedWater));
    }

    @Inject(method = "loadFromBucketTag", at = @At("TAIL"))
    private void vsq$loadSpongeStateFromBucket(CompoundTag tag, CallbackInfo ci) {
        this.vsq$spongeAbsorbedWater = vsq$isDrySponge(this.getItemBySlot(EquipmentSlot.BODY))
                ? Math.clamp(tag.getIntOr("VSQSpongeAbsorbedWater", 0), 0, VSQ_SPONGE_CAPACITY)
                : 0;
        this.vsq$lastSpongePosition = this.blockPosition();
    }

    @Override
    public void vsq$bodyItemChanged(ItemStack stack) {
        if (this.vsq$changingSpongeInternally) {
            return;
        }

        this.vsq$spongeAbsorbedWater = 0;
        // A null position lets a newly equipped dry sponge absorb water on the next tick.
        this.vsq$lastSpongePosition = vsq$isDrySponge(stack) ? null : this.blockPosition();
    }

    @Unique
    private void vsq$setSpongeBodyItem(ItemStack stack) {
        this.vsq$changingSpongeInternally = true;
        try {
            this.setItemSlot(EquipmentSlot.BODY, stack);
        } finally {
            this.vsq$changingSpongeInternally = false;
        }
    }

    @Unique
    private static boolean vsq$isDrySponge(ItemStack stack) {
        return stack.is(VSQ_DRIES_WATER) && !stack.is(Items.WET_SPONGE);
    }

    @Unique
    private void vsq$tickSponge(ServerLevel level) {
        ItemStack bodyItem = this.getItemBySlot(EquipmentSlot.BODY);
        BlockPos position = this.blockPosition();
        if (bodyItem.is(Items.WET_SPONGE)) {
            this.vsq$spongeAbsorbedWater = 0;
            this.vsq$lastSpongePosition = position;
            if (level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, position)) {
                this.vsq$setSpongeBodyItem(new ItemStack(Items.SPONGE));
                level.levelEvent(2009, position, 0);
                level.playSound(null, position, SoundEvents.WET_SPONGE_DRIES, SoundSource.BLOCKS, 1.0F,
                        (1.0F + level.getRandom().nextFloat() * 0.2F) * 0.7F);
            }
            return;
        }

        if (!vsq$isDrySponge(bodyItem)) {
            this.vsq$spongeAbsorbedWater = 0;
            this.vsq$lastSpongePosition = null;
            return;
        }

        if (position.equals(this.vsq$lastSpongePosition)) {
            return;
        }
        this.vsq$lastSpongePosition = position;

        int removed = SulfurCubeSpongeAbsorption.absorb(level, position, VSQ_SPONGE_CAPACITY - this.vsq$spongeAbsorbedWater);
        if (removed <= 0) {
            return;
        }

        this.vsq$spongeAbsorbedWater += removed;
        level.playSound(null, position, SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (this.vsq$spongeAbsorbedWater >= VSQ_SPONGE_CAPACITY) {
            this.vsq$setSpongeBodyItem(new ItemStack(Items.WET_SPONGE));
            this.vsq$spongeAbsorbedWater = 0;
        }
    }

    @Override
    public void vsq$setRedstonePowerForContent() {
        ItemStack bodyItem = this.getItemBySlot(EquipmentSlot.BODY);
        int redstonePower = VSQEntityRedstonePower.getContentPower(bodyItem);
        if (redstonePower == this.vsq$lastResolvedContentRedstonePower) {
            return;
        }
        this.vsq$lastResolvedContentRedstonePower = redstonePower;
        ((VSQEntityRedstonePowerAccess) this).vsq$setRedstonePower(redstonePower);
    }

    @Override
    public boolean vsq$isInLove() {
        return this.vsq$inLove > 0;
    }

    @Override
    public void vsq$resetLove() {
        this.vsq$inLove = 0;
        this.vsq$breedTime = 0;
        this.vsq$loveCause = null;
    }

    @Override
    @Nullable
    public ServerPlayer vsq$loveCause() {
        return this.vsq$loveCause;
    }

    @Unique
    private void vsq$setInLove(ServerPlayer player) {
        this.vsq$inLove = VSQ_IN_LOVE_TIME;
        this.vsq$breedTime = 0;
        this.vsq$loveCause = player;
        this.vsq$spawnLoveParticles();
    }

    @Unique
    @Nullable
    private SulfurCube vsq$findBreedPartner(ServerLevel level) {
        SulfurCube self = (SulfurCube) (Object) this;
        double closestDistance = Double.MAX_VALUE;
        SulfurCube closest = null;
        for (SulfurCube candidate : level.getEntitiesOfClass(SulfurCube.class, self.getBoundingBox().inflate(8.0))) {
            if (candidate == self || candidate.getAge() != 0 || !(candidate instanceof SulfurCubeBreedingState state) || !state.vsq$isInLove()) {
                continue;
            }

            double distance = self.distanceToSqr(candidate);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = candidate;
            }
        }

        return closest;
    }

    @Unique
    private boolean selfHasBreedingPriority(SulfurCube partner) {
        return ((SulfurCube) (Object) this).getId() < partner.getId();
    }

    @Unique
    private void vsq$moveTowardBreedPartner(SulfurCube partner) {
        SulfurCube self = (SulfurCube) (Object) this;
        self.lookAt(partner, 10.0F, 10.0F);
        if (self.getMoveControl() instanceof CubeMobMoveControlAccessor cubeMobMoveControl) {
            cubeMobMoveControl.vsq$setDirection(self.getYRot(), true);
            cubeMobMoveControl.vsq$setWantedMovement(1.0);
        }
    }

    @Unique
    private void vsq$spawnChildFromBreeding(ServerLevel level, SulfurCube partner) {
        if (!(partner instanceof SulfurCubeBreedingState partnerState) || !this.vsq$isInLove() || !partnerState.vsq$isInLove()) {
            return;
        }

        AgeableMob offspring = this.getBreedOffspring(level, partner);
        if (offspring == null) {
            return;
        }

        SulfurCube self = (SulfurCube) (Object) this;
        offspring.setBaby(true);
        offspring.snapTo(self.getX(), self.getY(), self.getZ(), 0.0F, 0.0F);
        ServerPlayer breeder = this.vsq$loveCause;
        if (breeder == null) {
            breeder = partnerState.vsq$loveCause();
        }

        this.setAge(VSQ_PARENT_AGE_AFTER_BREEDING);
        partner.setAge(VSQ_PARENT_AGE_AFTER_BREEDING);
        this.vsq$resetLove();
        partnerState.vsq$resetLove();
        level.addFreshEntityWithPassengers(offspring);
        if (breeder != null) {
            breeder.awardStat(Stats.ANIMALS_BRED);
        }

        if (level.getGameRules().get(GameRules.MOB_DROPS)) {
            level.addFreshEntity(new ExperienceOrb(level, self.getX(), self.getY(), self.getZ(), self.getRandom().nextInt(7) + 1));
        }

        this.vsq$spawnLoveParticles(level);
    }

    @Unique
    private void vsq$spawnLoveParticle() {
        SulfurCube sulfurCube = (SulfurCube) (Object) this;
        if (sulfurCube.level() instanceof ServerLevel serverLevel) {
            this.vsq$spawnLoveParticle(serverLevel);
        }
    }

    @Unique
    private void vsq$spawnLoveParticle(ServerLevel level) {
        SulfurCube sulfurCube = (SulfurCube) (Object) this;
        level.sendParticles(ParticleTypes.HEART, sulfurCube.getRandomX(1.0), sulfurCube.getRandomY() + 0.5, sulfurCube.getRandomZ(1.0), 1, 0.0, 0.0, 0.0, 0.0);
    }

    @Unique
    private void vsq$spawnLoveParticles() {
        SulfurCube sulfurCube = (SulfurCube) (Object) this;
        if (sulfurCube.level() instanceof ServerLevel serverLevel) {
            this.vsq$spawnLoveParticles(serverLevel);
        }
    }

    @Unique
    private void vsq$spawnLoveParticles(ServerLevel level) {
        for (int i = 0; i < 7; i++) {
            this.vsq$spawnLoveParticle(level);
        }
    }

    @Unique
    private static class SulfurGooTemptGoal extends TemptGoal.ForNonPathfinders {
        SulfurGooTemptGoal(Mob mob) {
            super(mob, 1.0, itemStack -> !mob.isBaby() && itemStack.is(VSQItems.SULFUR_GOO), false, 1.0);
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        protected void stopNavigation() {
            if (this.mob.getMoveControl() instanceof CubeMobMoveControlAccessor cubeMobMoveControl) {
                cubeMobMoveControl.vsq$setWantedMovement(0.0);
            }
        }

        @Override
        protected void navigateTowards(Player player) {
            this.mob.lookAt(player, 10.0F, 10.0F);
            if (this.mob.getMoveControl() instanceof CubeMobMoveControlAccessor cubeMobMoveControl) {
                cubeMobMoveControl.vsq$setDirection(this.mob.getYRot(), true);
            }
        }
    }
}
