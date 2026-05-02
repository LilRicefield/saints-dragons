package com.leon.saintsdragons.server.ai.goals.ignivorus;

import com.leon.saintsdragons.server.ai.goals.base.DragonAutonomousFlightGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonFlightBehaviorProfile;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.entity.LivingEntity;

public class IgnivorusFlightGoal extends DragonAutonomousFlightGoal<Ignivorus> {
    public IgnivorusFlightGoal(Ignivorus dragon) {
        super(dragon, DragonFlightBehaviorProfile.ignivorus(), 1.75D, 1.5D, Ignivorus.TAKEOFF_ANIMATION_TICKS);
    }

    @Override
    protected boolean canUseAutonomousFlight() {
        if (!super.canUseAutonomousFlight()) {
            return false;
        }
        if (dragon.isTame() || dragon.isAiSpecialCombatActive()) {
            return false;
        }
        if (dragon.isInWater() || dragon.isInWaterOrBubble() || dragon.isInLava()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        return target == null || !dragon.isTargetValid(target);
    }

    @Override
    protected boolean canContinueAutonomousFlight() {
        if (dragon.isTame()) {
            return false;
        }
        if (dragon.isAiSpecialCombatActive()) {
            return false;
        }
        if (dragon.isInWater() || dragon.isInWaterOrBubble() || dragon.isInLava()) {
            return false;
        }
        LivingEntity target = dragon.getTarget();
        if (target != null && dragon.isTargetValid(target)) {
            return false;
        }
        return super.canContinueAutonomousFlight();
    }

    @Override
    protected boolean shouldLandWhenAutonomousFlightBlocked() {
        return dragon.isTame()
                || dragon.isInWater()
                || dragon.isInWaterOrBubble()
                || dragon.isInLava();
    }

    @Override
    protected double getCruiseMinRange() {
        return 50.0D;
    }

    @Override
    protected double getCruiseExtraRange() {
        return 70.0D;
    }

    @Override
    protected double getMaxHeightAboveGround() {
        return 60.0D;
    }
}
