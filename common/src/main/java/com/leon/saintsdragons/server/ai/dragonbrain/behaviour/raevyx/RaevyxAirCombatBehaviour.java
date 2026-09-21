package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx;

import com.leon.saintsdragons.common.config.dragon.profile.RaevyxStatProfile;

import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonCombatDecisionSupport;
import com.leon.saintsdragons.server.ai.navigation.async.DragonFlightRequest;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AirCombatMovementBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.learning.DragonCombatLearning;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonCombatAim;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxBeamAbility;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RaevyxAirCombatBehaviour extends AirCombatMovementBehaviour<Raevyx> {
    private static final double MELEE_RANGE = RaevyxStatProfile.AirCombatBehaviour.MELEE_RANGE;
    private static final double RANGED_MIN_RANGE = RaevyxStatProfile.AirCombatBehaviour.RANGED_MIN_RANGE;
    private static final double RANGED_MAX_RANGE = Raevyx.BEAM_RANGE * 0.85D;
    private static final double ROAR_MIN_RANGE = RaevyxStatProfile.AirCombatBehaviour.ROAR_MIN_RANGE;
    private static final double ROAR_MAX_RANGE = RaevyxStatProfile.AirCombatBehaviour.ROAR_MAX_RANGE;
    private static final double CHASE_CONTAIN_RANGE = RaevyxStatProfile.AirCombatBehaviour.CHASE_CONTAIN_RANGE;
    private static final double ORBIT_ABORT_RANGE = RaevyxStatProfile.AirCombatBehaviour.ORBIT_ABORT_RANGE;
    private static final double TACTICAL_ESCAPE_RANGE = RaevyxStatProfile.AirCombatBehaviour.TACTICAL_ESCAPE_RANGE;
    private static final double ORBIT_RADIUS = RaevyxStatProfile.AirCombatBehaviour.ORBIT_RADIUS;
    private static final double GROUND_ATTACK_MIN_HEIGHT = 22.0D;
    private static final double GROUND_ATTACK_EXTRA_HEIGHT = 12.0D;
    private static final double ORBIT_STEP_RADIANS = Math.toRadians(58.0D);
    private static final double TARGET_FLEE_SPEED = RaevyxStatProfile.AirCombatBehaviour.TARGET_FLEE_SPEED;
    private static final double BREAKAWAY_DISTANCE = 30.0D;
    private static final double BREAKAWAY_LATERAL_OFFSET = 6.0D;
    private static final double ORBIT_SPEED = RaevyxStatProfile.AirCombatBehaviour.ORBIT_SPEED;
    private static final double DIRECT_CHASE_SPEED = RaevyxStatProfile.AirCombatBehaviour.DIRECT_CHASE_SPEED;
    private static final double DIVE_CHASE_SPEED = RaevyxStatProfile.AirCombatBehaviour.DIVE_CHASE_SPEED;
    private static final double DIRECT_COMMIT_SPEED = RaevyxStatProfile.AirCombatBehaviour.DIRECT_COMMIT_SPEED;
    private static final double DIVE_COMMIT_SPEED = RaevyxStatProfile.AirCombatBehaviour.DIVE_COMMIT_SPEED;
    private static final double BEAM_PASS_SPEED = RaevyxStatProfile.AirCombatBehaviour.BEAM_PASS_SPEED;
    private static final double ROAR_PASS_SPEED = RaevyxStatProfile.AirCombatBehaviour.ROAR_PASS_SPEED;
    private static final double BREAKAWAY_SPEED = RaevyxStatProfile.AirCombatBehaviour.BREAKAWAY_SPEED;
    private static final double ROUTE_ARRIVAL_DISTANCE_SQR = 25.0D;
    private static final double ORBIT_RETARGET_DISTANCE_SQR = 100.0D;
    private static final double COMMIT_ABORT_DISTANCE_SQR = 324.0D;
    private static final double BREAKAWAY_CLEAR_DISTANCE_SQR = 34.0D * 34.0D;
    private static final int MELEE_ATTACK_COOLDOWN_TICKS = RaevyxStatProfile.AirCombatBehaviour.MELEE_ATTACK_COOLDOWN_TICKS;
    private static final int BEAM_ATTACK_COOLDOWN_TICKS = RaevyxStatProfile.AirCombatBehaviour.BEAM_ATTACK_COOLDOWN_TICKS;
    private static final int BEAM_OPENING_TICKS = 10;
    private static final int ROAR_ATTACK_COOLDOWN_TICKS = RaevyxStatProfile.AirCombatBehaviour.ROAR_ATTACK_COOLDOWN_TICKS;
    private static final int ROAR_COOLDOWN_TICKS = RaevyxStatProfile.AirCombatBehaviour.ROAR_COOLDOWN_TICKS;
    private static final int ROAR_DECISION_INTERVAL_TICKS = 40;
    private static final int CHASE_CAPTURE_TICKS = RaevyxStatProfile.AirCombatBehaviour.CHASE_CAPTURE_TICKS;
    private static final int CHASE_MINIMUM_TICKS = RaevyxStatProfile.AirCombatBehaviour.CHASE_MINIMUM_TICKS;
    private static final int ORBIT_MINIMUM_TICKS = 20;
    private static final int ORBIT_MAXIMUM_TICKS = 40;
    private static final int BEAM_SETUP_TIMEOUT_TICKS = 60;
    private static final int ORBIT_RETARGET_INTERVAL_TICKS = 10;

    private AirPhase phase = AirPhase.CHASE;
    private int phaseTicks;
    private int chaseCaptureTicks;
    private int minimumChaseTicks;
    private int attackCooldown;
    private int rangedCooldown;
    private int roarCooldown;
    private int nextRoarTick;
    private int nextRoarDecisionTick;
    private int routeGraceTicks;
    private int attackSide = 1;
    private int beamSegment;
    private int beamAlignmentTicks;
    private int beamRetryTick;
    private int beamEscapeRetryTick;
    private int orbitWaypointsCompleted;
    private boolean roarEgressIssued;
    private boolean beamEscape;
    private String beamAvailability = "idle";
    private String lastDecision = "idle";
    private long flightHandoff;
    private double attackHeight = GROUND_ATTACK_MIN_HEIGHT;
    private double airPassHeight;

    @Nullable
    private Vec3 takeoffTarget;
    @Nullable
    private Vec3 beamSetupTarget;
    @Nullable
    private Vec3 beamSetupAnchor;
    @Nullable
    private Vec3 beamApproachOffset;

    @Nullable
    private Vec3 routeTarget;
    @Nullable
    private Vec3 orbitAnchor;
    @Nullable
    private Vec3 committedIntercept;
    @Nullable
    private Vec3 runDirection;
    @Nullable
    private Vec3 breakawayTarget;

    @Override
    protected void startAirCombat(DragonBrainContext<Raevyx> context) {
        flightHandoff = context.dragon().getCombatFlightState().handoffRevision();
        attackHeight = GROUND_ATTACK_MIN_HEIGHT + context.dragon().getRandom().nextDouble() * GROUND_ATTACK_EXTRA_HEIGHT;
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        phase = !context.dragon().isTakeoff() && target != null && !context.dragon().getCombatFlightState().targetNeedsFlight()
                && !isTargetFleeing(context.dragon(), target) ? AirPhase.ORBIT : AirPhase.CHASE;
        phaseTicks = 0;
        chaseCaptureTicks = 0;
        minimumChaseTicks = CHASE_MINIMUM_TICKS;
        routeGraceTicks = 0;
        attackSide = context.dragon().getRandom().nextBoolean() ? 1 : -1;
        beamSegment = 0;
        beamAlignmentTicks = 0;
        beamEscape = false;
        airPassHeight = context.dragon().getRandom().nextDouble() * 6.0D - 3.0D;
        orbitWaypointsCompleted = 0;
        roarEgressIssued = false;
        clearRouteState();
        if (phase == AirPhase.ORBIT) commandOrbitRoute(context, target);
        lastDecision = phase == AirPhase.ORBIT ? "orbit:ground-target-opening" : "chase:engage";
    }

    @Override
    protected void tickAirCombat(DragonBrainContext<Raevyx> context,
                                 LivingEntity target,
                                 boolean hasLineOfSight) {
        tickCooldowns(context.dragon());
        Raevyx dragon = context.dragon();
        if (flightHandoff != dragon.getCombatFlightState().handoffRevision()) startAirCombat(context);
        rangedCooldown = dragon.getAiBeamCooldownTicks();
        beamAvailability = beamBlockReason(dragon, target, hasLineOfSight);
        dragon.getLookControl().setLookAt(target, 100.0F, 100.0F);

        if (dragon.isTakeoff()) {
            commandTakeoffIntent(context, target);
            return;
        }
        if (takeoffTarget != null) {
            clearRouteState();
            phase = AirPhase.CHASE;
            phaseTicks = BEAM_OPENING_TICKS;
        }

        if (dragon.isDodging()) {
            if (phase != AirPhase.EVADE) {
                enterEvade(context, "evade:reactive-hit");
            }
            return;
        }
        if (phase == AirPhase.EVADE) {
            if (dragon.distanceTo(target) > CHASE_CONTAIN_RANGE || isTargetFleeing(dragon, target)) {
                enterChase(context, target, "chase:post-dodge-gap");
            } else {
                enterBreakaway(context, target, "breakaway:post-dodge");
            }
            return;
        }

        if (phase != AirPhase.CHASE && phase != AirPhase.BEAM_SETUP && phase != AirPhase.BEAM_PASS
                && maneuverDistance(dragon, target) > TACTICAL_ESCAPE_RANGE) {
            enterChase(context, target, "chase:target-escaped");
            return;
        }
        phaseTicks++;
        if (dragon.isAbilityActive(ModAbilities.RAEVYX_LIGHTNING_BEAM)
                && phase != AirPhase.BEAM_PASS
                && phase != AirPhase.CHASE) {
            enterBeamPass(context, target, "beam:resume-pass");
            return;
        }

        switch (phase) {
            case CHASE -> tickChase(context, target, hasLineOfSight);
            case ORBIT -> tickOrbit(context, target, hasLineOfSight);
            case BEAM_SETUP -> tickBeamSetup(context, target, hasLineOfSight);
            case MELEE_COMMIT -> tickMeleeCommit(context, target, hasLineOfSight);
            case MELEE_STRIKE -> tickMeleeStrike(context, target);
            case BEAM_PASS -> tickBeamPass(context, target);
            case ROAR_PASS -> tickRoarPass(context, target);
            case BREAK_AWAY -> tickBreakaway(context, target);
            case EVADE -> {
                // Dodge completion is handled above before phase time advances.
            }
        }
    }

    private void tickChase(DragonBrainContext<Raevyx> context,
                           LivingEntity target,
                           boolean hasLineOfSight) {
        Raevyx dragon = context.dragon();
        boolean dive = commandChaseIntent(context, target);
        boolean fleeing = isTargetFleeing(dragon, target);

        if (dragon.isAbilityActive(ModAbilities.RAEVYX_LIGHTNING_BEAM)) {
            chaseCaptureTicks = 0;
            commandBeamApproach(context, target);
            if (dragon.distanceTo(target) <= ORBIT_ABORT_RANGE) {
                enterBeamPass(context, target, "beam:pursuit-caught-up");
            } else {
                lastDecision = "beam:pursuit";
            }
            return;
        }
        if (shouldMakeBeamSpace(dragon, target)) {
            beamEscapeRetryTick = dragon.tickCount + 100;
            enterBreakaway(context, target, "breakaway:make-beam-space");
            beamEscape = true;
            return;
        }
        if (phaseTicks >= BEAM_OPENING_TICKS) {
            if (tryRoarOpening(context, target, hasLineOfSight)) return;
            if (tryBeamOpening(context, target, hasLineOfSight)) return;
        }

        if (attackCooldown <= 0
                && !isCurrentlyAttacking(dragon)
                && hasLineOfSight
                && dragon.distanceTo(target) <= MELEE_RANGE
                && isFacingTarget(dragon, target, 0.10D)
                && tryStartMeleeAttack(dragon)) {
            attackCooldown = MELEE_ATTACK_COOLDOWN_TICKS;
            chaseCaptureTicks = 0;
            lastDecision = "chase:pursuit-bite";
            return;
        }

        if (phaseTicks >= minimumChaseTicks && !isCurrentlyAttacking(dragon) && hasLineOfSight
                && !"ready".equals(beamAvailability) && maneuverDistance(dragon, target) <= ORBIT_ABORT_RANGE
                && (!fleeing || dragon.distanceTo(target) <= CHASE_CONTAIN_RANGE)) {
            startAttackPass(context, target, hasLineOfSight);
            return;
        }

        if (hasLineOfSight
                && !fleeing
                && maneuverDistance(dragon, target) <= CHASE_CONTAIN_RANGE) {
            chaseCaptureTicks++;
        } else {
            chaseCaptureTicks = 0;
        }
        if (phaseTicks >= minimumChaseTicks && chaseCaptureTicks >= CHASE_CAPTURE_TICKS) {
            enterOrbit(context, target, "orbit:target-contained");
            return;
        }
        lastDecision = fleeing
                ? "chase:target-fleeing"
                : dive ? "chase:dive-pursuit" : "chase:direct-pursuit";
    }

    private void commandTakeoffIntent(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        if (takeoffTarget == null) {
            var space = dragon.getAIMovement().flightSpace();
            Vec3 forward = horizontalDirection(targetCenter(target).subtract(dragon.position()), dragon.getLookAngle());
            Vec3 lateral = new Vec3(-forward.z, 0, forward.x).scale(attackSide * 6.0D);
            Vec3 lift = dragon.position().add(0, 8.0D, 0);
            for (Vec3 candidate : new Vec3[]{lift.add(forward.scale(12.0D)).add(lateral),
                    lift.add(forward.scale(12.0D)).subtract(lateral), lift}) {
                Vec3 fitted = space.fitDestination(candidate);
                if (fitted != null && space.corridorClear(dragon.position(), fitted)) {
                    takeoffTarget = fitted;
                    break;
                }
            }
        }
        if (takeoffTarget == null) {
            context.memories().set(DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.stop("raevyx-takeoff:no-clear-launch-lane"));
            lastDecision = "takeoff:no-clear-launch-lane";
            return;
        }
        routeTarget = takeoffTarget;
        lastDecision = "takeoff:clear-launch-lane";
        context.memories().set(DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.flight(DragonFlightRequest.track(takeoffTarget, ORBIT_SPEED, 2.0D)));
    }

    private boolean commandChaseIntent(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        boolean dive = shouldDiveChase(dragon, target, 7.0D, 42.0D);
        if (dragon.getCombatFlightState().targetNeedsFlight()) {
            Vec3 destination = flightFeet(dragon, predictTargetCenter(dragon, target, dive ? 3.0D : 6.0D, 12.0D));
            if (dive) {
                var clearance = dragon.getAIMovement().flightSpace().observe(destination);
                if (clearance != null && clearance.floorKnown()) {
                    destination = new Vec3(destination.x, Math.max(destination.y, clearance.floor() + 6.0D), destination.z);
                }
                dive = destination.y < dragon.getY() - 2.0D;
            }
            context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.flight(dive
                    ? DragonFlightRequest.dive(destination, DIVE_CHASE_SPEED)
                    : DragonFlightRequest.chase(destination, DIRECT_CHASE_SPEED)));
            return dive;
        }
        if (dive) {
            setDivingChaseIntent(
                    context,
                    target,
                    3.0D,
                    -0.25D,
                    0.08D,
                    0.12D,
                    DIVE_CHASE_SPEED
            );
        } else {
            var anchor = DragonTargetingHelper.movementAnchor(target);
            Vec3 destination = anchor.position()
                    .add(anchor.getDeltaMovement().multiply(1, 0, 1).scale(6.0D))
                    .add(0, anchor.getBbHeight() + 1.5D + Math.sin(dragon.tickCount * 0.08D) * 0.15D, 0);
            destination = groundAttackPosition(dragon, target, destination,
                    dragon.isAiAirBeamReady() && dragon.tickCount >= beamRetryTick ? attackHeight : 6.0D);
            context.memories().set(DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.flight(DragonFlightRequest.chase(destination, DIRECT_CHASE_SPEED)));
        }
        return dive;
    }

    private void tickOrbit(DragonBrainContext<Raevyx> context,
                           LivingEntity target,
                           boolean hasLineOfSight) {
        Raevyx dragon = context.dragon();
        if (maneuverDistance(dragon, target) > ORBIT_ABORT_RANGE || isTargetFleeing(dragon, target)) {
            enterChase(context, target, "chase:orbit-broken");
            return;
        }
        if (routeFailed(dragon)) {
            enterChase(context, target, "chase:orbit-route-failed");
            return;
        }

        if (phaseTicks >= BEAM_OPENING_TICKS) {
            if (tryRoarOpening(context, target, hasLineOfSight)) return;
            if (tryBeamOpening(context, target, hasLineOfSight)) return;
        }

        if (phaseTicks >= ORBIT_MINIMUM_TICKS && !isCurrentlyAttacking(dragon)
                && !"ready".equals(beamAvailability)) {
            startAttackPass(context, target, hasLineOfSight);
            return;
        }

        Vec3 currentTargetCenter = targetCenter(target);
        boolean targetShifted = orbitAnchor != null
                && orbitAnchor.distanceToSqr(currentTargetCenter) > ORBIT_RETARGET_DISTANCE_SQR;
        if (routeTarget == null
                || targetShifted && phaseTicks % ORBIT_RETARGET_INTERVAL_TICKS == 0) {
            commandOrbitRoute(context, target);
        }

        if (routeReached(dragon)) {
            orbitWaypointsCompleted++;
            if (phaseTicks < ORBIT_MINIMUM_TICKS) {
                commandOrbitRoute(context, target);
            }
        }

        boolean orbitEstablished = phaseTicks >= ORBIT_MINIMUM_TICKS
                && (orbitWaypointsCompleted >= 1 || phaseTicks >= ORBIT_MAXIMUM_TICKS);
        if (!orbitEstablished) {
            lastDecision = targetShifted ? "orbit:tracking-shift" : "orbit:circling";
            return;
        }

        if (isCurrentlyAttacking(dragon)) {
            if (routeReached(dragon)) {
                commandOrbitRoute(context, target);
            }
            lastDecision = "orbit:ability-active";
            return;
        }
        startAttackPass(context, target, hasLineOfSight);
    }

    private void startAttackPass(DragonBrainContext<Raevyx> context, LivingEntity target, boolean hasLineOfSight) {
        if (tryRoarOpening(context, target, hasLineOfSight)) return;
        enterMeleeCommit(context, target);
    }

    private boolean tryRoarOpening(DragonBrainContext<Raevyx> context, LivingEntity target, boolean hasLineOfSight) {
        Raevyx dragon = context.dragon();
        double distance = dragon.distanceTo(target);
        if (attackCooldown > 0 || dragon.tickCount < nextRoarTick || dragon.tickCount < nextRoarDecisionTick
                || dragon.isTakeoff() || isCurrentlyAttacking(dragon) || !hasLineOfSight
                || distance < ROAR_MIN_RANGE || distance <= MELEE_RANGE || distance > ROAR_MAX_RANGE
                || DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)
                || !canUseAiAbility(dragon, ModAbilities.RAEVYX_ROAR, true)) return false;

        // One choice per window, shared by pursuit and close passes, rather than a roll every tick.
        nextRoarDecisionTick = dragon.tickCount + ROAR_DECISION_INTERVAL_TICKS + dragon.getRandom().nextInt(21);
        float chance = dragon.isAiAirBeamReady() ? 0.30F : 0.45F;
        if (dragon.getRandom().nextFloat() >= chance || !tryStartRoar(dragon)) return false;

        attackCooldown = ROAR_ATTACK_COOLDOWN_TICKS;
        roarCooldown = ROAR_COOLDOWN_TICKS + dragon.getRandom().nextInt(41);
        nextRoarTick = dragon.tickCount + roarCooldown;
        enterRoarPass(context, target);
        return true;
    }

    private void tickMeleeCommit(DragonBrainContext<Raevyx> context,
                                 LivingEntity target,
                                 boolean hasLineOfSight) {
        Raevyx dragon = context.dragon();
        if (committedIntercept == null || runDirection == null || breakawayTarget == null) {
            enterMeleeCommit(context, target);
            return;
        }
        if (routeFailed(dragon)) {
            enterChase(context, target, "chase:commit-route-failed");
            return;
        }
        if (dragon.isAbilityActive(ModAbilities.RAEVYX_BITE)) {
            enterMeleeStrike(context, "bite:active");
            return;
        }

        Vec3 dragonCenter = dragon.getBoundingBox().getCenter();
        double remainingAlongRun = runDirection.dot(committedIntercept.subtract(dragonCenter));
        boolean targetEscapedCommit = phaseTicks > 8
                && targetCenter(target).distanceToSqr(committedIntercept) > COMMIT_ABORT_DISTANCE_SQR;
        if (targetEscapedCommit) {
            enterChase(context, target, "chase:target-broke-commit");
            return;
        }
        if (remainingAlongRun < -2.0D || phaseTicks >= 50) {
            enterBreakaway(context, target, "breakaway:missed-pass");
            return;
        }

        if (attackCooldown <= 0
                && !isCurrentlyAttacking(dragon)
                && hasLineOfSight
                && dragon.distanceTo(target) <= MELEE_RANGE
                && isFacingTarget(dragon, target, 0.15D)
                && tryStartMeleeAttack(dragon)) {
            attackCooldown = MELEE_ATTACK_COOLDOWN_TICKS;
            enterMeleeStrike(context, "bite:started-pass");
            return;
        }
        lastDecision = "commit:intercept";
    }

    private void tickMeleeStrike(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        if (routeTarget == null) {
            if (breakawayTarget == null) {
                enterBreakaway(context, target, "breakaway:no-strike-egress");
                return;
            }
            commandManeuver(context, breakawayTarget, BREAKAWAY_SPEED);
        }
        if (routeFailed(dragon)) {
            enterBreakaway(context, target, "breakaway:strike-route-failed");
            return;
        }
        if (phaseTicks >= 10 || phaseTicks >= 5 && !dragon.isAbilityActive(ModAbilities.RAEVYX_BITE)) {
            adoptCurrentRouteAsBreakaway("breakaway:bite-egress");
            return;
        }
        lastDecision = "strike:bite-pass";
    }

    private void tickBeamPass(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        boolean beamActive = dragon.isAbilityActive(ModAbilities.RAEVYX_LIGHTNING_BEAM);
        if (!beamActive && phaseTicks >= 8) {
            enterBreakaway(context, target, "breakaway:beam-complete");
            return;
        }
        if (routeFailed(dragon)) {
            enterChase(context, target, "beam:route-recovery");
            return;
        }
        if (phaseTicks % 6 == 0 || routeReached(dragon)) {
            commandBeamApproach(context, target);
        }
        if (phaseTicks >= 110) {
            enterBreakaway(context, target, "breakaway:beam-timeout");
            return;
        }
        lastDecision = "beam:tracking-pass";
    }

    private void tickRoarPass(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        if (routeFailed(dragon)) {
            enterBreakaway(context, target, "breakaway:roar-route-failed");
            return;
        }
        if (!roarEgressIssued && routeReached(dragon) && breakawayTarget != null) {
            commandManeuver(context, breakawayTarget, BREAKAWAY_SPEED);
            roarEgressIssued = true;
        }
        if (phaseTicks >= 5 && !dragon.isAbilityActive(ModAbilities.RAEVYX_ROAR)) {
            adoptCurrentRouteAsBreakaway("breakaway:roar-complete");
            return;
        }
        if (phaseTicks >= 40) {
            enterBreakaway(context, target, "breakaway:roar-timeout");
            return;
        }
        lastDecision = roarEgressIssued ? "roar:egress" : "roar:flyby";
    }

    private void tickBreakaway(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        if (routeTarget == null) {
            commandBreakawayRoute(context, target);
        }
        if (routeFailed(dragon)) {
            attackSide = -attackSide;
            enterChase(context, target, "chase:egress-route-failed");
            return;
        }

        boolean clear = dragon.distanceToSqr(target) >= (beamEscape ? 22.0D * 22.0D : BREAKAWAY_CLEAR_DISTANCE_SQR);
        if (phaseTicks >= 14 && (clear || routeReached(dragon) || phaseTicks >= 60)) {
            attackSide = -attackSide;
            if (beamEscape && tryBeamOpening(context, target, dragon.getSensing().hasLineOfSight(target))) return;
            enterChase(context, target, "chase:reacquire-after-run");
            return;
        }
        lastDecision = "breakaway:egress";
    }

    private void enterChase(DragonBrainContext<Raevyx> context,
                            LivingEntity target,
                            String decision) {
        phase = AirPhase.CHASE;
        phaseTicks = 0;
        chaseCaptureTicks = 0;
        orbitWaypointsCompleted = 0;
        minimumChaseTicks = context.dragon().distanceTo(target) <= CHASE_CONTAIN_RANGE
                ? 4
                : CHASE_MINIMUM_TICKS;
        clearRouteState();
        commandChaseIntent(context, target);
        lastDecision = decision;
    }

    private void enterOrbit(DragonBrainContext<Raevyx> context,
                            LivingEntity target,
                            String decision) {
        attackHeight = GROUND_ATTACK_MIN_HEIGHT + context.dragon().getRandom().nextDouble() * GROUND_ATTACK_EXTRA_HEIGHT;
        airPassHeight = context.dragon().getRandom().nextDouble() * 6.0D - 3.0D;
        phase = AirPhase.ORBIT;
        phaseTicks = 0;
        chaseCaptureTicks = 0;
        orbitWaypointsCompleted = 0;
        clearRouteState();
        commandOrbitRoute(context, target);
        lastDecision = decision;
    }

    private void commandOrbitRoute(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        Vec3 targetPosition = predictTargetCenter(dragon, target, 4.0D, 8.0D);
        Vec3 radial = horizontalDirection(
                dragon.getBoundingBox().getCenter().subtract(targetPosition),
                dragon.getLookAngle()
        );
        Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x).scale(attackSide);
        Vec3 orbitDirection = radial.scale(Math.cos(ORBIT_STEP_RADIANS))
                .add(tangent.scale(Math.sin(ORBIT_STEP_RADIANS)))
                .normalize();
        Vec3 orbitPosition = flightFeet(dragon, targetPosition)
                .add(orbitDirection.scale(ORBIT_RADIUS))
                .add(0.0D, airPassHeight, 0.0D);
        orbitPosition = groundAttackPosition(dragon, target, orbitPosition, attackHeight);
        orbitAnchor = targetCenter(target);
        commandManeuver(context, orbitPosition, ORBIT_SPEED);
    }

    private void enterMeleeCommit(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        dragon.getCombatFlightState().holdFlightFor(30);
        dragon.getCombatAim().clear();
        clearRouteState();
        phase = AirPhase.MELEE_COMMIT;
        phaseTicks = 0;
        committedIntercept = predictTargetCenter(dragon, target, 8.0D, 16.0D);
        runDirection = direction(
                committedIntercept.subtract(dragon.getBoundingBox().getCenter()),
                dragon.getLookAngle()
        );
        breakawayTarget = egressPosition(dragon, target, flightFeet(dragon, committedIntercept), runDirection);
        boolean dive = shouldDiveChase(dragon, target, 7.0D, 42.0D);
        commandManeuver(context, flightFeet(dragon, committedIntercept),
                dive ? DIVE_COMMIT_SPEED : DIRECT_COMMIT_SPEED, dive);
        lastDecision = dive ? "commit:dive" : "commit:direct";
    }

    private void enterMeleeStrike(DragonBrainContext<Raevyx> context, String decision) {
        phase = AirPhase.MELEE_STRIKE;
        phaseTicks = 0;
        routeTarget = null;
        if (breakawayTarget != null) {
            commandManeuver(context, breakawayTarget, BREAKAWAY_SPEED);
        }
        lastDecision = decision;
    }

    private void enterBeamPass(DragonBrainContext<Raevyx> context,
                               LivingEntity target,
                               String decision) {
        context.dragon().getCombatFlightState().holdFlightFor(30);
        phase = AirPhase.BEAM_PASS;
        phaseTicks = 0;
        commandBeamApproach(context, target);
        lastDecision = decision;
    }

    private void enterRoarPass(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        dragon.getCombatFlightState().holdFlightFor(30);
        Vec3 dragonCenter = dragon.getBoundingBox().getCenter();
        Vec3 center = predictTargetCenter(dragon, target, 4.0D, 10.0D);
        Vec3 radial = horizontalDirection(dragonCenter.subtract(center), dragon.getLookAngle());
        Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x).scale(attackSide);
        Vec3 passTarget = clampFlightY(
                dragon,
                flightFeet(dragon, center).add(radial.scale(-22.0D))
                        .add(tangent.scale(12.0D))
        );
        passTarget = groundAttackPosition(dragon, target, passTarget, 6.0D);

        phase = AirPhase.ROAR_PASS;
        phaseTicks = 0;
        clearRouteState();
        runDirection = direction(passTarget.subtract(dragon.position()), dragon.getLookAngle());
        breakawayTarget = egressPosition(dragon, target, passTarget, runDirection);
        roarEgressIssued = false;
        commandManeuver(context, passTarget, ROAR_PASS_SPEED);
        lastDecision = "roar:started-flyby";
    }

    private void commandBeamApproach(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        if (beamSetupTarget == null || beamApproachOffset == null) {
            if (!selectBeamPosition(dragon, target)) return;
        }
        Vec3 destination = phase == AirPhase.BEAM_SETUP ? beamSetupTarget
                : predictTargetCenter(dragon, target, 3.0D, 6.0D).add(beamApproachOffset);
        Vec3 toTarget = targetCenter(target).subtract(dragon.getBoundingBox().getCenter());
        Vec3 correction = destination.subtract(dragon.position());
        boolean movingIntoShot = horizontalDirection(correction, dragon.getLookAngle())
                .dot(horizontalDirection(toTarget, dragon.getLookAngle())) > 0.85D;
        // Slow lateral/backward adjustments let the body turn toward the beam instead of chasing its own orbit.
        double speed = !movingIntoShot || beamAlignmentTicks > 0
                ? 0.35D / Math.max(0.1D, dragon.getFlightSpeed())
                : Mth.clamp((dragon.getCombatFlightState().targetVelocity().horizontalDistance() + 0.35D)
                        / Math.max(0.1D, dragon.getFlightSpeed()), BEAM_PASS_SPEED, DIRECT_CHASE_SPEED);
        DragonFlightRequest request = DragonFlightRequest.track(destination, speed, 2.0D);
        routeTarget = request.target();
        beamSegment = dragon.isBeaming() ? 1 : 0;
        context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.flight(request));
    }

    private boolean selectBeamPosition(Raevyx dragon, LivingEntity target) {
        Vec3 center = predictTargetCenter(dragon, target, 3.0D, 6.0D);
        Vec3 radial = horizontalDirection(dragon.getBoundingBox().getCenter().subtract(center), dragon.getLookAngle());
        Vec3 tangent = new Vec3(-radial.z, 0, radial.x).scale(attackSide * 4.0D);
        double spacing = dragon.getCombatFlightState().targetNeedsFlight() ? 28.0D : Math.max(28.0D, attackHeight * 1.25D);
        var expected = dragon.getCombatLearning().expectation(target,
                DragonCombatLearning.Attack.BEAM, true);
        spacing += expected.spacingBonus();
        if (expected.response() == DragonCombatLearning.Response.RETREAT) {
            spacing -= 4.0D * expected.confidence();
        }
        double side = switch (expected.response()) {
            case LEFT -> 1.0D;
            case RIGHT -> -1.0D;
            default -> 0.0D;
        };
        // Commit this correction with the firing position; do not chase each fresh observation sideways.
        Vec3 targetLeft = new Vec3(radial.z, 0, -radial.x);
        tangent = tangent.add(targetLeft.scale(side * 4.0D * expected.confidence()));
        Vec3 position = flightFeet(dragon, center).add(radial.scale(spacing)).add(tangent);
        position = groundAttackPosition(dragon, target, position, attackHeight * 0.75D);
        var decisions = dragon.getCombatDecisionSupport();
        Vec3 mouth = dragon.getBeamStartAnchor(1.0F);
        Vec3 fitted = decisions == null ? dragon.getAIMovement().flightSpace().fitDestination(position)
                : decisions.choosePosition("beam-setup", target, position,
                mouth == null ? dragon.getEyePosition() : mouth, RANGED_MAX_RANGE,
                dragon.getAIMovement().flightSpace()::fitDestination);
        if (fitted == null) return false;
        beamSetupTarget = fitted;
        beamSetupAnchor = targetCenter(target);
        beamApproachOffset = fitted.subtract(center);
        routeGraceTicks = 3;
        return true;
    }

    private void enterBreakaway(DragonBrainContext<Raevyx> context,
                                LivingEntity target,
                                String decision) {
        context.dragon().getCombatFlightState().holdFlightFor(25);
        Vec3 preservedRunDirection = runDirection;
        phase = AirPhase.BREAK_AWAY;
        phaseTicks = 0;
        clearRouteState();
        runDirection = preservedRunDirection;
        commandBreakawayRoute(context, target);
        lastDecision = decision;
    }

    private void commandBreakawayRoute(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        Vec3 dragonCenter = dragon.getBoundingBox().getCenter();
        Vec3 forward = runDirection;
        if (forward == null || forward.lengthSqr() < 1.0E-6D) {
            forward = dragon.getDeltaMovement().lengthSqr() > 0.04D
                    ? dragon.getDeltaMovement()
                    : dragonCenter.subtract(targetCenter(target));
        }
        forward = horizontalDirection(forward, dragon.getLookAngle());
        breakawayTarget = egressPosition(dragon, target, dragon.position(), forward);
        runDirection = forward;
        commandManeuver(context, breakawayTarget, BREAKAWAY_SPEED);
    }

    private Vec3 egressPosition(Raevyx dragon, LivingEntity target, Vec3 start, Vec3 forward) {
        Vec3 horizontal = horizontalDirection(forward, dragon.getLookAngle());
        Vec3 tangent = new Vec3(-horizontal.z, 0, horizontal.x).scale(attackSide);
        Vec3 destination = start.add(horizontal.scale(BREAKAWAY_DISTANCE))
                .add(tangent.scale(isBeingPursued(dragon, target) ? 14.0D : BREAKAWAY_LATERAL_OFFSET));
        if (dragon.getCombatFlightState().targetNeedsFlight()) {
            double targetY = flightFeet(dragon, targetCenter(target)).y;
            double height = targetY + Mth.clamp(dragon.getY() - targetY, -4.0D, 4.0D);
            if (isBeingPursued(dragon, target)) height = Math.min(height, dragon.getY() - 6.0D);
            var space = dragon.getAIMovement().flightSpace();
            Vec3 lower = space.fitDestination(new Vec3(destination.x, height, destination.z));
            if (lower != null && space.corridorClear(dragon.position(), lower)) return lower;
            // Keep the escape level when terrain blocks the lower lane; the navigator still checks the route.
            return clampFlightY(dragon, new Vec3(destination.x, dragon.getY(), destination.z));
        }
        return groundAttackPosition(dragon, target, destination, attackHeight);
    }

    private void adoptCurrentRouteAsBreakaway(String decision) {
        phase = AirPhase.BREAK_AWAY;
        phaseTicks = 0;
        lastDecision = decision;
    }

    private void enterEvade(DragonBrainContext<Raevyx> context, String decision) {
        phase = AirPhase.EVADE;
        phaseTicks = 0;
        clearRouteState();
        context.memories().set(
                DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.stop("raevyx-air-combat:evade")
        );
        lastDecision = decision;
    }

    private void commandManeuver(DragonBrainContext<Raevyx> context, Vec3 target, double speed) {
        commandManeuver(context, target, speed, false);
    }

    private void commandManeuver(DragonBrainContext<Raevyx> context, Vec3 target, double speed, boolean dive) {
        routeTarget = clampFlightY(context.dragon(), target);
        routeGraceTicks = 3;
        context.memories().set(
                DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.flight(new DragonFlightRequest(routeTarget, speed,
                        dive ? DragonFlightRequest.Purpose.DIVE : DragonFlightRequest.Purpose.MANEUVER,
                        Math.sqrt(ROUTE_ARRIVAL_DISTANCE_SQR), DragonFlightRequest.Arrival.PASS_THROUGH))
        );
    }

    private boolean routeReached(Raevyx dragon) {
        return routeTarget != null
                && (dragon.position().distanceToSqr(routeTarget) <= ROUTE_ARRIVAL_DISTANCE_SQR
                || routeGraceTicks <= 0 && dragon.getAIMovement().hasArrived());
    }

    private boolean routeFailed(Raevyx dragon) {
        return routeTarget != null && routeGraceTicks <= 0 && dragon.getAIMovement().hasFailed();
    }

    private void tickCooldowns(Raevyx dragon) {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        roarCooldown = Math.max(0, nextRoarTick - dragon.tickCount);
        if (routeGraceTicks > 0) {
            routeGraceTicks--;
        }
    }

    private boolean tryBeamOpening(DragonBrainContext<Raevyx> context, LivingEntity target, boolean hasLineOfSight) {
        Raevyx dragon = context.dragon();
        beamAvailability = beamBlockReason(dragon, target, hasLineOfSight);
        if (!"ready".equals(beamAvailability)) return false;
        clearRouteState();
        if (!selectBeamPosition(dragon, target)) {
            beamRetryTick = dragon.tickCount + 100;
            dragon.getCombatFlightState().deferRangedFlightFor(100);
            beamAvailability = "no-firing-position";
            return false;
        }
        phase = AirPhase.BEAM_SETUP;
        phaseTicks = 0;
        beamAlignmentTicks = 0;
        chaseCaptureTicks = 0;
        tickBeamSetup(context, target, hasLineOfSight);
        return true;
    }

    private void tickBeamSetup(DragonBrainContext<Raevyx> context, LivingEntity target, boolean hasLineOfSight) {
        Raevyx dragon = context.dragon();
        beamAvailability = beamBlockReason(dragon, target, hasLineOfSight);
        if ("ready".equals(beamAvailability)) {
            if (phaseTicks >= BEAM_SETUP_TIMEOUT_TICKS) beamAvailability = "alignment-timeout";
            else if (routeFailed(dragon)) beamAvailability = "setup-route-failed";
            else if (beamSetupAnchor == null || targetCenter(target).distanceToSqr(beamSetupAnchor) > 144.0D) {
                beamAvailability = "target-left-setup";
            }
        }
        if (!"ready".equals(beamAvailability)) {
            abandonBeamSetup(context, target, "beam:" + beamAvailability);
            return;
        }
        DragonCombatAim.Shot shot = dragon.getAiBeamShot(target, RANGED_MAX_RANGE);
        if (shot != DragonCombatAim.Shot.ALIGNED && !shot.needsAlignment()) {
            abandonBeamSetup(context, target, "beam:shot-" + shot.name().toLowerCase());
            return;
        }
        if (dragon.getCombatAim().ready(3) && tryStartRangedAttack(dragon)) {
            beamAlignmentTicks = 0;
            attackCooldown = BEAM_ATTACK_COOLDOWN_TICKS;
            enterBeamPass(context, target, "beam:aligned-opening");
        } else {
            beamAlignmentTicks++;
            commandBeamApproach(context, target);
            lastDecision = "beam:braking-to-align";
        }
    }

    private void abandonBeamSetup(DragonBrainContext<Raevyx> context, LivingEntity target, String reason) {
        Raevyx dragon = context.dragon();
        var decisions = dragon.getCombatDecisionSupport();
        if (decisions != null) {
            if (reason.contains("route-failed")) decisions.fail(DragonCombatDecisionSupport.Failure.ROUTE_FAILED, beamSetupTarget);
            else if (reason.contains("alignment-timeout")) decisions.failSetup(beamSetupTarget);
            else if (reason.contains("shot-blocked")) decisions.fail(DragonCombatDecisionSupport.Failure.BLOCKED_SHOT, beamSetupTarget);
        }
        beamRetryTick = dragon.tickCount + 100;
        beamAlignmentTicks = 0;
        dragon.getCombatAim().clear();
        dragon.getCombatFlightState().deferRangedFlightFor(100);
        if (maneuverDistance(dragon, target) <= ORBIT_ABORT_RANGE) enterMeleeCommit(context, target);
        else enterChase(context, target, "chase:beam-setup-ended");
        lastDecision = reason + ":" + phase.name().toLowerCase();
    }

    private String beamBlockReason(Raevyx dragon, LivingEntity target, boolean visible) {
        if (dragon.isAbilityActive(ModAbilities.RAEVYX_LIGHTNING_BEAM)) return "active";
        if (dragon.isTakeoff()) return "takeoff";
        if (dragon.getAiBeamCooldownTicks() > 0) return "cooldown";
        if (dragon.getBeamEnergy() < 0.6F || !dragon.canUseBeam()) return "energy";
        if (!dragon.isAiBeamReady()) return "followup-needed";
        if (RaevyxBeamAbility.isAtAiBeamMercyThreshold(target)) return "target-low-health";
        if (DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)) return "bite-prey";
        if (dragon.tickCount < beamRetryTick) return "setup-retry";
        if (attackCooldown > 0 || isCurrentlyAttacking(dragon)
                || !canUseAiAbility(dragon, ModAbilities.RAEVYX_LIGHTNING_BEAM, true)) return "ability-pacing";
        if (!visible) return "no-sight";
        if (dragon.distanceTo(target) < RANGED_MIN_RANGE) return "too-close";
        if (dragon.distanceTo(target) > RANGED_MAX_RANGE) return "too-far";
        return "ready";
    }

    private boolean tryStartMeleeAttack(Raevyx dragon) {
        return canUseAiAbility(dragon, ModAbilities.RAEVYX_BITE, false)
                && startAiAbility(dragon, ModAbilities.RAEVYX_BITE, false, 20, 20, 0, 18);
    }

    private boolean tryStartRangedAttack(Raevyx dragon) {
        return canUseAiAbility(dragon, ModAbilities.RAEVYX_LIGHTNING_BEAM, true)
                && startAiAbility(dragon, ModAbilities.RAEVYX_LIGHTNING_BEAM, true, 12, 0, 40, 0);
    }

    private boolean tryStartRoar(Raevyx dragon) {
        return canUseAiAbility(dragon, ModAbilities.RAEVYX_ROAR, true)
                && startAiAbility(dragon, ModAbilities.RAEVYX_ROAR, true, 24, 70, 80, 32);
    }

    private boolean isCurrentlyAttacking(Raevyx dragon) {
        return dragon.isAbilityActive(ModAbilities.RAEVYX_BITE)
                || dragon.isAbilityActive(ModAbilities.RAEVYX_LIGHTNING_BEAM)
                || dragon.isAbilityActive(ModAbilities.RAEVYX_ROAR);
    }

    private boolean canUseAiAbility(Raevyx dragon,
                                    DragonAbilityType<?, ?> abilityType,
                                    boolean majorAbility) {
        return dragon.combatManager.canStartAiAbility(abilityType, majorAbility);
    }

    private boolean startAiAbility(Raevyx dragon,
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
        if (started && abilityType != ModAbilities.RAEVYX_LIGHTNING_BEAM
                && abilityType != ModAbilities.RAEVYX_ROAR) dragon.recordAiBeamFollowup();
        return started;
    }

    private boolean isFacingTarget(Raevyx dragon, LivingEntity target, double threshold) {
        Vec3 toTarget = targetCenter(target).subtract(dragon.getBoundingBox().getCenter());
        if (toTarget.lengthSqr() <= 1.0E-6D) {
            return true;
        }
        Vec3 look = Vec3.directionFromRotation(dragon.getXRot(), dragon.yHeadRot);
        return look.normalize().dot(toTarget.normalize()) >= threshold;
    }

    private boolean isTargetFleeing(Raevyx dragon, LivingEntity target) {
        Vec3 awayFromDragon = targetCenter(target).subtract(dragon.getBoundingBox().getCenter());
        if (!dragon.getCombatFlightState().targetNeedsFlight()) awayFromDragon = awayFromDragon.multiply(1, 0, 1);
        if (awayFromDragon.lengthSqr() <= 1.0E-6D) {
            return false;
        }
        return dragon.getCombatFlightState().targetVelocity().dot(awayFromDragon.normalize()) >= TARGET_FLEE_SPEED;
    }

    private boolean isBeingPursued(Raevyx dragon, LivingEntity target) {
        if (!dragon.getCombatFlightState().targetNeedsFlight() || dragon.distanceTo(target) > 24.0D) return false;
        Vec3 towardDragon = dragon.getBoundingBox().getCenter().subtract(targetCenter(target)).normalize();
        Vec3 velocity = dragon.getCombatFlightState().targetVelocity();
        return velocity.dot(towardDragon) > 0.15D
                && velocity.subtract(dragon.getDeltaMovement()).dot(towardDragon) > 0.10D;
    }

    private boolean shouldMakeBeamSpace(Raevyx dragon, LivingEntity target) {
        return dragon.tickCount >= beamEscapeRetryTick && dragon.isAiAirBeamReady()
                && dragon.tickCount >= beamRetryTick && !isCurrentlyAttacking(dragon)
                && canUseAiAbility(dragon, ModAbilities.RAEVYX_LIGHTNING_BEAM, true)
                && dragon.distanceTo(target) < CHASE_CONTAIN_RANGE && isBeingPursued(dragon, target);
    }

    private Vec3 predictTargetCenter(Raevyx dragon, LivingEntity target, double ticks, double maxLeadDistance) {
        Vec3 prediction = dragon.getCombatLearning().predictCenter(target, ticks, maxLeadDistance);
        if (prediction != null) return prediction;
        return dragon.getBrain().getMemory(DragonMemories.LAST_SEEN_TARGET)
                .filter(observation -> target.getUUID().equals(observation.sourceUuid()))
                .map(observation -> observation.position())
                .orElseGet(() -> dragon.getBoundingBox().getCenter());
    }

    private Vec3 targetCenter(LivingEntity target) {
        return DragonTargetingHelper.movementAnchor(target).getBoundingBox().getCenter();
    }

    private Vec3 flightFeet(Raevyx dragon, Vec3 center) {
        return center.add(0, -dragon.getBbHeight() * 0.5D, 0);
    }

    private Vec3 clampFlightY(Raevyx dragon, Vec3 target) {
        double minY = dragon.level().getMinBuildHeight() + 4.0D;
        double maxY = dragon.level().getMaxBuildHeight() - 4.0D;
        return new Vec3(target.x, Mth.clamp(target.y, minY, maxY), target.z);
    }

    private double maneuverDistance(Raevyx dragon, LivingEntity target) {
        Vec3 separation = target.position().subtract(dragon.position());
        return dragon.getCombatFlightState().targetNeedsFlight() ? separation.length() : separation.horizontalDistance();
    }

    private Vec3 groundAttackPosition(Raevyx dragon, LivingEntity target, Vec3 destination, double height) {
        if (dragon.getCombatFlightState().targetNeedsFlight()
                || DragonTargetingHelper.isMovementAnchorInWater(target)) return destination;
        var space = dragon.getAIMovement().flightSpace();
        var local = space.observe(dragon.position());
        if (local == null || !local.clear()) return destination;
        double ceiling = dragon.level().getMaxBuildHeight() - dragon.getBbHeight() - 2.0D;
        if (local.ceilingKnown()) ceiling = Math.min(ceiling, local.ceiling() - dragon.getBbHeight() - 2.0D);
        double wantedY = Math.min(target.getY() + height, ceiling);
        // Sample the destination column at our current height, so a cave roof cannot become the new floor.
        var ahead = space.observe(new Vec3(destination.x, dragon.getY(), destination.z));
        if (ahead != null && ahead.clear() && ahead.ceilingKnown()) {
            ceiling = Math.min(ceiling, ahead.ceiling() - dragon.getBbHeight() - 2.0D);
            wantedY = Math.min(wantedY, ceiling);
        }
        Vec3 fitted = space.fitDestination(new Vec3(destination.x, wantedY, destination.z));
        return fitted != null && fitted.y <= ceiling ? fitted : destination;
    }

    private Vec3 horizontalDirection(Vec3 direction, Vec3 fallback) {
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() <= 1.0E-6D) {
            horizontal = new Vec3(fallback.x, 0.0D, fallback.z);
        }
        return horizontal.lengthSqr() <= 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : horizontal.normalize();
    }

    private Vec3 direction(Vec3 direction, Vec3 fallback) {
        if (direction.lengthSqr() > 1.0E-6D) {
            return direction.normalize();
        }
        return fallback.lengthSqr() > 1.0E-6D
                ? fallback.normalize()
                : new Vec3(0.0D, 0.0D, 1.0D);
    }

    private void clearRouteState() {
        takeoffTarget = null;
        beamSetupTarget = null;
        beamSetupAnchor = null;
        beamApproachOffset = null;
        beamAlignmentTicks = 0;
        beamEscape = false;
        routeTarget = null;
        orbitAnchor = null;
        committedIntercept = null;
        runDirection = null;
        breakawayTarget = null;
        routeGraceTicks = 0;
    }

    @Override
    protected void stopAirCombat(DragonBrainContext<Raevyx> context) {
        context.dragon().getCombatAim().clear();
        beamAlignmentTicks = 0;
        phase = AirPhase.CHASE;
        phaseTicks = 0;
        chaseCaptureTicks = 0;
        minimumChaseTicks = CHASE_MINIMUM_TICKS;
        beamSegment = 0;
        orbitWaypointsCompleted = 0;
        roarEgressIssued = false;
        clearRouteState();
        lastDecision = "stopped";
        beamAvailability = "idle";
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>(super.getDragonBrainDebugDetails());
        details.put("air_phase", phase.name().toLowerCase());
        details.put("air_attack_height", Integer.toString(Mth.floor(attackHeight)));
        details.put("air_route_y", routeTarget == null ? "none" : Integer.toString(Mth.floor(routeTarget.y)));
        details.put("air_decision", lastDecision);
        details.put("air_phase_ticks", Integer.toString(phaseTicks));
        details.put("air_chase_capture", Integer.toString(chaseCaptureTicks));
        details.put("air_chase_minimum", Integer.toString(minimumChaseTicks));
        details.put("air_orbit_waypoints", Integer.toString(orbitWaypointsCompleted));
        details.put("air_side", attackSide > 0 ? "left" : "right");
        details.put("air_attack_cooldown", Integer.toString(attackCooldown));
        details.put("air_beam_cooldown", Integer.toString(rangedCooldown));
        details.put("air_beam_availability", beamAvailability);
        details.put("air_beam_alignment_ticks", Integer.toString(beamAlignmentTicks));
        details.put("air_roar_cooldown", Integer.toString(roarCooldown));
        details.put("air_beam_segment", Integer.toString(beamSegment));
        return Map.copyOf(details);
    }

    private enum AirPhase {
        CHASE,
        ORBIT,
        BEAM_SETUP,
        MELEE_COMMIT,
        MELEE_STRIKE,
        BEAM_PASS,
        ROAR_PASS,
        BREAK_AWAY,
        EVADE
    }
}
