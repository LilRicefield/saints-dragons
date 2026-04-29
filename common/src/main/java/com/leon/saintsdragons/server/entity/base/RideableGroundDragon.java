package com.leon.saintsdragons.server.entity.base;

import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.server.entity.util.GroundDragonJumpHelper;
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
    private float playerJumpPendingScale = 0.0F;
    private boolean riderJumping = false;

    protected RideableGroundDragon(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
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
            handleStartJump(parseGroundJumpPower(abilityName));
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
    }

    private void queueRiderJump(int jumpPower) {
        if (jumpPower < 0) {
            jumpPower = 0;
        }
        playerJumpPendingScale = jumpPower >= 90
                ? 1.0F
                : 0.25F + 0.55F * jumpPower / 90.0F;
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        if (onGround()) {
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
        GroundDragonJumpHelper.jump(this, jumpScale, travelVector,
                getRiderJumpMinVertical(), getRiderJumpMaxVertical(), getRiderJumpForwardBoost());
        riderJumping = true;
        onGroundDragonJumped(Mth.floor(jumpScale * 100.0F));
    }

    private Vec3 getCurrentRiderJumpInput() {
        if (getControllingPassenger() instanceof Player player) {
            return new Vec3(player.xxa, 0.0D, player.zza);
        }
        return new Vec3(getLastRiderStrafe(), 0.0D, getLastRiderForward());
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
        setGroundMoveStateFromAI(1);
    }

    protected abstract double getRiderJumpMinVertical();

    protected abstract double getRiderJumpMaxVertical();

    protected abstract double getRiderJumpForwardBoost();

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
        queueRiderJump(jumpPower);
        if (playerJumpPendingScale > 0.0F) {
            executeRiderJump(playerJumpPendingScale, getCurrentRiderJumpInput());
            playerJumpPendingScale = 0.0F;
        }
    }

    @Override
    public void handleStopJump() {
    }

    @Override
    public int getJumpCooldown() {
        return 0;
    }

}