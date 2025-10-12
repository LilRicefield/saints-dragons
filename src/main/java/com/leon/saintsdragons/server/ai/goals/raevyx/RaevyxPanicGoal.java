package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Panic goal for Lightning Dragon - moved out of main entity class
 */
public class RaevyxPanicGoal extends Goal {
    private final Raevyx wyvern;
    private double posX, posY, posZ;

    public RaevyxPanicGoal(Raevyx wyvern) {
        this.wyvern = wyvern;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Do not panic if commanded to sit or being ridden
        if (wyvern.isOrderedToSit() || wyvern.isVehicle() || wyvern.isPassenger()) {
            return false;
        }

        // Tamed dragons only panic in Wander mode (command == 2)
        if (wyvern.isTame() && wyvern.getCommand() != 2) {
            return false;
        }

        // Trigger panic when recently hurt and low health
        return wyvern.getLastHurtByMob() != null && wyvern.getHealth() < wyvern.getMaxHealth() * 0.3f;
    }

    @Override
    public void start() {
        Vec3 vec = DefaultRandomPos.getPos(wyvern, 16, 7);
        if (vec != null) {
            this.posX = vec.x;
            this.posY = vec.y;
            this.posZ = vec.z;
            wyvern.setRunning(true);
        }
    }

    @Override
    public void tick() {
        wyvern.getNavigation().moveTo(posX, posY, posZ, 2.0);
    }

    @Override
    public boolean canContinueToUse() {
        // Abort if state no longer permits panic
        if (wyvern.isOrderedToSit() || wyvern.isVehicle() || wyvern.isPassenger()) {
            return false;
        }
        if (wyvern.isTame() && wyvern.getCommand() != 2) {
            return false;
        }

        // Continue while still moving and health remains under threshold
        return !wyvern.getNavigation().isDone() && wyvern.getHealth() < wyvern.getMaxHealth() * 0.5f;
    }

    @Override
    public void stop() {
        wyvern.setRunning(false);
        this.posX = this.posY = this.posZ = 0.0D;
    }
}
