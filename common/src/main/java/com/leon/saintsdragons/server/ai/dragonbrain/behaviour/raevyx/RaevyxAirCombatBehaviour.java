package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.raevyx;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AirCombatMovementBehaviour;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxBeamAbility;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RaevyxAirCombatBehaviour extends AirCombatMovementBehaviour<Raevyx> {
    private static final double MELEE_RANGE = 7.0D;
    private static final double RANGED_MIN_RANGE = 20.0D;
    private static final double RANGED_MAX_RANGE = 70.0D;
    private static final double ROAR_MIN_RANGE = 6.0D;
    private static final double ROAR_MAX_RANGE = 40.0D;
    private static final double CHASE_CONTAIN_RANGE = 18.0D;
    private static final double ORBIT_ABORT_RANGE = 32.0D;
    private static final double TACTICAL_ESCAPE_RANGE = 46.0D;
    private static final double ORBIT_RADIUS = 22.0D;
    private static final double ORBIT_HEIGHT_OFFSET = 4.0D;
    private static final double ORBIT_STEP_RADIANS = Math.toRadians(58.0D);
    private static final double TARGET_FLEE_SPEED = 0.10D;
    private static final double BREAKAWAY_DISTANCE = 30.0D;
    private static final double BREAKAWAY_LATERAL_OFFSET = 6.0D;
    private static final double BREAKAWAY_CLIMB = 8.0D;
    private static final double ORBIT_SPEED = 4.0D;
    private static final double DIRECT_CHASE_SPEED = 7.0D;
    private static final double DIVE_CHASE_SPEED = 8.0D;
    private static final double DIRECT_COMMIT_SPEED = 6.0D;
    private static final double DIVE_COMMIT_SPEED = 7.5D;
    private static final double BEAM_PASS_SPEED = 4.5D;
    private static final double ROAR_PASS_SPEED = 4.75D;
    private static final double BREAKAWAY_SPEED = 6.5D;
    private static final double ROUTE_ARRIVAL_DISTANCE_SQR = 25.0D;
    private static final double ORBIT_RETARGET_DISTANCE_SQR = 100.0D;
    private static final double COMMIT_ABORT_DISTANCE_SQR = 324.0D;
    private static final double BREAKAWAY_CLEAR_DISTANCE_SQR = 34.0D * 34.0D;
    private static final int MELEE_ATTACK_COOLDOWN_TICKS = 20;
    private static final int BEAM_ATTACK_COOLDOWN_TICKS = 60;
    private static final int BEAM_COOLDOWN_TICKS = 2400;
    private static final int ROAR_ATTACK_COOLDOWN_TICKS = 24;
    private static final int ROAR_COOLDOWN_TICKS = 160;
    private static final int CHASE_CAPTURE_TICKS = 12;
    private static final int CHASE_MINIMUM_TICKS = 12;
    private static final int ORBIT_MINIMUM_TICKS = 20;
    private static final int ORBIT_MAXIMUM_TICKS = 80;
    private static final int ORBIT_WAYPOINTS_REQUIRED = 2;
    private static final int ORBIT_RETARGET_INTERVAL_TICKS = 10;
    private static final int SHOT_FROM_BELOW_THRESHOLD = 3;

    private AirPhase phase = AirPhase.CHASE;
    private int phaseTicks;
    private int chaseCaptureTicks;
    private int minimumChaseTicks;
    private int attackCooldown;
    private int rangedCooldown;
    private int roarCooldown;
    private int routeGraceTicks;
    private int attackSide = 1;
    private int beamSegment;
    private int orbitWaypointsCompleted;
    private boolean roarEgressIssued;
    private int shotFromBelowCounter;
    private long lastDamageTick;
    private String lastDecision = "idle";

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
    @Nullable
    private Vec3 beamRadial;
    @Nullable
    private Vec3 beamTangent;

    @Override
    protected void startAirCombat(DragonBrainContext<Raevyx> context) {
        phase = AirPhase.CHASE;
        phaseTicks = 0;
        chaseCaptureTicks = 0;
        minimumChaseTicks = CHASE_MINIMUM_TICKS;
        attackCooldown = 0;
        routeGraceTicks = 0;
        attackSide = context.dragon().getRandom().nextBoolean() ? 1 : -1;
        beamSegment = 0;
        orbitWaypointsCompleted = 0;
        roarEgressIssued = false;
        clearRouteState();
        lastDecision = "chase:engage";
    }

    @Override
    protected void tickAirCombat(DragonBrainContext<Raevyx> context,
                                 LivingEntity target,
                                 boolean hasLineOfSight) {
        tickCooldowns();
        Raevyx dragon = context.dragon();
        dragon.getLookControl().setLookAt(target, 100.0F, 100.0F);

        if (checkEmergencyLanding(context, target)) {
            lastDecision = "landing:shot-from-below";
            return;
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

        if (phase != AirPhase.CHASE
                && dragon.distanceTo(target) > TACTICAL_ESCAPE_RANGE) {
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

        if (dragon.isTakeoff()) {
            chaseCaptureTicks = 0;
            lastDecision = "chase:takeoff-pursuit";
            return;
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

        if (hasLineOfSight
                && !fleeing
                && dragon.distanceTo(target) <= CHASE_CONTAIN_RANGE) {
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

    private boolean commandChaseIntent(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        boolean dive = shouldDiveChase(dragon, target, 7.0D, 42.0D);
        if (dive) {
            setPredictedChaseIntent(
                    context,
                    target,
                    3.0D,
                    -0.25D,
                    0.08D,
                    0.12D,
                    DIVE_CHASE_SPEED
            );
        } else {
            setPredictedChaseIntent(
                    context,
                    target,
                    6.0D,
                    1.5D,
                    0.08D,
                    0.15D,
                    DIRECT_CHASE_SPEED
            );
        }
        return dive;
    }

    private void tickOrbit(DragonBrainContext<Raevyx> context,
                           LivingEntity target,
                           boolean hasLineOfSight) {
        Raevyx dragon = context.dragon();
        if (dragon.distanceTo(target) > ORBIT_ABORT_RANGE || isTargetFleeing(dragon, target)) {
            enterChase(context, target, "chase:orbit-broken");
            return;
        }
        if (routeFailed(dragon)) {
            enterChase(context, target, "chase:orbit-route-failed");
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
            if (phaseTicks < ORBIT_MINIMUM_TICKS
                    || orbitWaypointsCompleted < ORBIT_WAYPOINTS_REQUIRED) {
                commandOrbitRoute(context, target);
            }
        }

        boolean orbitEstablished = phaseTicks >= ORBIT_MINIMUM_TICKS
                && orbitWaypointsCompleted >= ORBIT_WAYPOINTS_REQUIRED;
        if (!orbitEstablished) {
            if (phaseTicks >= ORBIT_MAXIMUM_TICKS) {
                enterChase(context, target, "chase:orbit-timeout");
                return;
            }
            lastDecision = targetShifted ? "orbit:tracking-shift" : "orbit:circling";
            return;
        }

        double distance = dragon.distanceTo(target);
        if (isCurrentlyAttacking(dragon)) {
            if (routeReached(dragon)) {
                commandOrbitRoute(context, target);
            }
            lastDecision = "orbit:ability-active";
            return;
        }
        if (attackCooldown <= 0
                && roarCooldown <= 0
                && hasLineOfSight
                && distance >= ROAR_MIN_RANGE
                && distance <= ROAR_MAX_RANGE
                && !DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)
                && tryStartRoar(dragon)) {
            attackCooldown = ROAR_ATTACK_COOLDOWN_TICKS;
            roarCooldown = ROAR_COOLDOWN_TICKS;
            enterRoarPass(context, target);
            return;
        }
        if (attackCooldown <= 0
                && rangedCooldown <= 0
                && hasLineOfSight
                && distance >= RANGED_MIN_RANGE
                && distance <= RANGED_MAX_RANGE
                && canUseRangedAttack(dragon, target)
                && tryStartRangedAttack(dragon)) {
            attackCooldown = BEAM_ATTACK_COOLDOWN_TICKS;
            rangedCooldown = BEAM_COOLDOWN_TICKS;
            enterBeamPass(context, target, "beam:started-pass");
            return;
        }
        enterMeleeCommit(context, target);
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
            commandStrict(context, breakawayTarget, BREAKAWAY_SPEED);
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
            commandBeamSegment(context, target, 2);
            lastDecision = "beam:route-recovery";
            return;
        }
        if (routeReached(dragon) && beamSegment < 2) {
            commandBeamSegment(context, target, beamSegment + 1);
        }
        if (phaseTicks >= 110) {
            enterBreakaway(context, target, "breakaway:beam-timeout");
            return;
        }
        lastDecision = "beam:strafing-" + beamSegment;
    }

    private void tickRoarPass(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        if (routeFailed(dragon)) {
            enterBreakaway(context, target, "breakaway:roar-route-failed");
            return;
        }
        if (!roarEgressIssued && routeReached(dragon) && breakawayTarget != null) {
            commandStrict(context, breakawayTarget, BREAKAWAY_SPEED);
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

        boolean clear = dragon.distanceToSqr(target) >= BREAKAWAY_CLEAR_DISTANCE_SQR;
        if (phaseTicks >= 14 && (clear || routeReached(dragon) || phaseTicks >= 60)) {
            attackSide = -attackSide;
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
        Vec3 targetPosition = predictTargetCenter(target, 4.0D, 8.0D);
        Vec3 radial = horizontalDirection(
                dragon.getBoundingBox().getCenter().subtract(targetPosition),
                dragon.getLookAngle()
        );
        Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x).scale(attackSide);
        Vec3 orbitDirection = radial.scale(Math.cos(ORBIT_STEP_RADIANS))
                .add(tangent.scale(Math.sin(ORBIT_STEP_RADIANS)))
                .normalize();
        Vec3 orbitPosition = targetPosition
                .add(orbitDirection.scale(ORBIT_RADIUS))
                .add(0.0D, ORBIT_HEIGHT_OFFSET, 0.0D);
        orbitAnchor = targetCenter(target);
        commandStrict(context, orbitPosition, ORBIT_SPEED);
    }

    private void enterMeleeCommit(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        phase = AirPhase.MELEE_COMMIT;
        phaseTicks = 0;
        committedIntercept = clampFlightY(dragon, predictTargetCenter(target, 8.0D, 16.0D));
        runDirection = direction(
                committedIntercept.subtract(dragon.getBoundingBox().getCenter()),
                dragon.getLookAngle()
        );
        Vec3 horizontalRun = horizontalDirection(runDirection, dragon.getLookAngle());
        Vec3 tangent = new Vec3(-horizontalRun.z, 0.0D, horizontalRun.x).scale(attackSide);
        breakawayTarget = clampFlightY(
                dragon,
                committedIntercept
                        .add(runDirection.scale(BREAKAWAY_DISTANCE))
                        .add(tangent.scale(BREAKAWAY_LATERAL_OFFSET))
                        .add(0.0D, BREAKAWAY_CLIMB, 0.0D)
        );
        double speed = shouldDiveChase(dragon, target, 7.0D, 42.0D)
                ? DIVE_COMMIT_SPEED
                : DIRECT_COMMIT_SPEED;
        commandStrict(context, committedIntercept, speed);
        lastDecision = speed == DIVE_COMMIT_SPEED ? "commit:dive" : "commit:direct";
    }

    private void enterMeleeStrike(DragonBrainContext<Raevyx> context, String decision) {
        phase = AirPhase.MELEE_STRIKE;
        phaseTicks = 0;
        routeTarget = null;
        if (breakawayTarget != null) {
            commandStrict(context, breakawayTarget, BREAKAWAY_SPEED);
        }
        lastDecision = decision;
    }

    private void enterBeamPass(DragonBrainContext<Raevyx> context,
                               LivingEntity target,
                               String decision) {
        Raevyx dragon = context.dragon();
        phase = AirPhase.BEAM_PASS;
        phaseTicks = 0;
        Vec3 center = targetCenter(target);
        beamRadial = horizontalDirection(
                dragon.getBoundingBox().getCenter().subtract(center),
                dragon.getLookAngle()
        );
        beamTangent = new Vec3(-beamRadial.z, 0.0D, beamRadial.x).scale(attackSide);
        commandBeamSegment(context, target, 0);
        lastDecision = decision;
    }

    private void enterRoarPass(DragonBrainContext<Raevyx> context, LivingEntity target) {
        Raevyx dragon = context.dragon();
        Vec3 dragonCenter = dragon.getBoundingBox().getCenter();
        Vec3 center = predictTargetCenter(target, 4.0D, 10.0D);
        Vec3 radial = horizontalDirection(dragonCenter.subtract(center), dragon.getLookAngle());
        Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x).scale(attackSide);
        Vec3 passTarget = clampFlightY(
                dragon,
                center.add(radial.scale(-22.0D))
                        .add(tangent.scale(12.0D))
                        .add(0.0D, 5.0D, 0.0D)
        );

        phase = AirPhase.ROAR_PASS;
        phaseTicks = 0;
        clearRouteState();
        runDirection = direction(passTarget.subtract(dragonCenter), dragon.getLookAngle());
        breakawayTarget = clampFlightY(
                dragon,
                passTarget.add(runDirection.scale(24.0D)).add(0.0D, 6.0D, 0.0D)
        );
        roarEgressIssued = false;
        commandStrict(context, passTarget, ROAR_PASS_SPEED);
        lastDecision = "roar:started-flyby";
    }

    private void commandBeamSegment(DragonBrainContext<Raevyx> context,
                                    LivingEntity target,
                                    int segment) {
        Raevyx dragon = context.dragon();
        if (beamRadial == null || beamTangent == null) {
            Vec3 center = targetCenter(target);
            beamRadial = horizontalDirection(
                    dragon.getBoundingBox().getCenter().subtract(center),
                    dragon.getLookAngle()
            );
            beamTangent = new Vec3(-beamRadial.z, 0.0D, beamRadial.x).scale(attackSide);
        }
        Vec3 center = predictTargetCenter(target, 4.0D, 10.0D);
        Vec3 destination = switch (segment) {
            case 0 -> center.add(beamTangent.scale(30.0D))
                    .add(beamRadial.scale(-6.0D))
                    .add(0.0D, 4.0D, 0.0D);
            case 1 -> center.add(beamRadial.scale(-28.0D))
                    .add(beamTangent.scale(-10.0D))
                    .add(0.0D, 6.0D, 0.0D);
            default -> center.add(beamRadial.scale(35.0D))
                    .add(beamTangent.scale(-16.0D))
                    .add(0.0D, 10.0D, 0.0D);
        };
        beamSegment = Math.min(2, segment);
        commandStrict(context, destination, beamSegment == 2 ? BREAKAWAY_SPEED : BEAM_PASS_SPEED);
    }

    private void enterBreakaway(DragonBrainContext<Raevyx> context,
                                LivingEntity target,
                                String decision) {
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
        forward = direction(forward, dragon.getLookAngle());
        Vec3 horizontalForward = horizontalDirection(forward, dragon.getLookAngle());
        Vec3 tangent = new Vec3(-horizontalForward.z, 0.0D, horizontalForward.x).scale(attackSide);
        breakawayTarget = clampFlightY(
                dragon,
                dragonCenter
                        .add(forward.scale(BREAKAWAY_DISTANCE))
                        .add(tangent.scale(BREAKAWAY_LATERAL_OFFSET))
                        .add(0.0D, BREAKAWAY_CLIMB, 0.0D)
        );
        runDirection = forward;
        commandStrict(context, breakawayTarget, BREAKAWAY_SPEED);
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

    private void commandStrict(DragonBrainContext<Raevyx> context, Vec3 target, double speed) {
        routeTarget = clampFlightY(context.dragon(), target);
        routeGraceTicks = 3;
        context.memories().set(
                DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.strictAir(routeTarget, speed)
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

    private void tickCooldowns() {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (rangedCooldown > 0) {
            rangedCooldown--;
        }
        if (roarCooldown > 0) {
            roarCooldown--;
        }
        if (routeGraceTicks > 0) {
            routeGraceTicks--;
        }
    }

    private boolean checkEmergencyLanding(DragonBrainContext<Raevyx> context, LivingEntity target) {
        if (context.memories().get(DragonMemories.GROUND_ROUTE_ABANDONED).orElse(false)) {
            shotFromBelowCounter = 0;
            return false;
        }
        Raevyx dragon = context.dragon();
        long currentTick = dragon.level().getGameTime();
        if (dragon.hurtTime > 0 && currentTick != lastDamageTick) {
            lastDamageTick = currentTick;
            if (target.getY() < dragon.getY() - 5.0D
                    && ++shotFromBelowCounter >= SHOT_FROM_BELOW_THRESHOLD) {
                context.memories().set(
                        DragonMemories.MOVEMENT_INTENT,
                        DragonMovementIntent.transitionToGround(
                                target,
                                dragon.getAiAirCombatSettings().landingSpeed()
                        )
                );
                shotFromBelowCounter = 0;
                return true;
            }
        }
        if (currentTick - lastDamageTick > 100L) {
            shotFromBelowCounter = 0;
        }
        return false;
    }

    private boolean canUseRangedAttack(Raevyx dragon, LivingEntity target) {
        return !DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)
                && !RaevyxBeamAbility.isAtAiBeamMercyThreshold(target);
    }

    private boolean tryStartMeleeAttack(Raevyx dragon) {
        return canUseAiAbility(dragon, ModAbilities.RAEVYX_BITE, false)
                && startAiAbility(dragon, ModAbilities.RAEVYX_BITE, false, 20, 20, 0, 18);
    }

    private boolean tryStartRangedAttack(Raevyx dragon) {
        return canUseAiAbility(dragon, ModAbilities.RAEVYX_LIGHTNING_BEAM, true)
                && startAiAbility(dragon, ModAbilities.RAEVYX_LIGHTNING_BEAM, true, 60, 2400, 160, 80);
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
        if (awayFromDragon.lengthSqr() <= 1.0E-6D) {
            return false;
        }
        return target.getDeltaMovement().dot(awayFromDragon.normalize()) >= TARGET_FLEE_SPEED;
    }

    private Vec3 predictTargetCenter(LivingEntity target, double ticks, double maxLeadDistance) {
        Vec3 center = targetCenter(target);
        Vec3 lead = target.getDeltaMovement().scale(ticks);
        if (lead.lengthSqr() > maxLeadDistance * maxLeadDistance) {
            lead = lead.normalize().scale(maxLeadDistance);
        }
        return center.add(lead);
    }

    private Vec3 targetCenter(LivingEntity target) {
        return target.getBoundingBox().getCenter();
    }

    private Vec3 clampFlightY(Raevyx dragon, Vec3 target) {
        double minY = dragon.level().getMinBuildHeight() + 4.0D;
        double maxY = dragon.level().getMaxBuildHeight() - 4.0D;
        return new Vec3(target.x, Mth.clamp(target.y, minY, maxY), target.z);
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
        routeTarget = null;
        orbitAnchor = null;
        committedIntercept = null;
        runDirection = null;
        breakawayTarget = null;
        beamRadial = null;
        beamTangent = null;
        routeGraceTicks = 0;
    }

    @Override
    protected void stopAirCombat(DragonBrainContext<Raevyx> context) {
        phase = AirPhase.CHASE;
        phaseTicks = 0;
        chaseCaptureTicks = 0;
        minimumChaseTicks = CHASE_MINIMUM_TICKS;
        attackCooldown = 0;
        beamSegment = 0;
        orbitWaypointsCompleted = 0;
        roarEgressIssued = false;
        clearRouteState();
        lastDecision = "stopped";
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("air_phase", phase.name().toLowerCase());
        details.put("air_decision", lastDecision);
        details.put("air_phase_ticks", Integer.toString(phaseTicks));
        details.put("air_chase_capture", Integer.toString(chaseCaptureTicks));
        details.put("air_chase_minimum", Integer.toString(minimumChaseTicks));
        details.put("air_orbit_waypoints", Integer.toString(orbitWaypointsCompleted));
        details.put("air_side", attackSide > 0 ? "left" : "right");
        details.put("air_attack_cooldown", Integer.toString(attackCooldown));
        details.put("air_beam_cooldown", Integer.toString(rangedCooldown));
        details.put("air_roar_cooldown", Integer.toString(roarCooldown));
        details.put("air_beam_segment", Integer.toString(beamSegment));
        return Map.copyOf(details);
    }

    private enum AirPhase {
        CHASE,
        ORBIT,
        MELEE_COMMIT,
        MELEE_STRIKE,
        BEAM_PASS,
        ROAR_PASS,
        BREAK_AWAY,
        EVADE
    }
}
