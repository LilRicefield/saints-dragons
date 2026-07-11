package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans;

import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AutonomousFlightBehaviour;
import com.leon.saintsdragons.server.ai.goals.base.DragonFlightBehaviorProfile;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.entity.LivingEntity;

public class VolitansAutonomousFlightBehaviour extends AutonomousFlightBehaviour<Volitans> {
    public VolitansAutonomousFlightBehaviour() {
        super(DragonFlightBehaviorProfile.volitans(), 1.25D, 1.0D, Volitans.TAKEOFF_ANIMATION_TICKS);
    }

    @Override
    protected boolean canUseAutonomousFlight(Volitans dragon) {
        if (!super.canUseAutonomousFlight(dragon) || dragon.isTame()) {
            return false;
        }
        return canFlyOutsideCombat(dragon);
    }

    @Override
    protected boolean canContinueAutonomousFlight(Volitans dragon) {
        if (!super.canContinueAutonomousFlight(dragon) || dragon.isBaby() || dragon.isTame()) {
            return false;
        }
        return canFlyOutsideCombat(dragon);
    }

    @Override
    protected boolean shouldLandWhenAutonomousFlightBlocked(Volitans dragon) {
        return dragon.isTame()
                || dragon.isBurrowing()
                || dragon.isInWater()
                || dragon.isInWaterOrBubble()
                || dragon.isInLava();
    }

    @Override
    protected void beginAutonomousTakeoff(Volitans dragon) {
        dragon.beginAiTakeoff();
    }

    @Override
    protected double getCruiseMinRange(Volitans dragon) {
        return dragon.isFlightControllerStuck() || dragon.horizontalCollision ? 24.0D : 32.0D;
    }

    @Override
    protected double getCruiseExtraRange(Volitans dragon) {
        return dragon.isFlightControllerStuck() || dragon.horizontalCollision ? 32.0D : 48.0D;
    }

    @Override
    protected double getMaxHeightAboveGround(Volitans dragon) {
        return 34.0D;
    }

    private boolean canFlyOutsideCombat(Volitans dragon) {
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
