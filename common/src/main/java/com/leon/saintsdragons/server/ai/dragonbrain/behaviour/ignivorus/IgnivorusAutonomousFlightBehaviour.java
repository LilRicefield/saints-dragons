package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus;

import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.DragonFlightBehaviorProfile;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class IgnivorusAutonomousFlightBehaviour extends AutonomousFlightBehaviour<Ignivorus> {
    public IgnivorusAutonomousFlightBehaviour() {
        super(DragonFlightBehaviorProfile.ignivorus(), 1.75D, 1.5D, Ignivorus.TAKEOFF_ANIMATION_TICKS);
    }

    @Override
    protected boolean canUseAutonomousFlight(Ignivorus dragon) {
        if (!super.canUseAutonomousFlight(dragon)
                || dragon.isTame()
                || dragon.isAiSpecialCombatActive()
                || dragon.isPhase2Active()
                || dragon.shouldSuspendRoostWandering()
                || dragon.isInWater()
                || dragon.isInWaterOrBubble()
                || dragon.isInLava()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        return target == null || !dragon.isTargetValid(target);
    }

    @Override
    protected boolean canContinueAutonomousFlight(Ignivorus dragon) {
        if (!super.canContinueAutonomousFlight(dragon)
                || dragon.isTame()
                || dragon.isAiSpecialCombatActive()
                || dragon.isPhase2Active()
                || dragon.shouldSuspendRoostWandering()
                || dragon.isInWater()
                || dragon.isInWaterOrBubble()
                || dragon.isInLava()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        return target == null || !dragon.isTargetValid(target);
    }

    @Override
    protected boolean shouldLandWhenAutonomousFlightBlocked(Ignivorus dragon) {
        return dragon.isTame()
                || dragon.isPhase2Active()
                || dragon.shouldSuspendRoostWandering()
                || dragon.isInWater()
                || dragon.isInWaterOrBubble()
                || dragon.isInLava();
    }

    @Override
    protected double getCruiseMinRange(Ignivorus dragon) {
        return 16.0D;
    }

    @Override
    protected double getCruiseExtraRange(Ignivorus dragon) {
        return Ignivorus.ROOST_WANDER_RADIUS - getCruiseMinRange(dragon);
    }

    @Override
    protected double getMaxHeightAboveGround(Ignivorus dragon) {
        return 60.0D;
    }

    @Override
    protected int getCruiseTargetAttempts(Ignivorus dragon) {
        return 10;
    }

    @Override
    protected boolean isCruiseTargetAllowed(Ignivorus dragon, Vec3 cruiseTarget) {
        return dragon.isWithinRoostWanderArea(cruiseTarget);
    }
}
