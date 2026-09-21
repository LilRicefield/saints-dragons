package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans;

import com.leon.saintsdragons.common.config.dragon.profile.VolitansStatProfile;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public class VolitansWaterCombatBehaviour extends DragonBehaviour<Volitans> {
    private static final int ROAR_COOLDOWN_TICKS = VolitansStatProfile.WaterCombatBehaviour.ROAR_COOLDOWN_TICKS;
    private static final double BITE_RANGE = VolitansStatProfile.WaterCombatBehaviour.BITE_RANGE;
    private static final double CLAW_RANGE = VolitansStatProfile.WaterCombatBehaviour.CLAW_RANGE;
    private static final double GORE_RANGE = VolitansStatProfile.WaterCombatBehaviour.GORE_RANGE;
    private static final double ROAR_MIN_RANGE = VolitansStatProfile.WaterCombatBehaviour.ROAR_MIN_RANGE;
    private static final double ROAR_MAX_RANGE = VolitansStatProfile.WaterCombatBehaviour.ROAR_MAX_RANGE;
    private static final int MELEE_CADENCE_TICKS = 30;

    private Volitans dragon;
    private int attackCooldown = 0;

    public VolitansWaterCombatBehaviour() {
        super(Map.of(DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean canStart(DragonBrainContext<Volitans> context) {
        this.dragon = context.dragon();
        if (dragon.isAiSpecialCombatActive() || dragon.isAiSpecialCombatReserved()) {
            return false;
        }
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (!canFightTarget(target)) {
            return false;
        }
        return dragon.isInWaterOrBubble() || dragon.isUnderWater();
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Volitans> context) {
        this.dragon = context.dragon();
        if (dragon.isAiSpecialCombatActive() || dragon.isAiSpecialCombatReserved()) {
            return false;
        }
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (!canFightTarget(target)) {
            return false;
        }
        return dragon.isInWaterOrBubble() || dragon.isUnderWater();
    }

    @Override
    protected void start(DragonBrainContext<Volitans> context) {
        this.dragon = context.dragon();
        dragon.setAggressive(true);
    }

    @Override
    protected void stop(DragonBrainContext<Volitans> context) {
        dragon.setAggressive(false);
        dragon.getWaterCombatMovement().reset();
        attackCooldown = 0;
    }

    @Override
    protected void tick(DragonBrainContext<Volitans> context) {
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (!dragon.isTargetValid(target)) {
            return;
        }

        double gap = getGapToTarget(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);
        if (!hasLineOfSight) return;
        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (dragon.isGroundCombatAbilityActive()) {
            return;
        }

        if (attackCooldown > 0 || dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0 || dragon.isGroundMobilityActive()) {
            return;
        }

        if (gap <= GORE_RANGE) {
            if (!dragon.getWaterCombatMovement().makingSpace(target)) tryMelee(target, gap);
            return;
        }

        if (dragon.getBreathCombat().tryStart(target)) {
            return;
        }

        tryRoar(gap);
    }

    private boolean canFightTarget(LivingEntity target) {
        if (!dragon.isTargetValid(target)) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (dragon.isVehicle() || dragon.isOrderedToSit() || dragon.isBaby()) {
            return false;
        }
        if (!(dragon.isInWaterOrBubble() || dragon.isUnderWater())) {
            return false;
        }
        return dragon.distanceToSqr(target) <= getMaxAggroDistanceSqr();
    }

    private boolean tryRoar(double gap) {
        if (gap < ROAR_MIN_RANGE || gap > ROAR_MAX_RANGE) {
            return false;
        }
        if (!canUseAiAbility(ModAbilities.VOLITANS_ROAR, true)) {
            return false;
        }
        return startAiAbility(ModAbilities.VOLITANS_ROAR, true, 24, ROAR_COOLDOWN_TICKS, 120, 48);
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return dragon == null ? Map.of() : Map.of("breath", dragon.getBreathCombat().debugSummary(),
                "swim_tactic", dragon.getWaterCombatMovement().debugSummary());
    }

    private void tryMelee(LivingEntity target, double gap) {
        if (gap <= BITE_RANGE) {
            if (DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)) {
                if (canUseAiAbility(ModAbilities.VOLITANS_BITE, false)) {
                    startAiAbility(ModAbilities.VOLITANS_BITE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
                }
                return;
            }
            float roll = dragon.getRandom().nextFloat();
            if (roll < 0.42F) {
                if (canUseAiAbility(ModAbilities.VOLITANS_CLAW, false)) {
                    startAiAbility(ModAbilities.VOLITANS_CLAW, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
                }
            } else if (roll < 0.72F) {
                if (canUseAiAbility(ModAbilities.VOLITANS_BITE, false)) {
                    startAiAbility(ModAbilities.VOLITANS_BITE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
                }
            } else if (canUseAiAbility(ModAbilities.VOLITANS_HORN_GORE, false)) {
                startAiAbility(ModAbilities.VOLITANS_HORN_GORE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
            }
            return;
        }

        if (gap <= CLAW_RANGE) {
            if (dragon.getRandom().nextFloat() < 0.58F) {
                if (canUseAiAbility(ModAbilities.VOLITANS_CLAW, false)) {
                    startAiAbility(ModAbilities.VOLITANS_CLAW, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
                }
            } else if (canUseAiAbility(ModAbilities.VOLITANS_HORN_GORE, false)) {
                startAiAbility(ModAbilities.VOLITANS_HORN_GORE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
            }
            return;
        }

        if (gap <= GORE_RANGE && canUseAiAbility(ModAbilities.VOLITANS_HORN_GORE, false)) {
            startAiAbility(ModAbilities.VOLITANS_HORN_GORE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
        }
    }

    private boolean canUseAiAbility(DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        return dragon.combatManager.canStartAiAbility(abilityType, majorAbility);
    }

    private boolean startAiAbility(DragonAbilityType<?, ?> abilityType,
                                   boolean majorAbility,
                                   int cadenceTicks,
                                   int abilityCooldownTicks,
                                   int majorCooldownTicks,
                                   int repeatLockoutTicks) {
        boolean started = dragon.combatManager.tryUseAiAbility(
                abilityType,
                majorAbility,
                cadenceTicks,
                abilityCooldownTicks,
                majorCooldownTicks,
                repeatLockoutTicks
        );
        if (started) {
            attackCooldown = Math.max(attackCooldown, cadenceTicks);
        }
        return started;
    }

    private double getGapToTarget(LivingEntity target) {
        double centerDistance = dragon.distanceTo(target);
        double combinedRadii = (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        return Math.max(0.0D, centerDistance - combinedRadii);
    }

    private double getMaxAggroDistanceSqr() {
        double followRange = dragon.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 32.0D;
        }
        return followRange * followRange;
    }
}
