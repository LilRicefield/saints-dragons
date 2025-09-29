package com.leon.saintsdragons.server.entity.dragons.riftdrake;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.ai.navigation.DragonSwimMoveControl;
import com.leon.saintsdragons.server.ai.navigation.DragonSwimNavigate;
import com.leon.saintsdragons.server.ai.goals.riftdrake.RiftDrakeFindWaterGoal;
import com.leon.saintsdragons.server.ai.goals.riftdrake.RiftDrakeLeaveWaterGoal;
import com.leon.saintsdragons.server.ai.goals.riftdrake.RiftDrakeRandomSwimGoal;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.handlers.*;
import com.leon.saintsdragons.server.entity.handler.DragonKeybindHandler;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.AquaticDragon;
import com.leon.saintsdragons.server.entity.controller.riftdrake.RiftDrakeRiderController;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreathAirGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.annotation.Nonnull;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;
import com.leon.saintsdragons.server.entity.base.RideableDragonData;

public class RiftDrakeEntity extends RideableDragonBase implements AquaticDragon {

    private static final EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RIDER_FORWARD = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIDER_STRAFE = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ACCELERATING = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SWIMMING = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SWIM_TURN = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SWIM_PITCH = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.INT);
    
    // Flight mode data accessor (not used for ground dragon but required by interface)
    private static final EntityDataAccessor<Integer> DATA_FLIGHT_MODE = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_GOING_UP = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GOING_DOWN = SynchedEntityData.defineId(RiftDrakeEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final DragonKeybindHandler keybindHandler = new DragonKeybindHandler(this);
    private final RiftDrakeAnimationHandler animationHandler = new RiftDrakeAnimationHandler(this);
    private final RiftDrakeInteractionHandler interactionHandler = new RiftDrakeInteractionHandler(this);
    private final RiftDrakeRiderController riderController;
    private final PathNavigation groundNavigation;
    private final DragonSwimNavigate waterNavigation;
    private final MoveControl landMoveControl;
    private final DragonSwimMoveControl swimMoveControl;
    private final RiftDrakeLookController landLookControl;
    private RiftDrakeRandomSwimGoal waterSwimGoal;
    private boolean swimming;
    private int swimTicks;
    private int ticksInWater;
    private int ticksOutOfWater;
    private float swimTurnSmoothedYaw;
    private int swimTurnState;
    private int swimPitchStateTicks;

    public RiftDrakeEntity(EntityType<? extends RiftDrakeEntity> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        this.groundNavigation = new DragonPathNavigateGround(this, level);
        this.waterNavigation = new DragonSwimNavigate(this, level);
        this.landMoveControl = new RiftDrakeMoveControl(this);
        this.swimMoveControl = new DragonSwimMoveControl(this, 6.0F, 0.08D, 0.12D);
        this.landLookControl = new RiftDrakeLookController(this);
        this.navigation = this.groundNavigation;
        this.moveControl = this.landMoveControl;
        this.lookControl = this.landLookControl;
        this.riderController = new RiftDrakeRiderController(this);
        this.setRideable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createLivingAttributes()
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
        this.entityData.define(DATA_SWIM_TURN, 0);
        this.entityData.define(DATA_SWIM_PITCH, 0);
        this.entityData.define(DATA_FLIGHT_MODE, -1);
        this.entityData.define(DATA_GOING_UP, false);
        this.entityData.define(DATA_GOING_DOWN, false);
    }
    
    @Override
    protected void defineRideableDragonData() {
        // Data accessors are already defined in defineSynchedData()
    }
    
    // ===== REQUIRED ABSTRACT METHODS FROM RIDEABLEDRAGONBASE =====
    
    @Override
    protected EntityDataAccessor<Float> getRiderForwardAccessor() {
        return DATA_RIDER_FORWARD;
    }
    
    @Override
    protected EntityDataAccessor<Float> getRiderStrafeAccessor() {
        return DATA_RIDER_STRAFE;
    }
    
    @Override
    protected EntityDataAccessor<Integer> getGroundMoveStateAccessor() {
        return DATA_GROUND_MOVE_STATE;
    }
    
    @Override
    protected EntityDataAccessor<Integer> getFlightModeAccessor() {
        return DATA_FLIGHT_MODE;
    }
    
    @Override
    protected EntityDataAccessor<Boolean> getGoingUpAccessor() {
        return DATA_GOING_UP;
    }
    
    @Override
    protected EntityDataAccessor<Boolean> getGoingDownAccessor() {
        return DATA_GOING_DOWN;
    }
    
    @Override
    protected EntityDataAccessor<Boolean> getAcceleratingAccessor() {
        return DATA_ACCELERATING;
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
        controllers.add(new AnimationController<>(this, "swim_direction", 4, animationHandler::swimDirectionPredicate));
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

            // Ensure proper look control when being ridden
            if (this.getControllingPassenger() != null && this.lookControl != landLookControl) {
                this.lookControl = landLookControl;
            }

            this.tickAnimationStates();
            this.updateSwimOrientationState();
        }
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            // Clear any AI navigation when being ridden
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }

            if (this.isInWaterOrBubble()) {
                handleRiddenSwimming(motion);
            } else {
                setGoingUp(false);
                setGoingDown(false);
                // Use vanilla movement system for proper camera-relative movement
                // This will call getRiddenInput() and getRiddenSpeed() properly
                super.travel(motion);
            }
        } else {
            // Normal AI movement
            if (this.isInWater()) {
                handleRiddenSwimming(motion);
            } else {
                super.travel(motion);
            }
        }
    }

    @Override
    public @NotNull PathNavigation getNavigation() {
        return swimming ? waterNavigation : groundNavigation;
    }

    @Override
    public PathNavigation getAquaticNavigation() {
        return waterNavigation;
    }

    @Override
    public void onEnterWater() {
        this.setDeltaMovement(this.getDeltaMovement());
        this.tickAnimationStates();
    }

    @Override
    public void onExitWater() {
        this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
        this.tickAnimationStates();
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {

        // Use interaction handler for all interactions
        InteractionResult handlerResult = interactionHandler.handleInteraction(player, hand);
        if (handlerResult != InteractionResult.PASS) {
            return handlerResult;
        }

        // Fall back to base implementation for any unhandled interactions
        return super.mobInteract(player, hand);
    }
    
    /**
     * Allow interaction handler to call super.mobInteract
     */
    public InteractionResult superMobInteract(Player player, InteractionHand hand) {
        return super.mobInteract(player, hand);
    }
    
    @Override
    public boolean isFood(@Nonnull net.minecraft.world.item.ItemStack stack) {
        // Rift Drakes prefer fish like other dragons
        return stack.is(net.minecraft.world.item.Items.COD) || 
               stack.is(net.minecraft.world.item.Items.SALMON) || 
               stack.is(net.minecraft.world.item.Items.TROPICAL_FISH);
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

    @Override
    public void setAccelerating(boolean accelerating) {
        this.entityData.set(DATA_ACCELERATING, accelerating);
    }

    public void setGroundMoveStateFromRider(int state) {
        int s = Mth.clamp(state, 0, 2);
        if (this.entityData.get(DATA_GROUND_MOVE_STATE) != s) {
            this.entityData.set(DATA_GROUND_MOVE_STATE, s);
        }
        this.syncAnimState(s, getSyncedFlightMode());
    }
    
    // ===== REQUIRED METHODS FROM RIDEABLEDRAGONBASE =====
    
    @Override
    public boolean isRunning() {
        return false; // Rift Drake doesn't have running state
    }
    
    @Override
    public void setRunning(boolean running) {
        // Rift Drake doesn't have running state
    }
    
    @Override
    public boolean isDragonFlying() {
        return false; // Rift Drake doesn't fly
    }
    
    @Override
    public boolean isHovering() {
        return false; // Rift Drake doesn't hover
    }
    
    @Override
    public boolean isTakeoff() {
        return false; // Rift Drake doesn't take off
    }
    
    @Override
    public boolean isLanding() {
        return false; // Rift Drake doesn't land
    }
    
    @Override
    public int getFlightMode() {
        return -1; // Ground mode
    }


    public void handleJumpRequest() {
        if (this.isInWater()) {
            // Enhanced water jump - more powerful for aquatic creature
            Vec3 jump = new Vec3(0.0D, 0.6D, 0.0D);
            this.setDeltaMovement(this.getDeltaMovement().add(jump));
            this.hasImpulse = true;
        } else if (this.onGround()) {
            // Ground jump - standard jump height
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x, 0.42D, movement.z);
            this.hasImpulse = true;
        }
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        Vec3 input = riderController.getRiddenInput(player, deltaIn);
        
        // Capture rider inputs for animation state
        if (!level().isClientSide) {
            float fwd = (float) Mth.clamp(input.z, -1.0D, 1.0D);
            float str = (float) Mth.clamp(input.x, -1.0D, 1.0D);
            setLastRiderForward(RideableDragonData.applyInputThreshold(fwd));
            setLastRiderStrafe(RideableDragonData.applyInputThreshold(str));
        }
        return input;
    }

    @Override
    protected float getRiddenSpeed(@Nonnull @NotNull Player rider) {
        return riderController.getRiddenSpeed(rider);
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        riderController.tickRidden(player, travelVector);
    }

    @Override
    public void removePassenger(@NotNull Entity passenger) {
        super.removePassenger(passenger);
        if (!this.level().isClientSide) {
            this.setAccelerating(false);
            this.setLastRiderForward(0.0F);
            this.setLastRiderStrafe(0.0F);
            this.setGroundMoveStateFromRider(0);
        }
    }

    // Let RideableDragonBase handle tickAnimationStates() for proper networking

    @Override
    public void jumpFromGround() {
        super.jumpFromGround();
        this.setGroundMoveStateFromRider(1);
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

    // ===== RIDING METHODS =====
    
    @Override
    public double getPassengersRidingOffset() {
        return riderController.getPassengersRidingOffset();
    }

    @Override
    protected void positionRider(@Nonnull @NotNull Entity passenger, @Nonnull @NotNull Entity.MoveFunction moveFunction) {
        riderController.positionRider(passenger, moveFunction);
    }

    @Override
    public @Nullable net.minecraft.world.entity.LivingEntity getControllingPassenger() {
        return riderController.getControllingPassenger();
    }


    @Override
    public net.minecraft.world.entity.AgeableMob getBreedOffspring(@Nonnull net.minecraft.server.level.ServerLevel level, @Nonnull net.minecraft.world.entity.AgeableMob other) {
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
        
        this.entityData.set(DATA_SWIMMING, true);
        if (waterSwimGoal != null) {
            waterSwimGoal.forceTrigger();
        }
    }

    private void exitSwimState() {
        swimming = false;
        this.navigation = groundNavigation;
        this.moveControl = landMoveControl;
        this.lookControl = landLookControl; // Always use land look control when exiting water
        this.waterNavigation.stop();
        Vec3 delta = this.getDeltaMovement();
        this.setDeltaMovement(new Vec3(delta.x, 0.0D, delta.z));
        this.entityData.set(DATA_SWIMMING, false);
        this.entityData.set(DATA_SWIM_TURN, 0);
        this.entityData.set(DATA_SWIM_PITCH, 0);
        this.swimTurnSmoothedYaw = 0.0F;
        this.swimTurnState = 0;
        this.swimPitchStateTicks = 0;
    }

    private void updateSwimOrientationState() {
        if (level().isClientSide) {
            return;
        }

        int desiredTurn = 0;
        int desiredPitchState = this.entityData.get(DATA_SWIM_PITCH);

        boolean shouldTrack = this.isTame()
                && this.isVehicle()
                && this.getControllingPassenger() instanceof Player player
                && this.isOwnedBy(player)
                && this.isInWaterOrBubble();

        if (shouldTrack) {
            float yawDelta = Mth.wrapDegrees(this.getYRot() - this.yRotO);
            swimTurnSmoothedYaw = swimTurnSmoothedYaw * 0.6F + yawDelta * 0.4F;
            float enter = 0.35F;
            float exit = 0.12F;

            int targetState = swimTurnState;
            if (swimTurnSmoothedYaw > enter) {
                targetState = -1;
            } else if (swimTurnSmoothedYaw < -enter) {
                targetState = 1;
            } else if (Math.abs(swimTurnSmoothedYaw) < exit) {
                targetState = 0;
            }

            if (targetState != swimTurnState) {
                swimTurnState = targetState;
            }
            desiredTurn = swimTurnState;

            if (this.isGoingUp()) {
                desiredPitchState = -1;
                swimPitchStateTicks = 6;
            } else if (this.isGoingDown()) {
                desiredPitchState = 1;
                swimPitchStateTicks = 6;
            } else {
                if (desiredPitchState != 0) {
                    if (swimPitchStateTicks > 0) {
                        swimPitchStateTicks--;
                    } else {
                        desiredPitchState = 0;
                    }
                }
            }
        } else {
            swimTurnSmoothedYaw *= 0.5F;
            if (swimTurnState != 0 && Math.abs(swimTurnSmoothedYaw) < 0.05F) {
                swimTurnState = 0;
            }
            if (this.entityData.get(DATA_SWIM_TURN) != 0) {
                desiredTurn = 0;
            }

            if (desiredPitchState != 0) {
                if (++swimPitchStateTicks > 4) {
                    desiredPitchState = 0;
                    swimPitchStateTicks = 0;
                }
            } else {
                swimPitchStateTicks = 0;
            }
        }

        if (this.entityData.get(DATA_SWIM_TURN) != desiredTurn) {
            this.entityData.set(DATA_SWIM_TURN, desiredTurn);
        }

        if (this.entityData.get(DATA_SWIM_PITCH) != desiredPitchState) {
            this.entityData.set(DATA_SWIM_PITCH, desiredPitchState);
        }
    }

    public boolean isSwimming() {
        if (level().isClientSide) {
            return this.entityData.get(DATA_SWIMMING);
        }
        return swimming;
    }

    public int getSwimTurnDirection() {
        return Mth.clamp(this.entityData.get(DATA_SWIM_TURN), -1, 1);
    }

    public boolean isSwimmingDown() {
        return this.isSwimming() && this.entityData.get(DATA_SWIM_PITCH) > 0;
    }

    public boolean isSwimmingUp() {
        return this.isSwimming() && this.entityData.get(DATA_SWIM_PITCH) < 0;
    }

    private void handleRiddenSwimming(Vec3 input) {
        Vec3 velocity = this.getDeltaMovement();
        Vec3 wishDir = input;

        double swimSpeed = getSwimSpeed();
        if (isAccelerating()) {
            swimSpeed *= 1.6D;
        }

        double strafe = wishDir.x;
        double forward = wishDir.z;
        float yawRad = this.getYRot() * ((float)Math.PI / 180F);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double worldX = strafe * cos - forward * sin;
        double worldZ = forward * cos + strafe * sin;

        double dx = worldX * 0.85D * swimSpeed;
        double dz = worldZ * 0.85D * swimSpeed;

        double dy = velocity.y;
        if (isGoingUp()) {
            dy = Math.min(swimSpeed, dy + 0.12D * swimSpeed);
        } else if (isGoingDown()) {
            dy = Math.max(-swimSpeed, dy - 0.12D * swimSpeed);
        } else {
            dy *= 0.90D;
        }

        Vec3 desired = new Vec3(dx, dy, dz);
        Vec3 blended = velocity.add(desired.subtract(velocity).scale(0.28D));

        double dragFactor = this.isControlledByLocalInstance() ? 0.92D : 0.94D;
        blended = blended.multiply(dragFactor, 0.92D, dragFactor);

        if (!isGoingUp() && !isGoingDown() && getTarget() == null) {
            blended = blended.add(0.0D, -0.01D, 0.0D);
        }

        this.setDeltaMovement(blended);
        this.move(MoverType.SELF, this.getDeltaMovement());
    }
    
    /**
     * Check if the dragon is dying (health below 10%)
     */
    public boolean isDying() {
        return this.getHealth() < this.getMaxHealth() * 0.1f;
    }
    
    // Required methods for RideableDragon interface
    @Override
    public boolean isGoingUp() {
        return this.entityData.get(DATA_GOING_UP);
    }
    
    @Override
    public void setGoingUp(boolean goingUp) {
        this.entityData.set(DATA_GOING_UP, goingUp);
    }
    
    @Override
    public boolean isGoingDown() {
        return this.entityData.get(DATA_GOING_DOWN);
    }
    
    @Override
    public void setGoingDown(boolean goingDown) {
        this.entityData.set(DATA_GOING_DOWN, goingDown);
    }

    private static class RiftDrakeMoveControl extends MoveControl {

        public RiftDrakeMoveControl(RiftDrakeEntity drake) {
            super(drake);
        }

        @Override
        public void tick() {
            super.tick();
        }
    }

    // ===== LOOK CONTROLLER =====
    public static class RiftDrakeLookController extends LookControl {
        private final RiftDrakeEntity dragon;

        public RiftDrakeLookController(RiftDrakeEntity dragon) {
            super(dragon);
            this.dragon = dragon;
        }

        @Override
        public void tick() {
            if (this.dragon.isAlive()) {
                super.tick();
            }
        }
    }
}
