package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

public class VolitansGroundCombatBehaviour extends DragonBehaviour<Volitans> {
    public static final double BITE_RANGE = 4.1D;
    public static final double CHASE_STOP_RANGE = 3.5D;
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
    private static final int INITIAL_CHASE_COMMIT_TICKS = 32;
    private static final int RETREAT_CHASE_COMMIT_TICKS = 36;
    private static final int POST_ABILITY_CHASE_COMMIT_TICKS = 18;
    private static final int FAILED_PRESSURE_CHASE_COMMIT_TICKS = 12;
    private static final int PRESSURE_STABLE_TICKS = 12;
    private static final int PRESSURE_DECISION_INTERVAL_TICKS = 8;
    private static final int ROAR_OPENER_WINDOW_TICKS = 100;
    private static final int BURROW_ROUTE_STALL_TICKS = 24;
    private static final double TARGET_STABLE_SPEED = 0.12D;
    private static final double STABLE_GAP_DELTA = 0.06D;
    private static final double RETREATING_SPEED = 0.07D;

    private Volitans dragon;
    private DragonBrainContext<Volitans> currentContext;
    private int burrowCooldown = 0;
    private int poisonBallHoldTicks = 0;
    private int breathHoldTicks = 0;
    private boolean usedRoarOpener = false;
    private int roarOpenerWindowTicks;
    private int chaseCommitTicks;
    private int pressureDecisionCooldown;
    private int stableRangeTicks;
    private boolean wasAbilityHoldingLastTick = false;
    private boolean wasBurrowAbilityHoldingLastTick = false;
    private double previousGap = Double.NaN;
    private double lastGap = -1.0D;
    private double lastGapDelta;
    private double lastTargetAwaySpeed;
    private boolean lastLineOfSight;
    private CombatCommitment commitment = CombatCommitment.CHASE;
    private String lastDecision = "idle";

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
        roarOpenerWindowTicks = ROAR_OPENER_WINDOW_TICKS;
        chaseCommitTicks = INITIAL_CHASE_COMMIT_TICKS;
        pressureDecisionCooldown = 0;
        stableRangeTicks = 0;
        wasAbilityHoldingLastTick = false;
        wasBurrowAbilityHoldingLastTick = false;
        previousGap = Double.NaN;
        lastGap = -1.0D;
        lastGapDelta = 0.0D;
        lastTargetAwaySpeed = 0.0D;
        lastLineOfSight = false;
        commitment = CombatCommitment.CHASE;
        lastDecision = "chase:engaged";
    }

    @Override
    protected void stop(DragonBrainContext<Volitans> context) {
        dragon.setAggressive(false);
        poisonBallHoldTicks = 0;
        breathHoldTicks = 0;
        chaseCommitTicks = 0;
        pressureDecisionCooldown = 0;
        stableRangeTicks = 0;
        roarOpenerWindowTicks = 0;
        wasAbilityHoldingLastTick = false;
        wasBurrowAbilityHoldingLastTick = false;
        previousGap = Double.NaN;
        commitment = CombatCommitment.CHASE;
        lastDecision = "stopped";
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
        if (burrowCooldown > 0) {
            burrowCooldown--;
        }
        if (pressureDecisionCooldown > 0) {
            pressureDecisionCooldown--;
        }
        if (roarOpenerWindowTicks > 0) {
            roarOpenerWindowTicks--;
        } else {
            usedRoarOpener = true;
        }

        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (!dragon.isTargetValid(target)) {
            releaseHeldAbilityWithoutTarget();
            lastDecision = "invalid-target";
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double gap = getGapToTarget(target);
        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);
        TargetMotion targetMotion = updateTargetMotion(target, gap, hasLineOfSight);

        if (handleActiveAbility(target, gap, hasLineOfSight)) {
            wasAbilityHoldingLastTick = true;
            commitment = dragon.isBurrowing()
                    ? CombatCommitment.BURROW
                    : CombatCommitment.ABILITY;
            lastDecision = dragon.isBurrowing() ? "burrow:pursuing" : "ability:committed";
            return;
        }
        if (wasAbilityHoldingLastTick) {
            wasAbilityHoldingLastTick = false;
            chaseCommitTicks = Math.max(chaseCommitTicks, POST_ABILITY_CHASE_COMMIT_TICKS);
            if (wasBurrowAbilityHoldingLastTick) {
                dragon.getAiCombatPacing().setGlobalActionLock(10);
                wasBurrowAbilityHoldingLastTick = false;
            }
        }

        if (gap <= GORE_RANGE) {
            chaseCommitTicks = 0;
            commitment = CombatCommitment.MELEE;
            if (dragon.getAiCombatPacing().getCadenceCooldownTicks() <= 0
                    && !dragon.isGroundCombatAbilityActive()
                    && tryMelee(target, gap, targetMotion.awaySpeed())) {
                lastDecision = "melee:started";
            } else {
                lastDecision = "melee:holding-range";
            }
            return;
        }

        if (!hasLineOfSight && tryBurrowApproach(context, gap)) {
            commitment = CombatCommitment.BURROW;
            lastDecision = "burrow:lost-route";
            return;
        }

        if (!hasLineOfSight) {
            commitToChase(RETREAT_CHASE_COMMIT_TICKS, "chase:no-line-of-sight");
            return;
        }

        if (targetMotion.openingDistance()) {
            commitToChase(RETREAT_CHASE_COMMIT_TICKS, "chase:target-retreating");
        }
        if (chaseCommitTicks > 0) {
            chaseCommitTicks--;
            commitment = CombatCommitment.CHASE;
            if (!targetMotion.openingDistance()) {
                lastDecision = "chase:committed";
            }
            return;
        }

        if (pressureDecisionCooldown > 0 || stableRangeTicks < PRESSURE_STABLE_TICKS) {
            commitment = CombatCommitment.CHASE;
            lastDecision = pressureDecisionCooldown > 0
                    ? "chase:decision-cooldown"
                    : "chase:stabilizing-target";
            return;
        }

        pressureDecisionCooldown = PRESSURE_DECISION_INTERVAL_TICKS;
        if (tryBurrowApproach(context, gap)) {
            commitment = CombatCommitment.BURROW;
            lastDecision = "burrow:tactical-approach";
            return;
        }
        if (tryRoarOpener(gap)) {
            commitment = CombatCommitment.PRESSURE;
            lastDecision = "pressure:roar-opener";
            return;
        }
        if (tryPoisonBall(gap)) {
            commitment = CombatCommitment.PRESSURE;
            lastDecision = "pressure:poison-ball";
            return;
        }
        if (tryBreath(gap, targetMotion.awaySpeed())) {
            commitment = CombatCommitment.PRESSURE;
            lastDecision = "pressure:breath";
            return;
        }

        commitToChase(FAILED_PRESSURE_CHASE_COMMIT_TICKS, "chase:no-pressure-opening");
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
        if (target.isInWaterOrBubble()) {
            return false;
        }
        return !isTargetAirborne(target);
    }

    private boolean handleActiveAbility(LivingEntity target, double gap, boolean hasLineOfSight) {
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_POISON_BALL)) {
            holdForCommittedAbility();
            if (--poisonBallHoldTicks <= 0 || !hasLineOfSight || gap < 5.0D || gap > 28.0D) {
                dragon.requestPoisonBallRelease();
            }
            return true;
        }
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BREATH)) {
            holdForCommittedAbility();
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
                holdForCommittedAbility();
            }
            if (dragon.isBurrowing() && gap <= 4.75D) {
                dragon.requestBurrowExit(true);
            }
            return true;
        }
        if (dragon.isGroundCombatAbilityActive()) {
            holdForCommittedAbility();
            return true;
        }
        if (dragon.isGroundMobilityActive()) {
            return true;
        }
        return false;
    }

    private boolean tryRoarOpener(double gap) {
        if (usedRoarOpener
                || roarOpenerWindowTicks <= 0
                || dragon.isGroundCombatAbilityActive()
                || gap < 5.0D
                || gap > ROAR_OPEN_RANGE
                || !canUseAiAbility(ModAbilities.VOLITANS_ROAR, true)) {
            return false;
        }
        if (startAiAbility(ModAbilities.VOLITANS_ROAR, true, 24, 200, 120, 48)) {
            usedRoarOpener = true;
            return true;
        }
        return false;
    }

    private boolean tryPoisonBall(double gap) {
        if (!canUseAiAbility(ModAbilities.VOLITANS_POISON_BALL, true)
                || dragon.isGroundCombatAbilityActive()) {
            return false;
        }
        if (gap < POISON_BALL_MIN_RANGE || gap > POISON_BALL_MAX_RANGE) {
            return false;
        }
        if (!startAiAbility(ModAbilities.VOLITANS_POISON_BALL, true, 16, 110, 90, 36)) {
            return false;
        }
        poisonBallHoldTicks = 18 + dragon.getRandom().nextInt(8);
        return true;
    }

    private boolean tryBurrowApproach(DragonBrainContext<Volitans> context, double gap) {
        if (dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0
                || burrowCooldown > 0
                || dragon.isGroundCombatAbilityActive()
                || dragon.isGroundMobilityActive()) {
            return false;
        }
        if (gap < BURROW_MIN_RANGE || gap > BURROW_MAX_RANGE) {
            return false;
        }

        long unreachableSince = context.memories()
                .get(DragonMemories.CANT_REACH_WALK_TARGET_SINCE)
                .orElse(Long.MAX_VALUE);
        boolean routeStalled = unreachableSince != Long.MAX_VALUE
                && context.gameTime() - unreachableSince >= BURROW_ROUTE_STALL_TICKS;
        boolean underPressure = dragon.hurtTime > 0;
        if (!routeStalled && !underPressure) {
            return false;
        }
        if (!startAiAbility(ModAbilities.VOLITANS_BURROW, true, 14, 0, 80, 28)) {
            return false;
        }
        burrowCooldown = 220;
        return true;
    }

    private boolean tryBreath(double gap, double targetAwaySpeed) {
        if (!canUseAiAbility(ModAbilities.VOLITANS_BREATH, true)
                || dragon.isGroundCombatAbilityActive()) {
            return false;
        }
        if (gap < BREATH_MIN_RANGE || gap > BREATH_MAX_RANGE) {
            return false;
        }
        if (targetAwaySpeed >= RETREATING_SPEED) {
            return false;
        }
        dragon.setBreathMode(dragon.getRandom().nextFloat() < 0.65F ? 1 : 0);
        if (!startAiAbility(ModAbilities.VOLITANS_BREATH, true, 18, 150, 110, 42)) {
            return false;
        }
        breathHoldTicks = 60 + dragon.getRandom().nextInt(35);
        return true;
    }

    private boolean tryMelee(LivingEntity target, double gap, double targetAwaySpeed) {
        if (dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0
                || dragon.isGroundCombatAbilityActive()) {
            return false;
        }

        if (gap <= BITE_RANGE) {
            if (DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)) {
                return tryMeleeAbility(ModAbilities.VOLITANS_BITE);
            }
            if (targetAwaySpeed >= RETREATING_SPEED
                    && tryMeleeAbility(ModAbilities.VOLITANS_HORN_GORE)) {
                return true;
            }

            float roll = dragon.getRandom().nextFloat();
            if (roll < 0.5F) {
                return tryMeleeAbility(ModAbilities.VOLITANS_CLAW)
                        || tryMeleeAbility(ModAbilities.VOLITANS_BITE)
                        || tryMeleeAbility(ModAbilities.VOLITANS_HORN_GORE);
            }
            return tryMeleeAbility(ModAbilities.VOLITANS_BITE)
                    || tryMeleeAbility(ModAbilities.VOLITANS_CLAW)
                    || tryMeleeAbility(ModAbilities.VOLITANS_HORN_GORE);
        }

        if (gap <= CLAW_RANGE) {
            return tryMeleeAbility(ModAbilities.VOLITANS_CLAW)
                    || tryMeleeAbility(ModAbilities.VOLITANS_HORN_GORE);
        }

        return gap <= GORE_RANGE && tryMeleeAbility(ModAbilities.VOLITANS_HORN_GORE);
    }

    private boolean tryMeleeAbility(
            com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> abilityType) {
        return canUseAiAbility(abilityType, false)
                && startAiAbility(
                abilityType,
                false,
                MELEE_CADENCE_TICKS,
                MELEE_CADENCE_TICKS,
                0,
                24
        );
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
            claimMovementForAbility();
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

    private TargetMotion updateTargetMotion(LivingEntity target, double gap, boolean hasLineOfSight) {
        double gapDelta = Double.isFinite(previousGap) ? gap - previousGap : 0.0D;
        previousGap = gap;

        Vec3 toTarget = target.position().subtract(dragon.position());
        Vec3 horizontalDirection = new Vec3(toTarget.x, 0.0D, toTarget.z);
        Vec3 targetVelocity = target.getDeltaMovement();
        Vec3 horizontalVelocity = new Vec3(targetVelocity.x, 0.0D, targetVelocity.z);
        double horizontalSpeed = horizontalVelocity.length();
        double awaySpeed = horizontalDirection.lengthSqr() > 1.0E-6D
                ? horizontalVelocity.dot(horizontalDirection.normalize())
                : 0.0D;
        boolean openingDistance = gapDelta > STABLE_GAP_DELTA || awaySpeed >= RETREATING_SPEED;
        boolean stable = hasLineOfSight
                && gapDelta <= STABLE_GAP_DELTA
                && horizontalSpeed <= TARGET_STABLE_SPEED;

        stableRangeTicks = stable
                ? Math.min(PRESSURE_STABLE_TICKS * 2, stableRangeTicks + 1)
                : 0;
        lastGap = gap;
        lastGapDelta = gapDelta;
        lastTargetAwaySpeed = awaySpeed;
        lastLineOfSight = hasLineOfSight;
        return new TargetMotion(gapDelta, awaySpeed, horizontalSpeed, openingDistance);
    }

    private void commitToChase(int ticks, String reason) {
        chaseCommitTicks = Math.max(chaseCommitTicks, ticks);
        commitment = CombatCommitment.CHASE;
        lastDecision = reason;
    }

    private void holdForCommittedAbility() {
        claimMovementForAbility();
    }

    private void releaseHeldAbilityWithoutTarget() {
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_POISON_BALL)) {
            dragon.requestPoisonBallRelease();
        }
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BREATH)) {
            dragon.forceEndActiveAbility();
        }
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BURROW)) {
            dragon.requestBurrowExit(false);
        }
    }

    private void claimMovementForAbility() {
        if (currentContext != null) {
            currentContext.memories().erase(DragonMemories.MOVEMENT_INTENT);
            currentContext.memories().erase(DragonMemories.WALK_TARGET);
            currentContext.memories().erase(DragonMemories.PATH);
        }
        dragon.getAIMovement().stop();
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("commitment", commitment.name().toLowerCase());
        details.put("decision", lastDecision);
        details.put("gap", String.format("%.2f", lastGap));
        details.put("gap_delta", String.format("%.3f", lastGapDelta));
        details.put("target_away_speed", String.format("%.3f", lastTargetAwaySpeed));
        details.put("stable_ticks", Integer.toString(stableRangeTicks));
        details.put("chase_commit_ticks", Integer.toString(chaseCommitTicks));
        details.put("pressure_cooldown", Integer.toString(pressureDecisionCooldown));
        details.put("line_of_sight", Boolean.toString(lastLineOfSight));
        return Map.copyOf(details);
    }

    private boolean isTargetAirborne(LivingEntity target) {
        return DragonTargetingHelper.isTargetAirborne(target, 8.0D) && !target.isInWaterOrBubble();
    }

    private enum CombatCommitment {
        CHASE,
        MELEE,
        PRESSURE,
        BURROW,
        ABILITY
    }

    private record TargetMotion(double gapDelta,
                                double awaySpeed,
                                double horizontalSpeed,
                                boolean openingDistance) {
    }
}
