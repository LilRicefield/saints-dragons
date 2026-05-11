package com.leon.saintsdragons.server.entity.controller;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.flight.DragonRiderFlightPhysics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class DragonRiderControllerHelper {
    private DragonRiderControllerHelper() {
    }

    @Nullable
    public static Player getRidingPlayer(RideableDragonBase dragon) {
        return dragon.getControllingPassenger() instanceof Player player ? player : null;
    }

    @Nullable
    public static LivingEntity getOwnerControllingPassenger(RideableDragonBase dragon) {
        Entity entity = dragon.getFirstPassenger();
        if (entity instanceof Player player && dragon.isTame() && dragon.isOwnedBy(player)) {
            return player;
        }
        return null;
    }

    public static Vec3 riddenInput(Player player, boolean flying,
                                   double groundStrafe, double groundForward,
                                   double flightStrafe, double flightForward) {
        float backwardScale = player.zza < 0.0F ? 0.5F : 1.0F;
        if (flying) {
            return new Vec3(player.xxa * flightStrafe, 0.0D, player.zza * flightForward * backwardScale);
        }
        return new Vec3(player.xxa * groundStrafe, 0.0D, player.zza * groundForward * backwardScale);
    }

    public static void clearRiderFallAndTarget(RideableDragonBase dragon, Player player) {
        player.fallDistance = 0.0F;
        dragon.fallDistance = 0.0F;
        dragon.setTarget(null);
    }

    public static void syncYawToRider(RideableDragonBase dragon, Player player, float flyingBlend, float groundBlend) {
        boolean flying = dragon.isFlying();
        float currentYaw = dragon.getYRot();
        float targetYaw = player.getYRot();
        float rawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float blend = flying ? flyingBlend : groundBlend;
        float newYaw = currentYaw + rawDiff * blend;
        dragon.setYRot(newYaw);
        dragon.yBodyRot = newYaw;
        dragon.yHeadRot = newYaw;
        if (!flying) {
            dragon.setXRot(0.0F);
        }
    }

    public static void syncPitchToRider(RideableDragonBase dragon, Player player, float blend, float maxAbsPitchDegrees) {
        float targetPitch = Mth.clamp(player.getXRot(), -maxAbsPitchDegrees, maxAbsPitchDegrees);
        float blendedPitch = Mth.lerp(blend, dragon.getXRot(), targetPitch);
        dragon.xRotO = dragon.getXRot();
        dragon.setXRot(blendedPitch);
    }

    public static float resolveRiderPitchRadians(RideableDragonBase dragon, Player player, float keyPitchDegrees) {
        return resolveRiderPitchRadians(dragon, player, keyPitchDegrees, false);
    }

    public static float resolveRiderPitchRadians(RideableDragonBase dragon, Player player,
                                                 float keyPitchDegrees, boolean lockPitch) {
        if (lockPitch) {
            return 0.0F;
        }
        if (dragon.isRiderPitchKeyMode()) {
            return resolveKeyPitchRadians(dragon, keyPitchDegrees);
        }
        return (float) Math.toRadians(player.getXRot());
    }

    public static float resolveRiderPitchDegrees(RideableDragonBase dragon, Player player, float keyPitchDegrees) {
        return (float) Math.toDegrees(resolveRiderPitchRadians(dragon, player, keyPitchDegrees));
    }

    public static float resolveRiderSwimVisualPitchRadians(RideableDragonBase dragon, Player player,
                                                           float keyPitchDegrees, boolean hasMovementInput) {
        if (dragon.isRiderPitchKeyMode()) {
            return Mth.clamp(-resolveKeyPitchRadians(dragon, keyPitchDegrees), -Mth.HALF_PI, Mth.HALF_PI);
        }
        if (hasMovementInput) {
            return Mth.clamp(-(float) Math.toRadians(player.getXRot()), -Mth.HALF_PI, Mth.HALF_PI);
        }
        return 0.0F;
    }

    public static float resolveKeyPitchRadians(RideableDragonBase dragon, float keyPitchDegrees) {
        if (dragon.isGoingUp()) {
            return (float) -Math.toRadians(keyPitchDegrees);
        }
        if (dragon.isGoingDown()) {
            return (float) Math.toRadians(keyPitchDegrees);
        }
        return 0.0F;
    }

    public static Vec3 computeFlightVelocity(RideableDragonBase dragon, Player player, Vec3 motion,
                                             float pitchRad, boolean keyPitchMode,
                                             double baseSpeed, double cruiseMult, double sprintMult,
                                             double strafePower, double dragNoInput,
                                             double ascendThrust, double descendThrust,
                                             double terminalVelocity,
                                             double takeoffBoost,
                                             boolean takeoffBoostActive) {
        return computeFlightVelocity(
                dragon,
                player,
                motion,
                pitchRad,
                keyPitchMode,
                baseSpeed,
                cruiseMult,
                sprintMult,
                strafePower,
                dragNoInput,
                ascendThrust,
                descendThrust,
                terminalVelocity,
                takeoffBoost,
                takeoffBoostActive,
                true,
                0.20D
        );
    }

    public static Vec3 computeFlightVelocity(RideableDragonBase dragon, Player player, Vec3 motion,
                                             float pitchRad, boolean keyPitchMode,
                                             double baseSpeed, double cruiseMult, double sprintMult,
                                             double strafePower, double dragNoInput,
                                             double ascendThrust, double descendThrust,
                                             double terminalVelocity,
                                             double takeoffBoost,
                                             boolean takeoffBoostActive,
                                             boolean takeoffBoostRequiresGoingUp,
                                             double takeoffMinVertical) {
        double targetSpeed = (dragon.isAccelerating() ? sprintMult : cruiseMult) * baseSpeed;
        float pitchDegrees = (float) Math.toDegrees(pitchRad);
        DragonRiderFlightPhysics.DiveResponse diveResponse =
                DragonRiderFlightPhysics.computeDiveResponse(pitchDegrees, keyPitchMode);
        targetSpeed *= diveResponse.speedMultiplier();

        double forwardInput = motion.z;
        double strafeInput = motion.x;
        boolean hasInput = Math.abs(forwardInput) > 0.01D || Math.abs(strafeInput) > 0.01D;
        float yawRad = (float) Math.toRadians(dragon.getYRot());
        double forwardXZ = Math.cos(pitchRad);
        double forwardX = -Math.sin(yawRad) * forwardXZ;
        double forwardY = keyPitchMode ? 0.0D : -Math.sin(pitchRad);
        double forwardZ = Math.cos(yawRad) * forwardXZ;
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        double targetDirX = forwardX * forwardInput + rightX * strafeInput * strafePower;
        double targetDirY = forwardY * forwardInput * 1.35D;
        double targetDirZ = forwardZ * forwardInput + rightZ * strafeInput * strafePower;
        double dirLength = Math.sqrt(targetDirX * targetDirX + targetDirY * targetDirY + targetDirZ * targetDirZ);

        Vec3 current = dragon.getDeltaMovement();
        Vec3 velocity;
        if (hasInput && dirLength > 0.01D) {
            targetDirX /= dirLength;
            targetDirY /= dirLength;
            targetDirZ /= dirLength;
            Vec3 targetVelocity = new Vec3(targetDirX * targetSpeed, targetDirY * targetSpeed, targetDirZ * targetSpeed);
            velocity = new Vec3(
                    Mth.lerp(diveResponse.acceleration(), current.x, targetVelocity.x),
                    Mth.lerp(diveResponse.acceleration(), current.y, targetVelocity.y),
                    Mth.lerp(diveResponse.acceleration(), current.z, targetVelocity.z)
            ).scale(1.0D - diveResponse.drag());
        } else {
            velocity = current.scale(1.0D - dragNoInput);
            if (velocity.length() < 0.01D) {
                velocity = Vec3.ZERO;
            }
        }

        double vertical = velocity.y;
        boolean diving = !keyPitchMode && pitchDegrees >= 45.0F && hasInput;
        if (!diving) {
            if (takeoffBoostActive && (!takeoffBoostRequiresGoingUp || dragon.isGoingUp())) {
                vertical = Math.max(vertical + takeoffBoost, takeoffMinVertical);
            } else if (dragon.isGoingUp()) {
                vertical += ascendThrust;
            } else if (dragon.isGoingDown()) {
                vertical -= descendThrust;
            }
        }

        vertical = Mth.clamp(vertical, -terminalVelocity, terminalVelocity);
        if (velocity.length() > targetSpeed) {
            velocity = velocity.normalize().scale(targetSpeed);
        }
        return new Vec3(velocity.x, vertical, velocity.z);
    }
}
