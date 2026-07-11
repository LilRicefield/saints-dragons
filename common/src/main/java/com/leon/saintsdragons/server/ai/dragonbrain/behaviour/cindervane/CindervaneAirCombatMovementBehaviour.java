package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AirCombatMovementBehaviour;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.LivingEntity;

public class CindervaneAirCombatMovementBehaviour extends AirCombatMovementBehaviour<Cindervane> {
    private static final double BITE_TRIGGER_RANGE = 6.0D;
    private static final double BITE_APPROACH_DISTANCE = 3.5D;
    private static final double CHASE_SPEED = 2.0D;
    private static final double DIVE_CHASE_SPEED = 3.1D;
    private static final double DIVE_CHASE_MIN_HEIGHT_ADVANTAGE = 7.0D;
    private static final double DIVE_CHASE_MAX_HORIZONTAL_DISTANCE = 42.0D;

    @Override
    protected void tickAirCombat(DragonBrainContext<Cindervane> context,
                                 LivingEntity target,
                                 boolean hasLineOfSight) {
        Cindervane dragon = context.dragon();
        double distance = dragon.distanceTo(target);
        if (distance <= BITE_TRIGGER_RANGE && hasLineOfSight) {
            setMeleePositionIntent(context, target, 0.0D, BITE_APPROACH_DISTANCE, 1.2D, 0.7D);
        } else if (shouldDiveChase(
                dragon,
                target,
                DIVE_CHASE_MIN_HEIGHT_ADVANTAGE,
                DIVE_CHASE_MAX_HORIZONTAL_DISTANCE
        )) {
            setPredictedChaseIntent(context, target, 3.0D, -0.25D, 0.08D, 0.12D, DIVE_CHASE_SPEED);
        } else {
            setPredictedChaseIntent(context, target, 4.0D, 0.5D, 0.15D, 0.35D, CHASE_SPEED);
        }
    }
}
