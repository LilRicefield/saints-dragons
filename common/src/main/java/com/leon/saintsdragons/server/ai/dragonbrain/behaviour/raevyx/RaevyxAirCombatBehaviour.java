package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx;

import com.leon.saintsdragons.common.config.dragon.profile.RaevyxStatProfile;

import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonCombatDecisionSupport;
import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonSurfaceAttackFlight;
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
import java.util.UUID;

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
    private boolean waterCombat;
    private double waterSurface = Double.NaN;
    private int nextWaterObservationTick;
    private int waterContactUntil;
    private int nextWaterBiteTick;
    private int nextWaterRouteTick;
    private boolean waterRouteBlocked;
    private boolean waterPassEntered;
    private double waterBeamPassSpeed = BEAM_PASS_SPEED;
    @Nullable
    private UUID combatTargetId;

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
    protected boolean usesSharedSurfaceCombat() {
        return false;
    }

    @Override
    protected void startAirCombat(DragonBrainContext<Raevyx> context) {
        flightHandoff = context.dragon().getCombatFlightState().handoffRevision();
        attackHeight = GROUND_ATTACK_MIN_HEIGHT + context.dragon().getRandom().nextDouble() * GROUND_ATTACK_EXTRA_HEIGHT;
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        combatTargetId = target == null ? null : target.getUUID();
        waterCombat = target != null && DragonTargetingHelper.isMovementAnchorInWater(target);
        waterSurface = Double.NaN;
        nextWaterObservationTick = 0;
        waterContactUntil = context.dragon().tickCount + RaevyxStatProfile.WaterSurfaceCombat.SURFACE_RECHECK_TICKS;
        nextWaterBiteTick = Math.max(nextWaterBiteTick,
                context.dragon().tickCount + RaevyxStatProfile.WaterSurfaceCombat.BITE_INTERVAL_TICKS);
        waterRouteBlocked = false;
        nextWaterRouteTick = 0;
        if (waterCombat && context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false)) {
            observeWaterSurface(context.dragon(), target);
        }
        phase = !context.dragon().isTakeoff() && target != null && !context.dragon().getCombatFlightState().targetNeedsFlight()
                && !waterCombat
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
        boolean visible = context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false);
        if (!visible && (waterCombat || DragonTargetingHelper.isMovementAnchorInWater(target))) return;
        boolean targetInWater = DragonTargetingHelper.isMovementAnchorInWater(target);
        if (targetInWater) {
            waterContactUntil = dragon.tickCount + RaevyxStatProfile.WaterSurfaceCombat.SURFACE_RECHECK_TICKS;
        }
        boolean wantsWaterCombat = targetInWater
                || waterCombat && dragon.tickCount < waterContactUntil;
        if (flightHandoff != dragon.getCombatFlightState().handoffRevision()
                || !target.getUUID().equals(combatTargetId) || waterCombat != wantsWaterCombat) startAirCombat(context);
        if (waterCombat) {
            observeWaterSurface(dragon, target);
            if (waterRouteBlocked && dragon.tickCount < nextWaterRouteTick) return;
        }
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

        if (waterCombat && routeTarget != null && !waterRouteClear(dragon)) {
            enterBreakaway(context, target, "surface:route-obstructed");
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
                && phase != AirPhase.CHASE
                && !(waterCombat && phase == AirPhase.BREAK_AWAY)) {
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
            if (waterCombat) {
                enterBeamPass(context, target, "surface:resume-beam-pass");
                return;
            }
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
        if (waterCombat || phaseTicks >= BEAM_OPENING_TICKS) {
            if ((waterCombat || dragon.getCombatFlightState().targetNeedsFlight())
                    && tryBeamOpening(context, target, hasLineOfSight)) return;
            if (tryRoarOpening(context, target, hasLineOfSight)) return;
            if (!waterCombat && !dragon.getCombatFlightState().targetNeedsFlight()
                    && tryBeamOpening(context, target, hasLineOfSight)) return;
        }

        if (attackCooldown <= 0
                && !waterCombat
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
                && (!fleeing || (waterCombat ? maneuverDistance(dragon, target) : dragon.distanceTo(target)) <= CHASE_CONTAIN_RANGE)) {
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
        if (waterCombat) {
            if (routeTarget == null || phaseTicks % RaevyxStatProfile.WaterSurfaceCombat.SURFACE_RECHECK_TICKS == 0
                    || routeReached(dragon)) {
                Vec3 destination = waterAttackPosition(dragon, predictTargetCenter(dragon, target, 6, 12),
                        RaevyxStatProfile.WaterSurfaceCombat.ATTACK_HEIGHT);
                commandManeuver(context, destination, DIRECT_CHASE_SPEED);
            }
            return false;
        }
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

        if (waterCombat || phaseTicks >= BEAM_OPENING_TICKS) {
            if (waterCombat && tryBeamOpening(context, target, hasLineOfSight)) return;
            if (tryRoarOpening(context, target, hasLineOfSight)) return;
            if (!waterCombat && tryBeamOpening(context, target, hasLineOfSight)) return;
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
        if (waterCombat && tryBeamOpening(context, target, hasLineOfSight)) return;
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
        if (waterCombat && !surfaceBiteReachable(dragon, target)) {
            enterBreakaway(context, target, "surface:bite-target-submerged");
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
        if (waterCombat && (routeReached(dragon)
                || remainingAlongRun <= Math.max(2.0D, dragon.getDeltaMovement().length() * 2.0D)
                || dragon.getY() + Math.min(0, dragon.getDeltaMovement().y)
                        * RaevyxStatProfile.WaterSurfaceCombat.BITE_PULL_OUT_LEAD_TICKS
                        <= waterSurface + RaevyxStatProfile.WaterSurfaceCombat.SURFACE_CLEARANCE)) {
            enterMeleeStrike(context, "surface:bite-pull-out");
            return;
        }
        lastDecision = waterCombat ? "surface:bite-intercept" : "commit:intercept";
    }

    private void tickMeleeStrike(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        if (waterCombat && attackCooldown <= 0 && !isCurrentlyAttacking(dragon)
                && surfaceBiteReachable(dragon, target) && dragon.getSensing().hasLineOfSight(target)
                && dragon.distanceTo(target) <= MELEE_RANGE && isFacingTarget(dragon, target, 0.15D)
                && tryStartMeleeAttack(dragon)) {
            attackCooldown = MELEE_ATTACK_COOLDOWN_TICKS;
        }
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
        if (waterCombat) {
            if (routeFailed(dragon)) {
                enterBreakaway(context, target, "surface:beam-lane-blocked");
            } else if (routeReached(dragon) || dragon.getCombatAim().flightPassOverextended(target)
                    || phaseTicks >= BEAM_SETUP_TIMEOUT_TICKS
                    || phaseTicks >= 8 && !dragon.isAbilityActive(ModAbilities.RAEVYX_LIGHTNING_BEAM)) {
                enterBreakaway(context, target, "surface:beam-pass-complete");
            } else {
                lastDecision = "surface:beam-pass";
            }
            return;
        }
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
        lastDecision = "beam:steering-pass";
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
                .add(orbitDirection.scale(waterCombat ? RaevyxStatProfile.WaterSurfaceCombat.ATTACK_RADIUS : ORBIT_RADIUS))
                .add(0.0D, airPassHeight, 0.0D);
        orbitPosition = groundAttackPosition(dragon, target, orbitPosition, attackHeight);
        orbitAnchor = targetCenter(target);
        commandManeuver(context, orbitPosition, ORBIT_SPEED);
    }

    private void enterMeleeCommit(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        if (waterCombat && (dragon.tickCount < nextWaterBiteTick || !surfaceBiteReachable(dragon, target))) {
            enterBreakaway(context, target, "surface:make-attack-space");
            return;
        }
        dragon.getCombatFlightState().holdFlightFor(30);
        dragon.getCombatAim().clear();
        clearRouteState();
        phase = AirPhase.MELEE_COMMIT;
        phaseTicks = 0;
        committedIntercept = predictTargetCenter(dragon, target, 8.0D, 16.0D);
        if (waterCombat) {
            Vec3 contact = waterAttackPosition(dragon, committedIntercept,
                    RaevyxStatProfile.WaterSurfaceCombat.SURFACE_CLEARANCE);
            committedIntercept = contact.add(0, dragon.getBbHeight() * 0.5D, 0);
            nextWaterBiteTick = dragon.tickCount + RaevyxStatProfile.WaterSurfaceCombat.BITE_INTERVAL_TICKS;
        }
        runDirection = direction(
                committedIntercept.subtract(dragon.getBoundingBox().getCenter()),
                dragon.getLookAngle()
        );
        breakawayTarget = egressPosition(dragon, target, flightFeet(dragon, committedIntercept), runDirection);
        if (waterCombat) {
            var space = dragon.getAIMovement().flightSpace();
            Vec3 contact = flightFeet(dragon, committedIntercept);
            double reach = RaevyxStatProfile.WaterSurfaceCombat.BITE_RANGE;
            if (contact.distanceToSqr(target.position()) > reach * reach
                    || !space.fits(contact) || !space.fits(breakawayTarget)
                    || !space.corridorClear(dragon.position(), contact)
                    || !space.corridorClear(contact, breakawayTarget)) {
                enterBreakaway(context, target, "surface:no-clear-bite-pass");
                return;
            }
        }
        boolean dive = !waterCombat && shouldDiveChase(dragon, target, 7.0D, 42.0D);
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
        if (waterCombat) {
            if ((beamSetupTarget == null || breakawayTarget == null) && !selectWaterBeamLane(dragon, target)) {
                enterBreakaway(context, target, "surface:no-clear-beam-lane");
                return;
            }
            if (dragon.isAbilityActive(ModAbilities.RAEVYX_LIGHTNING_BEAM)) waterPassEntered = true;
            commandManeuver(context, waterPassEntered ? breakawayTarget : beamSetupTarget,
                    waterPassEntered ? waterBeamPassSpeed : ORBIT_SPEED);
            return;
        }
        if (dragon.isAbilityActive(ModAbilities.RAEVYX_LIGHTNING_BEAM)) {
            commandSteeringBeamPass(context, target);
            return;
        }
        if (dragon.getCombatFlightState().targetNeedsFlight()) {
            // Keep the firing lane moving with the opponent; a stationary setup position
            // otherwise forces us to brake while the opponent leaves beam range.
            Vec3 center = predictTargetCenter(dragon, target, 3.0D, 6.0D);
            Vec3 toTarget = center.subtract(dragon.getBoundingBox().getCenter());
            Vec3 motion = dragon.getCombatFlightState().targetVelocity();
            Vec3 course = horizontalDirection(motion.horizontalDistanceSqr() > 0.09D && motion.dot(toTarget) > 0
                    ? motion : toTarget, dragon.getLookAngle());
            Vec3 side = new Vec3(-course.z, 0, course.x).scale(attackSide * 4.0D);
            Vec3 destination = dragon.getAIMovement().flightSpace().fitDestination(
                    flightFeet(dragon, center).subtract(course.scale(28.0D)).add(side));
            if (destination == null) {
                commandChaseIntent(context, target);
                return;
            }
            boolean closing = motion.dot(course) > 0.6D && toTarget.dot(course) > 40.0D;
            double speed = closing ? DIRECT_CHASE_SPEED
                    : Mth.clamp((motion.horizontalDistance() + 0.35D) / Math.max(0.1D, dragon.getFlightSpeed()),
                            BEAM_PASS_SPEED, DIRECT_CHASE_SPEED);
            if (routeTarget == null) routeGraceTicks = 3;
            routeTarget = destination;
            beamSegment = dragon.isBeaming() ? 1 : 0;
            context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.flight(closing
                    ? DragonFlightRequest.chase(destination, speed)
                    : DragonFlightRequest.track(destination, speed, 2.0D)));
            return;
        }
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

    private void commandSteeringBeamPass(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        if (!dragon.getCombatLearning().hasVisibleObservation(target)) return;
        Vec3 targetFeet = flightFeet(dragon, predictTargetCenter(dragon, target, 3.0D, 6.0D));
        if (!dragon.getCombatFlightState().targetNeedsFlight()
                && !DragonTargetingHelper.isMovementAnchorInWater(target)) {
            targetFeet = new Vec3(targetFeet.x,
                    DragonTargetingHelper.movementAnchor(target).getY() + attackHeight * 0.75D, targetFeet.z);
        }
        Vec3 destination = dragon.getCombatAim().steerFlightPass(targetFeet, attackSide * 4.0D);
        Vec3 fitted = dragon.getAIMovement().flightSpace().fitDestination(destination);
        if (fitted == null) {
            commandChaseIntent(context, target);
            return;
        }
        double speed = dragon.getCombatAim().flightPassSpeed(targetFeet,
                dragon.getCombatFlightState().targetVelocity(), DIRECT_CHASE_SPEED,
                RaevyxStatProfile.BeamAbility.AI_AIR_FIRING_DISTANCE);
        if (routeTarget == null) routeGraceTicks = 3;
        routeTarget = fitted;
        beamSegment = dragon.isBeaming() ? 1 : 0;
        context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.flight(
                DragonFlightRequest.maneuver(fitted, speed, 2.0D, DragonFlightRequest.Arrival.PASS_THROUGH)));
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
        if (waterCombat) {
            var space = dragon.getAIMovement().flightSpace();
            for (float angle : new float[]{0, (float) Math.toRadians(60 * attackSide),
                    (float) Math.toRadians(-60 * attackSide)}) {
                Vec3 course = forward.yRot(angle);
                Vec3 exit = fitWaterDestination(dragon, egressPosition(dragon, target, dragon.position(), course));
                if (exit != null && space.corridorClear(dragon.position(), exit)) {
                    breakawayTarget = exit;
                    runDirection = course;
                    commandManeuver(context, exit, BREAKAWAY_SPEED);
                    return;
                }
            }
        }
        breakawayTarget = egressPosition(dragon, target, dragon.position(), forward);
        runDirection = forward;
        commandManeuver(context, breakawayTarget, BREAKAWAY_SPEED);
    }

    private Vec3 egressPosition(Raevyx dragon, LivingEntity target, Vec3 start, Vec3 forward) {
        Vec3 horizontal = horizontalDirection(forward, dragon.getLookAngle());
        Vec3 tangent = new Vec3(-horizontal.z, 0, horizontal.x).scale(attackSide);
        Vec3 destination = start.add(horizontal.scale(BREAKAWAY_DISTANCE))
                .add(tangent.scale(isBeingPursued(dragon, target) ? 14.0D : BREAKAWAY_LATERAL_OFFSET));
        if (waterCombat) {
            Vec3 aboveWater = waterAttackPosition(dragon, destination, RaevyxStatProfile.WaterSurfaceCombat.ATTACK_HEIGHT);
            return new Vec3(aboveWater.x, Math.max(dragon.getY(), aboveWater.y), aboveWater.z);
        }
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
        if (waterCombat) {
            Raevyx dragon = context.dragon();
            routeTarget = fitWaterDestination(dragon, routeTarget);
            waterRouteBlocked = routeTarget == null
                    || !dragon.getAIMovement().flightSpace().corridorClear(dragon.position(), routeTarget);
            if (waterRouteBlocked) {
                routeTarget = null;
                nextWaterRouteTick = dragon.tickCount + RaevyxStatProfile.WaterSurfaceCombat.SURFACE_RECHECK_TICKS;
                context.memories().set(DragonMemories.MOVEMENT_INTENT,
                        DragonMovementIntent.stop("raevyx-surface:no-clear-lane"));
                return;
            }
        }
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
        return waterCombat && waterRouteBlocked
                || routeTarget != null && routeGraceTicks <= 0 && dragon.getAIMovement().hasFailed();
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
        if (waterCombat ? !selectWaterBeamLane(dragon, target)
                : !dragon.getCombatFlightState().targetNeedsFlight() && !selectBeamPosition(dragon, target)) {
            deferBeamSetup(dragon);
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
        if (waterCombat) {
            tickWaterBeamSetup(context, target, hasLineOfSight);
            return;
        }
        Raevyx dragon = context.dragon();
        beamAvailability = beamBlockReason(dragon, target, hasLineOfSight);
        if ("ready".equals(beamAvailability)) {
            if (phaseTicks >= BEAM_SETUP_TIMEOUT_TICKS) beamAvailability = "alignment-timeout";
            else if (routeFailed(dragon)) beamAvailability = "setup-route-failed";
            else if (!dragon.getCombatFlightState().targetNeedsFlight()
                    && (beamSetupAnchor == null || targetCenter(target).distanceToSqr(beamSetupAnchor) > 144.0D)) {
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
            lastDecision = dragon.getCombatFlightState().targetNeedsFlight()
                    ? "beam:aligning-in-pursuit" : "beam:braking-to-align";
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
        deferBeamSetup(dragon);
        beamAlignmentTicks = 0;
        dragon.getCombatAim().clear();
        if (waterCombat) enterBreakaway(context, target, "surface:beam-setup-ended");
        else if (maneuverDistance(dragon, target) <= ORBIT_ABORT_RANGE) enterMeleeCommit(context, target);
        else enterChase(context, target, "chase:beam-setup-ended");
        lastDecision = reason + ":" + phase.name().toLowerCase();
    }

    private void deferBeamSetup(Raevyx dragon) {
        int retryTicks = waterCombat ? RaevyxStatProfile.WaterSurfaceCombat.BEAM_SETUP_RETRY_TICKS : 100;
        beamRetryTick = dragon.tickCount + retryTicks;
        dragon.getCombatFlightState().deferRangedFlightFor(retryTicks);
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

    private void observeWaterSurface(Raevyx dragon, LivingEntity target) {
        if (dragon.tickCount < nextWaterObservationTick) return;
        nextWaterObservationTick = dragon.tickCount + RaevyxStatProfile.WaterSurfaceCombat.SURFACE_RECHECK_TICKS;
        waterSurface = DragonSurfaceAttackFlight.surfaceHeight(dragon.level(),
                DragonTargetingHelper.movementAnchor(target).blockPosition());
    }

    private Vec3 waterAttackPosition(Raevyx dragon, Vec3 position, double height) {
        double y = Double.isFinite(waterSurface) ? waterSurface + height : dragon.getY();
        return new Vec3(position.x, y, position.z);
    }

    private @Nullable Vec3 fitWaterDestination(Raevyx dragon, Vec3 position) {
        double floor = Double.isFinite(waterSurface)
                ? waterSurface + RaevyxStatProfile.WaterSurfaceCombat.SURFACE_CLEARANCE : dragon.getY();
        Vec3 fitted = dragon.getAIMovement().flightSpace().fitDestination(
                new Vec3(position.x, Math.max(floor, position.y), position.z));
        return fitted != null && fitted.y >= floor ? fitted : null;
    }

    private boolean waterRouteClear(Raevyx dragon) {
        if (routeTarget == null) return false;
        if (dragon.tickCount % RaevyxStatProfile.WaterSurfaceCombat.SURFACE_RECHECK_TICKS != 0) return true;
        return (!Double.isFinite(waterSurface)
                || routeTarget.y >= waterSurface + RaevyxStatProfile.WaterSurfaceCombat.SURFACE_CLEARANCE)
                && dragon.getAIMovement().flightSpace().corridorClear(dragon.position(), routeTarget);
    }

    private boolean surfaceBiteReachable(Raevyx dragon, LivingEntity target) {
        return Double.isFinite(waterSurface) && target.getEyeY() >= waterSurface - 1.0D
                && dragon.getCombatLearning().hasVisibleObservation(target);
    }

    private boolean selectWaterBeamLane(Raevyx dragon, LivingEntity target) {
        Vec3 center = waterAttackPosition(dragon, predictTargetCenter(dragon, target, 4, 8),
                RaevyxStatProfile.WaterSurfaceCombat.ATTACK_HEIGHT);
        Vec3 course = horizontalDirection(center.subtract(dragon.position()), dragon.getLookAngle());
        Vec3 lateral = new Vec3(-course.z, 0, course.x);
        double sideOffset = RaevyxStatProfile.WaterSurfaceCombat.BEAM_PASS_SIDE_OFFSET;
        double height = center.y - target.getY();
        double rangeAlongRun = Math.sqrt(Math.max(0, RANGED_MAX_RANGE * RANGED_MAX_RANGE
                - height * height - sideOffset * sideOffset)) - Math.sqrt(ROUTE_ARRIVAL_DISTANCE_SQR);
        if (rangeAlongRun < RaevyxStatProfile.WaterSurfaceCombat.ATTACK_RADIUS) return false;
        int firingTicks = RaevyxBeamAbility.STARTUP_TICKS + RaevyxStatProfile.WaterSurfaceCombat.BEAM_FIRING_WINDOW_TICKS;
        double passSpeed = Math.min(dragon.getFlightSpeed() * BEAM_PASS_SPEED, rangeAlongRun / firingTicks);
        double runIn = Math.max(RaevyxStatProfile.WaterSurfaceCombat.ATTACK_RADIUS, passSpeed * firingTicks);
        var space = dragon.getAIMovement().flightSpace();
        for (int side : new int[]{attackSide, -attackSide}) {
            Vec3 offset = lateral.scale(side * sideOffset);
            Vec3 entry = fitWaterDestination(dragon, center.subtract(course.scale(runIn)).add(offset));
            Vec3 exit = fitWaterDestination(dragon, center.add(course.scale(
                    RaevyxStatProfile.WaterSurfaceCombat.PASS_LENGTH)).add(offset));
            if (entry == null || exit == null || !space.corridorClear(dragon.position(), entry)
                    || !space.corridorClear(entry, exit)) continue;
            beamSetupTarget = entry;
            beamSetupAnchor = targetCenter(target);
            breakawayTarget = exit;
            runDirection = course;
            waterPassEntered = false;
            waterBeamPassSpeed = passSpeed / Math.max(0.01D, dragon.getFlightSpeed());
            attackSide = side;
            return true;
        }
        return false;
    }

    private void tickWaterBeamSetup(DragonBrainContext<Raevyx> context, LivingEntity target, boolean hasLineOfSight) {
        Raevyx dragon = context.dragon();
        beamAvailability = beamBlockReason(dragon, target, hasLineOfSight);
        boolean enteringLane = !waterPassEntered || !isFollowingWaterBeamLane(dragon);
        boolean adjustingRange = enteringLane && ("too-close".equals(beamAvailability) || "too-far".equals(beamAvailability));
        if ("ready".equals(beamAvailability) || adjustingRange) {
            if (phaseTicks >= RaevyxStatProfile.WaterSurfaceCombat.BEAM_SETUP_TIMEOUT_TICKS) beamAvailability = "alignment-timeout";
            else if (routeFailed(dragon)) beamAvailability = "setup-route-failed";
            else if (beamSetupAnchor == null
                    || targetCenter(target).distanceToSqr(beamSetupAnchor) > ORBIT_RETARGET_DISTANCE_SQR) {
                beamAvailability = "target-left-setup";
            }
        }
        if (!"ready".equals(beamAvailability)
                && !(enteringLane && ("too-close".equals(beamAvailability) || "too-far".equals(beamAvailability)))) {
            abandonBeamSetup(context, target, "surface:beam-setup-" + beamAvailability);
            return;
        }
        if (routeTarget == null) commandBeamApproach(context, target);
        if (waterRouteBlocked) return;
        if (!waterPassEntered && routeReached(dragon)) {
            waterPassEntered = true;
            commandBeamApproach(context, target);
        }
        boolean followingLane = waterPassEntered && isFollowingWaterBeamLane(dragon);
        if (waterPassEntered && (routeReached(dragon)
                || followingLane && dragon.getCombatAim().flightPassOverextended(target))) {
            abandonBeamSetup(context, target, "surface:beam-window-passed");
            return;
        }
        DragonCombatAim.Shot shot = dragon.getAiBeamShot(target, RANGED_MAX_RANGE);
        if (followingLane && shot != DragonCombatAim.Shot.ALIGNED && !shot.needsAlignment()) {
            abandonBeamSetup(context, target, "surface:beam-shot-" + shot.name().toLowerCase());
            return;
        }
        if (followingLane && "ready".equals(beamAvailability)
                && shot == DragonCombatAim.Shot.ALIGNED && dragon.getCombatAim().ready(3)
                && tryStartRangedAttack(dragon)) {
            beamAlignmentTicks = 0;
            attackCooldown = BEAM_ATTACK_COOLDOWN_TICKS;
            enterBeamPass(context, target, "surface:beam-aligned-pass");
        } else {
            beamAlignmentTicks++;
            lastDecision = followingLane ? "surface:beam-aligning-pass"
                    : waterPassEntered ? "surface:beam-turn-in" : "surface:beam-run-in";
        }
    }

    private boolean isFollowingWaterBeamLane(Raevyx dragon) {
        Vec3 motion = dragon.getDeltaMovement().multiply(1, 0, 1);
        return runDirection != null && motion.lengthSqr() > 0.01D
                && motion.normalize().dot(runDirection) > 0.5D;
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
        if (waterCombat || !dragon.getCombatFlightState().targetNeedsFlight()) awayFromDragon = awayFromDragon.multiply(1, 0, 1);
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
        return !waterCombat && dragon.getCombatFlightState().targetNeedsFlight() ? separation.length() : separation.horizontalDistance();
    }

    private Vec3 groundAttackPosition(Raevyx dragon, LivingEntity target, Vec3 destination, double height) {
        if (waterCombat) return waterAttackPosition(dragon, destination, RaevyxStatProfile.WaterSurfaceCombat.ATTACK_HEIGHT);
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
        waterPassEntered = false;
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
        waterCombat = false;
        waterSurface = Double.NaN;
        combatTargetId = null;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>(super.getDragonBrainDebugDetails());
        details.put("air_phase", phase.name().toLowerCase());
        details.put("surface_combat", waterCombat ? "raevyx:" + phase.name().toLowerCase() : "inactive");
        details.put("air_attack_height", Integer.toString(Mth.floor(
                waterCombat ? RaevyxStatProfile.WaterSurfaceCombat.ATTACK_HEIGHT : attackHeight)));
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
