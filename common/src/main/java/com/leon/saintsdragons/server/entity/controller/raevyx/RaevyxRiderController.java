package com.leon.saintsdragons.server.entity.controller.raevyx;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxBeamAbility;
import com.leon.saintsdragons.server.flight.DragonRiderFlightPhysics;
import com.leon.saintsdragons.server.flight.DragonRiderSeat;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record RaevyxRiderController(Raevyx wyvern) {
    private static final double SEAT_BASE_FACTOR = 0.50D;
    private static final double SEAT_HEIGHT_ADJUST = 0.00D;
    private static final double CRUISE_SPEED_MULT = 4.95;
    private static final double SPRINT_SPEED_MULT = 5.75;
    private static final double DRAG_NO_INPUT = 0.5;
    private static final double STRAFE_POWER = 0.5;
    private static final double ASCEND_THRUST = 1.2D;
    private static final double DESCEND_THRUST = 1.0D;
    private static final double TERMINAL_VELOCITY = 1.5D;

    @Nullable
    public Player getRidingPlayer() {
        if (wyvern.getControllingPassenger() instanceof Player player) {
            return player;
        }
        return null;
    }

    public Vec3 getRiddenInput(Player player, @SuppressWarnings("unused") Vec3 deltaIn) {
        float f = player.zza < 0.0F ? 0.5F : 1.0F;
        if (wyvern.isFlying()) {
            return new Vec3(player.xxa * 0.4F, 0.0F, player.zza * 1.0F * f);
        } else {
            return new Vec3(player.xxa * 0.5F, 0.0D, player.zza * 0.9F * f);
        }
    }

    public void tickRidden(Player player, @SuppressWarnings("unused") Vec3 travelVector) {
        player.fallDistance = 0.0F;
        wyvern.fallDistance = 0.0F;
        wyvern.setTarget(null);
        boolean flying = wyvern.isFlying();
        float currentYaw = wyvern.getYRot();
        float targetYaw = player.getYRot();
        float rawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float blend = flying ? 0.35f : 0.28f;
        float newYaw = currentYaw + (rawDiff * blend);
        wyvern.setYRot(newYaw);
        wyvern.yBodyRot = newYaw;
        wyvern.yHeadRot = newYaw;
        if (!flying) {
            wyvern.setXRot(0.0F);
        }
        if (wyvern.onGround()) {
            player.fallDistance = 0.0F;
            wyvern.fallDistance = 0.0F;
        }
    }

    public float getRiddenSpeed(@SuppressWarnings("unused") Player rider) {
        if (wyvern.isFlying()) {
            return (float) getMountedFlightBaseSpeed();
        } else {
            boolean isMoving = wyvern.getDeltaMovement().horizontalDistanceSqr() > 0.0001;
            boolean running = wyvern.isAccelerating() && isMoving;
            wyvern.setRunning(running);
            float base = (float) (running ? Raevyx.RIDER_RUN_SPEED : Raevyx.RIDER_WALK_SPEED);
            return base * wyvern.getHappinessSpeedMultiplier();
        }
    }

    public void handleRiderMovement(Player player, Vec3 motion) {
        if (wyvern.getNavigation().getPath() != null) {
            wyvern.getNavigation().stop();
        }
        if (wyvern.isFlying()) {
            final double baseSpeed = getMountedFlightBaseSpeed();
            final boolean sprinting = wyvern.isAccelerating();
            double targetSpeed = (sprinting ? SPRINT_SPEED_MULT : CRUISE_SPEED_MULT) * baseSpeed;

            Vec3 currentVelocity = wyvern.getDeltaMovement();
            final boolean keyPitchMode = wyvern.isRiderPitchKeyMode();
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
            boolean hasInput = Math.abs(forwardInput) > 0.01 || Math.abs(strafeInput) > 0.01;
            float yawRad = (float) Math.toRadians(wyvern.getYRot());
            double forwardXZ = Math.cos(pitchRad);
            double forwardX = -Math.sin(yawRad) * forwardXZ;
            double forwardY = keyPitchMode ? 0.0 : -Math.sin(pitchRad);
            double forwardZ = Math.cos(yawRad) * forwardXZ;
            double rightX = Math.cos(yawRad);
            double rightZ = Math.sin(yawRad);
            double targetDirX = forwardX * forwardInput + rightX * (strafeInput * STRAFE_POWER);
            double targetDirY = forwardY * forwardInput * 1.35;
            double targetDirZ = forwardZ * forwardInput + rightZ * (strafeInput * STRAFE_POWER);
            double dirLength = Math.sqrt(targetDirX * targetDirX + targetDirY * targetDirY + targetDirZ * targetDirZ);
            Vec3 newVelocity;
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
                    Mth.lerp(diveAcceleration, currentVelocity.x, targetVelocity.x),
                    Mth.lerp(diveAcceleration, currentVelocity.y, targetVelocity.y),
                    Mth.lerp(diveAcceleration, currentVelocity.z, targetVelocity.z)
                );
                newVelocity = newVelocity.scale(1.0 - diveDrag);

            } else {
                newVelocity = currentVelocity.scale(1.0 - DRAG_NO_INPUT);
                if (newVelocity.length() < 0.01) {
                    newVelocity = Vec3.ZERO;
                }
            }
            double verticalVel = newVelocity.y;
            boolean isDiving = !keyPitchMode && pitchDegrees >= 45.0f && hasInput;
            if (!isDiving) {
                if (wyvern.getRiderTakeoffTicks() > 0) {
                    verticalVel += ASCEND_THRUST;
                } else if (wyvern.isGoingUp()) {
                    verticalVel += ASCEND_THRUST;
                } else if (wyvern.isGoingDown()) {
                    verticalVel -= DESCEND_THRUST;
                }
                verticalVel = Mth.clamp(verticalVel, -TERMINAL_VELOCITY, TERMINAL_VELOCITY);
            }
            Vec3 finalVelocity = new Vec3(newVelocity.x, verticalVel, newVelocity.z);
            wyvern.move(MoverType.SELF, finalVelocity);
            wyvern.setDeltaMovement(finalVelocity);
            wyvern.calculateEntityAnimation(true);
            player.fallDistance = 0.0F;
            wyvern.fallDistance = 0.0F;
        }
    }

    private float getEffectivePitchRadians(Player player) {
        DragonAbility<?> ability = wyvern.getActiveAbility();
        boolean lockPitch = wyvern.isBeaming()
                || (ability instanceof RaevyxBeamAbility && ability.isUsing());
        if (lockPitch) {
            return 0.0f;
        }
        if (wyvern.isRiderPitchKeyMode()) {
            return getKeyPitchRadians();
        }
        return (float) Math.toRadians(player.getXRot());
    }

    private float getKeyPitchRadians() {
        if (wyvern.isGoingUp()) {
            return (float) -Math.toRadians(Raevyx.RIDER_KEY_PITCH_DEG);
        }
        if (wyvern.isGoingDown()) {
            return (float) Math.toRadians(Raevyx.RIDER_KEY_PITCH_DEG);
        }
        return 0.0f;
    }

    private double getMountedFlightBaseSpeed() {
        return wyvern.getFlightSpeed();
    }
    
    public double getPassengersRidingOffset() {
        return (double) wyvern.getBbHeight() * SEAT_BASE_FACTOR;
    }
    
    public void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction moveFunction) {
        DragonRiderSeat.positionLocatorRider(
                wyvern,
                passenger,
                moveFunction,
                getPassengersRidingOffset() + SEAT_HEIGHT_ADJUST,
                wyvern.getClientLocatorPosition("passengerLocator")
        );
    }
    
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        return DragonRiderSeat.findRadialGroundDismount(
                passenger,
                wyvern,
                new double[] { 2.5D, 3.5D, 1.8D },
                new int[] { 0, 30, -30, 60, -60, 90, -90, 150, -150, 180 },
                6,
                2.0D
        );
    }
    
    @Nullable 
    public LivingEntity getControllingPassenger() {
        Entity entity = wyvern.getFirstPassenger();
        if (entity instanceof Player player && wyvern.isTame() && wyvern.isOwnedBy(player)) {
            return player;
        }
        return null;
    }

    public void requestRiderTakeoff() {
        wyvern.tryRiderTakeoff(getControllingPassenger() instanceof Player player ? player : null);
    }
}