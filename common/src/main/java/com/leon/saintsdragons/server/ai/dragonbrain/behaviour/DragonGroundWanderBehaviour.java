package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class DragonGroundWanderBehaviour<T extends RideableDragonBase> extends DragonBehaviour<T> {
    private final double speed;
    private final int interval;
    private final int targetAttempts;
    private final Predicate<T> eligibility;
    private final BiPredicate<T, Vec3> targetFilter;
    private Vec3 target;

    public DragonGroundWanderBehaviour(double speed, int interval) {
        this(speed, interval, 1, dragon -> true, (dragon, position) -> true);
    }

    public DragonGroundWanderBehaviour(double speed,
                                       int interval,
                                       int targetAttempts,
                                       Predicate<T> eligibility,
                                       BiPredicate<T, Vec3> targetFilter) {
        this.speed = speed;
        this.interval = interval;
        this.targetAttempts = Math.max(1, targetAttempts);
        this.eligibility = Objects.requireNonNull(eligibility);
        this.targetFilter = Objects.requireNonNull(targetFilter);
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (!basicConditions(dragon) || dragon.getRandom().nextInt(interval) != 0) {
            return false;
        }
        target = null;
        for (int attempt = 0; attempt < targetAttempts; attempt++) {
            Vec3 candidate = DefaultRandomPos.getPos(dragon, 20, 8);
            if (candidate != null && targetFilter.test(dragon, candidate)) {
                target = candidate;
                break;
            }
        }
        return target != null;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return basicConditions(context.dragon()) && context.dragon().getAIMovement().isPathing();
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        if (target != null) {
            context.dragon().setGroundMoveStateFromAI(1);
            context.dragon().getAIMovement().moveToGroundPosition(target, speed, false);
        }
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        if (context.dragon().getAIMovement().isPathing()) {
            context.dragon().setGroundMoveStateFromAI(1);
        }
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        context.dragon().setGroundMoveStateFromAI(0);
        if (!context.dragon().isAerial()) {
            context.dragon().getAIMovement().stop();
        }
        target = null;
    }

    private boolean basicConditions(T dragon) {
        return !dragon.isAerial()
                && !dragon.isOrderedToSit()
                && !dragon.isSittingDownAnimation()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isInLove()
                && (dragon.canSwim() || !dragon.isInWaterOrBubble())
                && (dragon.getTarget() == null || !dragon.getTarget().isAlive())
                && (!dragon.isTame() || dragon.getCommand() == 2)
                && eligibility.test(dragon);
    }
}
