package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public final class DragonSwimFollowBehaviour<T extends RideableDragonBase & SemiAquaticDragon>
        extends DragonBehaviour<T> {
    private static final double BABY_START_DISTANCE_SQR = 8.0D * 8.0D;
    private static final double BABY_STOP_DISTANCE_SQR = 6.0D * 6.0D;

    private final Class<T> dragonClass;
    private final float turnSpeed;
    private final double speedModifier;
    private final double startDistanceSqr;
    private final double stopDistanceSqr;
    private final Predicate<T> eligibility;
    @Nullable
    private LivingEntity followTarget;

    public DragonSwimFollowBehaviour(Class<T> dragonClass,
                                     float turnSpeed,
                                     double speedModifier,
                                     double startDistance,
                                     double stopDistance) {
        this(dragonClass, turnSpeed, speedModifier, startDistance, stopDistance, dragon -> true);
    }

    public DragonSwimFollowBehaviour(Class<T> dragonClass,
                                     float turnSpeed,
                                     double speedModifier,
                                     double startDistance,
                                     double stopDistance,
                                     Predicate<T> eligibility) {
        this.dragonClass = dragonClass;
        this.turnSpeed = turnSpeed;
        this.speedModifier = speedModifier;
        this.startDistanceSqr = startDistance * startDistance;
        this.stopDistanceSqr = stopDistance * stopDistance;
        this.eligibility = Objects.requireNonNull(eligibility);
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (!basicConditions(dragon)) {
            return false;
        }
        followTarget = resolveTarget(dragon);
        if (followTarget == null) {
            return false;
        }
        return dragon.distanceToSqr(followTarget) > startDistance(dragon, followTarget);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        return basicConditions(dragon)
                && followTarget != null
                && followTarget.isAlive()
                && followTarget.level() == dragon.level()
                && dragon.distanceToSqr(followTarget) > stopDistance(dragon, followTarget);
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        context.dragon().getNavigation().stop();
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (followTarget == null) {
            return;
        }
        dragon.getNavigation().stop();
        Vec3 position = followTarget.position().add(0.0D, followTarget.getEyeHeight() * 0.5D, 0.0D);
        double speed = dragon.getSwimSpeed() * speedModifier;
        if (dragon.distanceToSqr(followTarget) > 15.0D * 15.0D) {
            speed *= 1.5D;
        }
        AsyncSwimController controller = dragon.getAiSwimController();
        if (controller.trackTarget(position, speed, turnSpeed)) {
            controller.serverTick();
        }
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        context.dragon().getAiSwimController().stop();
        followTarget = null;
    }

    private boolean basicConditions(T dragon) {
        return dragon.canSwim()
                && eligibility.test(dragon)
                && dragon.isInWaterOrBubble()
                && !dragon.isInLove()
                && !dragon.isVehicle()
                && !dragon.isAerial()
                && (dragon.getTarget() == null || !dragon.getTarget().isAlive());
    }

    @Nullable
    private LivingEntity resolveTarget(T dragon) {
        if (dragon.isBaby() && !dragon.isTame() && dragon.getOwner() == null) {
            List<T> nearby = dragon.level().getEntitiesOfClass(
                    dragonClass,
                    dragon.getBoundingBox().inflate(12.0D, 6.0D, 12.0D),
                    candidate -> candidate != dragon && candidate.isAlive() && !candidate.isBaby()
            );
            T closest = null;
            double closestDistance = Double.MAX_VALUE;
            for (T candidate : nearby) {
                double distance = dragon.distanceToSqr(candidate);
                if (distance < closestDistance) {
                    closest = candidate;
                    closestDistance = distance;
                }
            }
            if (closest != null) {
                return closest;
            }
        }
        if (!dragon.isTame() || dragon.isOrderedToSit()) {
            return null;
        }
        LivingEntity owner = dragon.getOwner();
        return owner != null && owner.isAlive() && owner.level() == dragon.level() ? owner : null;
    }

    private double startDistance(T dragon, LivingEntity target) {
        if (isBabyFollowingOwner(dragon, target)) {
            return DragonBabyOwnerFollowTuning.START_DISTANCE
                    * DragonBabyOwnerFollowTuning.START_DISTANCE;
        }
        return dragon.isBaby() && dragonClass.isInstance(target) && !dragonClass.cast(target).isBaby()
                ? BABY_START_DISTANCE_SQR
                : startDistanceSqr;
    }

    private double stopDistance(T dragon, LivingEntity target) {
        if (isBabyFollowingOwner(dragon, target)) {
            return DragonBabyOwnerFollowTuning.STOP_DISTANCE
                    * DragonBabyOwnerFollowTuning.STOP_DISTANCE;
        }
        return dragon.isBaby() && dragonClass.isInstance(target) && !dragonClass.cast(target).isBaby()
                ? BABY_STOP_DISTANCE_SQR
                : stopDistanceSqr;
    }

    private boolean isBabyFollowingOwner(T dragon, LivingEntity target) {
        return dragon.isBaby() && dragon.isTame() && target == dragon.getOwner();
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of("follow_target", followTarget == null ? "none" : followTarget.getName().getString());
    }
}
