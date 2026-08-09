package com.leon.saintsdragons.server.entity.controller.atroxiia;

import com.leon.saintsdragons.server.entity.controller.DragonRiderControllerHelper;
import com.leon.saintsdragons.server.entity.controller.GroundDragonRiderControllerHelper;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record AtroxiiaRiderController(Atroxiia dragon) {
    private static final float RIDER_KEY_PITCH_DEG = 25.0F;
    private static final double SEAT_BASE_FACTOR = 0.8D;
    private static final double SWIM_SPEED = 0.30D;
    private static final double SPRINT_SWIM_SPEED = 0.42D;
    private static final double SWIM_RESPONSE = 0.28D;
    private static final double SWIM_ASCEND_THRUST = 0.10D;
    private static final double SWIM_DESCEND_THRUST = 0.12D;
    private static final double SWIM_VERTICAL_LIMIT = 0.36D;
    private static final double SWIM_PITCH_VERTICAL_SCALE = 0.65D;

    @Nullable
    public Player getRidingPlayer() {
        return GroundDragonRiderControllerHelper.getRidingPlayer(dragon);
    }

    public Vec3 getRiddenInput(Player player, @SuppressWarnings("unused") Vec3 deltaIn) {
        if (dragon.isInWaterOrBubble()) {
            float reverseScale = player.zza < 0.0F ? 0.5F : 1.0F;
            return new Vec3(player.xxa * 0.6F, 0.0D, player.zza * reverseScale);
        }
        return GroundDragonRiderControllerHelper.standardGroundInput(player);
    }

    public void tickRidden(Player player, @SuppressWarnings("unused") Vec3 travelVector) {
        GroundDragonRiderControllerHelper.tickStandardGroundRider(dragon, player);
    }

    public float getRiddenSpeed(Player player) {
        if (dragon.isInWaterOrBubble()) {
            return (float) (dragon.isAccelerating() ? SPRINT_SWIM_SPEED : SWIM_SPEED);
        }
        double speed = dragon.isAccelerating() ? Atroxiia.RIDER_RUN_SPEED : Atroxiia.RIDER_WALK_SPEED;
        return (float) (speed * dragon.getHappinessSpeedMultiplier());
    }

    public void handleRiddenSwimming(Player player, Vec3 input) {
        Vec3 velocity = dragon.getDeltaMovement();
        double swimSpeed = dragon.isAccelerating() ? SPRINT_SWIM_SPEED : SWIM_SPEED;

        double forwardInput = input.z;
        double strafeInput = input.x;
        boolean hasInput = Math.abs(forwardInput) > 0.01D || Math.abs(strafeInput) > 0.01D;
        float yawRad = (float) Math.toRadians(dragon.getYRot());
        float pitchRad = DragonRiderControllerHelper.resolveRiderPitchRadians(
                dragon,
                player,
                RIDER_KEY_PITCH_DEG
        );
        double forwardXZ = Math.cos(pitchRad);
        double forwardX = -Math.sin(yawRad) * forwardXZ;
        double forwardY = -Math.sin(pitchRad);
        double forwardZ = Math.cos(yawRad) * forwardXZ;
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        boolean verticalInputActive = dragon.isGoingUp() || dragon.isGoingDown();

        Vec3 desiredDirection = new Vec3(
                forwardX * forwardInput + rightX * strafeInput * 0.6D,
                verticalInputActive ? 0.0D : forwardY * forwardInput * SWIM_PITCH_VERTICAL_SCALE,
                forwardZ * forwardInput + rightZ * strafeInput * 0.6D
        );

        Vec3 desiredVelocity;
        if (hasInput && desiredDirection.lengthSqr() > 1.0E-6D) {
            desiredVelocity = desiredDirection.normalize().scale(swimSpeed);
        } else {
            desiredVelocity = new Vec3(0.0D, velocity.y * 0.9D, 0.0D);
        }

        Vec3 blended = velocity.add(desiredVelocity.subtract(velocity).scale(SWIM_RESPONSE));
        double horizontalDrag = dragon.isControlledByLocalInstance() ? 0.92D : 0.94D;
        blended = blended.multiply(horizontalDrag, 0.96D, horizontalDrag);

        double verticalVelocity = blended.y;
        if (dragon.isGoingUp()) {
            verticalVelocity = Math.min(SWIM_VERTICAL_LIMIT, verticalVelocity + SWIM_ASCEND_THRUST);
        } else if (dragon.isGoingDown()) {
            verticalVelocity = Math.max(-SWIM_VERTICAL_LIMIT, verticalVelocity - SWIM_DESCEND_THRUST);
        }
        blended = new Vec3(blended.x, verticalVelocity, blended.z);

        dragon.setDeltaMovement(blended);
        dragon.move(MoverType.SELF, blended);
        dragon.hasImpulse = true;
    }

    public double getPassengersRidingOffset() {
        return dragon.getBbHeight() * SEAT_BASE_FACTOR;
    }

    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        GroundDragonRiderControllerHelper.positionLocatorRider(dragon, passenger, moveFunction, getPassengersRidingOffset());
    }

    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return GroundDragonRiderControllerHelper.getDismountLocationForPassenger(dragon, passenger);
    }

    @Nullable
    public Player getControllingPassenger() {
        return GroundDragonRiderControllerHelper.getOwnedControllingPassenger(dragon);
    }
}
