package com.leon.saintsdragons.server.ai.goals.cindervane;

import com.leon.saintsdragons.server.ai.goals.base.DragonAutonomousFlightGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonFlightBehaviorProfile;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.LivingEntity;

public class CindervaneFlightGoal extends DragonAutonomousFlightGoal<Cindervane> {
    private boolean wasThundering;
    private boolean wasRaining;

    public CindervaneFlightGoal(Cindervane dragon) {
        super(dragon, DragonFlightBehaviorProfile.cindervane(), 1.25D, 1.0D, Cindervane.TAKEOFF_ANIMATION_TICKS);
    }

    @Override
    protected boolean canUseAutonomousFlight() {
        if (!super.canUseAutonomousFlight()) {
            return false;
        }
        if (isFollowingPackLeader() || isInOwnerFollowMode()) {
            return false;
        }
        if (dragon.isInWater() || dragon.isInWaterOrBubble() || dragon.isInLava()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        if (target != null && target.isAlive()) {
            return false;
        }
        if (dragon.hasNearbyAssignedBabies(Cindervane.class) && !dragon.isOverStandardFlightDanger()) {
            return false;
        }
        return !(dragon.isTame() && dragon.getCommand() == 2);
    }

    @Override
    protected boolean canContinueAutonomousFlight() {
        if (isFollowingPackLeader() || isInOwnerFollowMode()) {
            return false;
        }
        if (dragon.isInWater() || dragon.isInWaterOrBubble() || dragon.isInLava()) {
            return false;
        }
        if (dragon.isTame() && dragon.getCommand() == 2) {
            return false;
        }
        if (dragon.hasNearbyAssignedBabies(Cindervane.class) && !dragon.isOverStandardFlightDanger()) {
            return false;
        }
        return super.canContinueAutonomousFlight();
    }

    @Override
    protected boolean shouldLandWhenAutonomousFlightBlocked() {
        return dragon.isInWater()
                || dragon.isInWaterOrBubble()
                || dragon.isInLava()
                || (dragon.isTame() && dragon.getCommand() == 2)
                || (dragon.hasNearbyAssignedBabies(Cindervane.class) && !dragon.isOverStandardFlightDanger());
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
        if (isWildNight()) {
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
    protected boolean shouldTakeOff() {
        if (isWildNight()) {
            return false;
        }
        return super.shouldTakeOff();
    }

    @Override
    protected double getCruiseMinRange() {
        return 80.0D;
    }

    @Override
    protected double getCruiseExtraRange() {
        return 120.0D;
    }

    @Override
    protected double getMaxHeightAboveGround() {
        if (dragon.level().isThundering()) {
            return 20.0D;
        }
        if (dragon.level().isRaining()) {
            return 30.0D;
        }
        return 80.0D;
    }

    private boolean isWildNight() {
        long dayTime = dragon.level().getDayTime() % 24000L;
        return !dragon.isTame() && dayTime >= 13000L && dayTime < 23000L;
    }

    private boolean isInOwnerFollowMode() {
        LivingEntity owner = dragon.getOwner();
        return dragon.isTame()
                && dragon.getCommand() == 0
                && owner != null
                && owner.isAlive()
                && owner.level() == dragon.level();
    }

    private boolean isFollowingPackLeader() {
        if (!dragon.canParticipateInPack()) {
            return false;
        }
        java.util.UUID leaderUuid = dragon.getPackLeaderUuid();
        return leaderUuid != null && !leaderUuid.equals(dragon.getUUID());
    }

    private boolean weatherChangedToStorm(boolean thundering, boolean raining) {
        boolean changed = (thundering && !wasThundering) || (raining && !wasRaining);
        wasThundering = thundering;
        wasRaining = raining;
        return changed;
    }
}
