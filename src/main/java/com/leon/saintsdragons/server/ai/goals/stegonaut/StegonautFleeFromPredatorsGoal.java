package com.leon.saintsdragons.server.ai.goals.stegonaut;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Simple flee behavior for Stegonaut - runs away from Raevyx and other Stegonauts.
 * Replaces the problematic play-dead mechanic with straightforward avoidance.
 */
public class StegonautFleeFromPredatorsGoal extends Goal {
    private final Stegonaut stegonaut;
    private final double fleeSpeed;
    private final double detectionRange;
    private LivingEntity threatEntity;
    private Path fleePath;
    private int fleeTimer;
    private static final int FLEE_DURATION = 100; // 5 seconds
    private static final double MIN_FLEE_DISTANCE = 16.0D; // Stop fleeing when 16 blocks away

    public StegonautFleeFromPredatorsGoal(Stegonaut stegonaut, double fleeSpeed, double detectionRange) {
        this.stegonaut = stegonaut;
        this.fleeSpeed = fleeSpeed;
        this.detectionRange = detectionRange;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Don't flee if sleeping, dying, or in water (they hate water already)
        if (stegonaut.isSleeping() || stegonaut.isDying() || stegonaut.isInWaterOrBubble()) {
            return false;
        }

        // Don't flee if ordered to sit
        if (stegonaut.isOrderedToSit()) {
            return false;
        }

        // Look for threatening entities nearby
        List<LivingEntity> nearbyEntities = stegonaut.level().getEntitiesOfClass(
                LivingEntity.class,
                stegonaut.getBoundingBox().inflate(detectionRange),
                entity -> entity.isAlive() && isThreateningEntity(entity)
        );

        if (nearbyEntities.isEmpty()) {
            return false;
        }

        // Find the closest threat
        LivingEntity closestThreat = null;
        double closestDistSq = Double.MAX_VALUE;

        for (LivingEntity entity : nearbyEntities) {
            double distSq = stegonaut.distanceToSqr(entity);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closestThreat = entity;
            }
        }

        if (closestThreat == null) {
            return false;
        }

        // Try to find a flee path away from the threat
        Vec3 fleePos = DefaultRandomPos.getPosAway(stegonaut, 16, 7, closestThreat.position());
        if (fleePos == null) {
            return false;
        }

        // Check if the flee position is actually farther from the threat
        if (closestThreat.distanceToSqr(fleePos.x, fleePos.y, fleePos.z) < closestDistSq) {
            return false;
        }

        PathNavigation navigation = stegonaut.getNavigation();
        Path path = navigation.createPath(fleePos.x, fleePos.y, fleePos.z, 0);

        if (path == null || !path.canReach()) {
            return false;
        }

        this.threatEntity = closestThreat;
        this.fleePath = path;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop fleeing if conditions change
        if (stegonaut.isSleeping() || stegonaut.isInWaterOrBubble()) {
            return false;
        }

        // Stop if we've fled far enough
        if (threatEntity != null && stegonaut.distanceToSqr(threatEntity) > MIN_FLEE_DISTANCE * MIN_FLEE_DISTANCE) {
            return false;
        }

        // Stop if the threat is gone
        if (threatEntity == null || !threatEntity.isAlive() || threatEntity.isRemoved()) {
            return false;
        }

        // Stop if we've been fleeing too long
        if (fleeTimer > FLEE_DURATION) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        if (fleePath != null) {
            stegonaut.getNavigation().moveTo(fleePath, fleeSpeed);
        }
        fleeTimer = 0;
    }

    @Override
    public void stop() {
        threatEntity = null;
        fleePath = null;
        fleeTimer = 0;
        stegonaut.getNavigation().stop();
    }

    @Override
    public void tick() {
        fleeTimer++;

        // Continue fleeing away from the threat
        if (threatEntity != null && threatEntity.isAlive()) {
            // Occasionally recalculate flee path
            if (fleeTimer % 20 == 0) {
                Vec3 fleePos = DefaultRandomPos.getPosAway(stegonaut, 16, 7, threatEntity.position());
                if (fleePos != null) {
                    stegonaut.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, fleeSpeed);
                }
            }
        }
    }

    /**
     * Check if an entity is threatening to the Stegonaut
     */
    private boolean isThreateningEntity(LivingEntity entity) {
        // Flee from Raevyx (dangerous predator)
        if (entity instanceof Raevyx raevyx) {
            // If tamed, only flee from wild Raevyx
            if (stegonaut.isTame()) {
                return !raevyx.isTame();
            }
            return true;
        }

        // Flee from other Stegonauts (territorial behavior)
        if (entity instanceof Stegonaut otherStegonaut) {
            // Don't flee from self or if both are tamed
            if (otherStegonaut == stegonaut) {
                return false;
            }
            // If tamed, don't flee from other tamed stegonauts
            if (stegonaut.isTame() && otherStegonaut.isTame()) {
                return false;
            }
            return true;
        }

        return false;
    }
}
