package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.DragonAirCombatHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourInterruption;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonOwnerFollowTarget;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonOwnerTeleport;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import com.leon.saintsdragons.server.ai.navigation.async.DragonFlightSpace;
import com.leon.saintsdragons.server.ai.navigation.async.DragonFlightRequest;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public final class DragonFollowOwnerBehaviour<T extends RideableFlyingDragon> extends DragonBehaviour<T> {
    private static final double AIR_CATCH_UP_DISTANCE = 18.0D;
    private static final double AIR_CATCH_UP_MULTIPLIER = 1.35D;
    private static final double OWNER_AIRBORNE_CLEARANCE = 4.0D;
    private static final int FAILED_GROUND_PATH_RETRY_TICKS = 10;
    private static final int GROUND_TRIAL_TICKS = 40;
    private static final int GROUND_STALL_TICKS = 80;
    private static final int OUTPACED_TICKS = 80;
    private static final int LANDING_SEARCH_INTERVAL = 20;
    private static final int LANDING_STALL_TICKS = 100;
    private static final double LANDING_MAX_VERTICAL_DELTA = 4.0D;
    private static final Config BABY_CONFIG = new Config(
            DragonBabyOwnerFollowTuning.START_DISTANCE,
            DragonBabyOwnerFollowTuning.STOP_DISTANCE,
            DragonBabyOwnerFollowTuning.TELEPORT_DISTANCE,
            DragonBabyOwnerFollowTuning.RUN_DISTANCE,
            24.0D, 10.0D, 2.5D,
            DragonBabyOwnerFollowTuning.WALK_SPEED,
            DragonBabyOwnerFollowTuning.RUN_SPEED,
            DragonBabyOwnerFollowTuning.MAX_WALK_SPEED,
            DragonBabyOwnerFollowTuning.MAX_RUN_SPEED,
            4.0D
    );

    private final Config adultConfig;
    private final Consumer<T> takeoffStarter;
    private final DragonOwnerFollowWaterHandoff waterHandoff = new DragonOwnerFollowWaterHandoff();
    private int groundRepathCooldown;
    private boolean retryingGround;
    private int groundFailures;
    private long groundSince;
    private long groundSampleAt;
    private Vec3 groundSamplePosition;
    private Vec3 groundSampleOwner;
    private double groundSampleGap;
    private int stalledTicks;
    private int outpacedTicks;
    private boolean wasAerial;
    private boolean finishingOnFoot;
    private UUID ownerAnchorId;
    private long nextOwnerObservation;
    private long ownerAirborneSince = -1;
    private long ownerGroundedSince = -1;
    private boolean ownerAirborne;
    private Vec3 observedOwnerPosition;
    private long observedOwnerAt;
    private double ownerHorizontalSpeed;
    private Vec3 landingTarget;
    private Vec3 landingProgressPosition;
    private long landingProgressAt;
    private long nextLandingValidation;
    private long nextLandingSearch;
    private Vec3 landingSetupTarget;
    private Vec3 landingSetupOwner;
    private long landingSetupAt;
    private int landingSetupAttempt;
    private Vec3 lastAirTarget;
    private Vec3 lastGroundTarget;
    private String mode = "idle";
    private String decision = "idle";

    public DragonFollowOwnerBehaviour(Config config, Consumer<T> takeoffStarter) {
        super(Map.of(DragonMemories.MOVEMENT_INTENT, MemoryStatus.REGISTERED));
        this.adultConfig = Objects.requireNonNull(config);
        this.takeoffStarter = Objects.requireNonNull(takeoffStarter);
    }

    @Override
    public DragonBehaviourInterruption interruptionType() {
        return DragonBehaviourInterruption.OWNER_FOLLOW;
    }

    @Override
    public boolean requestsInterruption(DragonBrainContext<T> context) {
        return canStart(context);
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        if (!canFollow(dragon, owner)) return false;
        Config config = configFor(dragon);
        // A nearby flying follower still needs to finish landing beside a grounded owner.
        if (dragon.isAerial() && !isOwnerAirborne(owner)) return true;
        if (!dragon.isBaby() && isOwnerAirborne(owner)
                && Math.abs(DragonOwnerFollowTarget.anchorPosition(owner).y - dragon.getY()) > OWNER_AIRBORNE_CLEARANCE) {
            return true;
        }
        double startDistanceSqr = dragon.isAerial()
                ? DragonOwnerFollowTarget.startDistanceSqr(owner, config.startDistance * config.startDistance)
                : DragonOwnerFollowTarget.groundStartDistanceSqr(dragon, owner, config.startDistance, config.stopDistance);
        return DragonOwnerFollowTarget.anchorDistanceToSqr(dragon, owner) > startDistanceSqr;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        if (!canFollow(dragon, owner)) return false;
        if (dragon.isAerial()) return true;
        if (!dragon.isBaby() && isOwnerAirborne(owner)) return true;
        double stopDistance = DragonOwnerFollowTarget.groundStopDistance(dragon, owner, configFor(dragon).stopDistance);
        return DragonOwnerFollowTarget.anchorDistanceToSqr(dragon, owner) > stopDistance * stopDistance;
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        resetTracking();
        T dragon = context.dragon();
        wasAerial = dragon.isAerial();
        resetGroundProgress(dragon, dragon.getOwner(), context.gameTime());
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        if (owner == null) return;
        Config config = configFor(dragon);
        long now = context.gameTime();
        observeOwner(owner, now);
        if (wasAerial && !dragon.isAerial()) {
            finishingOnFoot = true;
            clearLandingTracking();
            resetGroundProgress(dragon, owner, now);
            dragon.getAIMovement().clearGroundPathFailureHistory();
            decision = "landed:finish-on-foot";
        }
        wasAerial = dragon.isAerial();
        if (dragon.areRiderControlsLocked() || dragon.getActiveAbility() != null || dragon.isAiLandingRecoveryActive()) {
            mode = "paused";
            context.memories().erase(DragonMemories.MOVEMENT_INTENT);
            dragon.getAIMovement().stop();
            resetGroundProgress(dragon, owner, now);
            return;
        }

        double distance = Math.sqrt(DragonOwnerFollowTarget.anchorDistanceToSqr(dragon, owner));
        if (distance > config.teleportDistance && dragon.isGroundedForTeleport()
                && DragonOwnerTeleport.attempt(dragon, owner)) {
            // Teleporting next to a grounded owner is a ground arrival.
            dragon.clearAerialState();
            clearLandingTracking();
            resetGroundProgress(dragon, owner, now);
            finishingOnFoot = true;
            decision = "teleported:ground-arrival";
            return;
        }

        Vec3 lookTarget = DragonOwnerFollowTarget.visualTarget(owner);
        dragon.getLookControl().setLookAt(lookTarget.x, lookTarget.y, lookTarget.z, 10.0F, 10.0F);
        if (dragon.isBaby() && dragon.isAerial()) {
            if (dragon.onGround()) dragon.clearAerialState();
            else context.memories().set(DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.transitionToGround(DragonOwnerFollowTarget.groundTarget(dragon, owner), config.flightSpeed));
            return;
        }
        if (dragon.isAerial()) {
            waterHandoff.release();
            if (updateLanding(context, dragon, owner, config)) return;
            followInAir(context, dragon, owner, config);
            return;
        }

        sampleGroundProgress(dragon, owner, now);
        if (shouldTakeoff(dragon, owner, distance, now, config)) {
            waterHandoff.release();
            context.memories().erase(DragonMemories.MOVEMENT_INTENT);
            dragon.getAIMovement().stopAndClearAllMovement();
            takeoffStarter.accept(dragon);
            if (dragon.isAerial()) {
                wasAerial = true;
                finishingOnFoot = false;
                clearLandingTracking();
                nextLandingSearch = now + LANDING_SEARCH_INTERVAL;
                followInAir(context, dragon, owner, config);
                return;
            }
        }
        followOnGround(dragon, owner, config);
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        context.dragon().setAccelerating(false);
        context.dragon().getAIMovement().stopAndClearAllMovement();
        waterHandoff.release();
        mode = "idle";
        resetTracking();
    }

    private boolean canFollow(T dragon, @Nullable LivingEntity owner) {
        return hasOwnerFollowPriority(dragon) && !dragon.isOrderedToSit() && !dragon.isInLove()
                && !dragon.isVehicle() && !dragon.isPassenger() && !dragon.isDying() && !dragon.isSleepLocked()
                && !dragon.isSittingDownAnimation() && !dragon.isInWaterOrBubble()
                && owner != null && owner.isAlive() && owner.level() == dragon.level();
    }

    static boolean hasOwnerFollowPriority(RideableDragonBase dragon) {
        LivingEntity owner = dragon.getOwner();
        LivingEntity target = dragon.getTarget();
        return dragon.isTame() && dragon.getCommand() == 0 && owner != null && owner.isAlive()
                && owner.level() == dragon.level() && (target == null || !target.isAlive());
    }

    private boolean shouldTakeoff(T dragon, LivingEntity owner, double distance, long now, Config config) {
        if (!canTriggerFlight(dragon)) return false;
        if (ownerAirborne) {
            decision = "takeoff:owner-airborne";
            return true;
        }
        double stopDistance = DragonOwnerFollowTarget.groundStopDistance(dragon, owner, config.stopDistance);
        if (distance <= stopDistance + 2 || now - groundSince < GROUND_TRIAL_TICKS) return false;
        if (stalledTicks >= GROUND_STALL_TICKS || groundFailures >= 2 && stalledTicks >= GROUND_TRIAL_TICKS) {
            decision = "takeoff:ground-route-stalled";
            return true;
        }
        if (distance > Math.max(config.flightTriggerDistance, stopDistance + 8) && outpacedTicks >= OUTPACED_TICKS) {
            decision = "takeoff:walking-cannot-catch-up";
            return true;
        }
        return false;
    }

    private boolean canTriggerFlight(T dragon) {
        return !dragon.isBaby() && dragon.canTakeoff() && !dragon.areRiderControlsLocked()
                && DragonAirCombatHelper.canTriggerAiFlight(dragon);
    }

    private void sampleGroundProgress(T dragon, LivingEntity owner, long now) {
        if (groundSamplePosition == null) resetGroundProgress(dragon, owner, now);
        int elapsed = (int) Math.min(40, now - groundSampleAt);
        if (elapsed < 20) return;
        Vec3 anchor = DragonOwnerFollowTarget.anchorPosition(owner);
        double gap = dragon.position().distanceTo(anchor);
        double moved = dragon.position().distanceTo(groundSamplePosition);
        double ownerMoved = anchor.distanceTo(groundSampleOwner);
        double closed = groundSampleGap - gap;
        // Count detours as progress too; the ground pathfinder owns reaching its intermediate nodes.
        if (moved >= 0.75D || closed >= 0.75D) {
            stalledTicks = 0;
            groundFailures = 0;
        } else {
            stalledTicks += elapsed;
        }
        outpacedTicks = ownerMoved >= 1.0D && closed < 0.25D
                ? Math.min(OUTPACED_TICKS, outpacedTicks + elapsed) : Math.max(0, outpacedTicks - elapsed);
        groundSamplePosition = dragon.position();
        groundSampleOwner = anchor;
        groundSampleGap = gap;
        groundSampleAt = now;
    }

    private void resetGroundProgress(T dragon, @Nullable LivingEntity owner, long now) {
        groundSince = groundSampleAt = now;
        groundSamplePosition = dragon.position();
        groundSampleOwner = owner == null ? dragon.position() : DragonOwnerFollowTarget.anchorPosition(owner);
        groundSampleGap = dragon.position().distanceTo(groundSampleOwner);
        groundFailures = stalledTicks = outpacedTicks = groundRepathCooldown = 0;
        retryingGround = false;
        lastGroundTarget = null;
    }

    private boolean updateLanding(DragonBrainContext<T> context, T dragon, LivingEntity owner, Config config) {
        long now = context.gameTime();
        var movement = dragon.getAIMovement();
        Vec3 anchor = DragonOwnerFollowTarget.anchorPosition(owner);
        if (ownerAirborne) {
            if (landingTarget != null || movement.hasActiveLandingTransition() || dragon.isLanding()) {
                cancelLanding(context, "owner-airborne");
            }
            landingSetupTarget = null;
            return false;
        }
        if (dragon.isTakeoff()) return false;
        if (movement.hasActiveLandingTransition()) {
            if (landingTarget == null) {
                landingTarget = movement.getActiveLandingTarget();
                landingProgressPosition = dragon.position();
                landingProgressAt = now;
            }
            mode = "landing";
            if (now >= nextLandingValidation) {
                nextLandingValidation = now + 10;
                double radius = landingRadius(dragon, owner, config) + 6.0D;
                Entity ownerAnchor = DragonOwnerFollowTarget.anchor(owner);
                LivingEntity livingAnchor = ownerAnchor instanceof LivingEntity living ? living : owner;
                if (!movement.isTacticalGroundTransitionTargetValid(
                        landingTarget, livingAnchor, radius, LANDING_MAX_VERTICAL_DELTA + 2)) {
                    cancelLanding(context, "owner-or-site-moved");
                    return false;
                }
            }
            if (dragon.position().distanceToSqr(landingProgressPosition) >= 0.25D) {
                landingProgressPosition = dragon.position();
                landingProgressAt = now;
            }
            if (now - landingProgressAt > LANDING_STALL_TICKS || movement.hasFailed()) {
                cancelLanding(context, "approach-stalled");
                return false;
            }
            context.memories().erase(DragonMemories.MOVEMENT_INTENT);
            dragon.setAccelerating(false);
            decision = "landing:committed";
            return true;
        }
        if (!dragon.isAerial()) {
            clearLandingTracking();
            decision = dragon.isInWaterOrBubble() ? "landed:water-handoff" : "landed:ground-contact";
            return true;
        }
        if (landingTarget != null || dragon.isLanding()) {
            cancelLanding(context, "route-ended-before-touchdown");
            return false;
        }
        double searchDistance = Math.max(config.flightTriggerDistance * 1.5D,
                Math.max(12, dragon.getBbWidth() * 3.0D) + 24 + landingRadius(dragon, owner, config));
        double groundCatchUpSpeed = dragon.getAttributeValue(Attributes.MOVEMENT_SPEED) * config.maxRunSpeed;
        if (ownerHorizontalSpeed > groundCatchUpSpeed * 0.9D) {
            landingSetupTarget = null;
            decision = "air:owner-outpacing-ground-speed";
            return false;
        }
        if (horizontalDistance(dragon.position(), anchor) > searchDistance || ownerGroundedSince < 0
                || now - ownerGroundedSince < 20 || now < nextLandingSearch) return false;
        nextLandingSearch = now + LANDING_SEARCH_INTERVAL;
        double stop = DragonOwnerFollowTarget.groundStopDistance(dragon, owner, config.stopDistance);
        if (movement.requestOwnerFollowLanding(anchor, stop, landingRadius(dragon, owner, config),
                LANDING_MAX_VERTICAL_DELTA, config.flightSpeed)) {
            landingTarget = movement.getActiveLandingTarget();
            landingProgressPosition = dragon.position();
            landingProgressAt = now;
            nextLandingValidation = now + 10;
            landingSetupTarget = null;
            context.memories().erase(DragonMemories.MOVEMENT_INTENT);
            dragon.setAccelerating(false);
            mode = "landing";
            decision = "landing:near-owner";
            return true;
        }
        chooseLandingSetup(dragon, owner, now);
        return false;
    }

    private void cancelLanding(DragonBrainContext<T> context, String reason) {
        T dragon = context.dragon();
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        dragon.getAIMovement().stopAndClearAllMovement();
        dragon.setLanding(false);
        if (!dragon.onGround()) dragon.beginAiFlight();
        clearLandingTracking();
        nextLandingSearch = context.gameTime() + LANDING_SEARCH_INTERVAL;
        decision = "landing-cancelled:" + reason;
    }

    private double landingRadius(T dragon, LivingEntity owner, Config config) {
        double stop = DragonOwnerFollowTarget.groundStopDistance(dragon, owner, config.stopDistance);
        return Math.max(config.landingDistance, Math.max(stop + 4, dragon.getBbWidth() + 4));
    }

    private void chooseLandingSetup(T dragon, LivingEntity owner, long now) {
        Vec3 anchor = DragonOwnerFollowTarget.anchorPosition(owner);
        if (landingSetupTarget != null && !dragon.getAIMovement().hasFailed()
                && dragon.distanceToSqr(landingSetupTarget) > 16 && now - landingSetupAt < 100
                && anchor.distanceToSqr(landingSetupOwner) < 100) return;
        Vec3 radial = dragon.position().subtract(anchor).multiply(1, 0, 1).normalize();
        if (radial.lengthSqr() < 1.0E-6D) radial = Vec3.directionFromRotation(0, dragon.getYRot()).scale(-1);
        double radius = Math.max(24, dragon.getBbWidth() * 3 + 12);
        double height = Math.max(6, dragon.getBbHeight() * 1.5D);
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians((landingSetupAttempt + i) * 60);
            Vec3 offset = new Vec3(radial.x * Math.cos(angle) - radial.z * Math.sin(angle), 0,
                    radial.x * Math.sin(angle) + radial.z * Math.cos(angle)).scale(radius);
            Vec3 candidate = fitFlightTarget(dragon, anchor.add(offset).add(0, height, 0));
            if (candidate != null && dragon.distanceToSqr(candidate) > 16) {
                landingSetupTarget = candidate;
                landingSetupOwner = anchor;
                landingSetupAt = now;
                landingSetupAttempt = (landingSetupAttempt + i + 1) % 6;
                decision = "landing:make-approach-room";
                return;
            }
        }
        landingSetupTarget = null;
        decision = "landing:no-local-approach";
    }

    private void followInAir(DragonBrainContext<T> context, T dragon, LivingEntity owner, Config config) {
        mode = "air";
        lastGroundTarget = null;
        if (ownerAirborne) landingSetupTarget = null;
        if (landingSetupTarget != null && DragonOwnerFollowTarget.anchorPosition(owner).distanceToSqr(landingSetupOwner) > 100) {
            landingSetupTarget = null;
        }
        Vec3 desired = landingSetupTarget != null ? landingSetupTarget : flightTarget(dragon, owner, ownerAirborne, config);
        Vec3 target = fitFlightTarget(dragon, desired);
        if (target == null) {
            context.memories().set(DragonMemories.MOVEMENT_INTENT, DragonMovementIntent.holdPosition());
            decision = "air:no-clear-follow-position";
            return;
        }
        boolean catchUp = dragon.distanceToSqr(target) > AIR_CATCH_UP_DISTANCE * AIR_CATCH_UP_DISTANCE;
        double speed = catchUp ? config.flightSpeed * AIR_CATCH_UP_MULTIPLIER : config.flightSpeed;
        dragon.setAccelerating(catchUp);
        context.memories().set(DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.flight(DragonFlightRequest.track(target, speed, 1.0D)));
        lastAirTarget = target;
    }

    private @Nullable Vec3 fitFlightTarget(T dragon, Vec3 target) {
        var space = dragon.getAIMovement().flightSpace();
        double ceiling = dragon.level().getMaxBuildHeight() - dragon.getBbHeight() - 1;
        var local = space.observe(dragon.position());
        if (local != null && local.clear() && local.ceilingKnown()) {
            ceiling = Math.min(ceiling, local.ceiling() - dragon.getBbHeight() - 1);
        }
        double floor = dragon.level().getMinBuildHeight() + 1;
        Vec3 fitted = space.fitDestination(new Vec3(target.x, Mth.clamp(target.y, floor, Math.max(floor, ceiling)), target.z));
        return fitted != null && fitted.y <= ceiling ? fitted : null;
    }

    private void followOnGround(T dragon, LivingEntity owner, Config config) {
        mode = finishingOnFoot ? "finish-on-foot" : "ground";
        decision = finishingOnFoot ? "ground:finish-owner-approach" : "ground:walking-first";
        waterHandoff.activate(dragon);
        Vec3 followTarget = DragonOwnerFollowTarget.groundTarget(dragon, owner);
        lastGroundTarget = followTarget;
        double distance = Math.sqrt(DragonOwnerFollowTarget.groundFollowDistanceToSqr(dragon, owner, followTarget));
        double stopDistance = DragonOwnerFollowTarget.groundStopDistance(dragon, owner, config.stopDistance);
        if (distance <= stopDistance) {
            dragon.setAccelerating(false);
            dragon.getAIMovement().stop();
            groundRepathCooldown = 0;
            decision = "ground:beside-owner";
            return;
        }
        if (dragon.getAIMovement().hasFailed() && !retryingGround) {
            groundFailures++;
            dragon.getAIMovement().stop();
            groundRepathCooldown = FAILED_GROUND_PATH_RETRY_TICKS;
            retryingGround = true;
        }
        boolean running = distance > config.runDistance;
        dragon.setAccelerating(running);
        double baseSpeed = running ? config.runSpeed : config.walkSpeed;
        double speed = Math.min(baseSpeed * (1.0D + distance / 50.0D), running ? config.maxRunSpeed : config.maxWalkSpeed);
        if (groundRepathCooldown > 0) groundRepathCooldown--;
        if (!retryingGround && dragon.getAIMovement().hasArrived()) groundRepathCooldown = 0;
        if (groundRepathCooldown <= 0) {
            boolean accepted = DragonOwnerFollowTarget.isMounted(owner)
                    ? dragon.getAIMovement().moveToProgressiveGroundPosition(followTarget, speed, running, 1.0D)
                    : dragon.getAIMovement().moveToProgressiveGroundPosition(followTarget, speed, running);
            int baseCooldown = (int) Math.ceil(distance * (running ? 0.3D : 0.45D));
            groundRepathCooldown = accepted ? Mth.clamp(baseCooldown, running ? 4 : 6, running ? 18 : 24)
                    : FAILED_GROUND_PATH_RETRY_TICKS;
            retryingGround = !accepted;
            if (!accepted) groundFailures++;
        }
    }

    private void observeOwner(LivingEntity owner, long now) {
        Entity anchor = DragonOwnerFollowTarget.anchor(owner);
        UUID anchorId = anchor.getUUID();
        if (!anchorId.equals(ownerAnchorId)) {
            ownerAnchorId = anchorId;
            ownerAirborneSince = ownerGroundedSince = -1;
            ownerAirborne = false;
            observedOwnerPosition = null;
            ownerHorizontalSpeed = 0;
            nextOwnerObservation = 0;
        }
        if (now < nextOwnerObservation) return;
        nextOwnerObservation = now + 4;
        if (observedOwnerPosition != null && now > observedOwnerAt) {
            double speed = horizontalDistance(anchor.position(), observedOwnerPosition) / (now - observedOwnerAt);
            ownerHorizontalSpeed += (speed - ownerHorizontalSpeed) * 0.5D;
        }
        observedOwnerPosition = anchor.position();
        observedOwnerAt = now;
        if (isOwnerAirborne(owner)) {
            ownerGroundedSince = -1;
            if (ownerAirborneSince < 0) ownerAirborneSince = now;
            if (now - ownerAirborneSince >= 8) ownerAirborne = true;
        } else {
            ownerAirborneSince = -1;
            if (ownerGroundedSince < 0) ownerGroundedSince = now;
            if (now - ownerGroundedSince >= 20) ownerAirborne = false;
        }
    }

    private boolean isOwnerAirborne(LivingEntity owner) {
        Entity anchor = DragonOwnerFollowTarget.anchor(owner);
        if (anchor instanceof DragonFlightCapable flightCapable) {
            return flightCapable.isFlying() || flightCapable.isTakeoff() || flightCapable.isHovering()
                    || flightCapable.isLanding() && isSubstantiallyAirborne(anchor);
        }
        return isSubstantiallyAirborne(anchor);
    }

    private boolean isSubstantiallyAirborne(Entity entity) {
        return !entity.onGround() && !entity.isInWaterOrBubble()
                && DragonFlightSpace.heightAboveLocalFloor(entity, OWNER_AIRBORNE_CLEARANCE + 2.0D) > OWNER_AIRBORNE_CLEARANCE;
    }

    private Vec3 flightTarget(T dragon, LivingEntity owner, boolean airborne, Config config) {
        if (!airborne) {
            Vec3 anchor = DragonOwnerFollowTarget.anchorPosition(owner);
            Vec3 look = DragonOwnerFollowTarget.anchor(owner).getLookAngle();
            return anchor.add(-look.x * 1.5D, Math.max(6, dragon.getBbHeight() * 1.5D), -look.z * 1.5D);
        }
        return DragonOwnerFollowTarget.airFormationTarget(owner, config.hoverHeight, 3.0D,
                Math.sin(dragon.tickCount * 0.2D) * 0.3D);
    }

    private Config configFor(T dragon) {
        return dragon.isBaby() ? BABY_CONFIG : adultConfig;
    }

    private void clearLandingTracking() {
        landingTarget = landingProgressPosition = landingSetupTarget = landingSetupOwner = null;
        nextLandingValidation = nextLandingSearch = 0;
    }

    private void resetTracking() {
        groundRepathCooldown = groundFailures = stalledTicks = outpacedTicks = 0;
        lastAirTarget = lastGroundTarget = groundSamplePosition = groundSampleOwner = null;
        ownerAnchorId = null;
        observedOwnerPosition = null;
        ownerHorizontalSpeed = 0;
        ownerAirborne = finishingOnFoot = retryingGround = false;
        ownerAirborneSince = ownerGroundedSince = -1;
        nextOwnerObservation = 0;
        clearLandingTracking();
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        return first.subtract(second).horizontalDistance();
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("mode", mode);
        details.put("decision", decision);
        details.put("owner_airborne", Boolean.toString(ownerAirborne));
        details.put("owner_speed", String.format(Locale.ROOT, "%.2f", ownerHorizontalSpeed));
        details.put("ground_stalled_ticks", Integer.toString(stalledTicks));
        details.put("outpaced_ticks", Integer.toString(outpacedTicks));
        details.put("ground_failures", Integer.toString(groundFailures));
        details.put("ground_repath", Integer.toString(groundRepathCooldown));
        details.put("ground_target", lastGroundTarget == null ? "none" : lastGroundTarget.toString());
        details.put("air_target", lastAirTarget == null ? "none" : lastAirTarget.toString());
        details.put("landing_target", landingTarget == null ? "none" : landingTarget.toString());
        details.put("water_handoff", Boolean.toString(waterHandoff.isActive()));
        return Map.copyOf(details);
    }

    public record Config(double startDistance,
                         double stopDistance,
                         double teleportDistance,
                         double runDistance,
                         double flightTriggerDistance,
                         double landingDistance,
                         double hoverHeight,
                         double walkSpeed,
                         double runSpeed,
                         double maxWalkSpeed,
                         double maxRunSpeed,
                         double flightSpeed) {
        public static Config raevyx() {
            return withStandardGround(30.0D, 10.0D, 2.5D, 4.0D);
        }

        public static Config cindervane() {
            return withStandardGround(30.0D, 10.0D, 2.5D, 4.0D);
        }

        public static Config ignivorus() {
            return withStandardGround(20.0D, 10.0D, 2.5D, 4.0D);
        }

        public static Config volitans() {
            return withStandardGround(24.0D, 10.0D, 2.5D, 4.0D);
        }

        private static Config withStandardGround(double flightTriggerDistance,
                                                 double landingDistance,
                                                 double hoverHeight,
                                                 double flightSpeed) {
            return new Config(
                    DragonAdultOwnerFollowTuning.START_DISTANCE,
                    DragonAdultOwnerFollowTuning.STOP_DISTANCE,
                    DragonAdultOwnerFollowTuning.TELEPORT_DISTANCE,
                    DragonAdultOwnerFollowTuning.RUN_DISTANCE,
                    flightTriggerDistance,
                    landingDistance,
                    hoverHeight,
                    DragonAdultOwnerFollowTuning.WALK_SPEED,
                    DragonAdultOwnerFollowTuning.RUN_SPEED,
                    DragonAdultOwnerFollowTuning.MAX_WALK_SPEED,
                    DragonAdultOwnerFollowTuning.MAX_RUN_SPEED,
                    flightSpeed
            );
        }
    }
}
