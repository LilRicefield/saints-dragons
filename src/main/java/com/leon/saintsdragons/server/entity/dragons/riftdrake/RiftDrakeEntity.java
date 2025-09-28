package com.leon.saintsdragons.server.entity.dragons.riftdrake;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.ai.navigation.DragonSwimMoveControl;
import com.leon.saintsdragons.server.ai.navigation.DragonSwimNavigate;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers.RiftDrakeAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers.RiftDrakeFindWaterGoal;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers.RiftDrakeLeaveWaterGoal;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers.RiftDrakeRandomSwimGoal;
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
import net.minecraft.world.entity.ai.goal.BreathAirGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.util.LandRandomPos;
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
import com.leon.saintsdragons.server.entity.base.RideableDragonData;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;

public class RiftDrakeEntity extends DragonEntity implements RideableDragon, AquaticDragon {

    private static final EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RIDER_FORWARD = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIDER_STRAFE = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ACCELERATING = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SWIMMING = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.BOOLEAN);

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
    private RiftDrakeRandomSwimGoal waterSwimGoal;
    private boolean swimming;
    private int swimTicks;
    private int ticksInWater;
    private int ticksOutOfWater;

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
        this.entityData.define(DATA_GROUND_MOVE_STATE, 0);
        this.entityData.define(DATA_RIDER_FORWARD, 0.0F);
        this.entityData.define(DATA_RIDER_STRAFE, 0.0F);
        this.entityData.define(DATA_ACCELERATING, false);
        this.entityData.define(DATA_SWIMMING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new BreathAirGoal(this));
        this.goalSelector.addGoal(2, new RiftDrakeLeaveWaterGoal(this));
        this.goalSelector.addGoal(3, new RiftDrakeFindWaterGoal(this));
        this.waterSwimGoal = new RiftDrakeRandomSwimGoal(this, 1.0D, 30);
        this.goalSelector.addGoal(4, waterSwimGoal);
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
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
                ticksInWater = Math.min(ticksInWater + 1, 1200);
                ticksOutOfWater = 0;
            } else {
                swimTicks = Math.max(swimTicks - 1, 0);
                ticksOutOfWater = Math.min(ticksOutOfWater + 1, 1200);
                ticksInWater = 0;
            }

            if (inWater && !swimming) {
                enterSwimState();
            } else if (!inWater && swimming) {
                exitSwimState();
            }

            this.tickAnimationStates();
        }
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player rider) {
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }

            Vec3 input = this.getRiddenInput(rider, motion);
            double forward = input.z;
            double strafe = input.x;
            float speed = this.getRiddenSpeed(rider);

            float yaw = rider.getYRot();
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
            if (!this.isInWater()) {
                this.setXRot(0.0F);
            }

            if (this.isInWater()) {
                Vec3 buoyancy = this.getBuoyancyVector();
                Vec3 delta = this.getDeltaMovement();
                Vec3 thrust = new Vec3(strafe * speed, 0.0D, forward * speed);

                Vec3 rotated = Vec3.directionFromRotation(0.0F, this.getYRot());
                Vec3 right = new Vec3(rotated.z, 0.0D, -rotated.x);
                Vec3 forwardVec = new Vec3(rotated.x, 0.0D, rotated.z).normalize();
                Vec3 desired = forwardVec.scale(thrust.z).add(right.scale(thrust.x));

                Vec3 next = delta.add(desired).add(0.0D, buoyancy.y, 0.0D);
                double max = this.getSwimSpeed();
                if (next.lengthSqr() > max * max) {
                    next = next.normalize().scale(max);
                }
                this.setDeltaMovement(next);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.91D));
            } else {
                this.setSpeed(speed);
                super.travel(new Vec3(strafe, motion.y, forward));
            }
            return;
        }
        if (this.isInWater()) {
            this.moveRelative(this.getSpeed(), motion);
            this.move(MoverType.SELF, this.getDeltaMovement());
            Vec3 delta = this.getDeltaMovement();
            float drag = this.isControlledByLocalInstance() ? 0.9F : 0.92F;
            this.setDeltaMovement(delta.multiply(drag, 0.9D, drag));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }
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
        this.tickAnimationStates();
    }

    @Override
    public void onExitWater() {
        this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
        this.tickAnimationStates();
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

    public float getLastRiderForward() {
        return this.entityData.get(DATA_RIDER_FORWARD);
    }

    public float getLastRiderStrafe() {
        return this.entityData.get(DATA_RIDER_STRAFE);
    }

    @Override
    public boolean isAccelerating() {
        return this.entityData.get(DATA_ACCELERATING);
    }

    public int getGroundMoveState() {
        return this.entityData.get(DATA_GROUND_MOVE_STATE);
    }

    @Override
    public int getSyncedFlightMode() {
        return -1;
    }

    public int getEffectiveGroundState() {
        return this.entityData.get(DATA_GROUND_MOVE_STATE);
    }

    @Override
    public void setAccelerating(boolean accelerating) {
        this.entityData.set(DATA_ACCELERATING, accelerating);
    }

    public void setGroundMoveStateFromRider(int state) {
        int s = Mth.clamp(state, 0, 2);
        if (this.entityData.get(DATA_GROUND_MOVE_STATE) != s) {
            this.entityData.set(DATA_GROUND_MOVE_STATE, s);
            this.syncAnimState(s, getSyncedFlightMode());
        }
    }

    public void handleJumpRequest() {
        if (this.isInWater()) {
            Vec3 jump = new Vec3(0.0D, 0.42D, 0.0D);
            this.setDeltaMovement(this.getDeltaMovement().add(jump));
            this.hasImpulse = true;
        } else if (this.onGround()) {
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x, 0.42D, movement.z);
            this.hasImpulse = true;
        }
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        Vec3 input = super.getRiddenInput(player, deltaIn);
        if (!level().isClientSide) {
            float fwd = (float) Mth.clamp(input.z, -1.0D, 1.0D);
            float str = (float) Mth.clamp(input.x, -1.0D, 1.0D);
            setLastRiderForward(RideableDragonData.applyInputThreshold(fwd));
            setLastRiderStrafe(RideableDragonData.applyInputThreshold(str));
        }
        return input;
    }

    @Override
    protected float getRiddenSpeed(@NotNull Player rider) {
        float base = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
        float swim = (float) this.getSwimSpeed();
        if (this.isInWater()) {
            return this.isAccelerating() ? swim * 1.2F : swim;
        }
        return this.isAccelerating() ? base * 1.4F : base * 0.75F;
    }

    @Override
    public void removePassenger(@NotNull net.minecraft.world.entity.Entity passenger) {
        super.removePassenger(passenger);
        if (!this.level().isClientSide) {
            this.setAccelerating(false);
            this.setLastRiderForward(0.0F);
            this.setLastRiderStrafe(0.0F);
            this.setGroundMoveStateFromRider(0);
        }
    }

    @Override
    public void tickAnimationStates() {
        if (level().isClientSide) {
            return;
        }

        boolean ridden = this.getControllingPassenger() instanceof Player;
        int moveState = 0;

        if (this.isSwimming()) {
            double speed = this.getDeltaMovement().horizontalDistanceSqr();
            moveState = speed > 0.01D ? (this.isAccelerating() ? 2 : 1) : 0;
        } else if (ridden) {
            float fwd = this.entityData.get(DATA_RIDER_FORWARD);
            float str = this.entityData.get(DATA_RIDER_STRAFE);

            if (RideableDragonData.isSignificantRiderInput(fwd, str)) {
                moveState = this.isAccelerating() ? 2 : 1;
            } else {
                double vel = this.getDeltaMovement().horizontalDistanceSqr();
                moveState = RideableDragonData.getRiddenGroundStateFromVelocity(vel);
            }
        } else {
            double vel = this.getDeltaMovement().horizontalDistanceSqr();
            moveState = RideableDragonData.getGroundStateFromVelocity(vel);
        }

        if (this.entityData.get(DATA_GROUND_MOVE_STATE) != moveState) {
            this.entityData.set(DATA_GROUND_MOVE_STATE, moveState);
            this.syncAnimState(moveState, getSyncedFlightMode());
        }

        float forward = this.entityData.get(DATA_RIDER_FORWARD);
        float strafe = this.entityData.get(DATA_RIDER_STRAFE);
        if (forward != 0.0F || strafe != 0.0F) {
            this.entityData.set(DATA_RIDER_FORWARD, RideableDragonData.decayRiderInput(forward));
            this.entityData.set(DATA_RIDER_STRAFE, RideableDragonData.decayRiderInput(strafe));
        }

        if (this.isAccelerating() && moveState == 0) {
            this.setAccelerating(false);
        }
    }

    @Override
    public void jumpFromGround() {
        super.jumpFromGround();
        this.setGroundMoveStateFromRider(1);
    }

    @Override
    public void syncAnimState(int groundState, int flightMode) {
        if (level().isClientSide) {
            return;
        }
        this.setAnimData(com.leon.saintsdragons.common.network.DragonAnimTickets.GROUND_STATE, groundState);
        this.setAnimData(com.leon.saintsdragons.common.network.DragonAnimTickets.FLIGHT_MODE, flightMode);
    }

    public void initializeRiderState() {
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_GROUND_MOVE_STATE, 0);
            this.entityData.set(DATA_RIDER_FORWARD, 0.0F);
            this.entityData.set(DATA_RIDER_STRAFE, 0.0F);
            this.setAccelerating(false);
        }
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
        this.entityData.set(DATA_SWIMMING, true);
        if (waterSwimGoal != null) {
            waterSwimGoal.forceTrigger();
        }
    }

    private void exitSwimState() {
        swimming = false;
        this.navigation = groundNavigation;
        this.moveControl = landMoveControl;
        this.lookControl = landLookControl;
        this.waterNavigation.stop();
        Vec3 delta = this.getDeltaMovement();
        this.setDeltaMovement(new Vec3(delta.x, 0.0D, delta.z));
        this.entityData.set(DATA_SWIMMING, false);
    }

    public boolean isSwimming() {
        if (level().isClientSide) {
            return this.entityData.get(DATA_SWIMMING);
        }
        return swimming;
    }

    @Nullable
    public Vec3 pickSwimTarget(double radius, double verticalRange) {
        return LandRandomPos.getPos(this, (int) radius, (int) verticalRange);
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
