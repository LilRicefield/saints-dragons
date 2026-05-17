package com.leon.saintsdragons.server.flight;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class DragonGroundedAerialRecovery {

    private DragonGroundedAerialRecovery() {
    }

    public static int tick(
            Level level,
            boolean onGround,
            boolean inWaterOrBubble,
            boolean inLava,
            boolean takeoff,
            boolean flying,
            boolean hovering,
            boolean landing,
            boolean ignoreGroundedTakeoffRecovery,
            Vec3 deltaMovement,
            int groundedTicks,
            int graceTicks,
            double upwardVelocityTolerance,
            Runnable markLandedNow
    ) {
        if (level.isClientSide || !onGround || inWaterOrBubble || inLava) {
            return 0;
        }

        if (landing) {
            return 0;
        }

        if ((takeoff && ignoreGroundedTakeoffRecovery) || (!takeoff && !flying && !hovering)) {
            return 0;
        }

        if (deltaMovement.y > upwardVelocityTolerance) {
            return 0;
        }

        int nextTicks = groundedTicks + 1;
        if (nextTicks >= Math.max(1, graceTicks)) {
            markLandedNow.run();
            return 0;
        }

        return nextTicks;
    }
}
