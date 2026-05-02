package com.leon.saintsdragons.server.ai.goals.stegonaut;

import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;


public class StegonautGroundWanderGoal extends RandomStrollGoal {

    private final Stegonaut drake;

    public StegonautGroundWanderGoal(Stegonaut drake, double speed, int interval) {
        super(drake, speed, interval);
        this.drake = drake;
    }

    @Override
    public boolean canUse() {
        if (drake.isOrderedToSit() || drake.isVehicle() || drake.isPassenger()) {
            return false;
        }

        if (drake.isInWaterOrBubble()) {
            return false;
        }

        if (drake.getTarget() != null && drake.getTarget().isAlive()) {
            if (!drake.isTargetValid(drake.getTarget()) || !drake.canTarget(drake.getTarget())) {
                drake.setTarget(null);
            } else {
                return false;
            }
        }

        if (drake.isInLove()) {
            return false;
        }

        if (drake.isTame()) {
            int command = drake.getCommand();
            if (command != 2) {
                return false;
            }
        }

        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (drake.getTarget() != null && drake.getTarget().isAlive()) {
            if (!drake.isTargetValid(drake.getTarget()) || !drake.canTarget(drake.getTarget())) {
                drake.setTarget(null);
            } else {
                return false;
            }
        }

        if (drake.isInLove()) {
            return false;
        }

        if (drake.isInWaterOrBubble()) {
            return false;
        }
        if (drake.isOrderedToSit()) {
            return false;
        }

        return super.canContinueToUse();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        return DefaultRandomPos.getPos(this.mob, 20, 8);
    }
}