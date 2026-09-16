package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.entity.ability.DragonCombatAim;
import com.leon.saintsdragons.server.ai.navigation.async.DragonFlightRequest;
import com.leon.saintsdragons.server.entity.component.VolitansBreathCombatComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AirCombatMovementBehaviour;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.Map;

public class VolitansAirCombatBehaviour extends AirCombatMovementBehaviour<Volitans> {
    private static final double MELEE_RANGE = 6.0D;
    private static final double POISON_MAX_RANGE = 32.0D;
    private static final double ROAR_MAX_RANGE = 12.0D;
    private static final double CHASE_HEIGHT_OFFSET = 2.0D;
    private static final double CHASE_SPEED = 2.3D;
    private static final double DIVE_CHASE_SPEED = 3.5D;
    private static final double DIVE_CHASE_MIN_HEIGHT_ADVANTAGE = 7.0D;
    private static final double DIVE_CHASE_MAX_HORIZONTAL_DISTANCE = 42.0D;
    private static final double POSITION_SPEED = 0.85D;
    private static final double BITE_APPROACH_DISTANCE = 3.5D;
    private static final int MELEE_CADENCE_TICKS = 30;

    private int attackCooldown;
    private int breathApproachTicks;
    private Vec3 takeoffTarget;
    private Volitans currentDragon;
    private String lastDecision = "idle";
    private int poisonHoldTicks;

    @Override
    protected boolean checkExtraStartConditions(Volitans dragon, LivingEntity target) {
        return canUseAirCombat(dragon);
    }

    @Override
    protected boolean checkExtraContinueConditions(Volitans dragon, LivingEntity target) {
        return canUseAirCombat(dragon);
    }

    @Override
    protected void tickAirCombat(DragonBrainContext<Volitans> context,
                                 LivingEntity target,
                                 boolean hasLineOfSight) {
        Volitans dragon = context.dragon();
        currentDragon = dragon;
        if (dragon.isTakeoff()) {
            tickTakeoff(context, target);
            return;
        }
        takeoffTarget = null;
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        double distance = dragon.distanceTo(target);
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_POISON_BALL)) {
            setAbilityApproachIntent(context, target, POSITION_SPEED * 0.75D);
            if (--poisonHoldTicks <= 0 || !hasLineOfSight || distance < 8.0D || distance > 36.0D) {
                dragon.requestPoisonBallRelease();
            }
            return;
        }

        if (dragon.isAbilityActive(ModAbilities.VOLITANS_BREATH)) {
            setBreathApproachIntent(context, target);
            breathApproachTicks = 0;
            lastDecision = "breath:moving-pass";
            return;
        }

        if (dragon.isAbilityActive(ModAbilities.VOLITANS_ROAR)) {
            setAbilityApproachIntent(context, target, POSITION_SPEED * 0.85D);
            return;
        }

        if (dragon.getBreathCombat().makingSpace()) {
            setBreathApproachIntent(context, target);
            lastDecision = "breath:making-space";
            return;
        }

        double gap = Math.max(0, distance - (dragon.getBbWidth() + target.getBbWidth()) * 0.5D);
        if (gap <= MELEE_RANGE && hasLineOfSight) {
            lastDecision = "melee:closing";
            if (attackCooldown <= 0 && dragon.getAiCombatPacing().getCadenceCooldownTicks() <= 0) {
                tryMelee(dragon, target);
            }
            setMeleePositionIntent(context, target, 0.0D, BITE_APPROACH_DISTANCE, 1.2D, 0.7D);
            return;
        }

        if (hasLineOfSight && dragon.getBreathCombat().tryStart(target)) {
            breathApproachTicks = 0;
            setBreathApproachIntent(context, target);
            lastDecision = "breath:starting";
            return;
        }
        if (hasLineOfSight && dragon.getBreathCombat().ready(target)
                && distance <= VolitansBreathCombatComponent.FIRING_RANGE + 8
                && dragon.getAiBreathShot(target) != DragonCombatAim.Shot.BLOCKED) {
            if (++breathApproachTicks <= 40) {
                setBreathApproachIntent(context, target);
                lastDecision = "breath:aligning-pass";
                return;
            }
        } else {
            breathApproachTicks = 0;
        }

        if (attackCooldown <= 0
                && distance > ROAR_MAX_RANGE
                && distance <= POISON_MAX_RANGE
                && hasLineOfSight
                && canUseAiAbility(dragon, ModAbilities.VOLITANS_POISON_BALL, true)
                && startAiAbility(dragon, ModAbilities.VOLITANS_POISON_BALL, true, 14, 120, 90, 36)) {
            poisonHoldTicks = 20 + dragon.getRandom().nextInt(8);
            setAbilityApproachIntent(context, target, POSITION_SPEED * 0.8D);
            return;
        }

        if (attackCooldown <= 0
                && distance <= ROAR_MAX_RANGE
                && hasLineOfSight
                && canUseAiAbility(dragon, ModAbilities.VOLITANS_ROAR, true)
                && startAiAbility(dragon, ModAbilities.VOLITANS_ROAR, true, 12, 140, 120, 48)) {
            setAbilityApproachIntent(context, target, POSITION_SPEED * 0.9D);
            return;
        }

        lastDecision = "chase";
        if (shouldDiveChase(
                dragon,
                target,
                DIVE_CHASE_MIN_HEIGHT_ADVANTAGE,
                DIVE_CHASE_MAX_HORIZONTAL_DISTANCE
        )) {
            setDivingChaseIntent(context, target, 3.0D, -0.25D, 0.08D, 0.12D, DIVE_CHASE_SPEED);
        } else {
            setPredictedChaseIntent(context, target, 4.0D,
                    dragon.getCombatFlightState().targetNeedsFlight() ? CHASE_HEIGHT_OFFSET : 8.0D,
                    0.0D, 0.0D, CHASE_SPEED);
        }
    }

    @Override
    protected void stopAirCombat(DragonBrainContext<Volitans> context) {
        Volitans dragon = context.dragon();
        dragon.setAiSpecialCombatReserved(false);
        attackCooldown = 0;
        breathApproachTicks = 0;
        takeoffTarget = null;
        lastDecision = "stopped";
        if (dragon.isAbilityActive(ModAbilities.VOLITANS_POISON_BALL)) {
            dragon.requestPoisonBallRelease();
        }
    }

    private void tryMelee(Volitans dragon, LivingEntity target) {
        if (DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)) {
            if (canUseAiAbility(dragon, ModAbilities.VOLITANS_BITE, false)) {
                startAiAbility(
                        dragon,
                        ModAbilities.VOLITANS_BITE,
                        false,
                        MELEE_CADENCE_TICKS,
                        MELEE_CADENCE_TICKS,
                        0,
                        24
                );
            }
            return;
        }

        float roll = dragon.getRandom().nextFloat();
        if (roll < 0.40F && canUseAiAbility(dragon, ModAbilities.VOLITANS_BITE, false)) {
            startMeleeAbility(dragon, ModAbilities.VOLITANS_BITE);
        } else if (roll < 0.72F && canUseAiAbility(dragon, ModAbilities.VOLITANS_CLAW, false)) {
            startMeleeAbility(dragon, ModAbilities.VOLITANS_CLAW);
        } else if (canUseAiAbility(dragon, ModAbilities.VOLITANS_HORN_GORE, false)) {
            startMeleeAbility(dragon, ModAbilities.VOLITANS_HORN_GORE);
        }
    }

    private void startMeleeAbility(Volitans dragon, DragonAbilityType<?, ?> abilityType) {
        startAiAbility(
                dragon,
                abilityType,
                false,
                MELEE_CADENCE_TICKS,
                MELEE_CADENCE_TICKS,
                0,
                24
        );
    }

    private void setAbilityApproachIntent(DragonBrainContext<Volitans> context,
                                          LivingEntity target,
                                          double speed) {
        setPredictedChaseIntent(context, target, 5.0D, CHASE_HEIGHT_OFFSET, 0.12D, 0.5D, speed);
    }

    private void setBreathApproachIntent(DragonBrainContext<Volitans> context, LivingEntity target) {
        Volitans dragon = context.dragon();
        Vec3 destination = dragon.getBreathCombat().movingDestination(target, true);
        var space = dragon.getAIMovement().flightSpace();
        double floor = dragon.level().getMinBuildHeight() + 4;
        double ceiling = dragon.level().getMaxBuildHeight() - dragon.getBbHeight() - 2;
        var local = space.observe(dragon.position());
        if (local != null && local.clear() && local.ceilingKnown()) {
            ceiling = Math.min(ceiling, local.ceiling() - dragon.getBbHeight() - 2);
        }
        var ahead = space.observe(new Vec3(destination.x, dragon.getY(), destination.z));
        if (ahead != null && ahead.clear() && ahead.ceilingKnown()) {
            ceiling = Math.min(ceiling, ahead.ceiling() - dragon.getBbHeight() - 2);
        }
        double y = dragon.getCombatFlightState().targetNeedsFlight() ? destination.y
                : DragonTargetingHelper.movementAnchor(target).getY() + 8;
        Vec3 preferred = new Vec3(destination.x, Mth.clamp(y, floor, Math.max(floor, ceiling)), destination.z);
        Vec3 fitted;
        boolean passing = dragon.isAbilityActive(ModAbilities.VOLITANS_BREATH) || dragon.getBreathCombat().makingSpace();
        var decisions = dragon.getCombatDecisionSupport();
        if (!passing && decisions != null) {
            double maximumY = ceiling;
            fitted = decisions.choosePosition("water-air-approach", target, preferred, dragon.getBreathOrigin(),
                    VolitansBreathCombatComponent.FIRING_RANGE, candidate -> {
                        Vec3 fit = space.fitDestination(candidate);
                        return fit != null && fit.y <= maximumY ? fit : null;
                    });
        } else {
            fitted = space.fitDestination(preferred);
        }
        if (fitted == null || fitted.y > ceiling) {
            context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.holdPosition());
            // Give the normal chase/landing planner the next opening if this firing lane is unusable.
            breathApproachTicks = 40;
            dragon.getCombatFlightState().deferRangedFlightFor(60);
            return;
        }
        double speed = passing ? Mth.clamp(0.42D + dragon.getCombatFlightState().targetVelocity().length(), 0.42D, 1.25D)
                / Math.max(0.01D, dragon.getFlightSpeed()) : POSITION_SPEED;
        context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.flight(
                DragonFlightRequest.maneuver(fitted, speed, 3,
                        passing ? DragonFlightRequest.Arrival.PASS_THROUGH : DragonFlightRequest.Arrival.BRAKE)));
    }

    private void tickTakeoff(DragonBrainContext<Volitans> context, LivingEntity target) {
        Volitans dragon = context.dragon();
        if (takeoffTarget == null) {
            Vec3 direction = target.position().subtract(dragon.position()).multiply(1, 0, 1).normalize();
            var space = dragon.getAIMovement().flightSpace();
            for (Vec3 candidate : new Vec3[] {
                    dragon.position().add(direction.scale(14)).add(0, 8, 0),
                    dragon.position().add(0, 8, 0) }) {
                Vec3 fitted = space.fitDestination(candidate);
                if (fitted != null && fitted.y >= dragon.getY() + 3 && space.corridorClear(dragon.position(), fitted)) {
                    takeoffTarget = fitted;
                    break;
                }
            }
        }
        context.memories().set(DragonMemories.MOVEMENT_INTENT, takeoffTarget == null
                ? DragonMovementIntent.holdPosition()
                : DragonMovementIntent.flight(DragonFlightRequest.track(takeoffTarget, POSITION_SPEED, 3)));
        lastDecision = "takeoff";
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>(super.getDragonBrainDebugDetails());
        details.put("decision", lastDecision);
        if (currentDragon != null) details.put("breath", currentDragon.getBreathCombat().debugSummary());
        return Map.copyOf(details);
    }

    private boolean canUseAirCombat(Volitans dragon) {
        return dragon.getAiAirCombatBlockReason() == null;
    }

    private boolean canUseAiAbility(Volitans dragon,
                                    DragonAbilityType<?, ?> abilityType,
                                    boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType)
                && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }

    private boolean startAiAbility(Volitans dragon,
                                   DragonAbilityType<?, ?> abilityType,
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
}
