package com.leon.saintsdragons.server.entity.ability;

import com.leon.saintsdragons.common.particle.ExpandingBreathSection;
import com.leon.saintsdragons.server.ai.navigation.async.DragonFlightRequest;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.flight.DragonFlightVisuals;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class DragonCombatAim {
    public static final Profile BEAM = new Profile(40, 50, 9, 0.0);
    public static final Profile FIRE = new Profile(70, 55, 6, 1.0);
    public static final Profile BREATH = new Profile(70, 55, 8, 1.0);

    private final RideableFlyingDragon dragon;
    private @Nullable LivingEntity target;
    private @Nullable Profile profile;
    private @Nullable Vec3 direction;
    private Vec3 desired = DragonAimHelper.DEFAULT_FORWARD;
    private int updatedTick = Integer.MIN_VALUE;
    private int facingTick = Integer.MIN_VALUE;
    private int assessedTick = Integer.MIN_VALUE;
    private int alignedTicks;
    private Shot shot = Shot.NO_TARGET;
    private double yawError;
    private double pitchError;
    private boolean sprintingApproach;

    public DragonCombatAim(RideableFlyingDragon dragon) {
        this.dragon = dragon;
    }

    public @Nullable Vec3 track(LivingEntity target, Vec3 origin, Profile profile) {
        if (!canControl() || target == null || !dragon.isTargetValid(target)) {
            clear();
            return null;
        }
        boolean changed = this.target != target || this.profile != profile || !isActive();
        if (changed) {
            direction = DragonAimHelper.fallbackHeadDirection(dragon);
            alignedTicks = 0;
            assessedTick = Integer.MIN_VALUE;
        }
        this.target = target;
        this.profile = profile;
        if (!changed && updatedTick == dragon.tickCount) return direction;
        updatedTick = dragon.tickCount;
        // A short, bounded lead compensates tracking lag without predicting a long hitscan flight time.
        Vec3 lead = target.getDeltaMovement().scale(profile.leadTicks());
        if (lead.lengthSqr() > 4.0D) lead = lead.normalize().scale(2.0D);
        Vec3 aimPoint = target.getEyePosition().add(0, -0.25D, 0).add(lead);
        Vec3 wanted = DragonAimHelper.directionTo(origin, aimPoint);
        if (wanted == null) return direction;
        desired = wanted;
        yawError = Mth.wrapDegrees(yaw(desired) - dragon.yBodyRot);
        float bodyPitch = dragon.isAerial()
                ? -DragonFlightVisuals.computeAiPitchTarget(dragon.getDeltaMovement()) * Mth.RAD_TO_DEG : 0.0F;
        pitchError = pitch(desired) - bodyPitch;
        Vec3 reachable = DragonAimHelper.clampDirectionToHead(desired, dragon.yBodyRot,
                bodyPitch, profile.yawLimit(), profile.pitchLimit());
        direction = DragonAimHelper.turnDirection(direction, reachable, profile.turnDegrees());
        applyFacing();
        return direction;
    }

    public boolean isActive() {
        return canControl() && target != null && target == dragon.getTarget() && target.isAlive()
                && direction != null && updatedTick != Integer.MIN_VALUE
                && dragon.tickCount >= updatedTick && dragon.tickCount - updatedTick <= 2;
    }

    private boolean canControl() {
        return !dragon.level().isClientSide && !dragon.isVehicle() && !dragon.isPassenger()
                && dragon.isAlive() && !dragon.isDying() && !dragon.isSleeping()
                && !dragon.isSleepTransitioning() && !dragon.isStayOrSitMuted()
                && !dragon.isTakeoff() && !dragon.isLanding();
    }

    public void applyFacing() {
        if (!isActive()) return;
        if ((!dragon.isAerial() || dragon.getDeltaMovement().horizontalDistanceSqr() < 0.01D)
                && facingTick != dragon.tickCount) {
            float bodyYaw = Mth.approachDegrees(dragon.getYRot(), yaw(desired), 6.0F);
            dragon.setYRot(bodyYaw);
            dragon.yBodyRot = bodyYaw;
        }
        facingTick = dragon.tickCount;
        // The flight model pitches from velocity; entity pitch carries the head aim to observers.
        dragon.setYHeadRot(dragon.yBodyRot + Mth.clamp(Mth.wrapDegrees(yaw(direction) - dragon.yBodyRot),
                -profile.yawLimit(), profile.yawLimit()));
        dragon.setXRot(pitch(direction));
    }

    public float flightYaw(float movementYaw) {
        if (!isActive()) return movementYaw;
        // At low speed the body can face the shot while making a small lateral correction.
        float allowance = (float) Mth.clamp((1.0D - dragon.getDeltaMovement().horizontalDistance()) * 240.0D, 0, 180);
        return movementYaw + Mth.clamp(Mth.wrapDegrees(yaw(desired) - movementYaw), -allowance, allowance);
    }

    public Shot assess(Vec3 origin, Vec3 firingDirection, LivingEntity target, double range,
                       double hitRadius, @Nullable ExpandingBreathSection.Profile spread) {
        if (target == null || !target.isAlive() || firingDirection == null) return recordShot(Shot.NO_TARGET);
        Vec3 forward = DragonAimHelper.normalizeOrNull(firingDirection);
        if (forward == null) return recordShot(Shot.NO_TARGET);
        AABB box = target.getBoundingBox();
        Vec3 center = box.getCenter();
        Vec3 nearest = closestPoint(box, origin);
        if (origin.distanceToSqr(nearest) > range * range) return recordShot(Shot.OUT_OF_RANGE);
        Vec3 contact = null;
        if (spread == null) {
            AABB hitBox = box.inflate(hitRadius);
            contact = hitBox.contains(origin) ? origin
                    : hitBox.clip(origin, origin.add(forward.scale(range))).orElse(null);
        } else {
            Vec3 right = forward.cross(Math.abs(forward.y) > 0.99D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
            Vec3 up = right.cross(forward).normalize();
            Vec3 offset = center.subtract(origin);
            double along = offset.dot(forward);
            double depth = projectedHalfSize(box, forward);
            double half = spread.halfWidthAt(Mth.clamp(along, 0, range));
            if (along + depth >= 0 && along - depth <= range
                    && Math.abs(offset.dot(right)) <= half + projectedHalfSize(box, right)
                    && Math.abs(offset.dot(up)) <= half + projectedHalfSize(box, up)) {
                contact = closestPoint(box, origin.add(forward.scale(Mth.clamp(along, 0, range))));
            }
        }
        // Check the covered part of the target, not only the center ray of a broad breath.
        if (!clearLine(origin, contact != null ? contact : center)) return recordShot(Shot.BLOCKED);
        if (contact != null) return recordShot(Shot.ALIGNED);
        boolean outsideArc = profile != null
                && (Math.abs(yawError) > profile.yawLimit() || Math.abs(pitchError) > profile.pitchLimit());
        return recordShot(outsideArc ? Shot.OUT_OF_ARC : Shot.ALIGNING);
    }

    private boolean clearLine(Vec3 start, Vec3 end) {
        if (start.distanceToSqr(end) < 1.0E-8D) return true;
        var hit = dragon.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, dragon));
        return hit.getType() == HitResult.Type.MISS || start.distanceToSqr(hit.getLocation()) + 1.0E-6D >= start.distanceToSqr(end);
    }

    private Shot recordShot(Shot value) {
        if (assessedTick != dragon.tickCount) {
            alignedTicks = value == Shot.ALIGNED
                    ? (assessedTick == dragon.tickCount - 1 ? alignedTicks + 1 : 1) : 0;
            assessedTick = dragon.tickCount;
        } else if (value != Shot.ALIGNED) {
            alignedTicks = 0;
        }
        shot = value;
        return value;
    }

    public boolean ready(int ticks) {
        return isActive() && shot == Shot.ALIGNED && alignedTicks >= ticks;
    }

    public DragonFlightRequest firingApproach(LivingEntity target, double maximumSpeed,
                                              double spacing, double sideOffset) {
        Vec3 center = target.position().add(0, target.getBbHeight() + 2.0D, 0);
        Vec3 toTarget = center.subtract(dragon.position());
        Vec3 targetMotion = target.getDeltaMovement().multiply(1, 0, 1);
        Vec3 course = targetMotion.lengthSqr() > 0.09D && targetMotion.dot(toTarget) > 0
                ? targetMotion.normalize() : toTarget.multiply(1, 0, 1).normalize();
        if (course.lengthSqr() < 1.0E-6D) course = Vec3.directionFromRotation(0, dragon.getYRot());
        Vec3 lateral = new Vec3(-course.z, 0, course.x);
        Vec3 destination = center.add(targetMotion.scale(3.0D))
                .subtract(course.scale(spacing)).add(lateral.scale(sideOffset));
        // Keep enough thrust to follow a fleeing target, then brake before reaching firing distance.
        double gap = toTarget.dot(course) - spacing;
        sprintingApproach = targetMotion.dot(course) > 0.6D
                && gap > (sprintingApproach ? 6.0D : 12.0D);
        if (sprintingApproach) return DragonFlightRequest.chase(destination, maximumSpeed);
        return DragonFlightRequest.track(destination, maximumSpeed, 2.0D);
    }

    public void clear() {
        target = null;
        profile = null;
        direction = null;
        updatedTick = facingTick = assessedTick = Integer.MIN_VALUE;
        alignedTicks = 0;
        shot = Shot.NO_TARGET;
        sprintingApproach = false;
    }

    public String debugSummary() {
        return !isActive() ? "idle" : shot.name().toLowerCase(java.util.Locale.ROOT)
                + ",yaw=" + Mth.floor(Math.abs(yawError)) + ",pitch=" + Mth.floor(Math.abs(pitchError))
                + ",stable=" + alignedTicks;
    }

    private static double projectedHalfSize(AABB box, Vec3 axis) {
        return (Math.abs(axis.x) * box.getXsize() + Math.abs(axis.y) * box.getYsize()
                + Math.abs(axis.z) * box.getZsize()) * 0.5D;
    }

    private static Vec3 closestPoint(AABB box, Vec3 point) {
        return new Vec3(Mth.clamp(point.x, box.minX, box.maxX), Mth.clamp(point.y, box.minY, box.maxY),
                Mth.clamp(point.z, box.minZ, box.maxZ));
    }

    private static float yaw(Vec3 direction) { return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z)); }
    private static float pitch(Vec3 direction) { return (float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())); }

    public record Profile(float yawLimit, float pitchLimit, float turnDegrees, double leadTicks) { }

    public static final class ShotGrace {
        private int lastTick = Integer.MIN_VALUE;
        private int lostTicks;
        private int alignmentTicks;
        private int blockedTicks;

        public boolean allows(Shot shot, int tick, int alignmentGrace, int obstructionGrace) {
            if (tick != lastTick) {
                lostTicks = shot == Shot.ALIGNED ? 0 : lostTicks + 1;
                alignmentTicks = shot.needsAlignment() ? alignmentTicks + 1 : 0;
                blockedTicks = shot == Shot.ALIGNED || shot.needsAlignment() ? 0 : blockedTicks + 1;
                lastTick = tick;
            }
            return alignmentTicks < alignmentGrace && blockedTicks < obstructionGrace
                    && lostTicks < alignmentGrace + obstructionGrace;
        }

        public void reset() {
            lastTick = Integer.MIN_VALUE;
            lostTicks = alignmentTicks = blockedTicks = 0;
        }
    }
    public enum Shot {
        ALIGNED, ALIGNING, OUT_OF_ARC, BLOCKED, OUT_OF_RANGE, NO_TARGET;

        public boolean needsAlignment() { return this == ALIGNING || this == OUT_OF_ARC; }
        public String reason() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
}
