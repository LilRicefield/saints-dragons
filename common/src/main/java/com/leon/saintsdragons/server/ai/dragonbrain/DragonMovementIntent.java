package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public sealed interface DragonMovementIntent permits DragonMovementIntent.None,
        DragonMovementIntent.Stop,
        DragonMovementIntent.HoldPosition,
        DragonMovementIntent.AutoPosition,
        DragonMovementIntent.StrictAirPosition,
        DragonMovementIntent.AutoTarget,
        DragonMovementIntent.GroundPosition,
        DragonMovementIntent.ProgressiveGroundPosition,
        DragonMovementIntent.GroundTarget,
        DragonMovementIntent.GroundTransitionPosition,
        DragonMovementIntent.GroundTransitionTarget {

    void apply(RideableDragonBase dragon);

    static DragonMovementIntent none() {
        return None.INSTANCE;
    }

    static DragonMovementIntent stop() {
        return stop("unspecified");
    }

    static DragonMovementIntent stop(String reason) {
        return new Stop(reason);
    }

    static DragonMovementIntent holdPosition() {
        return HoldPosition.INSTANCE;
    }

    static DragonMovementIntent auto(Vec3 target, double speed) {
        return new AutoPosition(target, speed);
    }

    static DragonMovementIntent strictAir(Vec3 target, double speed) {
        return new StrictAirPosition(target, speed);
    }

    static DragonMovementIntent auto(LivingEntity target, double speed) {
        return new AutoTarget(target, speed);
    }

    static DragonMovementIntent ground(Vec3 target, double speed, boolean running) {
        return new GroundPosition(target, speed, running);
    }

    static DragonMovementIntent progressiveGround(Vec3 target, double speed, boolean running) {
        return new ProgressiveGroundPosition(target, speed, running, Double.NaN);
    }

    static DragonMovementIntent progressiveGround(Vec3 target,
                                                   double speed,
                                                   boolean running,
                                                   double arrivalTolerance) {
        return new ProgressiveGroundPosition(target, speed, running, arrivalTolerance);
    }

    static DragonMovementIntent ground(LivingEntity target, double speed, boolean running) {
        return new GroundTarget(target, speed, running);
    }

    static DragonMovementIntent transitionToGround(Vec3 target, double speed) {
        return new GroundTransitionPosition(target, speed);
    }

    static DragonMovementIntent transitionToGround(LivingEntity target, double speed) {
        return new GroundTransitionTarget(target, speed);
    }

    static DragonMovementIntent transitionToGround(double speed) {
        return new GroundTransitionTarget(null, speed);
    }

    enum None implements DragonMovementIntent {
        INSTANCE;

        @Override
        public void apply(RideableDragonBase dragon) {
        }
    }

    record Stop(String reason) implements DragonMovementIntent {
        @Override
        public void apply(RideableDragonBase dragon) {
            dragon.getAIMovement().stop();
        }
    }

    enum HoldPosition implements DragonMovementIntent {
        INSTANCE;

        @Override
        public void apply(RideableDragonBase dragon) {
            dragon.getAIMovement().stopAndClearAllMovement();
        }
    }

    record AutoPosition(Vec3 target, double speed) implements DragonMovementIntent {
        @Override
        public void apply(RideableDragonBase dragon) {
            dragon.getAIMovement().setWaypoint(target, speed);
        }
    }

    record StrictAirPosition(Vec3 target, double speed) implements DragonMovementIntent {
        @Override
        public void apply(RideableDragonBase dragon) {
            dragon.getAIMovement().setAsyncAirWaypoint(target, speed);
        }
    }

    record AutoTarget(LivingEntity target, double speed) implements DragonMovementIntent {
        @Override
        public void apply(RideableDragonBase dragon) {
            dragon.getAIMovement().setWaypoint(target, speed);
        }
    }

    record GroundPosition(Vec3 target, double speed, boolean running) implements DragonMovementIntent {
        @Override
        public void apply(RideableDragonBase dragon) {
            dragon.getAIMovement().moveToGroundPosition(target, speed, running);
        }
    }

    record ProgressiveGroundPosition(Vec3 target,
                                     double speed,
                                     boolean running,
                                     double arrivalTolerance) implements DragonMovementIntent {
        @Override
        public void apply(RideableDragonBase dragon) {
            dragon.getAIMovement().moveToProgressiveGroundPosition(
                    target,
                    speed,
                    running,
                    arrivalTolerance
            );
        }
    }

    record GroundTarget(LivingEntity target, double speed, boolean running) implements DragonMovementIntent {
        @Override
        public void apply(RideableDragonBase dragon) {
            dragon.getAIMovement().moveToGroundTarget(target, speed, running);
        }
    }

    record GroundTransitionPosition(Vec3 target, double speed) implements DragonMovementIntent {
        @Override
        public void apply(RideableDragonBase dragon) {
            dragon.getAIMovement().requestGroundTransition(target, speed);
        }
    }

    record GroundTransitionTarget(LivingEntity target, double speed) implements DragonMovementIntent {
        @Override
        public void apply(RideableDragonBase dragon) {
            dragon.getAIMovement().requestGroundTransition(target, speed);
        }
    }
}
