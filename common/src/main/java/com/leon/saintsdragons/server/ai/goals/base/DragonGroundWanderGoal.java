package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Generic ground wandering goal for all rideable dragons.
 * Only active when not flying and respects command system.
 */
public class DragonGroundWanderGoal<T extends RideableDragonBase> extends DragonBaseGoal<T> {
    private final double speed;
    private final int interval;

    public DragonGroundWanderGoal(T dragon, double speed, int interval) {
        super(dragon);
        this.speed = speed;
        this.interval = interval;
    }

    @Override
    protected boolean canUseAdditional() {
        // Only wander on ground
        if (dragon.isFlying()) {
            return false;
        }

        // Don't wander during combat
        if (isInCombat()) {
            return false;
        }

        // Check command compatibility - only wander in Wander(2) mode or when untamed
        if (!checkCommandCompatible(2)) {
            return false;
        }

        // Random interval check
        return random.nextInt(interval) == 0;
    }

    @Override
    protected boolean canContinueAdditional() {
        // Stop if flying
        if (dragon.isFlying()) {
            return false;
        }

        // Stop if combat starts
        if (isInCombat()) {
            return false;
        }

        // Stop if command changes
        if (!checkCommandCompatible(2)) {
            return false;
        }

        // Continue while navigation is active
        return dragon.getNavigation().isInProgress();
    }

    @Override
    public void start() {
        Vec3 wanderPos = getWanderPosition();
        if (wanderPos != null) {
            dragon.setGroundMoveStateFromAI(1); // Walking
            dragon.getNavigation().moveTo(wanderPos.x, wanderPos.y, wanderPos.z, speed);
        }
    }

    @Override
    public void stop() {
        dragon.setGroundMoveStateFromAI(0); // Idle
        dragon.getNavigation().stop();
    }

    @Override
    public void tick() {
        // Maintain walk animation while moving
        if (dragon.getNavigation().isInProgress()) {
            dragon.setGroundMoveStateFromAI(1);
        }
    }

    /**
     * Find a suitable wander position.
     * Biases toward owner if tamed and far away.
     */
    @Nullable
    protected Vec3 getWanderPosition() {
        // If tamed and owner is far, bias movement toward owner
        if (dragon.isTame()) {
            var owner = dragon.getOwner();
            if (owner != null && dragon.distanceToSqr(owner) > 20.0 * 20.0) {
                // Move generally toward owner (60-degree cone)
                return DefaultRandomPos.getPosTowards(
                        dragon,
                        16, // range
                        7,  // vertical range
                        owner.position(),
                        (float) Math.PI / 3F
                );
            }
        }

        // Default random wandering
        return DefaultRandomPos.getPos(dragon, 20, 8);
    }
}
