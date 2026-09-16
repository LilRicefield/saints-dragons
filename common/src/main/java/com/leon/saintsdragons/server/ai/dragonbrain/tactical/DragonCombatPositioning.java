package com.leon.saintsdragons.server.ai.dragonbrain.tactical;

import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class DragonCombatPositioning {
    private DragonCombatPositioning() { }

    public static @Nullable Vec3 searchPosition(RideableFlyingDragon dragon, Vec3 evidence, Vec3 preferred,
                                                Function<Vec3, Vec3> fit) {
        var feedback = dragon.getCombatDecisionSupport();
        return feedback == null ? fit.apply(preferred)
                : choose(dragon, feedback, evidence, preferred, dragon.getEyePosition(),
                Math.max(16, preferred.distanceTo(evidence) + 16), fit);
    }

    static @Nullable Vec3 choose(RideableFlyingDragon dragon, DragonCombatDecisionSupport feedback,
                                 Vec3 focus, Vec3 preferred, @Nullable Vec3 mouth, double range,
                                 Function<Vec3, Vec3> fit) {
        Vec3 away = preferred.subtract(focus).multiply(1, 0, 1);
        if (away.lengthSqr() < 1.0E-6) away = Vec3.directionFromRotation(0, dragon.yBodyRot).scale(-1);
        away = away.normalize();
        double step = Mth.clamp(dragon.getBbWidth(), 4, 8);
        Vec3 side = new Vec3(-away.z, 0, away.x).scale(step);
        Vec3[] candidates = {preferred, preferred.add(side), preferred.subtract(side),
                preferred.add(away.scale(step)), preferred.subtract(away.scale(step * 0.5))};
        Vec3 best = null;
        double bestScore = -Double.MAX_VALUE;
        for (Vec3 candidate : candidates) {
            Vec3 fitted = fit.apply(candidate);
            if (fitted == null || !Double.isFinite(fitted.lengthSqr())) continue;
            var bounds = dragon.getBoundingBox().move(fitted.subtract(dragon.position()));
            if (!dragon.level().hasChunksAt(Mth.floor(bounds.minX), Mth.floor(bounds.minY), Mth.floor(bounds.minZ),
                    Mth.floor(bounds.maxX), Mth.floor(bounds.maxY), Mth.floor(bounds.maxZ))
                    || bounds.minX < dragon.level().getWorldBorder().getMinX()
                    || bounds.maxX > dragon.level().getWorldBorder().getMaxX()
                    || bounds.minZ < dragon.level().getWorldBorder().getMinZ()
                    || bounds.maxZ > dragon.level().getWorldBorder().getMaxZ()
                    || !dragon.level().noCollision(dragon, bounds)) continue;
            double score = score(dragon, feedback, focus, preferred, mouth, range, fitted);
            if (score > bestScore) {
                bestScore = score;
                best = fitted;
            }
        }
        return best;
    }

    public static double score(RideableFlyingDragon dragon, DragonCombatDecisionSupport feedback,
                               Vec3 focus, Vec3 preferred, @Nullable Vec3 mouth, double range, Vec3 candidate) {
        // Planning can run before an ability has a mouth anchor; damage still uses the real attack origin.
        Vec3 planningOrigin = mouth == null ? dragon.getEyePosition() : mouth;
        Vec3 origin = candidate.add(planningOrigin.subtract(dragon.position()));
        AABB sightBounds = new AABB(origin, focus);
        if (origin.distanceToSqr(focus) > range * range
                || !dragon.level().hasChunksAt(BlockPos.containing(sightBounds.minX, sightBounds.minY, sightBounds.minZ),
                BlockPos.containing(sightBounds.maxX, sightBounds.maxY, sightBounds.maxZ))) {
            return -Double.MAX_VALUE;
        }
        // This ranks a prospective shot. The ability still validates its real origin and trajectory.
        var hit = dragon.level().clip(new ClipContext(origin, focus, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, dragon));
        boolean clear = hit.getType() == HitResult.Type.MISS;
        Vec3 direction = focus.subtract(origin);
        double yaw = Math.toDegrees(Math.atan2(-direction.x, direction.z));
        double turn = Math.abs(Mth.wrapDegrees(yaw - dragon.yBodyRot));
        return (clear ? 30 : 0) - candidate.distanceTo(preferred) * 0.8
                - dragon.position().distanceTo(candidate) * 0.15 - turn * 0.025
                - feedback.positionPenalty(candidate);
    }
}
