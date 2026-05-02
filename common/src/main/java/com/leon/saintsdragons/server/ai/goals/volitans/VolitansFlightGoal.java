package com.leon.saintsdragons.server.ai.goals.volitans;

import com.leon.saintsdragons.server.ai.goals.base.DragonAutonomousFlightGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonFlightBehaviorProfile;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.entity.LivingEntity;

public class VolitansFlightGoal extends DragonAutonomousFlightGoal<Volitans> {
    public VolitansFlightGoal(Volitans dragon) {
        super(dragon, DragonFlightBehaviorProfile.volitans(), 1.25D, 1.0D, Volitans.TAKEOFF_ANIMATION_TICKS);
    }

    @Override
    protected boolean canUseAutonomousFlight() {
        if (!super.canUseAutonomousFlight() || dragon.isTame()) {
            return false;
        }
        return canFlyOutsideCombat();
    }

    @Override
    protected boolean canContinueAutonomousFlight() {
        if (dragon.isBaby() || dragon.isTame() || !canFlyOutsideCombat()) {
            return false;
        }
        return super.canContinueAutonomousFlight();
    }

    @Override
    protected boolean shouldLandWhenAutonomousFlightBlocked() {
        return dragon.isTame()
                || dragon.isBurrowing()
                || dragon.isInWater()
                || dragon.isInWaterOrBubble()
                || dragon.isInLava();
    }

    @Override
    protected void beginAutonomousTakeoff() {
        dragon.beginAiTakeoff();
    }

    @Override
    protected double getCruiseMinRange() {
        return dragon.isFlightControllerStuck() || dragon.horizontalCollision ? 24.0D : 32.0D;
    }

    @Override
    protected double getCruiseExtraRange() {
        return dragon.isFlightControllerStuck() || dragon.horizontalCollision ? 32.0D : 48.0D;
    }

    @Override
    protected double getMaxHeightAboveGround() {
        return 34.0D;
    }

    private boolean canFlyOutsideCombat() {
        if (dragon.isBurrowing() || dragon.isInWater() || dragon.isInWaterOrBubble() || dragon.isInLava()) {
            return false;
        }
        if (dragon.isAiSpecialCombatActive() || dragon.isAiSpecialCombatReserved()) {
            return false;
        }

        LivingEntity target = dragon.getTarget();
        return target == null || !dragon.isTargetValid(target);
    }
}
