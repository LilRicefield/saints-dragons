package com.leon.saintsdragons.server.ai.navigation.async;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

final class DragonFlightSteering {
    private final Mob dragon;
    private final DragonFlightSpace space;
    private Vec3 descentTarget;
    private Vec3 descentStart;
    private Vec3 descentDestination;
    private long descentExpires;
    private long nextDescentAttempt;
    private long nextDescentCheck;
    private final int preferredTurn;
    private String decision = "idle";

    DragonFlightSteering(Mob dragon) {
        this(dragon, dragon instanceof RideableDragonBase rideable
                ? rideable.getAIMovement().flightSpace() : new DragonFlightSpace(dragon));
    }

    DragonFlightSteering(Mob dragon, DragonFlightSpace space) {
        this.dragon = dragon;
        this.space = space;
        this.preferredTurn = (dragon.getUUID().getLeastSignificantBits() & 1L) == 0 ? 1 : -1;
    }

    Vec3 guide(Vec3 routeTarget, Vec3 destination, boolean finalLanding, double speed) {
        Vec3 position = dragon.position();
        long now = dragon.level().getGameTime();
        if (descentTarget != null) {
            if (finalLanding || descentDestination.distanceToSqr(destination) > 144.0D
                    || now >= descentExpires || position.distanceToSqr(descentTarget) < 16.0D
                    || passedDescentWaypoint(position)
                    || (now >= nextDescentCheck && !space.corridorClear(position, descentTarget))) {
                descentTarget = null;
            } else {
                if (now >= nextDescentCheck) nextDescentCheck = now + 4;
                decision = "descending-approach";
                return descentTarget;
            }
        }
        Vec3 remaining = destination.subtract(position);
        if (remaining.y < -4.0D && routeTarget.y > position.y
                && space.corridorClear(position, destination)) {
            routeTarget = position.add(remaining.normalize().scale(Math.min(remaining.length(), 16.0D)));
        }
        Vec3 next = routeTarget.subtract(position);
        if (!finalLanding && remaining.y < -12.0D
                && remaining.horizontalDistance() < -remaining.y * 1.4D
                && next.y < -1.0D && next.horizontalDistance() < -next.y * 1.5D
                && now >= nextDescentAttempt) {
            nextDescentAttempt = now + 12;
            Vec3 heading = dragon.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
            if (heading.lengthSqr() < 0.04D) heading = Vec3.directionFromRotation(0.0F, dragon.getYRot());
            heading = heading.normalize();
            double length = Mth.clamp(-remaining.y * 0.7D, 12.0D, 28.0D);
            for (double angle : new double[]{0.0D, preferredTurn * 40.0D, -preferredTurn * 40.0D}) {
                Vec3 forward = heading.yRot((float) Math.toRadians(angle));
                Vec3 candidate = position.add(forward.scale(length)).add(0.0D,
                        -Math.min(-remaining.y - 6.0D, length * 0.65D), 0.0D);
                if (!space.corridorClear(position, candidate) || !space.corridorClear(candidate, routeTarget)) continue;
                descentTarget = candidate;
                descentStart = position;
                descentDestination = destination;
                nextDescentCheck = now + 4;
                descentExpires = now + Mth.clamp(Mth.ceil(length / Math.max(0.3D, speed)) + 30, 40, 120);
                decision = "descending-approach";
                return candidate;
            }
        }
        decision = finalLanding ? "landing-corridor" : "following-route";
        return routeTarget;
    }

    Vec3 turnToward(Vec3 direction, double speed) {
        float desiredYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        Vec3 motion = dragon.getDeltaMovement();
        float currentYaw = motion.horizontalDistanceSqr() > 0.01D
                ? (float) Math.toDegrees(Math.atan2(-motion.x, motion.z)) : dragon.getYRot();
        double agility = Mth.clamp(2.0D / Math.sqrt(Math.max(1.0D, dragon.getBbWidth())), 0.6D, 1.25D);
        float turnLimit = (float) Mth.clamp(9.0D * agility / Math.max(1.0D, speed), 2.0D, 10.0D);
        float yaw = currentYaw + Mth.clamp(Mth.wrapDegrees(desiredYaw - currentYaw), -turnLimit, turnLimit);
        double horizontal = direction.horizontalDistance();
        if (horizontal < 1.0E-4D) return direction;
        Vec3 turned = Vec3.directionFromRotation(0.0F, yaw).scale(horizontal).add(0.0D, direction.y, 0.0D);
        double probe = Math.max(2.0D, speed * 4.0D);
        if (space.corridorClear(dragon.position(), dragon.position().add(turned.scale(probe)))) return turned;
        decision = "tight-clearance";
        return direction;
    }

    private boolean passedDescentWaypoint(Vec3 position) {
        Vec3 leg = descentTarget.subtract(descentStart).multiply(1.0D, 0.0D, 1.0D);
        return position.subtract(descentTarget).dot(leg) >= 0.0D;
    }

    Vec3 checkedVelocity(Vec3 velocity) {
        if (space.corridorClear(dragon.position(), dragon.position().add(velocity))) return velocity;
        decision = "braking-obstacle";
        for (double scale : new double[]{0.5D, 0.25D}) {
            Vec3 slower = velocity.scale(scale);
            if (space.corridorClear(dragon.position(), dragon.position().add(slower))) return slower;
        }
        return Vec3.ZERO;
    }

    void reset() {
        descentTarget = null;
        descentStart = null;
        descentDestination = null;
        nextDescentAttempt = 0L;
        nextDescentCheck = 0L;
        decision = "idle";
    }

    String debugSummary() {
        return decision + (descentTarget == null ? "" : ",maneuver=" + descentTarget);
    }
}
