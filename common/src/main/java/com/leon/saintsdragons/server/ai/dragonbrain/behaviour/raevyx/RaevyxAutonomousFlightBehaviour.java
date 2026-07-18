package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx;

import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.goals.base.DragonFlightBehaviorProfile;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;

public class RaevyxAutonomousFlightBehaviour extends AutonomousFlightBehaviour<Raevyx> {
    private static final double CRUISE_SPEED = 2.0D;
    private static final double LANDING_SPEED = 1.45D;

    private boolean wasThundering;
    private boolean wasRaining;

    public RaevyxAutonomousFlightBehaviour() {
        super(DragonFlightBehaviorProfile.raevyx(), CRUISE_SPEED, LANDING_SPEED, Raevyx.TAKEOFF_ANIMATION_TICKS);
    }

    @Override
    protected boolean canUseAutonomousFlight(Raevyx dragon) {
        if (!super.canUseAutonomousFlight(dragon) || dragon.isTame()) {
            return false;
        }
        return !dragon.hasNearbyAssignedBabies(Raevyx.class) || dragon.isOverStandardFlightDanger();
    }

    @Override
    protected boolean canContinueAutonomousFlight(Raevyx dragon) {
        return !dragon.isTame()
                && (!dragon.hasNearbyAssignedBabies(Raevyx.class) || dragon.isOverStandardFlightDanger())
                && super.canContinueAutonomousFlight(dragon);
    }

    @Override
    protected boolean shouldLandWhenAutonomousFlightBlocked(Raevyx dragon) {
        return dragon.isTame() || dragon.hasNearbyAssignedBabies(Raevyx.class);
    }

    @Override
    protected int getLandingCooldownTicks(Raevyx dragon) {
        boolean thundering = dragon.level().isThundering();
        boolean raining = !thundering && dragon.level().isRaining();
        if (thundering || weatherChangedToStorm(thundering, raining)) {
            return 0;
        }
        return raining ? profile.landingCooldownTicks() / 4 : profile.landingCooldownTicks();
    }

    @Override
    protected int getDecisionIntervalTicks(Raevyx dragon) {
        boolean thundering = dragon.level().isThundering();
        boolean raining = !thundering && dragon.level().isRaining();
        if (thundering) {
            return profile.decisionIntervalThunder();
        }
        if (raining) {
            return profile.decisionIntervalRain();
        }
        return profile.decisionIntervalClear();
    }

    @Override
    protected int getTakeoffRoll(Raevyx dragon) {
        boolean thundering = dragon.level().isThundering();
        boolean raining = !thundering && dragon.level().isRaining();
        if (thundering) {
            return profile.takeoffRollThunder();
        }
        if (raining) {
            return profile.takeoffRollRain();
        }
        return profile.takeoffRollClear();
    }

    @Override
    protected int getKeepFlyingRoll(Raevyx dragon) {
        boolean thundering = dragon.level().isThundering();
        boolean raining = !thundering && dragon.level().isRaining();
        if (thundering) {
            return profile.keepFlyingRollThunder();
        }
        if (raining) {
            return profile.keepFlyingRollRain();
        }
        return profile.keepFlyingRollClear();
    }

    @Override
    protected double getMaxHeightAboveGround(Raevyx dragon) {
        if (dragon.level().isThundering()) {
            return 90.0D;
        }
        if (dragon.level().isRaining()) {
            return 70.0D;
        }
        return 50.0D;
    }

    private boolean weatherChangedToStorm(boolean thundering, boolean raining) {
        boolean changed = (thundering && !wasThundering) || (raining && !wasRaining);
        wasThundering = thundering;
        wasRaining = raining;
        return changed;
    }
}
