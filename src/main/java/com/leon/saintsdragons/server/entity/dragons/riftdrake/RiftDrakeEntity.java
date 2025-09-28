package com.leon.saintsdragons.server.entity.dragons.riftdrake;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.ai.navigation.DragonSwimMoveControl;
import com.leon.saintsdragons.server.ai.navigation.DragonSwimNavigate;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers.RiftDrakeAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers.RiftDrakeWaterWanderGoal;
import com.leon.saintsdragons.server.entity.handler.DragonKeybindHandler;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.AquaticDragon;
import com.leon.saintsdragons.server.entity.interfaces.RideableDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RiftDrakeEntity extends DragonEntity implements RideableDragon, AquaticDragon {

    private static final EntityDataAccessor<Float> DATA_RIDER_FORWARD = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIDER_STRAFE = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ACCELERATING = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final DragonKeybindHandler keybindHandler = new DragonKeybindHandler(this);
    private final RiftDrakeAnimationHandler animationHandler = new RiftDrakeAnimationHandler(this);
    private final PathNavigation groundNavigation;
    private final DragonSwimNavigate waterNavigation;
    private final MoveControl landMoveControl;
    private final DragonSwimMoveControl swimMoveControl;
    private final LookControl landLookControl;
    private final SmoothSwimmingLookControl swimLookControl;
    private RiftDrakeWaterWanderGoal waterWanderGoal;
    private boolean swimming;
    private int swimTicks;
    @Nullable
    private Vec3 activeSwimTarget;

    public RiftDrakeEntity(EntityType<? extends RiftDrakeEntity> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        this.groundNavigation = new DragonPathNavigateGround(this, level);
        this.waterNavigation = new DragonSwimNavigate(this, level);
        this.landMoveControl = new RiftDrakeMoveControl(this);
        this.swimMoveControl = new DragonSwimMoveControl(this, 6.0F, 0.08D, 0.12D);
        this.landLookControl = new LookControl(this);
        this.swimLookControl = new SmoothSwimmingLookControl(this, 10);
        this.navigation = this.groundNavigation;
        this.moveControl = this.landMoveControl;
        this.lookControl = this.landLookControl;
        this.setRideable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DragonEntity.createDragonAttributes()
                .add(Attributes.MAX_HEALTH, 140.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RIDER_FORWARD, 0.0F);
        this.entityData.define(DATA_RIDER_STRAFE, 0.0F);
        this.entityData.define(DATA_ACCELERATING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.waterWanderGoal = new RiftDrakeWaterWanderGoal(this, 1.0D, 120);
        this.waterWanderGoal.setActive(false);
        this.goalSelector.addGoal(3, waterWanderGoal);
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, animationHandler::movementPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    @Override
    public void initializeAnimationState() {
        // TODO: derive initial animation state for Rift Drake
    }

    @Override
    public void resetAnimationState() {
        // TODO: recalculate animation state when needed
    }

    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }

    public DragonKeybindHandler getKeybindHandler() {
        return keybindHandler;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            boolean inWater = this.isInWater();
            if (inWater) {
                this.setAirSupply(this.getMaxAirSupply());
                swimTicks = Math.min(swimTicks + 1, 200);
            } else {
                swimTicks = Math.max(swimTicks - 1, 0);
            }
            
            // More sophisticated swimming detection
            boolean shouldBeSwimming = inWater && swimTicks > 10; // Need to be in water for a bit
            if (shouldBeSwimming && !swimming) {
                enterSwimState();
            } else if (!shouldBeSwimming && swimming) {
                exitSwimState();
            }
        }
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        if (this.isInWater()) {
            boolean controlled = this.isControlledByLocalInstance();
            Vec3 adjusted = new Vec3(motion.x, motion.y * 0.5D, motion.z);
            if (controlled) {
                this.moveRelative(0.05F, adjusted);
            }

            Vec3 delta = this.getDeltaMovement();
            Vec3 target = activeSwimTarget;
            if (target != null) {
                double verticalDiff = target.y - this.getY();
                double clamp = Mth.clamp(verticalDiff * 0.04D, -0.06D, 0.06D);
                delta = delta.add(0.0D, clamp, 0.0D);
            }

            Vec3 buoyancy = getBuoyancyVector();
            if (delta.lengthSqr() < 0.004D) {
                delta = delta.add(buoyancy.scale(0.6D));
            } else {
                delta = delta.add(buoyancy.scale(0.2D));
            }

            this.setDeltaMovement(delta);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(controlled ? 0.9D : 0.92D));
        } else {
            super.travel(motion);
        }
    }

    @Override
    public PathNavigation getNavigation() {
        return swimming ? waterNavigation : groundNavigation;
    }

    @Override
    public PathNavigation getAquaticNavigation() {
        return waterNavigation;
    }

    @Override
    public void onEnterWater() {
        this.setDeltaMovement(this.getDeltaMovement().add(getBuoyancyVector()));
    }

    @Override
    public void onExitWater() {
        this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
    }

    @Override
    public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        return super.mobInteract(player, hand);
    }

    @Override
    public void setLastRiderForward(float forward) {
        this.entityData.set(DATA_RIDER_FORWARD, forward);
    }

    @Override
    public void setLastRiderStrafe(float strafe) {
        this.entityData.set(DATA_RIDER_STRAFE, strafe);
    }

    @Override
    public boolean isAccelerating() {
        return this.entityData.get(DATA_ACCELERATING);
    }

    @Override
    public int getGroundMoveState() {
        return 0;
    }

    @Override
    public int getSyncedFlightMode() {
        return -1;
    }

    @Override
    public int getEffectiveGroundState() {
        return 0;
    }

    @Override
    public void setAccelerating(boolean accelerating) {
        this.entityData.set(DATA_ACCELERATING, accelerating);
    }

    @Override
    public int getMaxAirSupply() {
        return 600;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public double getSwimSpeed() {
        return 0.28D;
    }

    @Override
    public Vec3 getBuoyancyVector() {
        return new Vec3(0.0D, 0.03D, 0.0D);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Nullable
    @Override
    public Vec3 getDismountLocationForPassenger(@NotNull net.minecraft.world.entity.LivingEntity passenger) {
        Vec3 dismount = super.getDismountLocationForPassenger(passenger);
        if (dismount != null) {
            return dismount;
        }
        return this.position().add(1.0D, 0.0D, 0.0D);
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        return super.getRiddenInput(player, deltaIn);
    }

    @Override
    public void removePassenger(@NotNull net.minecraft.world.entity.Entity passenger) {
        super.removePassenger(passenger);
    }

    @Override
    public void tickAnimationStates() {
        // TODO: implement animation states
    }

    @Override
    public Vec3 getHeadPosition() {
        return this.getEyePosition();
    }

    @Override
    public Vec3 getMouthPosition() {
        return this.position().add(0, this.getBbHeight() * 0.8, 0);
    }

    @Override
    public net.minecraft.world.entity.AgeableMob getBreedOffspring(@NotNull net.minecraft.server.level.ServerLevel level, @NotNull net.minecraft.world.entity.AgeableMob other) {
        return null;
    }

    @Override
    public com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return null;
    }

    public static boolean canSpawn(EntityType<RiftDrakeEntity> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, net.minecraft.util.RandomSource random) {
        return level.getFluidState(pos).isSource() || level.getFluidState(pos.below()).isSource();
    }

    private void enterSwimState() {
        swimming = true;
        this.navigation = waterNavigation;
        this.moveControl = swimMoveControl;
        this.lookControl = swimLookControl;
        if (waterWanderGoal != null) {
            waterWanderGoal.setActive(true);
        }
    }

    private void exitSwimState() {
        swimming = false;
        this.navigation = groundNavigation;
        this.moveControl = landMoveControl;
        this.lookControl = landLookControl;
        this.waterNavigation.stop();
        if (waterWanderGoal != null) {
            waterWanderGoal.setActive(false);
        }
        clearSwimmingTarget();
        Vec3 delta = this.getDeltaMovement();
        this.setDeltaMovement(new Vec3(delta.x, 0.0D, delta.z));
    }

    public boolean isSwimming() {
        return swimming;
    }

    public void setSwimmingTarget(@Nullable Vec3 target) {
        this.activeSwimTarget = target;
    }

    public void clearSwimmingTarget() {
        this.activeSwimTarget = null;
    }

    @Nullable
    public Vec3 getSwimmingTarget() {
        return activeSwimTarget;
    }

    private static class RiftDrakeMoveControl extends MoveControl {
        private final RiftDrakeEntity drake;

        public RiftDrakeMoveControl(RiftDrakeEntity drake) {
            super(drake);
            this.drake = drake;
        }

        @Override
        public void tick() {
            super.tick();
        }
    }
}
