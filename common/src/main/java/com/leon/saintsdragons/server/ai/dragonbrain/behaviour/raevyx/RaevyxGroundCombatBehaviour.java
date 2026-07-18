package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.DragonAirCombatHelper;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxBeamAbility;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public class RaevyxGroundCombatBehaviour extends DragonBehaviour<Raevyx> {
    public static final double BITE_ONLY_PREY_RANGE = 1.35D;
    public static final double GORE_RANGE = 4.5D;

    private static final double BITE_RANGE = 3.0D;
    private static final double GROUND_REND_RANGE = 8.5D;
    private static final double GROUND_REND_MIN_RANGE = 3.4D;
    private static final int BEAM_COOLDOWN_TICKS = 3600;
    private static final int GROUND_REND_COOLDOWN_TICKS = 400;
    private static final int MODE_REEVALUATE_TICKS = 6;
    private static final int DAMAGE_MEMORY_TICKS = 30;
    private static final double DASH_MIN_RANGE = 8.0D;
    private static final double DASH_MAX_RANGE = 26.0D;
    private static final float BUDGET_REGEN_PER_TICK = 0.025F;
    private static final float DASH_COST = 0.62F;
    private static final float BEAM_COST = 0.25F;
    private static final int MOBILITY_LOCK_TICKS = 24;
    private static final int BEAM_LOCK_TICKS = 30;

    private int attackCooldown;
    private int beamCooldown;
    private int groundRendCooldown;
    private int postRoarGroundRendTicks;
    private CombatMode combatMode = CombatMode.PRESSURE;
    private int modeReevaluateCooldown;
    private int recentDamageTicks;
    private long lastDamageGameTime = Long.MIN_VALUE;
    private int mobilityLockTicks;
    private int beamLockTicks;
    private float mobilityBudget = 1.0F;

    public RaevyxGroundCombatBehaviour() {
        super(Map.of(DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean canStart(DragonBrainContext<Raevyx> context) {
        return isGroundCombatContext(context);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Raevyx> context) {
        return isGroundCombatContext(context);
    }

    @Override
    protected void start(DragonBrainContext<Raevyx> context) {
        context.dragon().setAggressive(true);
        resetCombatPacing();
    }

    @Override
    protected void tick(DragonBrainContext<Raevyx> context) {
        tickCooldowns();

        Raevyx dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null) {
            return;
        }

        if (isMovementCommitted(dragon)) {
            claimStationaryMovement(context, movementCommitmentReason(dragon));
            return;
        }

        boolean hasLineOfSight = dragon.getSensing().hasLineOfSight(target);
        double gap = gapToTarget(dragon, target);
        if (dragon.isInWaterOrBubble()) {
            if (gap <= meleeStopRange(dragon, target) && hasLineOfSight && tryMeleeAttack(dragon, target)) {
                claimStationaryMovement(context, "water-melee");
            }
            return;
        }

        boolean biteOnlyPrey = DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target);
        boolean beamReady = beamCooldown <= 0;
        if (!biteOnlyPrey) {
            tickCombatPacing(dragon, target, gap, hasLineOfSight, beamReady);
        }

        if (!biteOnlyPrey && tryGroundRendPressure(dragon, gap, hasLineOfSight)) {
            claimStationaryMovement(context, "ground-rend-pressure");
            return;
        }
        if (!biteOnlyPrey && tryRoar(dragon, gap, hasLineOfSight)) {
            claimStationaryMovement(context, "roar");
            return;
        }
        if (!biteOnlyPrey && shouldTryDash(dragon, gap, isCurrentlyAttacking(dragon))
                && tryGroundDash(dragon, target)) {
            attackCooldown = Math.max(attackCooldown, 12);
            claimStationaryMovement(context, "dash-start");
            return;
        }
        if (!biteOnlyPrey && tryPostRoarGroundRend(dragon, gap, hasLineOfSight)) {
            claimStationaryMovement(context, "post-roar-ground-rend");
            return;
        }
        if (!biteOnlyPrey && tryDirectedBeam(dragon, target, hasLineOfSight, beamReady)) {
            claimStationaryMovement(context, "beam-start");
            return;
        }

        if (gap <= meleeStopRange(dragon, target)
                && attackCooldown <= 0
                && dragon.getAiCombatPacing().getCadenceCooldownTicks() <= 0
                && tryMeleeAttack(dragon, target)) {
            claimStationaryMovement(context, "melee-start");
        }
    }

    @Override
    protected void stop(DragonBrainContext<Raevyx> context) {
        if (!context.memories().has(DragonMemories.ATTACK_TARGET)) {
            context.dragon().setAggressive(false);
        }
        resetCombatPacing();
    }

    public static double meleeStopRange(Raevyx dragon, LivingEntity target) {
        return DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target) ? BITE_ONLY_PREY_RANGE : GORE_RANGE;
    }

    private void tickCooldowns() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (beamCooldown > 0) {
            beamCooldown--;
        }
        if (groundRendCooldown > 0) {
            groundRendCooldown--;
        }
        if (postRoarGroundRendTicks > 0) {
            postRoarGroundRendTicks--;
        }
    }

    private boolean tryMeleeAttack(Raevyx dragon, LivingEntity target) {
        if (attackCooldown > 0 || isCurrentlyAttacking(dragon) || !dragon.getSensing().hasLineOfSight(target)) {
            return false;
        }

        double gap = gapToTarget(dragon, target);
        if (DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)) {
            if (gap <= BITE_ONLY_PREY_RANGE
                    && canUseAiAbility(dragon, ModAbilities.RAEVYX_BITE, false)
                    && startAiAbility(dragon, ModAbilities.RAEVYX_BITE, false, 20, 20, 0, 18)) {
                attackCooldown = 20;
                return true;
            }
            return false;
        }

        if (gap <= GROUND_REND_RANGE && gap > GROUND_REND_MIN_RANGE && groundRendCooldown <= 0) {
            if (startGroundRend(dragon)) {
                attackCooldown = 26;
                groundRendCooldown = GROUND_REND_COOLDOWN_TICKS;
                return true;
            }
            return false;
        }
        if (gap <= BITE_RANGE
                && canUseAiAbility(dragon, ModAbilities.RAEVYX_BITE, false)
                && startAiAbility(dragon, ModAbilities.RAEVYX_BITE, false, 20, 20, 0, 18)) {
            attackCooldown = 20;
            return true;
        }
        if (gap <= GORE_RANGE
                && canUseAiAbility(dragon, ModAbilities.RAEVYX_HORN_GORE, false)
                && startAiAbility(dragon, ModAbilities.RAEVYX_HORN_GORE, false, 20, 22, 0, 22)) {
            attackCooldown = 20;
            return true;
        }
        return false;
    }

    private boolean tryDirectedBeam(Raevyx dragon,
                                    LivingEntity target,
                                    boolean hasLineOfSight,
                                    boolean beamReady) {
        if (RaevyxBeamAbility.isAtAiBeamMercyThreshold(target)
                || !shouldTryBeam(dragon, gapToTarget(dragon, target), hasLineOfSight, beamReady)
                || !canUseAiAbility(dragon, ModAbilities.RAEVYX_LIGHTNING_BEAM, true)
                || !startAiAbility(dragon, ModAbilities.RAEVYX_LIGHTNING_BEAM, true, 60, BEAM_COOLDOWN_TICKS, 160, 80)) {
            return false;
        }
        attackCooldown = 60;
        beamCooldown = BEAM_COOLDOWN_TICKS;
        return true;
    }

    private boolean tryRoar(Raevyx dragon, double gap, boolean hasLineOfSight) {
        if (attackCooldown > 0
                || isCurrentlyAttacking(dragon)
                || !hasLineOfSight
                || gap > 18.0D
                || dragon.getRandom().nextFloat() >= 0.045F
                || !canUseAiAbility(dragon, ModAbilities.RAEVYX_ROAR, true)
                || !startAiAbility(dragon, ModAbilities.RAEVYX_ROAR, true, 24, 70, 80, 32)) {
            return false;
        }
        attackCooldown = 24;
        postRoarGroundRendTicks = 40;
        return true;
    }

    private boolean tryGroundDash(Raevyx dragon, LivingEntity target) {
        if (dragon.isAerial() || dragon.isInWaterOrBubble() || dragon.isDashing() || dragon.isDodging()) {
            return false;
        }

        double dx = target.getX() - dragon.getX();
        double dz = target.getZ() - dragon.getZ();
        if (dx * dx + dz * dz > 1.0E-4D) {
            float targetYaw = (float)(Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            dragon.setYRot(targetYaw);
            dragon.yBodyRot = targetYaw;
        }
        if (!dragon.beginForwardDashMotion(27, 50, 30.0D)) {
            return false;
        }
        dragon.triggerDashFeedback();
        return true;
    }

    private boolean tryPostRoarGroundRend(Raevyx dragon, double gap, boolean hasLineOfSight) {
        if (postRoarGroundRendTicks <= 0
                || attackCooldown > 0
                || isCurrentlyAttacking(dragon)
                || groundRendCooldown > 0
                || !hasLineOfSight
                || gap > GROUND_REND_RANGE
                || gap < GROUND_REND_MIN_RANGE
                || !startGroundRend(dragon)) {
            return false;
        }
        attackCooldown = 26;
        groundRendCooldown = GROUND_REND_COOLDOWN_TICKS;
        postRoarGroundRendTicks = 0;
        return true;
    }

    private boolean tryGroundRendPressure(Raevyx dragon, double gap, boolean hasLineOfSight) {
        if (attackCooldown > 0
                || isCurrentlyAttacking(dragon)
                || groundRendCooldown > 0
                || !hasLineOfSight
                || gap < GROUND_REND_MIN_RANGE
                || gap > GROUND_REND_RANGE
                || !startGroundRend(dragon)) {
            return false;
        }
        attackCooldown = 26;
        groundRendCooldown = GROUND_REND_COOLDOWN_TICKS;
        return true;
    }

    private boolean startGroundRend(Raevyx dragon) {
        return canUseAiAbility(dragon, ModAbilities.RAEVYX_GROUND_REND, true)
                && startAiAbility(
                dragon,
                ModAbilities.RAEVYX_GROUND_REND,
                true,
                26,
                GROUND_REND_COOLDOWN_TICKS,
                100,
                34
        ) && dragon.isAbilityActive(ModAbilities.RAEVYX_GROUND_REND);
    }

    private void resetCombatPacing() {
        combatMode = CombatMode.PRESSURE;
        modeReevaluateCooldown = 0;
        recentDamageTicks = 0;
        lastDamageGameTime = Long.MIN_VALUE;
        mobilityLockTicks = 0;
        beamLockTicks = 0;
        mobilityBudget = 1.0F;
    }

    private void tickCombatPacing(Raevyx dragon,
                                  LivingEntity target,
                                  double gap,
                                  boolean hasLineOfSight,
                                  boolean beamReady) {
        if (mobilityLockTicks > 0) {
            mobilityLockTicks--;
        }
        if (beamLockTicks > 0) {
            beamLockTicks--;
        }
        if (recentDamageTicks > 0) {
            recentDamageTicks--;
        }
        mobilityBudget = Math.min(1.0F, mobilityBudget + BUDGET_REGEN_PER_TICK);

        long gameTime = dragon.level().getGameTime();
        if (dragon.hurtTime > 0 && gameTime != lastDamageGameTime) {
            lastDamageGameTime = gameTime;
            recentDamageTicks = DAMAGE_MEMORY_TICKS;
        }
        if (modeReevaluateCooldown > 0) {
            modeReevaluateCooldown--;
            return;
        }
        modeReevaluateCooldown = MODE_REEVALUATE_TICKS;
        combatMode = decideMode(target, gap, hasLineOfSight, beamReady);
    }

    private boolean shouldTryDash(Raevyx dragon, double gap, boolean currentlyAttacking) {
        if (currentlyAttacking
                || dragon.isBeaming()
                || dragon.isDodging()
                || dragon.isDashing()
                || mobilityLockTicks > 0
                || gap < DASH_MIN_RANGE
                || gap > DASH_MAX_RANGE
                || mobilityBudget < DASH_COST) {
            return false;
        }

        float chance = switch (combatMode) {
            case SPACE -> 0.20F;
            case PRESSURE -> 0.14F;
            default -> 0.06F;
        };
        if (recentDamageTicks > 0) {
            chance *= 0.75F;
        }
        if (dragon.getRandom().nextFloat() >= chance) {
            return false;
        }
        mobilityBudget = Math.max(0.0F, mobilityBudget - DASH_COST);
        mobilityLockTicks = MOBILITY_LOCK_TICKS;
        beamLockTicks = Math.max(beamLockTicks, 12);
        return true;
    }

    private boolean shouldTryBeam(Raevyx dragon,
                                  double gap,
                                  boolean hasLineOfSight,
                                  boolean beamReady) {
        if (isCurrentlyAttacking(dragon)
                || attackCooldown > 0
                || !beamReady
                || !hasLineOfSight
                || beamLockTicks > 0
                || mobilityBudget < BEAM_COST) {
            return false;
        }

        float chance = switch (combatMode) {
            case PRESSURE -> 0.20F;
            case SPACE -> 0.10F;
            default -> 0.04F;
        };
        if (gap < 8.0D || gap > 32.0D) {
            chance *= 0.45F;
        }
        if (recentDamageTicks > 0) {
            chance *= 0.8F;
        }
        if (dragon.getRandom().nextFloat() >= chance) {
            return false;
        }
        mobilityBudget = Math.max(0.0F, mobilityBudget - BEAM_COST);
        beamLockTicks = BEAM_LOCK_TICKS;
        return true;
    }

    private CombatMode decideMode(LivingEntity target,
                                  double gap,
                                  boolean hasLineOfSight,
                                  boolean beamReady) {
        if (recentDamageTicks > 0 && gap <= 14.0D) {
            return CombatMode.EVADE;
        }
        if (gap <= 5.0D) {
            return CombatMode.REPOSITION;
        }
        if (gap > 12.0D) {
            return CombatMode.SPACE;
        }
        if (hasLineOfSight && beamReady && gap >= 8.0D && gap <= 28.0D) {
            return CombatMode.PRESSURE;
        }
        if (isLikelyMeleeThreat(target, gap)) {
            return CombatMode.EVADE;
        }
        return CombatMode.PRESSURE;
    }

    private boolean isLikelyMeleeThreat(LivingEntity target, double gap) {
        if (gap > 6.0D) {
            return false;
        }
        if (target instanceof Player player) {
            return player.getAttackStrengthScale(0.0F) > 0.85F;
        }
        return target.swingTime > 0;
    }

    private boolean isGroundCombatContext(DragonBrainContext<Raevyx> context) {
        Raevyx dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null
                || context.memories().get(DragonMemories.GROUND_ROUTE_ABANDONED).orElse(false)
                || !DragonAirCombatHelper.isValidCombatTarget(dragon, target)
                || dragon.isVehicle()
                || dragon.isPassenger()
                || dragon.isOrderedToSit()
                || dragon.isAerial()
                || context.memories().get(DragonMemories.TARGET_AIRBORNE).orElse(false)
                || dragon.distanceToSqr(target) > DragonAirCombatHelper.maxAggroDistanceSqr(dragon, 32.0D)) {
            return false;
        }
        DragonLocomotionMode mode = context.memories()
                .get(DragonMemories.LOCOMOTION_MODE)
                .orElse(dragon.getLocomotionMode());
        return mode == DragonLocomotionMode.GROUND || mode == DragonLocomotionMode.WATER;
    }

    private boolean isMovementCommitted(Raevyx dragon) {
        return dragon.isGroundRending()
                || dragon.isDashing()
                || dragon.isDodging()
                || isCurrentlyAttacking(dragon);
    }

    private boolean isCurrentlyAttacking(Raevyx dragon) {
        return dragon.isAbilityActive(ModAbilities.RAEVYX_BITE)
                || dragon.isAbilityActive(ModAbilities.RAEVYX_HORN_GORE)
                || dragon.isAbilityActive(ModAbilities.RAEVYX_GROUND_REND)
                || dragon.isAbilityActive(ModAbilities.RAEVYX_LIGHTNING_BEAM)
                || dragon.isAbilityActive(ModAbilities.RAEVYX_ROAR);
    }

    private double gapToTarget(Raevyx dragon, LivingEntity target) {
        double combinedRadii = (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        return Math.max(0.0D, dragon.distanceTo(target) - combinedRadii);
    }

    private String movementCommitmentReason(Raevyx dragon) {
        if (dragon.isGroundRending()) {
            return "committed:ground-rend";
        }
        if (dragon.isDashing()) {
            return "committed:dash";
        }
        if (dragon.isDodging()) {
            return "committed:dodge";
        }
        if (dragon.getActiveAbility() != null) {
            return "committed:ability=" + dragon.getActiveAbility().getClass().getSimpleName();
        }
        return "committed:unknown";
    }

    private void claimStationaryMovement(DragonBrainContext<Raevyx> context, String reason) {
        context.memories().set(DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.stop("raevyx-combat:" + reason));
    }

    private boolean canUseAiAbility(Raevyx dragon,
                                    DragonAbilityType<?, ?> abilityType,
                                    boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType)
                && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }

    private boolean startAiAbility(Raevyx dragon,
                                   DragonAbilityType<?, ?> abilityType,
                                   boolean majorAbility,
                                   int cadenceTicks,
                                   int abilityCooldownTicks,
                                   int majorCooldownTicks,
                                   int repeatLockoutTicks) {
        return dragon.combatManager.tryUseAiAbility(
                abilityType,
                majorAbility,
                cadenceTicks,
                abilityCooldownTicks,
                majorCooldownTicks,
                repeatLockoutTicks
        );
    }

    private enum CombatMode {
        PRESSURE,
        SPACE,
        EVADE,
        REPOSITION
    }
}
