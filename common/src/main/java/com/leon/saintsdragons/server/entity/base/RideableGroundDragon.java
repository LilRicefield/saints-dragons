package com.leon.saintsdragons.server.entity.base;

import com.leon.saintsdragons.common.network.DragonRiderAction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class RideableGroundDragon extends RideableDragonBase implements PlayerRideableJumping {
    private static final int MIN_JUMP_AIRBORNE_TICKS = 2;
    private static final int MAX_JUMP_LAUNCH_WAIT_TICKS = 8;
    private static final EntityDataAccessor<Boolean> DATA_RIDER_GROUND_JUMPING =
            SynchedEntityData.defineId(RideableGroundDragon.class, EntityDataSerializers.BOOLEAN);
    private float playerJumpPendingScale = 0.0F;
    private boolean riderJumping = false;
    private boolean riderJumpLeftGround = false;
    private int riderJumpAnimationTicks = 0;
    private int riderJumpAnimationHoldTicks = 0;
    private boolean trackingJumpLanding = false;
    private int jumpTrackingLaunchWaitTicks = 0;
    private int jumpTrackingAirborneTicks = 0;

    protected RideableGroundDragon(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RIDER_GROUND_JUMPING, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            tickRiderGroundJumpAnimationState();
        }
        if (usesGroundJumpLandingAnimation() && !isBaby()) {
            tickGroundJumpLandingAnimation();
        }
    }

    @Override
    public boolean canTakeoff() {
        return false;
    }

    @Override
    protected void applyRiderVerticalInput(Player player, boolean goingUp, boolean goingDown, boolean locked) {
        setGoingUp(false);
        setGoingDown(false);
    }

    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case GROUND_JUMP -> true;
            case TAKEOFF_REQUEST -> false;
            default -> super.supportsRiderAction(action);
        };
    }

    @Override
    protected boolean handleCustomRiderAction(ServerPlayer player, DragonRiderAction action,
                                              String abilityName, boolean locked) {
        if (!locked && action == DragonRiderAction.GROUND_JUMP) {
            onPlayerJump(parseGroundJumpPower(abilityName));
            return true;
        }
        return super.handleCustomRiderAction(player, action, abilityName, locked);
    }

    private int parseGroundJumpPower(String jumpPower) {
        if (jumpPower == null) {
            return 100;
        }
        try {
            return Mth.clamp(Integer.parseInt(jumpPower), 0, 100);
        } catch (NumberFormatException ignored) {
            return 100;
        }
    }

    @Override
    public void onPlayerJump(int jumpPower) {
        if (!canJump()) {
            return;
        }
        queueRiderJump(jumpPower);
    }

    private void queueRiderJump(int jumpPower) {
        if (jumpPower < 0) {
            jumpPower = 0;
        }
        playerJumpPendingScale = jumpPower >= 90
                ? 1.0F
                : 0.4F + 0.4F * jumpPower / 90.0F;
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        if (isGroundedForRiderJump()) {
            riderJumping = false;
            if (playerJumpPendingScale > 0.0F && !riderJumping) {
                executeRiderJump(playerJumpPendingScale, travelVector);
            }
            playerJumpPendingScale = 0.0F;
        }
    }

    private void executeRiderJump(float jumpScale, Vec3 travelVector) {
        if (!canJump()) {
            playerJumpPendingScale = 0.0F;
            return;
        }

        float charge = Mth.clamp(jumpScale, 0.0F, 1.0F);
        double vertical = getRiderJumpStrength() * charge * getBlockJumpFactor() + getJumpBoostPower();
        Vec3 current = getDeltaMovement();
        setDeltaMovement(current.x, vertical, current.z);
        if (travelVector.z > 0.0D) {
            float yawRad = getYRot() * Mth.DEG_TO_RAD;
            setDeltaMovement(getDeltaMovement().add(
                    -getRiderJumpForwardBoost() * Mth.sin(yawRad) * charge,
                    0.0D,
                    getRiderJumpForwardBoost() * Mth.cos(yawRad) * charge
            ));
        }
        hasImpulse = true;
        hurtMarked = true;
        fallDistance = 0.0F;
        riderJumping = true;
        onGroundDragonJumped(Mth.floor(jumpScale * 100.0F));
    }

    @Override
    public boolean canJump() {
        return isVehicle()
                && getFirstPassenger() instanceof Player player
                && canBeControlledBy(player)
                && isGroundedForRiderJump()
                && !isBaby()
                && !isInWaterOrBubble()
                && !areRiderControlsLocked()
                && canGroundDragonJump();
    }

    protected boolean isGroundedForRiderJump() {
        return onGround() || verticalCollisionBelow;
    }

    protected boolean canGroundDragonJump() {
        return true;
    }

    protected void onGroundDragonJumped(int jumpPower) {
        updateRiderGroundMoveState(getLastRiderForward(), getLastRiderStrafe());
        riderJumpLeftGround = false;
        riderJumpAnimationTicks = 0;
        riderJumpAnimationHoldTicks = 0;
        this.entityData.set(DATA_RIDER_GROUND_JUMPING, true);
        if (usesGroundJumpLandingAnimation()) {
            trackingJumpLanding = true;
            jumpTrackingLaunchWaitTicks = 0;
            jumpTrackingAirborneTicks = 0;
            triggerGroundJumpAnimation();
        }
    }

    private void tickGroundJumpLandingAnimation() {
        if (!trackingJumpLanding) {
            return;
        }

        if (!isAlive() || !isVehicle() || isInWaterOrBubble()) {
            resetGroundJumpLandingTracking();
            return;
        }

        boolean grounded = isGroundedForRiderJump();
        if (!grounded) {
            jumpTrackingAirborneTicks++;
            return;
        }

        if (jumpTrackingAirborneTicks >= MIN_JUMP_AIRBORNE_TICKS) {
            triggerGroundJumpLandedAnimation();
            resetGroundJumpLandingTracking();
        } else if (jumpTrackingAirborneTicks > 0) {
            resetGroundJumpLandingTracking();
        } else if (++jumpTrackingLaunchWaitTicks > MAX_JUMP_LAUNCH_WAIT_TICKS) {
            resetGroundJumpLandingTracking();
        }
    }

    private void resetGroundJumpLandingTracking() {
        trackingJumpLanding = false;
        jumpTrackingLaunchWaitTicks = 0;
        jumpTrackingAirborneTicks = 0;
    }

    protected boolean usesGroundJumpLandingAnimation() {
        return false;
    }

    protected void triggerGroundJumpAnimation() {
    }

    protected void triggerGroundJumpLandedAnimation() {
    }

    public boolean isRiddenGroundJumpAirborne() {
        return this.entityData.get(DATA_RIDER_GROUND_JUMPING)
                && isVehicle()
                && getControllingPassenger() instanceof Player
                && !isInWaterOrBubble()
                && !onGround()
                && !verticalCollisionBelow
                && (getDeltaMovement().y > 0.0D || riderJumpAnimationHoldTicks > 0);
    }

    private void tickRiderGroundJumpAnimationState() {
        if (!this.entityData.get(DATA_RIDER_GROUND_JUMPING)) {
            riderJumpLeftGround = false;
            riderJumpAnimationTicks = 0;
            return;
        }

        riderJumpAnimationTicks++;
        if (this.isInWaterOrBubble() || !this.isVehicle()) {
            stopRiderGroundJumpAnimation();
            return;
        }

        if (!this.onGround() && !this.verticalCollisionBelow) {
            riderJumpLeftGround = true;
            if (this.getDeltaMovement().y <= 0.0D) {
                if (riderJumpAnimationHoldTicks <= 0) {
                    riderJumpAnimationHoldTicks = getRiderGroundJumpAnimationFallHoldTicks();
                }
                if (riderJumpAnimationHoldTicks-- <= 0) {
                    stopRiderGroundJumpAnimation();
                }
            }
            return;
        }

        if (riderJumpLeftGround || riderJumpAnimationTicks > 8) {
            stopRiderGroundJumpAnimation();
        }
    }

    private void stopRiderGroundJumpAnimation() {
        this.entityData.set(DATA_RIDER_GROUND_JUMPING, false);
        riderJumpLeftGround = false;
        riderJumpAnimationTicks = 0;
        riderJumpAnimationHoldTicks = 0;
    }

    protected int getRiderGroundJumpAnimationFallHoldTicks() {
        return 0;
    }

    protected abstract double getRiderJumpStrength();

    protected abstract double getRiderJumpForwardBoost();

    protected void travelStandardRiddenGround(Player player, Vec3 riderInput, float riddenSpeed) {
        if (areRiderControlsLocked()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (getNavigation().getPath() != null) {
            getNavigation().stop();
        }
        setGoingUp(false);
        setGoingDown(false);
        travelRiddenGround(player, riderInput, riddenSpeed);
    }

    protected void travelRiddenGround(Player player, Vec3 riderInput, float riddenSpeed) {
        setSpeed(riddenSpeed);

        Vec3 input = normalizeGroundInput(riderInput);
        Vec3 current = getDeltaMovement();
        double horizontalScale = riddenSpeed * getRiddenGroundVelocityMultiplier();
        Vec3 desiredHorizontal = input.lengthSqr() > 1.0E-7D
                ? input.yRot(-getYRot() * Mth.DEG_TO_RAD).scale(horizontalScale)
                : Vec3.ZERO;

        setDeltaMovement(desiredHorizontal.x, current.y, desiredHorizontal.z);
        move(MoverType.SELF, getDeltaMovement());

        Vec3 moved = getDeltaMovement();
        float friction = onGround() ? getRiddenGroundFriction() : getRiddenAirFriction();
        double nextY = moved.y;
        if (!isNoGravity()) {
            nextY -= getRiddenGroundGravity();
        }
        setDeltaMovement(moved.x * friction, nextY * getRiddenVerticalDrag(), moved.z * friction);
        calculateEntityAnimation(false);
    }

    private Vec3 normalizeGroundInput(Vec3 riderInput) {
        double x = Mth.clamp(riderInput.x, -1.0D, 1.0D);
        double z = Mth.clamp(riderInput.z, -1.0D, 1.0D);
        Vec3 input = new Vec3(x, 0.0D, z);
        return input.lengthSqr() > 1.0D ? input.normalize() : input;
    }

    protected double getRiddenGroundVelocityMultiplier() {
        return 2.15D;
    }

    protected float getRiddenGroundFriction() {
        return 0.82F;
    }

    protected float getRiddenAirFriction() {
        return 0.91F;
    }

    protected double getRiddenGroundGravity() {
        return 0.08D;
    }

    protected double getRiddenVerticalDrag() {
        return 0.98D;
    }

    @Override
    public void handleStartJump(int jumpPower) {
    }

    @Override
    public void handleStopJump() {
    }

    @Override
    public int getJumpCooldown() {
        return 0;
    }

}
