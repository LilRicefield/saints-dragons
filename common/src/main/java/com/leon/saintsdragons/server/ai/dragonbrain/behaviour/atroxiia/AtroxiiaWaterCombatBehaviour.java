package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.atroxiia;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

public final class AtroxiiaWaterCombatBehaviour extends DragonBehaviour<Atroxiia> {
    private static final double BITE_RANGE = 5.0D;
    private static final int ATTACK_CADENCE_TICKS = 24;

    public AtroxiiaWaterCombatBehaviour() {
        super(Map.of(DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean canStart(DragonBrainContext<Atroxiia> context) {
        return isWaterCombatContext(context);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Atroxiia> context) {
        return isWaterCombatContext(context);
    }

    @Override
    protected void start(DragonBrainContext<Atroxiia> context) {
        context.dragon().setAggressive(true);
    }

    @Override
    protected void tick(DragonBrainContext<Atroxiia> context) {
        Atroxiia dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null) {
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (dragon.getActiveAbility() != null
                || dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0
                || gapToTarget(dragon, target) > BITE_RANGE
                || !dragon.getSensing().hasLineOfSight(target)) {
            return;
        }

        if (dragon.combatManager.canStart(ModAbilities.ATROXIIA_UNDERWATER_BITE)
                && dragon.getAiCombatPacing().canUse(ModAbilities.ATROXIIA_UNDERWATER_BITE, false)) {
            dragon.combatManager.tryUseAiAbility(
                    ModAbilities.ATROXIIA_UNDERWATER_BITE,
                    false,
                    ATTACK_CADENCE_TICKS,
                    ATTACK_CADENCE_TICKS,
                    0,
                    ATTACK_CADENCE_TICKS
            );
        }
    }

    @Override
    protected void stop(DragonBrainContext<Atroxiia> context) {
        if (!context.memories().has(DragonMemories.ATTACK_TARGET)) {
            context.dragon().setAggressive(false);
        }
    }

    private boolean isWaterCombatContext(DragonBrainContext<Atroxiia> context) {
        Atroxiia dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        return dragon.isInWaterOrBubble()
                && !dragon.isBaby()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && target != null
                && dragon.isTargetValid(target)
                && dragon.canTarget(target);
    }

    private double gapToTarget(Atroxiia dragon, LivingEntity target) {
        double combinedRadii = (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        return Math.max(0.0D, dragon.distanceTo(target) - combinedRadii);
    }
}
