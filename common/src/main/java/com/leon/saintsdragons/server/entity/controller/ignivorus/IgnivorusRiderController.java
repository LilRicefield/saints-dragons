package com.leon.saintsdragons.server.entity.controller.ignivorus;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles all riding mechanics for the Ignivorus
 */
public record IgnivorusRiderController(Ignivorus dragon) {

    // ===== SEAT TUNING CONSTANTS =====
    private static final double SEAT_BASE_FACTOR = 0.50D;

    // ===== FLIGHT PHYSICS =====
    private static final double CRUISE_SPEED_MULT = 4.0;
    private static final double SPRINT_SPEED_MULT = 7.0;
    private static final double ACCELERATION = 0.15;
    private static final double DRAG_WITH_INPUT = 0.08;
    private static final double DRAG_NO_INPUT = 0.5;
    private static final double STRAFE_POWER = 0.5;

    // ===== VERTICAL PHYSICS =====
    private static final double ASCEND_THRUST = 0.08D;
    private static final double DESCEND_THRUST = 1.0D;
    private static final double TERMINAL_VELOCITY = 1.5D;
    private static final double VERTICAL_DRAG = 0.92D;

    @Nullable
    public Player getRidingPlayer() {
        if (dragon.getControllingPassenger() instanceof Player player) {
            return player;
        }
        return null;
    }

    public Vec3 getRiddenInput(Player player, Vec3 deltaIn) {
        float f = player.zza < 0.0F ? 0.5F : 1.0F;
        if (dragon.isFlying()) {
            return new Vec3(player.xxa * 0.4F, 0.0F, player.zza * 1.0F * f);
        } else {
            return new Vec3(player.xxa * 0.5F, 0.0D, player.zza * 0.9F * f);
        }
    }

    public void tickRidden(Player player, Vec3 travelVector) {
        player.fallDistance = 0.0F;
        dragon.fallDistance = 0.0F;
        dragon.setTarget(null);

        boolean flying = dragon.isFlying();

        // Always sync yaw with player's look direction
        float currentYaw = dragon.getYRot();
        float targetYaw = player.getYRot();
        float rawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float blend = flying ? 0.35f : 0.28f;
        float newYaw = currentYaw + (rawDiff * blend);

        dragon.setYRot(newYaw);
        dragon.yBodyRot = newYaw;
        dragon.yHeadRot = newYaw;

        // Pitch control: ONLY use vertical input (Space/L-Alt), NOT mouse look
        if (flying) {
            float targetPitch = 0.0F;
            if (dragon.isGoingUp()) {
                targetPitch = -25.0F; // Pitch up when ascending
            } else if (dragon.isGoingDown()) {
                targetPitch = 20.0F; // Pitch down when descending
            }
            dragon.setXRot(targetPitch);
        } else {
            dragon.setXRot(0.0F);
        }

        // Auto-land when descending near ground (but not during takeoff animation)
        if (flying && dragon.onGround() && !dragon.isGoingUp() && !dragon.isTakeoff()) {
            dragon.setFlying(false);
            dragon.setLanding(false);
            dragon.setTakeoff(false);
        }

        if (dragon.onGround()) {
            player.fallDistance = 0.0F;
            dragon.fallDistance = 0.0F;
        }
    }

    public float getRiddenSpeed(Player rider) {
        if (dragon.isFlying()) {
            return (float) dragon.getAttributeValue(Attributes.FLYING_SPEED);
        } else {
            float baseSpeed = (float) dragon.getAttributeValue(Attributes.MOVEMENT_SPEED);
            if (dragon.isAccelerating()) {
                return baseSpeed * 0.7F;
            } else {
                return baseSpeed * 0.5F;
            }
        }
    }

    public void handleRiderMovement(Player player, Vec3 motion) {
        if (dragon.getNavigation().getPath() != null) {
            dragon.getNavigation().stop();
        }

        // Ground movement is handled by Ignivorus.travel() calling super.travel()
        if (dragon.isFlying()) {
            final double baseSpeed = dragon.getAttributeValue(Attributes.FLYING_SPEED);
            final boolean sprinting = dragon.isAccelerating();
            final double targetSpeed = (sprinting ? SPRINT_SPEED_MULT : CRUISE_SPEED_MULT) * baseSpeed;

            Vec3 currentVelocity = dragon.getDeltaMovement();
            Vec3 horizontalVel = new Vec3(currentVelocity.x, 0.0, currentVelocity.z);

            double forwardInput = motion.z;
            double strafeInput = motion.x;
            boolean hasInput = Math.abs(forwardInput) > 0.01 || Math.abs(strafeInput) > 0.01;

            float yawRad = (float) Math.toRadians(dragon.getYRot());
            double forwardX = -Math.sin(yawRad);
            double forwardZ = Math.cos(yawRad);
            double rightX = Math.cos(yawRad);
            double rightZ = Math.sin(yawRad);

            double targetDirX = forwardX * forwardInput + rightX * (strafeInput * STRAFE_POWER);
            double targetDirZ = forwardZ * forwardInput + rightZ * (strafeInput * STRAFE_POWER);
            double dirLength = Math.hypot(targetDirX, targetDirZ);

            Vec3 newHorizontalVel;

            if (hasInput && dirLength > 0.01) {
                targetDirX /= dirLength;
                targetDirZ /= dirLength;

                Vec3 targetVelocity = new Vec3(targetDirX * targetSpeed, 0, targetDirZ * targetSpeed);
                newHorizontalVel = new Vec3(
                    Mth.lerp(ACCELERATION, horizontalVel.x, targetVelocity.x),
                    0,
                    Mth.lerp(ACCELERATION, horizontalVel.z, targetVelocity.z)
                );
                newHorizontalVel = newHorizontalVel.scale(1.0 - DRAG_WITH_INPUT);
            } else {
                newHorizontalVel = horizontalVel.scale(1.0 - DRAG_NO_INPUT);
                if (newHorizontalVel.length() < 0.01) {
                    newHorizontalVel = Vec3.ZERO;
                }
            }

            double verticalVel = currentVelocity.y;

            // Auto-ascend during takeoff phase to ensure we get airborne
            if (dragon.isTakeoff() && dragon.timeFlying < 15) {
                verticalVel += ASCEND_THRUST * 1.5;
            } else if (dragon.isGoingUp()) {
                verticalVel += ASCEND_THRUST;
            } else if (dragon.isGoingDown()) {
                verticalVel -= DESCEND_THRUST;
            } else {
                verticalVel *= VERTICAL_DRAG;
            }
            verticalVel = Mth.clamp(verticalVel, -TERMINAL_VELOCITY, TERMINAL_VELOCITY);

            Vec3 finalVelocity = new Vec3(newHorizontalVel.x, verticalVel, newHorizontalVel.z);
            dragon.move(MoverType.SELF, finalVelocity);
            dragon.setDeltaMovement(finalVelocity);
            dragon.calculateEntityAnimation(true);

            player.fallDistance = 0.0F;
            dragon.fallDistance = 0.0F;
        }
    }

    public double getPassengersRidingOffset() {
        return (double) dragon.getBbHeight() * SEAT_BASE_FACTOR;
    }

    public void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction moveFunction) {
        if (!dragon.hasPassenger(passenger)) return;

        Vec3 passengerLoc = dragon.getClientLocatorPosition("passengerLocator");

        if (passengerLoc != null) {
            Vec3 dragonOldPos = new Vec3(dragon.xo, dragon.yo, dragon.zo);
            float oldYaw = dragon.yRotO;
            Vec3 worldOffset = passengerLoc.subtract(dragonOldPos);

            double oldYawRad = Math.toRadians(-oldYaw);
            double cosOld = Math.cos(oldYawRad);
            double sinOld = Math.sin(oldYawRad);
            double localX = worldOffset.x * cosOld - worldOffset.z * sinOld;
            double localY = worldOffset.y;
            double localZ = worldOffset.x * sinOld + worldOffset.z * cosOld;

            float currentYaw = dragon.getYRot();
            double currentYawRad = Math.toRadians(-currentYaw);
            double cosCurrent = Math.cos(currentYawRad);
            double sinCurrent = Math.sin(currentYawRad);
            double currentWorldX = localX * cosCurrent + localZ * sinCurrent;
            double currentWorldZ = -localX * sinCurrent + localZ * cosCurrent;

            Vec3 dragonCurrentPos = dragon.position();
            Vec3 passengerCurrentPos = dragonCurrentPos.add(currentWorldX, localY, currentWorldZ);

            moveFunction.accept(passenger, passengerCurrentPos.x, passengerCurrentPos.y, passengerCurrentPos.z);
        } else {
            double x = dragon.getX();
            double y = dragon.getY() + getPassengersRidingOffset() + passenger.getMyRidingOffset();
            double z = dragon.getZ();
            moveFunction.accept(passenger, x, y, z);
        }
    }

    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        passenger.fallDistance = 0.0F;
        Vec3 base = dragon.position();
        Vec3 direction = dragon.getViewVector(1.0F);
        return base.add(direction.scale(2.0));
    }

    @Nullable
    public LivingEntity getControllingPassenger() {
        Entity entity = dragon.getFirstPassenger();
        if (entity instanceof Player player && dragon.isTame() && dragon.isOwnedBy(player)) {
            return player;
        }
        return null;
    }

    public void requestRiderTakeoff() {
        if (!dragon.isTame() || getRidingPlayer() == null || dragon.isFlying()) return;

        dragon.timeFlying = 0;
        dragon.setFlying(true);
        dragon.setTakeoff(true);
        dragon.setHovering(false);
        dragon.setLanding(false);

        // Give strong upward launch velocity for takeoff
        Vec3 current = dragon.getDeltaMovement();
        dragon.setDeltaMovement(current.x, Math.max(current.y, 0.5D), current.z);
    }
}
