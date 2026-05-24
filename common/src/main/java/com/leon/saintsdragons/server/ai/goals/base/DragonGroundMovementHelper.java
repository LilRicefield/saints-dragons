package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class DragonGroundMovementHelper {
    private DragonGroundMovementHelper() {
    }

    public static void setGroundIdle(RideableDragonBase dragon) {
        dragon.setRunning(false);
        dragon.setGroundMoveStateFromAI(0);
    }

    public static void setGroundWalk(RideableDragonBase dragon) {
        dragon.setRunning(false);
        dragon.setGroundMoveStateFromAI(1);
    }

    public static void setGroundRun(RideableDragonBase dragon) {
        dragon.setRunning(true);
        dragon.setGroundMoveStateFromAI(2);
    }

    public static void setGroundMoveState(RideableDragonBase dragon, boolean running) {
        if (running) {
            setGroundRun(dragon);
        } else {
            setGroundWalk(dragon);
        }
    }

    public static void stopGroundMovement(RideableDragonBase dragon) {
        dragon.getNavigation().stop();
        setGroundIdle(dragon);
    }

    public static boolean moveToLivingTarget(RideableDragonBase dragon, LivingEntity target, double speed, boolean running) {
        setGroundMoveState(dragon, running);
        return dragon.getNavigation().moveTo(target, speed);
    }

    public static void moveToPosition(RideableDragonBase dragon, Vec3 target, double speed, boolean running) {
        setGroundMoveState(dragon, running);
        dragon.getNavigation().moveTo(target.x, target.y, target.z, speed);
    }
}
