package com.leon.saintsdragons.server.entity.controller.cindervane;

import com.leon.saintsdragons.server.flight.DragonRiderFlightPhysics;
import com.leon.saintsdragons.server.flight.DragonRiderSeat;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record CindervaneRiderController(Cindervane dragon) {

    private static final double SEAT_BASE_FACTOR = 0.05D;
    private static final double SEAT0_HEIGHT_ADJUST = 0.00D;
    private static final double SEAT1_HEIGHT_ADJUST = 0.00D;
    private static final double AUTO_GRAB_HEIGHT_ADJUST = 0.00D;
    private static final double CRUISE_SPEED_MULT = 3.75;
    private static final double SPRINT_SPEED_MULT = 4.55;
    private static final double DRAG_NO_INPUT = 0.45;
    private static final double STRAFE_POWER = 0.4;
    private static final double ASCEND_THRUST = 0.45D;
    private static final double DESCEND_THRUST = 0.85D;
    private static final double TERMINAL_VELOCITY = 1.2D;

    @Nullable
    public Player getRidingPlayer() {
        if (dragon.getControllingPassenger() instanceof Player player) {
            return player;
        }
        return null;
    }

    public Vec3 getRiddenInput(Player player, @SuppressWarnings("unused") Vec3 deltaIn) {
        float f = player.zza < 0.0F ? 0.5F : 1.0F;

        if (dragon.isFlying()) {
            return new Vec3(player.xxa * 0.3F, 0.0F, player.zza * 0.8F * f);
        } else {
            return new Vec3(player.xxa * 0.4F, 0.0D, player.zza * 0.7F * f);
        }
    }

    public void tickRidden(Player player, @SuppressWarnings("unused") Vec3 travelVector) {
        player.fallDistance = 0.0F;
        dragon.fallDistance = 0.0F;
        dragon.setTarget(null);

        boolean flying = dragon.isFlying();
        float currentYaw = dragon.getYRot();
        float targetYaw = player.getYRot();
        float rawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float blend = flying ? 0.30f : 0.25f;
        float newYaw = currentYaw + (rawDiff * blend);
        dragon.setYRot(newYaw);
        dragon.yBodyRot = newYaw;
        dragon.yHeadRot = newYaw;
        if (!flying) {
            dragon.setXRot(0.0F);
        }
        if (dragon.onGround()) {
            player.fallDistance = 0.0F;
            dragon.fallDistance = 0.0F;
        }
    }


    public float getRiddenSpeed(@SuppressWarnings("unused") Player rider) {
        if (dragon.isFlying()) {
            return (float) dragon.getAttributeValue(Attributes.FLYING_SPEED);
        } else {
            boolean running = dragon.isAccelerating();
            dragon.setRunning(running);
            float base = (float) (running ? Cindervane.RIDER_RUN_SPEED : Cindervane.RIDER_WALK_SPEED);
            return base * dragon.getHappinessSpeedMultiplier();
        }
    }

    public void handleRiderMovement(Player player, Vec3 motion) {
        if (dragon.getNavigation().getPath() != null) {
            dragon.getNavigation().stop();
        }

        if (dragon.isFlying()) {
            final double baseSpeed = dragon.getAttributeValue(Attributes.FLYING_SPEED);
            final boolean sprinting = dragon.isAccelerating();
            double targetSpeed = (sprinting ? SPRINT_SPEED_MULT : CRUISE_SPEED_MULT) * baseSpeed;
            Vec3 currentVel = dragon.getDeltaMovement();
            final boolean keyPitchMode = dragon.isRiderPitchKeyMode();
            float pitchRad = getEffectivePitchRadians(player);
            if (keyPitchMode) {
                pitchRad = 0.0f;
            }
            float pitchDegrees = (float) Math.toDegrees(pitchRad);
            DragonRiderFlightPhysics.DiveResponse diveResponse =
                    DragonRiderFlightPhysics.computeDiveResponse(pitchDegrees, keyPitchMode);
            double diveMultiplier = diveResponse.speedMultiplier();
            double diveAcceleration = diveResponse.acceleration();
            double diveDrag = diveResponse.drag();
            targetSpeed *= diveMultiplier;
            double forwardInput = motion.z;
            double strafeInput = motion.x;
            float yawRad = (float) Math.toRadians(dragon.getYRot());
            double forwardXZ = Math.cos(pitchRad);
            double forwardX = -Math.sin(yawRad) * forwardXZ;
            double forwardY = keyPitchMode ? 0.0 : -Math.sin(pitchRad);
            double forwardZ = Math.cos(yawRad) * forwardXZ;
            double strafeX = Math.cos(yawRad);
            double strafeZ = Math.sin(yawRad);
            double targetDirX = forwardX * forwardInput + strafeX * strafeInput * STRAFE_POWER;
            double targetDirY = forwardY * forwardInput * 1.35;
            double targetDirZ = forwardZ * forwardInput + strafeZ * strafeInput * STRAFE_POWER;
            double dirLength = Math.sqrt(targetDirX * targetDirX + targetDirY * targetDirY + targetDirZ * targetDirZ);
            Vec3 newVelocity;
            boolean hasInput = Math.abs(forwardInput) > 0.01 || Math.abs(strafeInput) > 0.01;

            if (hasInput && dirLength > 0.01) {
                targetDirX /= dirLength;
                targetDirY /= dirLength;
                targetDirZ /= dirLength;
                Vec3 targetVelocity = new Vec3(
                    targetDirX * targetSpeed,
                    targetDirY * targetSpeed,
                    targetDirZ * targetSpeed
                );
                newVelocity = new Vec3(
                    Mth.lerp(diveAcceleration, currentVel.x, targetVelocity.x),
                    Mth.lerp(diveAcceleration, currentVel.y, targetVelocity.y),
                    Mth.lerp(diveAcceleration, currentVel.z, targetVelocity.z)
                );
                newVelocity = newVelocity.scale(1.0 - diveDrag);

            } else {
                newVelocity = currentVel.scale(1.0 - DRAG_NO_INPUT);

                if (newVelocity.length() < 0.01) {
                    newVelocity = Vec3.ZERO;
                }
            }
            double finalSpeed = newVelocity.length();
            if (finalSpeed > targetSpeed) {
                newVelocity = newVelocity.scale(targetSpeed / finalSpeed);
            }
            double newVerticalVel = newVelocity.y;

            boolean isDiving = !keyPitchMode && pitchDegrees >= 45.0f && hasInput;

            if (!isDiving) {
                if (dragon.getRiderTakeoffTicks() > 0) {
                    double boost = ASCEND_THRUST * 0.85;
                    newVerticalVel = Math.max(newVerticalVel + boost, 0.45);
                } else if (dragon.isGoingUp()) {
                    newVerticalVel += ASCEND_THRUST;
                } else if (dragon.isGoingDown()) {
                    newVerticalVel -= DESCEND_THRUST;
                }

                newVerticalVel = Mth.clamp(newVerticalVel, -TERMINAL_VELOCITY, TERMINAL_VELOCITY);
            }
            Vec3 finalVelocity = new Vec3(newVelocity.x, newVerticalVel, newVelocity.z);
            dragon.move(MoverType.SELF, finalVelocity);
            dragon.setDeltaMovement(finalVelocity);
            dragon.calculateEntityAnimation(true);
            player.fallDistance = 0.0F;
            dragon.fallDistance = 0.0F;
        }
    }

    private float getEffectivePitchRadians(Player player) {
        if (dragon.isRiderPitchKeyMode()) {
            return getKeyPitchRadians();
        }
        return (float) Math.toRadians(player.getXRot());
    }

    private float getKeyPitchRadians() {
        if (dragon.isGoingUp()) {
            return (float) -Math.toRadians(Cindervane.RIDER_KEY_PITCH_DEG);
        }
        if (dragon.isGoingDown()) {
            return (float) Math.toRadians(Cindervane.RIDER_KEY_PITCH_DEG);
        }
        return 0.0f;
    }

    public double getPassengersRidingOffset() {
        return (double) dragon.getBbHeight() * SEAT_BASE_FACTOR;
    }
    
    public void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction moveFunction) {
        if (!dragon.hasPassenger(passenger)) return;

        if (dragon.isSlashGrabPassenger(passenger)) {
            final String locatorName = "automountBoneRight";
            Vec3 passengerLoc = dragon.getBonePositionForPassenger(locatorName);

            if (passengerLoc != null) {
                DragonRiderSeat.positionLocatorRider(
                        dragon,
                        passenger,
                        moveFunction,
                        getPassengersRidingOffset() + AUTO_GRAB_HEIGHT_ADJUST,
                        passengerLoc,
                        AUTO_GRAB_HEIGHT_ADJUST
                );
            } else {
                float yawRad = (float) Math.toRadians(dragon.getYRot());
                double localX = 0.95D;
                double localZ = -0.15D;
                double worldX = localX * Math.cos(yawRad) - localZ * Math.sin(yawRad);
                double worldZ = localX * Math.sin(yawRad) + localZ * Math.cos(yawRad);

                double x = dragon.getX() + worldX;
                double y = dragon.getY() + getPassengersRidingOffset() + AUTO_GRAB_HEIGHT_ADJUST + passenger.getMyRidingOffset();
                double z = dragon.getZ() + worldZ;
                moveFunction.accept(passenger, x, y, z);
            }
            return;
        }

        var passengers = dragon.getPassengers();
        int seatIndex = passengers.indexOf(passenger);

        if (seatIndex == -1) return; // Passenger not found
        final String locatorName = seatIndex == 0 ? "passengerSeat0" : "passengerSeat1";
        final double seatHeightAdjust = seatIndex == 0 ? SEAT0_HEIGHT_ADJUST : SEAT1_HEIGHT_ADJUST;
        Vec3 passengerLoc = null;
        if (dragon.level().isClientSide) {
            passengerLoc = dragon.getClientLocatorPosition(locatorName);
            if (passengerLoc == null && seatIndex == 0) {
                // Compatibility fallback for packs/older renders using single-seat locator.
                passengerLoc = dragon.getClientLocatorPosition("passengerLocator");
            }
        }

        if (passengerLoc != null) {
            DragonRiderSeat.positionLocatorRider(
                    dragon,
                    passenger,
                    moveFunction,
                    getPassengersRidingOffset() + seatHeightAdjust,
                    passengerLoc,
                    seatHeightAdjust
            );
        } else {
            float yawRad = (float) Math.toRadians(dragon.getYRot());
            double localX = seatIndex == 0 ? -0.45D : 0.45D;
            double localZ = 0.05D;
            double worldX = localX * Math.cos(yawRad) - localZ * Math.sin(yawRad);
            double worldZ = localX * Math.sin(yawRad) + localZ * Math.cos(yawRad);

            double x = dragon.getX() + worldX;
            double y = dragon.getY() + getPassengersRidingOffset() + seatHeightAdjust + passenger.getMyRidingOffset();
            double z = dragon.getZ() + worldZ;
            moveFunction.accept(passenger, x, y, z);
        }
    }
    
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        Vec3 base = dragon.position();

        if (dragon.isSlashGrabPassenger(passenger)) {
            // Slash-grab release should always dismount to dragon-right, not vanilla safe-spot left drift.
            float yawRad = (float) Math.toRadians(dragon.getYRot());
            double localX = 1.8D;
            double localZ = 0.1D;
            double worldX = localX * Math.cos(yawRad) - localZ * Math.sin(yawRad);
            double worldZ = localX * Math.sin(yawRad) + localZ * Math.cos(yawRad);
            return new Vec3(
                    base.x + worldX,
                    base.y + getPassengersRidingOffset() + 0.2D,
                    base.z + worldZ
            );
        }

        return DragonRiderSeat.findRadialGroundDismount(
                passenger,
                dragon,
                new double[] { 2.5D, 3.5D, 1.8D },
                new int[] { 0, 30, -30, 60, -60, 90, -90, 150, -150, 180 },
                6,
                2.0D
        );
    }
    
    @Nullable
    public LivingEntity getControllingPassenger() {
        // Only seat 0 (first passenger) can control, and only if they're the owner
        var passengers = dragon.getPassengers();
        if (passengers.isEmpty()) {
            return null;
        }

        Entity firstPassenger = passengers.get(0);
        if (firstPassenger instanceof Player player && dragon.isTame() && dragon.isOwnedBy(player)) {
            return player;
        }
        return null;
    }
    public void requestRiderTakeoff() {
        dragon.requestRiderTakeoff();
    }
}
