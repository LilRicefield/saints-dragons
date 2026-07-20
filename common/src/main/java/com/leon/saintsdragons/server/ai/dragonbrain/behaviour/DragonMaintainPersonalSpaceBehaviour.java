package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DragonMaintainPersonalSpaceBehaviour<T extends RideableDragonBase>
        extends DragonBehaviour<T> {
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final int REPATH_INTERVAL_TICKS = 6;
    private static final int MAX_MOVE_TICKS = 50;
    private static final double PERSONAL_PADDING = 1.25D;
    private static final double MIN_MOVE_DISTANCE = 2.5D;
    private static final double MAX_MOVE_DISTANCE = 8.0D;
    private static final double MOVE_SPEED = 0.6D;

    @Nullable
    private Vec3 target;
    private int moveTicks;
    private int repathCooldown;
    private int crowdedBy;
    private double nearestNeighborDistance = -1.0D;
    private boolean movementIssued;
    private String phase = "idle";

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (!canMakeRoom(context)
                || Math.floorMod(dragon.tickCount + dragon.getId(), SCAN_INTERVAL_TICKS) != 0) {
            return false;
        }
        target = findSeparationTarget(context);
        return target != null;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return target != null && moveTicks < MAX_MOVE_TICKS && canRemainActive(context);
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        moveTicks = 0;
        repathCooldown = 0;
        movementIssued = false;
        phase = "separating";
        issueMovement(context);
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        moveTicks++;
        if (context.dragon().getAIMovement().hasFailed()) {
            target = null;
            phase = "path_failed";
            return;
        }
        if (repathCooldown-- > 0 && context.dragon().getAIMovement().isPathing()) {
            return;
        }
        target = findSeparationTarget(context);
        if (target == null) {
            phase = "clear";
            context.dragon().getAIMovement().stop();
            movementIssued = false;
            return;
        }
        issueMovement(context);
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        if (movementIssued) {
            context.dragon().getAIMovement().stop();
        }
        target = null;
        moveTicks = 0;
        repathCooldown = 0;
        crowdedBy = 0;
        nearestNeighborDistance = -1.0D;
        movementIssued = false;
        if (!"path_failed".equals(phase)) {
            phase = "idle";
        }
    }

    private boolean canMakeRoom(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        return canRemainActive(context)
                && !dragon.getAIMovement().isPathing()
                && !dragon.getNavigation().isInProgress()
                && !context.memories().has(DragonMemories.MOVEMENT_INTENT);
    }

    private boolean canRemainActive(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        return dragon.isAlive()
                && !dragon.isDying()
                && !dragon.isAerial()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleeping()
                && !dragon.isSleepTransitioning()
                && !dragon.isSleepLocked()
                && !dragon.isInLove()
                && dragon.getActiveAbility() == null
                && (dragon.getTarget() == null || !dragon.getTarget().isAlive())
                && !context.memories().has(DragonMemories.ATTACK_TARGET)
                && !dragon.isHuntFoodPursuitActive();
    }

    @Nullable
    private Vec3 findSeparationTarget(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        AABB bounds = dragon.getBoundingBox();
        double horizontalSearch = Math.max(8.0D, dragon.getBbWidth() * 2.0D);
        double verticalSearch = Math.max(4.0D, dragon.getBbHeight());
        List<DragonEntity> nearby = context.level().getEntitiesOfClass(
                DragonEntity.class,
                bounds.inflate(horizontalSearch, verticalSearch, horizontalSearch),
                other -> other != dragon && other.isAlive() && !other.isRemoved()
        );

        Vec3 separation = Vec3.ZERO;
        double strongestOverlap = 0.0D;
        int contributors = 0;
        nearestNeighborDistance = -1.0D;
        for (DragonEntity other : nearby) {
            if (!verticallyClose(bounds, other.getBoundingBox()) || !shouldYieldTo(dragon, other)) {
                continue;
            }
            double dx = dragon.getX() - other.getX();
            double dz = dragon.getZ() - other.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            double desiredDistance = dragon.getBbWidth() * 0.5D
                    + other.getBbWidth() * 0.5D
                    + PERSONAL_PADDING;
            if (distance >= desiredDistance) {
                continue;
            }
            if (nearestNeighborDistance < 0.0D || distance < nearestNeighborDistance) {
                nearestNeighborDistance = distance;
            }
            double overlap = desiredDistance - distance;
            strongestOverlap = Math.max(strongestOverlap, overlap);
            Vec3 direction = distance > 1.0E-4D
                    ? new Vec3(dx / distance, 0.0D, dz / distance)
                    : fallbackDirection(dragon, other);
            separation = separation.add(direction.scale(overlap));
            contributors++;
        }
        crowdedBy = contributors;
        if (contributors == 0) {
            return null;
        }
        if (separation.horizontalDistanceSqr() < 1.0E-6D) {
            separation = fallbackDirection(dragon, null);
        }
        double distance = Math.max(
                MIN_MOVE_DISTANCE,
                Math.min(MAX_MOVE_DISTANCE, strongestOverlap + PERSONAL_PADDING)
        );
        Vec3 direction = new Vec3(separation.x, 0.0D, separation.z).normalize();
        return dragon.position().add(direction.scale(distance));
    }

    private boolean shouldYieldTo(T dragon, DragonEntity other) {
        if (!other.getBrain().checkMemory(DragonMemories.LOCOMOTION_MODE, MemoryStatus.REGISTERED)
                || other.isOrderedToSit()
                || other.isSleeping()
                || other.isSleepTransitioning()
                || other.isVehicle()
                || other.isPassenger()
                || other.getActiveAbility() != null) {
            return true;
        }
        if (other instanceof RideableDragonBase rideable
                && (rideable.getAIMovement().isPathing() || rideable.getNavigation().isInProgress())) {
            return true;
        }
        return dragon.getUUID().compareTo(other.getUUID()) > 0;
    }

    private boolean verticallyClose(AABB first, AABB second) {
        return first.maxY + PERSONAL_PADDING > second.minY
                && second.maxY + PERSONAL_PADDING > first.minY;
    }

    private Vec3 fallbackDirection(T dragon, @Nullable DragonEntity other) {
        int hash = dragon.getUUID().hashCode();
        if (other != null) {
            hash = 31 * hash + other.getUUID().hashCode();
        }
        double angle = Math.floorMod(hash, 360) * Math.PI / 180.0D;
        return new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
    }

    private void issueMovement(DragonBrainContext<T> context) {
        if (target == null) {
            return;
        }
        movementIssued = context.dragon().getAIMovement().setWaypoint(target, MOVE_SPEED, false);
        repathCooldown = REPATH_INTERVAL_TICKS;
        if (!movementIssued) {
            target = null;
            phase = "path_rejected";
        }
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("phase", phase);
        details.put("crowded_by", Integer.toString(crowdedBy));
        details.put("nearest", nearestNeighborDistance < 0.0D
                ? "none"
                : String.format(java.util.Locale.ROOT, "%.2f", nearestNeighborDistance));
        details.put("target", target == null ? "none" : target.toString());
        return Map.copyOf(details);
    }
}
