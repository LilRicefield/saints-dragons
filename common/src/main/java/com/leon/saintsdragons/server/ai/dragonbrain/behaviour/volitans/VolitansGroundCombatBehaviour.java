package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.goals.base.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public class VolitansGroundCombatBehaviour extends DragonBehaviour<Volitans> {
    public static final double BITE_RANGE = 4.1D;
    private static final double CLAW_RANGE = 5.1D;
    public static final double GORE_RANGE = 6.2D;
    private static final double BREATH_MIN_RANGE = 6.0D;
    private static final double BREATH_MAX_RANGE = 16.0D;
    private static final double POISON_BALL_MIN_RANGE = 8.0D;
    private static final double POISON_BALL_MAX_RANGE = 24.0D;
    private static final double ROAR_OPEN_RANGE = 14.0D;
    public static final float CHASE_SPEED = 1.2F;
    private static final double BURROW_MIN_RANGE = 10.0D;
    private static final double BURROW_MAX_RANGE = 30.0D;
    private static final double BURROW_CHASE_SPEED = 1.55D;
    private static final int MELEE_CADENCE_TICKS = 30;

    private Volitans dragon;
    private DragonBrainContext<Volitans> currentContext;
    private int attackCooldown = 0;
    private int burrowCooldown = 0;
    private int poisonBallHoldTicks = 0;
    private int breathHoldTicks = 0;
    private boolean usedRoarOpener = false;
    private int roarOpenerDelay = 0;
    private boolean wasAbilityHoldingLastTick = false;
    private boolean wasBurrowAbilityHoldingLastTick = false;

    public VolitansGroundCombatBehaviour() {
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
        return !dragon.isFlying() && !dragon.isTakeoff() && !dragon.isLanding() && !dragon.isHovering();
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Volitans> context) {
        this.dragon = context.dragon();
        if (dragon.isAiSpecialCombatActive() || dragon.isAiSpecialCombatReserved()) {
            return false;
        }
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (dragon.isGroundCombatAbilityActive() || dragon.isGroundMobilityActive()) {
            return true;
        }
        if (!canFightTarget(target)) {
            return false;
        }
        if (dragon.isFlying() || dragon.isTakeoff() || dragon.isLanding() || dragon.isHovering()) {
            return false;
        }
        return !isTargetAirborne(target);
    }

    @Override
    protected void start(DragonBrainContext<Volitans> context) {
        this.dragon = context.dragon();
        dragon.setAggressive(true);
        usedRoarOpener = false;
        roarOpenerDelay = 8;
        wasAbilityHoldingLastTick = false;
        wasBurrowAbilityHoldingLastTick = false;
    }

    @Override
    protected void stop(DragonBrainContext<Volitans> context) {
        dragon.setAggressive(false);
        attackCooldown = 0;
        poisonBallHoldTicks = 0;
        breathHoldTicks = 0;
        wasAbilityHoldingLastTick = false;
        wasBurrowAbilityHoldingLastTick = false;
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_POISON_BALL)) {
            dragon.requestPoisonBallRelease();
        }
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BREATH)) {
            dragon.forceEndActiveAbility();
        }
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BURROW)) {
            dragon.requestBurrowExit(false);
        }
        currentContext = null;
    }

    @Override
    protected void tick(DragonBrainContext<Volitans> context) {
        this.currentContext = context;
        if (attackCooldown > 0) attackCooldown--;
        if (burrowCooldown > 0) burrowCooldown--;

        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (!dragon.isTargetValid(target)) {
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double gap = getGapToTarget(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);

        if (handleActiveAbility(target, gap, hasLineOfSight)) {
            wasAbilityHoldingLastTick = true;
            return;
        }
        if (wasAbilityHoldingLastTick) {
            // Ability-owned movement just ended; immediately reacquire the chase instead of
            // waiting on a stale path refresh or long post-ability attack cooldown.
            wasAbilityHoldingLastTick = false;
            if (wasBurrowAbilityHoldingLastTick) {
                dragon.getAiCombatPacing().setGlobalActionLock(10);
                wasBurrowAbilityHoldingLastTick = false;
            }
        }

        if (!usedRoarOpener && hasLineOfSight && gap >= 5.0D && gap <= ROAR_OPEN_RANGE && canUseAiAbility(ModAbilities.VOLITANS_ROAR, true)) {
            if (roarOpenerDelay > 0) {
                roarOpenerDelay--;
            } else {
                stopMovement("volitans-combat:roar-opener");
                if (startAiAbility(ModAbilities.VOLITANS_ROAR, true, 24, 200, 120, 48)) {
                    usedRoarOpener = true;
                }
            }
            return;
        }
        usedRoarOpener = true;

        if (!hasLineOfSight) {
            return;
        }

        if (gap <= GORE_RANGE) {
            if (gap <= BITE_RANGE) {
                stopMovement("volitans-combat:melee-range");
            }
            if (attackCooldown <= 0 && dragon.getAiCombatPacing().getCadenceCooldownTicks() <= 0 && !dragon.isGroundCombatAbilityActive()) {
                tryMelee(target, gap);
                return;
            }
            return;
        }

        if (tryRoarPunish(gap)) {
            return;
        }

        if (tryBurrowApproach(target, gap, hasLineOfSight)) {
            return;
        }

        if (tryPoisonBall(gap)) {
            return;
        }

        if (tryBreath(gap)) {
            return;
        }

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
        if (dragon.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
            return false;
        }
        if (dragon.isInWaterOrBubble() || dragon.isUnderWater()) {
            return false;
        }
        return !isTargetAirborne(target);
    }

    private boolean handleActiveAbility(LivingEntity target, double gap, boolean hasLineOfSight) {
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_POISON_BALL)) {
            stopMovement("volitans-combat:poison-ball");
            if (--poisonBallHoldTicks <= 0 || !hasLineOfSight || gap < 5.0D || gap > 28.0D) {
                dragon.requestPoisonBallRelease();
            }
            return true;
        }
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BREATH)) {
            stopMovement("volitans-combat:breath");
            if (--breathHoldTicks <= 0 || !hasLineOfSight || gap < 4.5D || gap > 18.0D) {
                dragon.forceEndActiveAbility();
            }
            return true;
        }
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BURROW)) {
            wasBurrowAbilityHoldingLastTick = true;
            if (!dragon.isTargetValid(target) || dragon.distanceToSqr(target) > getMaxAggroDistanceSqr()) {
                dragon.requestBurrowExit(false);
                return true;
            }
            if (dragon.isBurrowing()) {
                dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
                dragon.getAIMovement().moveToGroundTarget(target, BURROW_CHASE_SPEED, true);
            } else {
                stopMovement("volitans-combat:burrow-start");
            }
            if (dragon.isBurrowing() && gap <= 4.75D) {
                dragon.requestBurrowExit(true);
            }
            return true;
        }
        if (dragon.shouldAiHoldPositionForAbility() || dragon.isGroundMobilityActive()) {
            stopMovement("volitans-combat:ability-locked");
            return true;
        }
        return false;
    }

    private boolean tryRoarPunish(double gap) {
        if (attackCooldown > 0 || !canUseAiAbility(ModAbilities.VOLITANS_ROAR, true) || dragon.isGroundCombatAbilityActive()) {
            return false;
        }
        if (gap < 4.5D || gap > 10.0D) {
            return false;
        }
        stopMovement("volitans-combat:roar");
        return startAiAbility(ModAbilities.VOLITANS_ROAR, true, 24, 200, 120, 48);
    }

    private boolean tryPoisonBall(double gap) {
        if (attackCooldown > 0 || !canUseAiAbility(ModAbilities.VOLITANS_POISON_BALL, true) || dragon.isGroundCombatAbilityActive()) {
            return false;
        }
        if (gap < POISON_BALL_MIN_RANGE || gap > POISON_BALL_MAX_RANGE) {
            return false;
        }
        stopMovement("volitans-combat:poison-ball-start");
        if (!startAiAbility(ModAbilities.VOLITANS_POISON_BALL, true, 16, 110, 90, 36)) {
            return false;
        }
        poisonBallHoldTicks = 18 + dragon.getRandom().nextInt(8);
        return true;
    }

    private boolean tryBurrowApproach(LivingEntity target, double gap, boolean hasLineOfSight) {
        if (attackCooldown > 0 || dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0 || burrowCooldown > 0 || dragon.isGroundCombatAbilityActive() || dragon.isGroundMobilityActive()) {
            return false;
        }
        if (gap < BURROW_MIN_RANGE || gap > BURROW_MAX_RANGE) {
            return false;
        }
        if (dragon.hurtTime <= 0 && hasLineOfSight && dragon.getRandom().nextFloat() >= 0.18F) {
            return false;
        }
        stopMovement("volitans-combat:burrow-start");
        if (!startAiAbility(ModAbilities.VOLITANS_BURROW, true, 14, 0, 80, 28)) {
            return false;
        }
        burrowCooldown = 220;
        return true;
    }

    private boolean tryBreath(double gap) {
        if (attackCooldown > 0 || !canUseAiAbility(ModAbilities.VOLITANS_BREATH, true) || dragon.isGroundCombatAbilityActive()) {
            return false;
        }
        if (gap < BREATH_MIN_RANGE || gap > BREATH_MAX_RANGE) {
            return false;
        }
        if (dragon.getRandom().nextFloat() >= 0.35F) {
            return false;
        }
        stopMovement("volitans-combat:breath-start");
        dragon.setBreathMode(dragon.getRandom().nextFloat() < 0.65F ? 1 : 0);
        if (!startAiAbility(ModAbilities.VOLITANS_BREATH, true, 18, 150, 110, 42)) {
            return false;
        }
        breathHoldTicks = 60 + dragon.getRandom().nextInt(35);
        return true;
    }

    private void tryMelee(LivingEntity target, double gap) {
        if (attackCooldown > 0 || dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0 || dragon.isGroundCombatAbilityActive()) {
            return;
        }

        if (gap <= BITE_RANGE) {
            if (DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)) {
                if (canUseAiAbility(ModAbilities.VOLITANS_BITE, false)) {
                    startAiAbility(ModAbilities.VOLITANS_BITE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
                }
                return;
            }
            float roll = dragon.getRandom().nextFloat();
            if (roll < 0.42F && canUseAiAbility(ModAbilities.VOLITANS_CLAW, false)) {
                startAiAbility(ModAbilities.VOLITANS_CLAW, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
            } else if (roll < 0.72F && canUseAiAbility(ModAbilities.VOLITANS_BITE, false)) {
                startAiAbility(ModAbilities.VOLITANS_BITE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
            } else if (canUseAiAbility(ModAbilities.VOLITANS_HORN_GORE, false)) {
                startAiAbility(ModAbilities.VOLITANS_HORN_GORE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
            }
            return;
        }

        if (gap <= CLAW_RANGE) {
            if (dragon.getRandom().nextFloat() < 0.58F && canUseAiAbility(ModAbilities.VOLITANS_CLAW, false)) {
                startAiAbility(ModAbilities.VOLITANS_CLAW, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
            } else if (canUseAiAbility(ModAbilities.VOLITANS_HORN_GORE, false)) {
                startAiAbility(ModAbilities.VOLITANS_HORN_GORE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
            }
            return;
        }

        if (gap <= GORE_RANGE && canUseAiAbility(ModAbilities.VOLITANS_HORN_GORE, false)) {
            startAiAbility(ModAbilities.VOLITANS_HORN_GORE, false, MELEE_CADENCE_TICKS, MELEE_CADENCE_TICKS, 0, 24);
        }
    }

    private boolean canUseAiAbility(com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> abilityType, boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType) && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }

    private boolean startAiAbility(com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> abilityType,
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

    public boolean isGroundMovementLocked() {
        return dragon != null && (dragon.isGroundCombatAbilityActive() || dragon.isGroundMobilityActive());
    }

    private void stopMovement(String reason) {
        if (currentContext != null) {
            currentContext.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.stop(reason));
        }
    }

    private boolean isTargetAirborne(LivingEntity target) {
        return DragonTargetingHelper.isTargetAirborne(target, 8.0D) && !target.isInWaterOrBubble();
    }
}
