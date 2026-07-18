package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DragonFollowParentBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    private static final double MAX_DISTANCE_SQ = 576.0D;
    private final Class<T> dragonClass;
    private final double speedModifier;
    @Nullable
    private T parent;
    private int repathCooldown;
    private int wanderCooldown;

    public DragonFollowParentBehaviour(Class<T> dragonClass, double speedModifier) {
        this.dragonClass = dragonClass;
        this.speedModifier = speedModifier;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T baby = context.dragon();
        if (!canFollow(baby)) {
            return false;
        }
        parent = resolveParent(baby);
        return parent != null && baby.distanceToSqr(parent) >= minimumDistanceSq(baby, parent);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        T baby = context.dragon();
        if (!canFollow(baby) || parent == null || !parent.isAlive() || parent.isBaby()) {
            return false;
        }
        UUID assigned = baby.getAssignedParentUuid();
        if (assigned == null || !assigned.equals(parent.getUUID())) {
            parent = resolveParent(baby);
            if (parent == null) {
                return false;
            }
        }
        double distance = baby.distanceToSqr(parent);
        return distance >= minimumDistanceSq(baby, parent) && distance <= MAX_DISTANCE_SQ;
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        repathCooldown = 0;
        wanderCooldown = 0;
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T baby = context.dragon();
        if (parent == null) {
            return;
        }
        baby.getLookControl().setLookAt(parent, 10.0F, baby.getMaxHeadXRot());
        double distance = baby.distanceToSqr(parent);
        double minimum = minimumDistanceSq(baby, parent);
        if (distance < minimum) {
            stopMovement(baby);
            wanderCooldown = 40 + baby.getRandom().nextInt(40);
            return;
        }
        if (wanderCooldown > 0) {
            wanderCooldown--;
            if (distance > minimum * 2.0D) {
                wanderCooldown = 0;
            }
            return;
        }
        if (--repathCooldown <= 0) {
            repathCooldown = 8;
            if (distance >= minimum * 1.2D) {
                moveTo(baby, parent);
            } else {
                stopMovement(baby);
                wanderCooldown = 20 + baby.getRandom().nextInt(20);
            }
        }
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        stopMovement(context.dragon());
        parent = null;
        wanderCooldown = 0;
    }

    private boolean canFollow(T baby) {
        return baby.isBaby()
                && !baby.isTame()
                && baby.getOwner() == null
                && !baby.isVehicle()
                && (!(baby instanceof RideableDragonBase rideable) || !rideable.isInWaterOrBubble())
                && (baby.canSwim() || !baby.isInWaterOrBubble());
    }

    @Nullable
    private T resolveParent(T baby) {
        List<T> nearby = baby.level().getEntitiesOfClass(dragonClass,
                baby.getBoundingBox().inflate(20.0D, 8.0D, 20.0D),
                adult -> !adult.isBaby() && adult.isAlive());
        UUID assigned = baby.getAssignedParentUuid();
        if (assigned != null) {
            for (T adult : nearby) {
                if (assigned.equals(adult.getUUID())) {
                    return adult;
                }
            }
            baby.clearAssignedParentUuid();
        }
        T closest = nearby.stream().filter(DragonEntity::isFemale)
                .min(Comparator.comparingDouble(baby::distanceToSqr)).orElse(null);
        if (closest != null) {
            baby.setAssignedParentUuid(closest.getUUID());
        }
        return closest;
    }

    private double minimumDistanceSq(T baby, T adult) {
        double adultRadius = Math.max(adult.getBbWidth(), adult.getBbWidth()) * 0.5D;
        double babyRadius = Math.max(baby.getBbWidth(), baby.getBbWidth()) * 0.5D;
        double minimum = Math.max(2.75D, adultRadius + babyRadius + 1.5D);
        return minimum * minimum;
    }

    private void moveTo(T baby, T adult) {
        if (baby instanceof RideableDragonBase rideable) {
            rideable.getAIMovement().moveToGroundTarget(adult, speedModifier, false);
        } else {
            baby.getNavigation().moveTo(adult, speedModifier);
        }
    }

    private void stopMovement(T baby) {
        if (baby instanceof RideableDragonBase rideable) {
            rideable.getAIMovement().stop();
        } else {
            baby.getNavigation().stop();
        }
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of("parent", parent == null ? "none" : parent.getName().getString());
    }
}
