package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.varasuchus;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.goals.base.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;


public class VarasuchusCombatBehaviour extends DragonBehaviour<Varasuchus> {
    public static final float CHASE_SPEED = 1.5F;
    public static final double BITE_RANGE = 5.0D;
    public static final double LAND_PREY_BITE_RANGE = 1.45D;
    private static final double HORN_RANGE = 5.0D;
    private static final double CLAW_RANGE = 3.5D;
    private static final int MELEE_CADENCE_TICKS = 30;
    private static final float PHASE_TWO_HEALTH_THRESHOLD = 0.5F;

    private Varasuchus drake;
    private DragonBrainContext<Varasuchus> currentContext;
    private int attackCooldown;

    public VarasuchusCombatBehaviour() {
        super(Map.of(DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean canStart(DragonBrainContext<Varasuchus> context) {
        this.drake = context.dragon();
        if (drake.isBaby()) {
            return false;
        }
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (!drake.isTargetValid(target)) {
            return false;
        }
        if (drake.isVehicle() || drake.isOrderedToSit()) {
            return false;
        }
        return isWithinAggroRange(target);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Varasuchus> context) {
        this.drake = context.dragon();
        if (drake.isBaby()) {
            return false;
        }
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (!drake.isTargetValid(target)) {
            return false;
        }
        if (drake.isVehicle() || drake.isOrderedToSit()) {
            return false;
        }
        return isWithinAggroRange(target);
    }

    @Override
    protected void start(DragonBrainContext<Varasuchus> context) {
        this.drake = context.dragon();
        drake.setAggressive(true);
    }

    @Override
    protected void stop(DragonBrainContext<Varasuchus> context) {
        drake.getAiSwimController().stop();
        drake.setAggressive(false);
        attackCooldown = 0;
        currentContext = null;
    }

    @Override
    protected void tick(DragonBrainContext<Varasuchus> context) {
        this.currentContext = context;
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null) {
            return;
        }

        drake.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (drake.isAbilityActive(ModAbilities.VARASUCHUS_PHASE_SHIFT)) {
            return;
        }

        if (shouldEnterPhaseTwo()) {
            drake.combatManager.tryUseAbility(ModAbilities.VARASUCHUS_PHASE_SHIFT);
            if (drake.isAbilityActive(ModAbilities.VARASUCHUS_PHASE_SHIFT)) {
                return;
            }
        }

        double gap = getGapToTarget(target);

        double meleeStopRange = getMeleeStopRange(target);
        if (gap <= HORN_RANGE) {
            if (gap <= meleeStopRange || isPerformingAttack()) {
                if (!drake.isInWaterOrBubble()) {
                    stopMovement("varasuchus-combat:melee-range");
                }
            }
            tryPerformAttacks(target);
        }
    }

    private void tryPerformAttacks(LivingEntity target) {
        if (attackCooldown > 0 || drake.getAiCombatPacing().getCadenceCooldownTicks() > 0 || isPerformingAttack()) {
            return;
        }

        if (!drake.getSensing().hasLineOfSight(target) && !drake.isInWaterOrBubble()) {
            return;
        }

        DragonAbilityType<Varasuchus, ?> ability = choosePrimaryAttack(target);
        if (ability != null && drake.combatManager.canStart(ability) && drake.getAiCombatPacing().canUse(ability, false)) {
            drake.combatManager.tryUseAbility(ability);
            drake.getAiCombatPacing().recordUse(ability, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, false, 0, 24);
            attackCooldown = MELEE_CADENCE_TICKS;
        }
    }

    private DragonAbilityType<Varasuchus, ?> choosePrimaryAttack(LivingEntity target) {
        double gap = getGapToTarget(target);
        boolean phaseTwo = drake.isPhaseTwoActive();
        if (DragonTargetingHelper.isBiteOnlyPreyTarget(target)) {
            double biteRange = getMeleeStopRange(target);
            if (gap <= biteRange) {
                return phaseTwo ? ModAbilities.VARASUCHUS_BITE2 : ModAbilities.VARASUCHUS_BITE;
            }
            return null;
        }

        if (gap <= CLAW_RANGE) {
            return ModAbilities.VARASUCHUS_HORN_GORE;
        }

        if (phaseTwo && gap <= BITE_RANGE && drake.getRandom().nextFloat() < 0.30F) {
            return ModAbilities.VARASUCHUS_SLASH_BARRAGE;
        }

        if (phaseTwo && gap <= HORN_RANGE) {
            return drake.getRandom().nextBoolean()
                    ? ModAbilities.VARASUCHUS_BITE2
                    : ModAbilities.VARASUCHUS_CLAW;
        }

        if (!phaseTwo && gap > CLAW_RANGE && gap <= HORN_RANGE && drake.getRandom().nextFloat() < 0.35f) {
            return ModAbilities.VARASUCHUS_TAIL_ATTACK;
        }

        if (gap <= BITE_RANGE) {
            return phaseTwo ? ModAbilities.VARASUCHUS_BITE2 : ModAbilities.VARASUCHUS_BITE;
        }

        if (gap <= HORN_RANGE) {
            return ModAbilities.VARASUCHUS_HORN_GORE;
        }

        return null;
    }

    private boolean isPerformingAttack() {
        return drake.isAbilityActive(ModAbilities.VARASUCHUS_BITE)
            || drake.isAbilityActive(ModAbilities.VARASUCHUS_BITE2)
            || drake.isAbilityActive(ModAbilities.VARASUCHUS_SLASH_BARRAGE)
            || drake.isAbilityActive(ModAbilities.VARASUCHUS_CLAW)
            || drake.isAbilityActive(ModAbilities.VARASUCHUS_HORN_GORE)
            || drake.isAbilityActive(ModAbilities.VARASUCHUS_TAIL_ATTACK)
            || drake.isAbilityActive(ModAbilities.VARASUCHUS_TAILGUARD);
    }

    private boolean shouldEnterPhaseTwo() {
        return !drake.isPhaseTwoActive()
            && drake.getHealth() <= drake.getMaxHealth() * PHASE_TWO_HEALTH_THRESHOLD;
    }

    private boolean isWithinAggroRange(LivingEntity target) {
        double followRange = drake.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 16.0D;
        }
        double maxDistanceSq = followRange * followRange;
        return drake.distanceToSqr(target) <= maxDistanceSq;
    }

    private double getGapToTarget(LivingEntity target) {
        double distance = drake.distanceTo(target);
        double combinedRadii = (drake.getBbWidth() + target.getBbWidth()) * 0.5;
        return Math.max(0.0D, distance - combinedRadii);
    }

    private double getMeleeStopRange(LivingEntity target) {
        return DragonTargetingHelper.isBiteOnlyPreyTarget(target) && !drake.isInWaterOrBubble()
                ? LAND_PREY_BITE_RANGE
                : BITE_RANGE;
    }

    public boolean isMovementLocked() {
        return drake != null && (isPerformingAttack()
                || drake.isAbilityActive(ModAbilities.VARASUCHUS_PHASE_SHIFT));
    }

    private void stopMovement(String reason) {
        if (currentContext != null) {
            currentContext.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.stop(reason));
        }
    }
}
