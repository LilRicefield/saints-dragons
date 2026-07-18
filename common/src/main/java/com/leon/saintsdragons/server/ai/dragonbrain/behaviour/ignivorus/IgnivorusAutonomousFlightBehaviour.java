package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus;

import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.DragonFlightBehaviorProfile;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.entity.LivingEntity;

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
                || dragon.isInWater()
                || dragon.isInWaterOrBubble()
                || dragon.isInLava();
    }

    @Override
    protected double getCruiseMinRange(Ignivorus dragon) {
        return 50.0D;
    }

    @Override
    protected double getCruiseExtraRange(Ignivorus dragon) {
        return 70.0D;
    }

    @Override
    protected double getMaxHeightAboveGround(Ignivorus dragon) {
        return 60.0D;
    }
}
