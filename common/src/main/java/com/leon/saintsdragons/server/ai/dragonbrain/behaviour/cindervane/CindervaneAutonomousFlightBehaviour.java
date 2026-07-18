package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane;

import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.DragonFlightBehaviorProfile;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class CindervaneAutonomousFlightBehaviour extends AutonomousFlightBehaviour<Cindervane> {
    private static final double CRUISE_SPEED = 1.25D;
    private static final double LANDING_SPEED = 1.0D;
    private static final double AUTONOMOUS_DIVE_SPEED = 2.2D;

    private boolean wasThundering;
    private boolean wasRaining;

    public CindervaneAutonomousFlightBehaviour() {
        super(DragonFlightBehaviorProfile.cindervane(), CRUISE_SPEED, LANDING_SPEED, Cindervane.TAKEOFF_ANIMATION_TICKS);
    }

    @Override
    protected boolean canUseAutonomousFlight(Cindervane dragon) {
        if (!super.canUseAutonomousFlight(dragon)
                || dragon.isTame()
                || isGroundedPackFollower(dragon)
                || dragon.isInWater()
                || dragon.isInWaterOrBubble()
                || dragon.isInLava()
                || dragon.hasNearbyAssignedBabies(Cindervane.class) && !dragon.isOverStandardFlightDanger()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        return target == null || !target.isAlive();
    }

    @Override
    protected boolean canContinueAutonomousFlight(Cindervane dragon) {
        if (!super.canContinueAutonomousFlight(dragon)
                || dragon.isTame()
                || isGroundedPackFollower(dragon)
                || dragon.isInWater()
                || dragon.isInWaterOrBubble()
                || dragon.isInLava()
                || dragon.hasNearbyAssignedBabies(Cindervane.class) && !dragon.isOverStandardFlightDanger()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        return target == null || !target.isAlive();
    }

    @Override
    protected boolean shouldLandWhenAutonomousFlightBlocked(Cindervane dragon) {
        return dragon.isInWater()
                || dragon.isInWaterOrBubble()
                || dragon.isInLava()
                || dragon.isTame()
                || dragon.hasNearbyAssignedBabies(Cindervane.class) && !dragon.isOverStandardFlightDanger();
    }

    @Override
    protected boolean shouldTakeOff(Cindervane dragon) {
        if (isWildNight(dragon)) {
            return false;
        }
        return super.shouldTakeOff(dragon);
    }

    @Override
    protected int getLandingCooldownTicks(Cindervane dragon) {
        boolean thundering = dragon.level().isThundering();
        boolean raining = !thundering && dragon.level().isRaining();
        if (thundering || weatherChangedToStorm(thundering, raining)) {
            return 0;
        }
        return raining ? profile.landingCooldownTicks() / 4 : profile.landingCooldownTicks();
    }

    @Override
    protected int getDecisionIntervalTicks(Cindervane dragon) {
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
    protected int getTakeoffRoll(Cindervane dragon) {
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
    protected int getKeepFlyingRoll(Cindervane dragon) {
        if (isWildNight(dragon)) {
            return 100;
        }
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
    protected double getCruiseMinRange(Cindervane dragon) {
        return 80.0D;
    }

    @Override
    protected double getCruiseExtraRange(Cindervane dragon) {
        return 120.0D;
    }

    @Override
    protected double getMaxHeightAboveGround(Cindervane dragon) {
        if (dragon.level().isThundering()) {
            return 20.0D;
        }
        if (dragon.level().isRaining()) {
            return 30.0D;
        }
        return 80.0D;
    }

    @Override
    protected Vec3 adjustCruiseTarget(Cindervane dragon, Vec3 cruiseTarget) {
        return isCurrentCruiseDive() ? cruiseTarget : CindervanePackFlightCoordinator.biasCruiseTarget(dragon, cruiseTarget);
    }

    @Override
    protected double getAutonomousDiveSpeed() {
        return AUTONOMOUS_DIVE_SPEED;
    }

    private boolean isWildNight(Cindervane dragon) {
        long dayTime = dragon.level().getDayTime() % 24000L;
        return !dragon.isTame() && dayTime >= 13000L && dayTime < 23000L;
    }

    private boolean isGroundedPackFollower(Cindervane dragon) {
        if (!dragon.canParticipateInPack()) {
            return false;
        }
        UUID leaderUuid = dragon.getPackLeaderUuid();
        return leaderUuid != null && !leaderUuid.equals(dragon.getUUID()) && !dragon.isAerial();
    }

    private boolean weatherChangedToStorm(boolean thundering, boolean raining) {
        boolean changed = (thundering && !wasThundering) || (raining && !wasRaining);
        wasThundering = thundering;
        wasRaining = raining;
        return changed;
    }
}
