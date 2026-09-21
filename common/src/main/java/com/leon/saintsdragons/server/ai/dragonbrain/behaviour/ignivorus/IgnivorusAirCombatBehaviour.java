package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus;

import com.leon.saintsdragons.common.config.dragon.profile.IgnivorusStatProfile;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.common.particle.ExpandingBreathSection;
import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonCombatDecisionSupport;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.dragonbrain.learning.DragonCombatLearning;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AirCombatMovementBehaviour;
import com.leon.saintsdragons.server.ai.navigation.async.DragonFlightRequest;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonCombatAim;
import com.leon.saintsdragons.server.entity.ability.DragonAimHelper;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireBreathAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireballAbility;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class IgnivorusAirCombatBehaviour extends AirCombatMovementBehaviour<Ignivorus> {
    private static final double CHASE_SPEED = IgnivorusStatProfile.AirCombatBehaviour.CHASE_SPEED;
    private static final double DIVE_SPEED = IgnivorusStatProfile.AirCombatBehaviour.DIVE_SPEED;
    private static final double APPROACH_SPEED = IgnivorusStatProfile.AirCombatBehaviour.APPROACH_SPEED;
    private static final double BREATH_PASS_SPEED = IgnivorusStatProfile.AirCombatBehaviour.BREATH_PASS_SPEED;
    private static final double EGRESS_SPEED = IgnivorusStatProfile.AirCombatBehaviour.EGRESS_SPEED;
    private static final double FIRING_RANGE = ExpandingBreathSection.DEFAULT_RANGE * 0.65D;
    private static final int APPROACH_TIMEOUT = 80;

    private AirPhase phase = AirPhase.CHASE;
    private int phaseTicks;
    private int attackSide = 1;
    private long flightHandoff;
    private long nextAttackDecision;
    private long nextApproach;
    private long routeIssuedAt;
    private long nextFireballRoute;
    private String lastDecision = "idle";
    @Nullable private Vec3 routeTarget;
    @Nullable private Vec3 approachAnchor;
    @Nullable private Vec3 runDirection;
    @Nullable private Vec3 takeoffTarget;

    @Override
    protected void startAirCombat(DragonBrainContext<Ignivorus> context) {
        flightHandoff = context.dragon().getCombatFlightState().handoffRevision();
        phase = AirPhase.CHASE;
        phaseTicks = 0;
        attackSide = context.dragon().getRandom().nextBoolean() ? 1 : -1;
        clearRouteState();
        lastDecision = "chase:engage";
    }

    @Override
    protected boolean checkExtraStartConditions(Ignivorus dragon, LivingEntity target) {
        return dragon.getAiAirCombatBlockReason() == null && !dragon.isTamingStunned();
    }

    @Override
    protected boolean checkExtraContinueConditions(Ignivorus dragon, LivingEntity target) {
        return checkExtraStartConditions(dragon, target);
    }

    @Override
    protected void tickAirCombat(DragonBrainContext<Ignivorus> context,
                                 LivingEntity target, boolean hasLineOfSight) {
        Ignivorus dragon = context.dragon();
        if (flightHandoff != dragon.getCombatFlightState().handoffRevision()) startAirCombat(context);
        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (dragon.isTakeoff()) {
            tickTakeoff(context, target);
            return;
        }
        if (takeoffTarget != null) {
            clearRouteState();
            phase = AirPhase.CHASE;
            phaseTicks = 0;
        }
        if (trySkyfall(context, target, hasLineOfSight)) return;
        phaseTicks++;
        if (dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIRE_BREATH)) {
            if (phase != AirPhase.BREATH_PASS) startBreathPass(context, target);
            tickBreathPass(context, target);
            return;
        }
        if (dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIREBALL)) {
            tickFireballPass(context, target, hasLineOfSight);
            return;
        }
        if (phase == AirPhase.BREATH_PASS || phase == AirPhase.FIREBALL_PASS) {
            enterEgress(context, target, "ranged-complete");
            return;
        }
        if (dragon.getActiveAbility() != null && !dragon.isAbilityActive(ModAbilities.IGNIVORUS_BITE)) {
            context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.holdPosition());
            lastDecision = "hold:active-ability";
            return;
        }
        switch (phase) {
            case CHASE -> tickChase(context, target, hasLineOfSight);
            case APPROACH -> tickApproach(context, target, hasLineOfSight);
            case BITE_PASS -> tickBitePass(context, target, hasLineOfSight);
            case EGRESS -> tickEgress(context, target, hasLineOfSight);
            default -> { }
        }
    }

    private void tickTakeoff(DragonBrainContext<Ignivorus> context, LivingEntity target) {
        Ignivorus dragon = context.dragon();
        if (takeoffTarget == null) {
            Vec3 forward = horizontal(targetCenter(target).subtract(dragon.position()), dragon);
            var space = dragon.getAIMovement().flightSpace();
            for (Vec3 candidate : new Vec3[] {
                    dragon.position().add(forward.scale(16.0D)).add(0, 10.0D, 0),
                    dragon.position().add(0, 10.0D, 0) }) {
                Vec3 fitted = space.fitDestination(candidate);
                if (fitted != null && fitted.y >= dragon.getY() + 4.0D
                        && space.corridorClear(dragon.position(), fitted)) {
                    takeoffTarget = fitted;
                    break;
                }
            }
        }
        issueRoute(context, takeoffTarget, APPROACH_SPEED, DragonFlightRequest.Arrival.BRAKE);
        lastDecision = takeoffTarget == null ? "takeoff:no-clear-lane" : "takeoff:forward-lift";
    }

    private boolean trySkyfall(DragonBrainContext<Ignivorus> context, LivingEntity target, boolean visible) {
        Ignivorus dragon = context.dragon();
        if (!visible || DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)
                || !dragon.shouldTriggerWildUltimateAtCurrentHealth()
                || !canUse(dragon, ModAbilities.IGNIVORUS_ULTIMATE, true)
                || !dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_ULTIMATE)) return false;
        dragon.getAiCombatPacing().recordUse(ModAbilities.IGNIVORUS_ULTIMATE, 20, 140, true, 180, 100);
        context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.holdPosition());
        lastDecision = "skyfall:low-health";
        return true;
    }

    private void tickChase(DragonBrainContext<Ignivorus> context, LivingEntity target, boolean visible) {
        Ignivorus dragon = context.dragon();
        double gap = bodyGap(dragon, target);
        if (visible && dragon.shouldFavorRangedCombat(target) && gap < 10.0D + dragon.getAiBreathSpacingBonus(target)
                && dragon.level().getGameTime() >= nextApproach && rangedReady(dragon, target)) {
            nextApproach = dragon.level().getGameTime() + 20;
            enterEgress(context, target, "make-firing-space");
            return;
        }
        if (visible && gap <= 7.0D) {
            phase = AirPhase.BITE_PASS;
            phaseTicks = 0;
            runDirection = horizontal(targetCenter(target).subtract(dragon.getBoundingBox().getCenter()), dragon);
            issueRoute(context, attackPosition(dragon, target,
                    predictedFeet(dragon, target, 4).add(runDirection.scale(14)), 5),
                    EGRESS_SPEED, DragonFlightRequest.Arrival.PASS_THROUGH);
            tickBitePass(context, target, true);
            return;
        }
        if (visible && dragon.level().getGameTime() >= nextApproach
                && gap >= 10.0D && dragon.distanceTo(target) <= FIRING_RANGE && rangedReady(dragon, target)) {
            enterApproach(context, target);
            return;
        }
        Vec3 destination = attackPosition(dragon, target, predictedFeet(dragon, target, 5),
                rangedReady(dragon, target) ? 14 : 6);
        if (destination == null) {
            issueRoute(context, null, CHASE_SPEED, DragonFlightRequest.Arrival.PASS_THROUGH);
            lastDecision = "chase:no-clear-destination";
            return;
        }
        boolean dive = dragon.getCombatFlightState().targetNeedsFlight()
                && shouldDiveChase(dragon, target, 10, 48);
        if (dive) {
            var clearance = dragon.getAIMovement().flightSpace().observe(destination);
            if (clearance != null && clearance.floorKnown()) {
                destination = new Vec3(destination.x, Math.max(destination.y, clearance.floor() + 6), destination.z);
            }
            dive = destination.y < dragon.getY() - 2;
        }
        context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.flight(dive
                ? DragonFlightRequest.dive(destination, DIVE_SPEED)
                : DragonFlightRequest.chase(destination, CHASE_SPEED)));
        routeTarget = destination;
        lastDecision = dive ? "chase:descending-intercept" : "chase:match-target-altitude";
    }

    private void enterApproach(DragonBrainContext<Ignivorus> context, LivingEntity target) {
        Ignivorus dragon = context.dragon();
        Vec3 radial = horizontal(dragon.position().subtract(targetCenter(target)), dragon);
        Vec3 tangent = new Vec3(-radial.z, 0, radial.x).scale(attackSide);
        var expectation = dragon.getCombatLearning().expectation(target, DragonCombatLearning.Attack.BREATH, true);
        double learnedSide = switch (expectation.response()) {
            case LEFT -> 4.0D * expectation.confidence();
            case RIGHT -> -4.0D * expectation.confidence();
            default -> 0.0D;
        };
        Vec3 attackForward = radial.scale(-1);
        Vec3 learnedOffset = new Vec3(-attackForward.z, 0, attackForward.x).scale(learnedSide);
        Vec3 preferred = predictedFeet(dragon, target, 4)
                .add(radial.scale(30 + dragon.getBbWidth() * 0.5D + dragon.getAiBreathSpacingBonus(target)))
                .add(tangent.scale(12)).add(learnedOffset);
        var decisions = dragon.getCombatDecisionSupport();
        Vec3 destination = decisions == null ? attackPosition(dragon, target, preferred, 14)
                : decisions.choosePosition("fire-approach", target, preferred, dragon.getFireBreathStartAnchor(1.0F), FIRING_RANGE,
                candidate -> attackPosition(dragon, target, candidate, 14));
        if (destination == null) {
            deferApproach(dragon);
            enterEgress(context, target, "no-firing-space");
            return;
        }
        phase = AirPhase.APPROACH;
        phaseTicks = 0;
        approachAnchor = targetCenter(target);
        runDirection = null;
        routeTarget = null;
        issueRoute(context, destination, APPROACH_SPEED, DragonFlightRequest.Arrival.BRAKE);
        lastDecision = "approach:broad-firing-angle";
    }

    private void tickApproach(DragonBrainContext<Ignivorus> context, LivingEntity target, boolean visible) {
        Ignivorus dragon = context.dragon();
        if (phaseTicks >= APPROACH_TIMEOUT || routeFailed(dragon) || !visible
                || !rangedReady(dragon, target) || bodyGap(dragon, target) < 8
                || approachAnchor == null || targetCenter(target).distanceToSqr(approachAnchor) > 400) {
            var decisions = dragon.getCombatDecisionSupport();
            if (decisions != null && visible && approachAnchor != null
                    && targetCenter(target).distanceToSqr(approachAnchor) <= 144) {
                if (routeFailed(dragon)) decisions.fail(DragonCombatDecisionSupport.Failure.ROUTE_FAILED, routeTarget);
                else if (phaseTicks >= APPROACH_TIMEOUT) decisions.failSetup(routeTarget);
            }
            deferApproach(dragon);
            enterEgress(context, target, "approach-aborted");
            return;
        }
        DragonCombatAim.Shot shot = dragon.getAiFireBreathShot(target, FIRING_RANGE);
        long now = dragon.level().getGameTime();
        if (now >= nextAttackDecision) {
            nextAttackDecision = now + 16;
            boolean breathReady = dragon.isAiAirBreathReady() && shot == DragonCombatAim.Shot.ALIGNED
                    && IgnivorusFireBreathAbility.canStartAiBreath(dragon, target);
            boolean favorRanged = dragon.shouldFavorRangedCombat(target);
            boolean fireballReady = canUseAirFireball(dragon, target)
                    && dragon.hasAiFireballShot(target, 64);
            double fireballWeight = dragon.getCombatLearning()
                    .expectation(target, DragonCombatLearning.Attack.PROJECTILE, true).attackWeight();
            double breathWeight = dragon.getCombatLearning()
                    .expectation(target, DragonCombatLearning.Attack.BREATH, true).attackWeight();
            double baseChance = favorRanged ? 0.50D : 0.35D;
            double fireballChance = baseChance * fireballWeight
                    / (baseChance * fireballWeight + (1.0D - baseChance) * breathWeight);
            if (fireballReady && (!breathReady || dragon.getRandom().nextFloat() < fireballChance)
                    && dragon.combatManager.tryUseAiAbility(ModAbilities.IGNIVORUS_FIREBALL,
                    true, 12, favorRanged ? 240 : 400, 60, 80)) {
                phase = AirPhase.FIREBALL_PASS;
                phaseTicks = 0;
                nextFireballRoute = 0;
                dragon.getCombatFlightState().holdFlightFor(60);
                lastDecision = favorRanged ? "fireball:ground-target-opening" : "fireball:phase2-opening";
                return;
            }
            if (breathReady && dragon.combatManager.tryUseAiAbility(ModAbilities.IGNIVORUS_FIRE_BREATH,
                    true, 12, 0, 30, 0)) {
                startBreathPass(context, target);
                return;
            }
        }
        Vec3 correction = routeTarget.subtract(dragon.position());
        Vec3 towardTarget = targetCenter(target).subtract(dragon.getBoundingBox().getCenter());
        double speed = correction.lengthSqr() > 1 && correction.normalize().dot(towardTarget.normalize()) < 0.6D
                ? 0.4D / Math.max(0.01D, dragon.getFlightSpeed()) : APPROACH_SPEED;
        context.memories().set(DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.flight(DragonFlightRequest.track(routeTarget, speed, 3)));
        lastDecision = "approach:" + shot.name().toLowerCase();
    }

    private void startBreathPass(DragonBrainContext<Ignivorus> context, LivingEntity target) {
        Ignivorus dragon = context.dragon();
        phase = AirPhase.BREATH_PASS;
        phaseTicks = 0;
        dragon.getCombatFlightState().holdFlightFor(40);
        Vec3 forward = horizontal(targetCenter(target).subtract(dragon.getBoundingBox().getCenter()), dragon);
        Vec3 tangent = new Vec3(-forward.z, 0, forward.x).scale(attackSide);
        runDirection = forward.add(tangent.scale(0.30D)).normalize();
        double length = Mth.clamp(dragon.distanceTo(target) + 24.0D, 48.0D, 72.0D);
        Vec3 destination = dragon.position().add(runDirection.scale(length));
        destination = new Vec3(destination.x, predictedFeet(dragon, target, 2).y, destination.z);
        issueRoute(context, attackPosition(dragon, target, destination, 12),
                BREATH_PASS_SPEED, DragonFlightRequest.Arrival.PASS_THROUGH);
        lastDecision = "breath:committed-pass";
    }

    private void tickFireballPass(DragonBrainContext<Ignivorus> context, LivingEntity target, boolean visible) {
        Ignivorus dragon = context.dragon();
        DragonAbility<?> active = dragon.getActiveAbility();
        if (!(active instanceof IgnivorusFireballAbility fireball)) return;
        if (phase != AirPhase.FIREBALL_PASS) {
            phase = AirPhase.FIREBALL_PASS;
            phaseTicks = 0;
            nextFireballRoute = 0;
        }
        DragonCombatAim.Shot shot = fireball.updateAiAim();
        dragon.getCombatFlightState().holdFlightFor(20);
        if (!visible) {
            lastDecision = "fireball:hold-last-course";
            return;
        }
        long now = dragon.level().getGameTime();
        if (now >= nextFireballRoute) {
            nextFireballRoute = now + 10;
            Vec3 toward = horizontal(targetCenter(target).subtract(dragon.position()), dragon);
            Vec3 heading = horizontal(dragon.getDeltaMovement().horizontalDistanceSqr() > 0.04D
                    ? dragon.getDeltaMovement() : Vec3.directionFromRotation(0, dragon.yBodyRot), dragon);
            boolean turnAround = heading.dot(toward) < 0;
            Vec3 destination = turnAround
                    ? dragon.position().add(DragonAimHelper.turnDirection(heading, toward, 45).scale(22))
                    : dragon.getCombatAim().firingApproach(target, APPROACH_SPEED,
                            28 + dragon.getBbWidth() * 0.5D, attackSide * 6).target();
            var decisions = dragon.getCombatDecisionSupport();
            destination = decisions == null || turnAround ? attackPosition(dragon, target, destination, 14)
                    : decisions.choosePosition("fireball-approach", target, destination, dragon.getFireBreathStartAnchor(1.0F), FIRING_RANGE,
                    candidate -> attackPosition(dragon, target, candidate, 14));
            if (destination == null) {
                fireball.cancelAiCast("no-alignment-space");
                enterEgress(context, target, "fireball:no-alignment-space");
                return;
            }
            issueRoute(context, destination, APPROACH_SPEED, DragonFlightRequest.Arrival.BRAKE);
        }
        if (routeFailed(dragon)) {
            fireball.cancelAiCast("alignment-route-blocked");
            enterEgress(context, target, "fireball:alignment-route-blocked");
            return;
        }
        double speed = Math.min(APPROACH_SPEED,
                (shot.needsAlignment() ? 0.4D : 0.65D) / Math.max(0.01D, dragon.getFlightSpeed()));
        context.memories().set(DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.flight(DragonFlightRequest.track(routeTarget, speed, 3)));
        lastDecision = "fireball:" + shot.reason();
    }

    private void tickBreathPass(DragonBrainContext<Ignivorus> context, LivingEntity target) {
        Ignivorus dragon = context.dragon();
        Vec3 offset = targetCenter(target).subtract(dragon.getBoundingBox().getCenter());
        double horizontalGap = offset.horizontalDistance() - (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        String stop = routeFailed(dragon) ? "blocked-pass"
                : horizontalGap < 3 && offset.y < -3 ? "target-underneath"
                : runDirection != null && offset.dot(runDirection) < -6 ? "passed-target"
                : reachedRoute(dragon) || phaseTicks >= 170 ? "pass-complete" : null;
        if (stop != null) {
            DragonAbility<?> active = dragon.getActiveAbility();
            if (active instanceof IgnivorusFireBreathAbility breath) breath.finishAiPass(stop);
            enterEgress(context, target, stop);
        } else {
            lastDecision = "breath:committed-pass";
        }
    }

    private void tickBitePass(DragonBrainContext<Ignivorus> context, LivingEntity target, boolean visible) {
        Ignivorus dragon = context.dragon();
        Vec3 toTarget = targetCenter(target).subtract(dragon.getBoundingBox().getCenter()).normalize();
        if (visible && bodyGap(dragon, target) <= 7 && dragon.getLookAngle().dot(toTarget) > 0.1D
                && canUse(dragon, ModAbilities.IGNIVORUS_BITE, false)) {
            dragon.combatManager.tryUseAiAbility(ModAbilities.IGNIVORUS_BITE, false, 30, 30, 0, 24);
        }
        if (routeFailed(dragon) || reachedRoute(dragon) || phaseTicks >= 32) {
            enterEgress(context, target, "bite-pass-complete");
        } else {
            lastDecision = "bite:forward-pass";
        }
    }

    private void enterEgress(DragonBrainContext<Ignivorus> context, LivingEntity target, String reason) {
        Ignivorus dragon = context.dragon();
        dragon.getCombatAim().clear();
        phase = AirPhase.EGRESS;
        phaseTicks = 0;
        Vec3 heading = runDirection != null ? runDirection
                : horizontal(dragon.getDeltaMovement().horizontalDistanceSqr() > 0.04D
                ? dragon.getDeltaMovement() : dragon.getLookAngle(), dragon);
        Vec3 tangent = new Vec3(-heading.z, 0, heading.x).scale(attackSide);
        Vec3 destination = dragon.position().add(heading.scale(28)).add(tangent.scale(8));
        if (dragon.getCombatFlightState().targetNeedsFlight()) {
            double targetY = predictedFeet(dragon, target, 0).y;
            destination = new Vec3(destination.x, targetY + Mth.clamp(dragon.getY() - targetY, -3, 3), destination.z);
        }
        dragon.getCombatFlightState().holdFlightFor(20);
        issueRoute(context, attackPosition(dragon, target, destination, 12),
                EGRESS_SPEED, DragonFlightRequest.Arrival.PASS_THROUGH);
        lastDecision = "egress:" + reason;
    }

    private void tickEgress(DragonBrainContext<Ignivorus> context, LivingEntity target, boolean visible) {
        Ignivorus dragon = context.dragon();
        if (routeFailed(dragon) || phaseTicks >= 55 || phaseTicks >= 16 && reachedRoute(dragon)
                || dragon.distanceTo(target) > FIRING_RANGE + 12) {
            phase = AirPhase.CHASE;
            phaseTicks = 0;
            clearRouteState();
            attackSide = -attackSide;
            tickChase(context, target, visible);
        } else {
            lastDecision = "egress:wide-turn";
        }
    }

    private void deferApproach(Ignivorus dragon) {
        nextApproach = dragon.level().getGameTime() + 120;
        dragon.getCombatFlightState().deferRangedFlightFor(120);
    }

    private boolean rangedReady(Ignivorus dragon, LivingEntity target) {
        return !DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)
                && (dragon.isAiAirBreathReady()
                || canUseAirFireball(dragon, target));
    }

    private boolean canUseAirFireball(Ignivorus dragon, LivingEntity target) {
        return (dragon.isPhase2Active() || dragon.shouldFavorRangedCombat(target))
                && canUse(dragon, ModAbilities.IGNIVORUS_FIREBALL, true);
    }

    private boolean canUse(Ignivorus dragon, DragonAbilityType<?, ?> ability, boolean major) {
        return !dragon.isTakeoff() && !dragon.isLanding()
                && dragon.combatManager.canStart(ability) && dragon.getAiCombatPacing().canUse(ability, major);
    }

    private void issueRoute(DragonBrainContext<Ignivorus> context, @Nullable Vec3 destination,
                            double speed, DragonFlightRequest.Arrival arrival) {
        if (destination == null) {
            routeTarget = null;
            context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.holdPosition());
            return;
        }
        if (routeTarget == null || routeTarget.distanceToSqr(destination) > 0.25D) {
            routeIssuedAt = context.dragon().level().getGameTime();
        }
        routeTarget = destination;
        context.memories().set(DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.flight(DragonFlightRequest.maneuver(destination, speed, 3, arrival)));
    }

    private boolean routeFailed(Ignivorus dragon) {
        return routeTarget == null || dragon.level().getGameTime() - routeIssuedAt > 3
                && dragon.getAIMovement().hasFailed();
    }

    private boolean reachedRoute(Ignivorus dragon) {
        return routeTarget != null && dragon.position().distanceToSqr(routeTarget) <= 16;
    }

    private Vec3 targetCenter(LivingEntity target) {
        return DragonTargetingHelper.movementAnchor(target).getBoundingBox().getCenter();
    }

    private double bodyGap(Ignivorus dragon, LivingEntity target) {
        return Math.max(0, dragon.distanceTo(target) - (dragon.getBbWidth() + target.getBbWidth()) * 0.5D);
    }

    private Vec3 predictedFeet(Ignivorus dragon, LivingEntity target, double ticks) {
        Vec3 prediction = dragon.getCombatLearning().predictCenter(target, ticks, 12);
        if (prediction != null) {
            if (!dragon.getCombatFlightState().targetNeedsFlight()) {
                prediction = new Vec3(prediction.x, targetCenter(target).y, prediction.z);
            }
            return prediction.add(0, -dragon.getBbHeight() * 0.5D, 0);
        }
        Vec3 velocity = dragon.getCombatFlightState().targetVelocity();
        if (!dragon.getCombatFlightState().targetNeedsFlight()) velocity = velocity.multiply(1, 0, 1);
        Vec3 lead = velocity.scale(ticks);
        if (lead.lengthSqr() > 144) lead = lead.normalize().scale(12);
        return targetCenter(target).add(lead).add(0, -dragon.getBbHeight() * 0.5D, 0);
    }

    private @Nullable Vec3 attackPosition(Ignivorus dragon, LivingEntity target, Vec3 destination, double height) {
        var space = dragon.getAIMovement().flightSpace();
        double ceiling = dragon.level().getMaxBuildHeight() - dragon.getBbHeight() - 2;
        var local = space.observe(dragon.position());
        if (local != null && local.clear() && local.ceilingKnown()) {
            ceiling = Math.min(ceiling, local.ceiling() - dragon.getBbHeight() - 2);
        }
        // Observe from our current flight space, so a cave roof cannot become the new floor.
        var ahead = space.observe(new Vec3(destination.x, dragon.getY(), destination.z));
        if (ahead != null && ahead.clear() && ahead.ceilingKnown()) {
            ceiling = Math.min(ceiling, ahead.ceiling() - dragon.getBbHeight() - 2);
        }
        double wantedY = dragon.getCombatFlightState().targetNeedsFlight() ? destination.y
                : DragonTargetingHelper.movementAnchor(target).getY() + height;
        wantedY = Mth.clamp(wantedY, dragon.level().getMinBuildHeight() + 4, Math.max(
                dragon.level().getMinBuildHeight() + 4, ceiling));
        Vec3 fitted = space.fitDestination(new Vec3(destination.x, wantedY, destination.z));
        return fitted != null && fitted.y <= ceiling ? fitted : null;
    }

    private Vec3 horizontal(Vec3 direction, Ignivorus dragon) {
        Vec3 result = direction.multiply(1, 0, 1);
        if (result.lengthSqr() < 1.0E-6D) result = dragon.getLookAngle().multiply(1, 0, 1);
        return result.lengthSqr() < 1.0E-6D ? new Vec3(0, 0, 1) : result.normalize();
    }

    private void clearRouteState() {
        routeTarget = null;
        approachAnchor = null;
        runDirection = null;
        takeoffTarget = null;
    }

    @Override
    protected void stopAirCombat(DragonBrainContext<Ignivorus> context) {
        context.dragon().getCombatAim().clear();
        phase = AirPhase.CHASE;
        phaseTicks = 0;
        clearRouteState();
        lastDecision = "stopped";
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>(super.getDragonBrainDebugDetails());
        details.put("air_phase", phase.name().toLowerCase());
        details.put("air_decision", lastDecision);
        details.put("air_phase_ticks", Integer.toString(phaseTicks));
        details.put("air_route_y", routeTarget == null ? "none" : Integer.toString(Mth.floor(routeTarget.y)));
        return details;
    }

    private enum AirPhase { CHASE, APPROACH, BREATH_PASS, FIREBALL_PASS, BITE_PASS, EGRESS }
}
