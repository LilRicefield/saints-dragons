package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class DragonGroundWanderGoal<T extends RideableDragonBase> extends DragonBaseGoal<T> {
    private final double speed;
    private final int interval;
    private boolean forceTrigger = false;

    public DragonGroundWanderGoal(T dragon, double speed, int interval) {
        super(dragon);
        this.speed = speed;
        this.interval = interval;
    }

    @Override
    protected boolean canUseAdditional() {
        if (dragon.isAerial()) {
            return false;
        }

        if (isInCombat()) {
            return false;
        }

        if (!checkCommandCompatible(2)) {
            return false;
        }

        if (forceTrigger) {
            return true;
        }

        return random.nextInt(interval) == 0;
    }

    @Override
    protected boolean canContinueAdditional() {
        if (dragon.isAerial()) {
            return false;
        }

        if (isInCombat()) {
            return false;
        }

        if (!checkCommandCompatible(2)) {
            return false;
        }

        return dragon.getAIMovement().isPathing();
    }

    @Override
    public void start() {
        forceTrigger = false;
        Vec3 wanderPos = getWanderPosition();
        if (wanderPos != null) {
            dragon.setGroundMoveStateFromAI(1);
            dragon.getAIMovement().moveToGroundPosition(wanderPos, speed, false);
        }
    }

    @Override
    public void stop() {
        dragon.setGroundMoveStateFromAI(0);
        if (!dragon.isAerial()) {
            dragon.getAIMovement().stop();
        }
    }

    @Override
    public void tick() {
        if (dragon.getAIMovement().isPathing()) {
            dragon.setGroundMoveStateFromAI(1);
        }
    }

    public void forceTrigger() {
        this.forceTrigger = true;
    }

    @Nullable
    protected Vec3 getWanderPosition() {
        return DefaultRandomPos.getPos(dragon, 20, 8);
    }
}
