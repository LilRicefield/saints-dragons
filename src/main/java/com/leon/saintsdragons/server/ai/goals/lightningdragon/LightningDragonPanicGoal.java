package com.leon.saintsdragons.server.ai.goals.lightningdragon;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

import static com.leon.saintsdragons.server.entity.dragons.lightningdragon.handlers.LightningDragonConstantsHandler.*;

/**
 * Panic goal for Lightning Dragon - moved out of main entity class
 */
public class LightningDragonPanicGoal extends Goal {
    private final LightningDragonEntity dragon;
    private double posX, posY, posZ;

    public LightningDragonPanicGoal(LightningDragonEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Do not panic if commanded to sit or being ridden
        if (dragon.isOrderedToSit() || dragon.isVehicle() || dragon.isPassenger()) {
            return false;
        }

        // Tamed dragons only panic in Wander mode (command == 2)
        if (dragon.isTame() && dragon.getCommand() != 2) {
            return false;
        }

        // Trigger panic when recently hurt and low health
        return dragon.getLastHurtByMob() != null && dragon.getHealth() < dragon.getMaxHealth() * 0.3f;
    }

    @Override
    public void start() {
        Vec3 vec = DefaultRandomPos.getPos(dragon, 16, 7);
        if (vec != null) {
            this.posX = vec.x;
            this.posY = vec.y;
            this.posZ = vec.z;
            dragon.setRunning(true);
        }
    }

    @Override
    public void tick() {
        dragon.getNavigation().moveTo(posX, posY, posZ, 2.0);
    }

    @Override
    public boolean canContinueToUse() {
        // Abort if state no longer permits panic
        if (dragon.isOrderedToSit() || dragon.isVehicle() || dragon.isPassenger()) {
            return false;
        }
        if (dragon.isTame() && dragon.getCommand() != 2) {
            return false;
        }

        // Continue while still moving and health remains under threshold
        return !dragon.getNavigation().isDone() && dragon.getHealth() < dragon.getMaxHealth() * 0.5f;
    }

    @Override
    public void stop() {
        dragon.setRunning(false);
        this.posX = this.posY = this.posZ = 0.0D;
    }
}
