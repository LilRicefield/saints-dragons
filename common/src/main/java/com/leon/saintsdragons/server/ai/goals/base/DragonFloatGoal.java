package com.leon.saintsdragons.server.ai.goals.base;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Gentle float goal for large dragons that prevents drowning without aggressive bouncing.
 * this only applies buoyancy when the dragon is actually sinking.
 * This prevents the bounce-out-of-water behavior seen with large entities.
 */
public class DragonFloatGoal extends Goal {

    private final Mob mob;
    private static final double GENTLE_BUOYANCY = 0.008; // Gentler than vanilla's 0.015
    private static final double SINKING_THRESHOLD = -0.01; // Only apply buoyancy if sinking

    public DragonFloatGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        // Activate when in water and not flying/falling rapidly
        return mob.isInWater() && mob.getFluidHeight(net.minecraft.tags.FluidTags.WATER) > mob.getFluidJumpThreshold();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        // Only apply upward force if dragon is actively sinking
        Vec3 deltaMovement = mob.getDeltaMovement();

        // If sinking (negative Y velocity), apply gentle buoyancy
        if (deltaMovement.y < SINKING_THRESHOLD) {
            // Apply gentler buoyancy than vanilla
            mob.setDeltaMovement(deltaMovement.add(0.0, GENTLE_BUOYANCY, 0.0));

            // If dragon is a swimmer or has jumping capability, allow jump
            if (mob.getRandom().nextFloat() < 0.8F) {
                mob.getJumpControl().jump();
            }
        }
        // If already floating or rising, don't add more upward force
    }
}
