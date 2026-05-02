package com.leon.saintsdragons.server.ai.goals.raevyx;

import com.leon.saintsdragons.server.ai.goals.base.DragonAutonomousFlightGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonFlightBehaviorProfile;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;

public class RaevyxFlightGoal extends DragonAutonomousFlightGoal<Raevyx> {
    private boolean wasThundering;
    private boolean wasRaining;

    public RaevyxFlightGoal(Raevyx dragon) {
        super(dragon, DragonFlightBehaviorProfile.raevyx(), 2.0D, 1.45D, Raevyx.TAKEOFF_ANIMATION_TICKS);
    }

    @Override
    protected boolean canUseAutonomousFlight() {
        if (!super.canUseAutonomousFlight()) {
            return false;
        }
        if (dragon.isTame()) {
            return false;
        }
        return !dragon.hasNearbyAssignedBabies(Raevyx.class) || dragon.isOverStandardFlightDanger();
    }

    @Override
    protected boolean canContinueAutonomousFlight() {
        if (dragon.isTame()) {
            return false;
        }
        return super.canContinueAutonomousFlight();
    }

    @Override
    protected boolean shouldLandWhenAutonomousFlightBlocked() {
        return dragon.isTame();
    }

    @Override
    protected int getLandingCooldownTicks() {
        boolean thundering = dragon.level().isThundering();
        boolean raining = !thundering && dragon.level().isRaining();
        if (thundering || weatherChangedToStorm(thundering, raining)) {
            return 0;
        }
        return raining ? profile.landingCooldownTicks() / 4 : profile.landingCooldownTicks();
    }

    @Override
    protected int getDecisionIntervalTicks() {
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
    protected int getTakeoffRoll() {
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
    protected int getKeepFlyingRoll() {
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
    protected double getMaxHeightAboveGround() {
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
