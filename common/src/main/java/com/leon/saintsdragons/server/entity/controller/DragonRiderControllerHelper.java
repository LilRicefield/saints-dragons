package com.leon.saintsdragons.server.entity.controller;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
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
        float mousePitchRad = (float) Math.toRadians(player.getXRot());
        if (!(dragon instanceof RideableFlyingDragon) || !dragon.isFlying()) {
            return mousePitchRad;
        }
        return applyVerticalFlightPitchBias(dragon, mousePitchRad);
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

    public static float resolveRiderFlightVisualPitchRadians(RideableDragonBase dragon, Player player,
                                                             float keyPitchDegrees,
                                                             boolean hasMovementInput) {
        boolean verticalPitchInput = dragon.isGoingUp() != dragon.isGoingDown();
        if (dragon.isRiderPitchKeyMode()) {
            return Mth.clamp(-resolveKeyPitchRadians(dragon, keyPitchDegrees), -Mth.HALF_PI, Mth.HALF_PI);
        }
        if (hasMovementInput || verticalPitchInput) {
            float pitchRad = resolveRiderPitchRadians(dragon, player, keyPitchDegrees);
            return Mth.clamp(-pitchRad, -Mth.HALF_PI, Mth.HALF_PI);
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

    private static float applyVerticalFlightPitchBias(RideableDragonBase dragon, float mousePitchRad) {
        if (dragon.isGoingUp() == dragon.isGoingDown()) {
            return Mth.clamp(mousePitchRad, -Mth.HALF_PI, Mth.HALF_PI);
        }

        boolean movingForward = dragon.getLastRiderForward() > 0.01F;
        float verticalPitchDegrees = movingForward ? 45.0F : 90.0F;
        float verticalPitchRad = (float) Math.toRadians(verticalPitchDegrees);
        float pitchBias = dragon.isGoingUp() ? -verticalPitchRad : verticalPitchRad;
        if (Math.signum(mousePitchRad) != Math.signum(pitchBias)
                && Math.abs(mousePitchRad) >= Math.abs(pitchBias)) {
            return 0.0F;
        }
        return Mth.clamp(mousePitchRad + pitchBias, -Mth.HALF_PI, Mth.HALF_PI);
    }
}
