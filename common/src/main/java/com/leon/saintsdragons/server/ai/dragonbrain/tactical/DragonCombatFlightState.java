package com.leon.saintsdragons.server.ai.dragonbrain.tactical;

import com.leon.saintsdragons.server.ai.DragonAirCombatHelper;
import com.leon.saintsdragons.server.ai.DragonAirCombatSettingsProvider;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.GroundPursuitFlightSettings;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonFlightEligibility;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.navigation.async.DragonFlightSpace;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class DragonCombatFlightState {
    private final RideableFlyingDragon dragon;
    private final Supplier<DragonCombatFlightProfile> profileProvider;
    private DragonCombatFlightProfile observedProfile;
    private final BooleanSupplier rangedReady;
    private final BooleanSupplier actionCommitted;
    private final GroundPursuitFlightSettings landing = GroundPursuitFlightSettings.standard();
    private UUID targetId;
    private long nextObservation;
    private long airborneSince = -1;
    private long groundedSince = -1;
    private long mediumSince;
    private long maneuverUntil;
    private long rangedFlightDeferredUntil;
    private long nextSpaceCheck;
    private long nextLandingSearch;
    private long landingRetryAt;
    private long landingProgressAt;
    private Vec3 landingProgressPosition;
    private long nextLandingValidation;
    private long nextVariation;
    private int airPreference;
    private long revision;
    private long handoffRevision;
    private boolean wasAerial;
    private boolean targetNeedsFlight;
    private boolean targetSettled;
    private boolean openingAvailable;
    private Vec3 landingPosition;
    private Vec3 observedTargetPosition;
    private Vec3 observedTargetVelocity = Vec3.ZERO;
    private long observedTargetTick;
    private String handoff = "idle";
    private String lastLandingFailure = "none";

    public DragonCombatFlightState(RideableFlyingDragon dragon, DragonCombatFlightProfile profile,
                                   BooleanSupplier rangedReady, BooleanSupplier actionCommitted) {
        this(dragon, () -> profile, rangedReady, actionCommitted);
    }

    public DragonCombatFlightState(RideableFlyingDragon dragon, Supplier<DragonCombatFlightProfile> profileProvider,
                                   BooleanSupplier rangedReady, BooleanSupplier actionCommitted) {
        this.dragon = dragon;
        this.profileProvider = profileProvider;
        this.rangedReady = rangedReady;
        this.actionCommitted = actionCommitted;
    }

    public static @Nullable DragonCombatFlightState get(DragonEntity dragon) {
        return dragon instanceof DragonAirCombatSettingsProvider provider ? provider.getCombatFlightState() : null;
    }

    public void observe() {
        long now = dragon.level().getGameTime();
        DragonCombatFlightProfile profile = profileProvider.get();
        if (!profile.equals(observedProfile)) {
            observedProfile = profile;
            nextSpaceCheck = 0;
            revision++;
        }
        LivingEntity target = dragon.getBrain().getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        UUID currentId = target == null ? null : target.getUUID();
        if (!java.util.Objects.equals(targetId, currentId)) {
            targetId = currentId;
            airborneSince = groundedSince = -1;
            targetNeedsFlight = targetSettled = false;
            landingPosition = null;
            observedTargetPosition = null;
            observedTargetVelocity = Vec3.ZERO;
            nextObservation = nextLandingSearch = nextSpaceCheck = 0;
            nextLandingValidation = 0;
            lastLandingFailure = "none";
            mediumSince = now;
            maneuverUntil = 0;
            rangedFlightDeferredUntil = 0;
            landingProgressPosition = null;
            revision++;
            handoffRevision++;
        }
        if (wasAerial != dragon.isAerial()) {
            wasAerial = dragon.isAerial();
            mediumSince = now;
            if (!wasAerial) {
                landingProgressPosition = null;
                handoff = "grounded:combat-plan";
            }
            revision++;
        }
        if (target == null || !dragon.getBrain().getMemory(DragonMemories.TARGET_VISIBLE).orElse(false)) {
            airborneSince = groundedSince = -1;
            observedTargetPosition = null;
            observedTargetVelocity = Vec3.ZERO;
            return;
        }
        if (now < nextObservation) return;
        nextObservation = now + 4;
        Entity anchor = DragonTargetingHelper.movementAnchor(target);
        if (observedTargetPosition != null && now > observedTargetTick) {
            observedTargetVelocity = anchor.position().subtract(observedTargetPosition).scale(1.0D / (now - observedTargetTick));
        }
        observedTargetPosition = anchor.position();
        observedTargetTick = now;
        boolean supported = anchor.onGround() || anchor.isInWaterOrBubble();
        double height = supported ? 0.0D : DragonFlightSpace.heightAboveLocalFloor(anchor, 12.0D);
        boolean separated = !supported && (height > 2.5D || anchor.getY() - dragon.getY() > 4.5D);
        if (separated) {
            if (airborneSince < 0) airborneSince = now;
            groundedSince = -1;
        } else {
            airborneSince = -1;
            // Jumping close to a floor still leaves a ground approach available.
            if (groundedSince < 0) groundedSince = now;
        }
        boolean needsFlight = separated && now - airborneSince >= profile.airborneConfirmationTicks();
        boolean settled = groundedSince >= 0 && now - groundedSince >= profile.groundedConfirmationTicks();
        // A brief touchdown does not cancel an established aerial pursuit.
        if (targetNeedsFlight && !settled) needsFlight = true;
        if (needsFlight != targetNeedsFlight || settled != targetSettled) revision++;
        targetNeedsFlight = needsFlight;
        targetSettled = settled;
    }

    public long revision() {
        return revision;
    }

    public long handoffRevision() {
        return handoffRevision;
    }

    public boolean targetNeedsFlight() {
        return targetNeedsFlight;
    }

    public Vec3 targetVelocity() {
        return observedTargetVelocity;
    }

    public void deferRangedFlightFor(int ticks) {
        rangedFlightDeferredUntil = Math.max(rangedFlightDeferredUntil, dragon.level().getGameTime() + ticks);
        revision++;
    }

    public boolean wantsFlight() {
        return tactic() == DragonTactic.AERIAL_PURSUIT;
    }

    public boolean wantsLanding() {
        return tactic() == DragonTactic.LANDING_APPROACH;
    }

    private DragonTactic tactic() {
        LivingEntity target = dragon.getBrain().getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null) return DragonTactic.NONE;
        return dragon.getBrain().getMemory(DragonMemories.TACTICAL_COMMITMENT)
                .filter(plan -> target.getUUID().equals(plan.targetUuid()))
                .map(DragonTacticalCommitment::tactic).orElse(DragonTactic.NONE);
    }

    public void holdFlightFor(int ticks) {
        maneuverUntil = Math.max(maneuverUntil, dragon.level().getGameTime() + ticks);
    }

    public List<Option> options(LivingEntity target, Vec3 focus) {
        observe();
        DragonCombatFlightProfile profile = observedProfile;
        long now = dragon.level().getGameTime();
        List<Option> options = new ArrayList<>();
        double distance = dragon.distanceTo(DragonTargetingHelper.movementAnchor(target));
        boolean locked = actionCommitted.getAsBoolean();
        boolean canUseRanged = now >= rangedFlightDeferredUntil && rangedReady.getAsBoolean();
        boolean aerial = dragon.isAerial();
        boolean flightAllowed = DragonFlightEligibility.movementBlockReason(dragon) == null;
        boolean canLaunch = !locked && flightAllowed && dragon.canTakeoff()
                && DragonAirCombatHelper.canTriggerAiFlight(dragon);
        boolean routeFailed = dragon.getAIMovement().hasRepeatedGroundPathFailures()
                || dragon.getBrain().getMemory(DragonMemories.CANT_REACH_WALK_TARGET_SINCE)
                .map(since -> now - since >= landing.stallTicks()).orElse(false);
        Vec3 separation = DragonTargetingHelper.movementAnchor(target).position().subtract(dragon.position());
        boolean highGround = separation.y >= landing.highGroundMinVerticalSeparation()
                && separation.horizontalDistance() <= landing.highGroundMaxHorizontalDistance();
        boolean needsPursuit = targetNeedsFlight || routeFailed || highGround;
        if (dragon.canSwim() && !aerial
                && (dragon.isInWaterOrBubble() || DragonTargetingHelper.isMovementAnchorInWater(target))) {
            options.add(new Option(DragonTactic.WATER_PURSUIT, 110, focus, "water-contact"));
            return options;
        }
        Vec3 escapeDirection = targetNeedsFlight ? separation : separation.multiply(1, 0, 1);
        boolean escaping = escapeDirection.length() >= 18.0D
                && observedTargetVelocity.dot(escapeDirection.normalize()) > 0.10D;
        if (now >= nextVariation) {
            airPreference = dragon.getRandom().nextInt(17) - 8;
            nextVariation = now + 80;
        }

        if (!aerial) {
            if (!canLaunch || !needsPursuit) {
                options.add(new Option(DragonTactic.GROUND_PURSUIT,
                        75 + profile.groundPreference() + (distance <= 14.0D ? 15 : 0), focus,
                        locked ? "committed-ground-action" : "ground-pressure"));
            }
            if (!canLaunch) return options;
            if (needsPursuit) {
                options.add(new Option(DragonTactic.AERIAL_PURSUIT, 140, focus,
                        targetNeedsFlight ? "target-left-ground" : "ground-route-unreachable"));
            } else if (now - mediumSince >= profile.groundCommitmentTicks()
                    && !DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)
                    && !DragonTargetingHelper.isMovementAnchorInWater(target)
                    && distance >= 12.0D && distance <= profile.firingRange()
                    && hasFlightOpening(target, now)) {
                int score = 62 + airPreference + (canUseRanged ? 35 : 0)
                        + (distance >= 24.0D ? 12 : 0) + (escaping ? 25 : 0);
                options.add(new Option(DragonTactic.AERIAL_PURSUIT, score, focus,
                        escaping ? "close-escaping-target" : "gain-firing-angle"));
            }
            return options;
        }

        int airScore = needsPursuit ? 140 : 80 + (canUseRanged ? 25 : 0) + (escaping ? 25 : 0);
        options.add(new Option(DragonTactic.AERIAL_PURSUIT, airScore, focus,
                needsPursuit ? "intercept-target" : locked ? "committed-air-action" : "air-attack-position"));
        if (!flightAllowed || locked || dragon.isTakeoff() || targetNeedsFlight || !targetSettled
                || now < maneuverUntil || now < landingRetryAt
                || now - mediumSince < profile.airCommitmentTicks()
                || DragonTargetingHelper.isMovementAnchorInWater(target)) return options;

        if (now >= nextLandingSearch && !hasLandingReservation()) {
            nextLandingSearch = now + landing.landingSearchIntervalTicks();
            if (landingPosition == null || !validLanding(target)) {
                landingPosition = dragon.getAIMovement().findTacticalGroundTransitionTarget(
                        DragonTargetingHelper.livingMovementAnchor(target), landing.landingSearchRadius(),
                        landing.landingMaxVerticalDelta());
            }
        }
        if (landingPosition != null) {
            int score = 82 + profile.groundPreference() - airPreference + (canUseRanged ? 0 : 25)
                    + (distance < 18.0D ? 12 : 0)
                    + (now - mediumSince > profile.airCommitmentTicks() * 3L ? 30 : 0);
            options.add(new Option(DragonTactic.LANDING_APPROACH, score, landingPosition, "land-for-ground-pressure"));
        }
        return options;
    }

    private boolean hasFlightOpening(LivingEntity target, long now) {
        DragonCombatFlightProfile profile = observedProfile;
        if (now < nextSpaceCheck) return openingAvailable;
        nextSpaceCheck = now + 20;
        openingAvailable = false;
        Vec3 radial = dragon.position().subtract(target.position()).multiply(1, 0, 1).normalize();
        if (radial.lengthSqr() < 0.01D) radial = new Vec3(1, 0, 0);
        Vec3 tangent = new Vec3(-radial.z, 0, radial.x);
        Vec3 lift = dragon.position().add(0, profile.approachHeight(), 0);
        var space = dragon.getAIMovement().flightSpace();
        if (!space.canTakeoff(profile.approachHeight())) return false;
        for (int side : new int[]{1, -1}) {
            Vec3 candidate = target.position().add(radial.scale(profile.approachRadius() * 0.7D))
                    .add(tangent.scale(side * profile.approachRadius() * 0.7D))
                    .add(0, profile.approachHeight(), 0);
            Vec3 fitted = space.fitDestination(candidate);
            if (fitted != null && space.corridorClear(lift, fitted)) {
                openingAvailable = true;
                break;
            }
        }
        return openingAvailable;
    }

    public void applyPlan() {
        if (!dragon.getBrain().getActiveActivities().contains(Activity.FIGHT)
                || !dragon.getBrain().getMemory(DragonMemories.TARGET_VISIBLE).orElse(false)
                || actionCommitted.getAsBoolean()
                || DragonFlightEligibility.movementBlockReason(dragon) != null) return;
        LivingEntity target = dragon.getBrain().getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null || !DragonAirCombatHelper.isValidCombatTarget(dragon, target)) return;
        long now = dragon.level().getGameTime();
        if (wantsFlight()) {
            if (dragon.isLanding() || dragon.getAIMovement().hasActiveLandingTransition()
                    || dragon.getBrain().hasMemoryValue(DragonMemories.TACTICAL_LANDING_POSITION)) {
                clearMovement();
                dragon.setLanding(false);
                if (!dragon.onGround()) dragon.beginAiFlight();
                handoff = "landing-cancelled:resume-pursuit";
            }
            if (!dragon.isAerial() && dragon.canTakeoff() && DragonAirCombatHelper.canTriggerAiFlight(dragon)) {
                clearMovement();
                dragon.getAIMovement().clearGroundPathFailureHistory();
                DragonAirCombatHelper.startOrResumeFlight(dragon,
                        ((DragonAirCombatSettingsProvider) dragon).getAiAirCombatSettings().takeoffAnimationTicks());
                handoff = "takeoff:combat-plan";
            }
        } else if (wantsLanding() && dragon.isAerial() && !dragon.isTakeoff()) {
            if (targetNeedsFlight || landingPosition == null) {
                rejectLanding(now, targetNeedsFlight ? "target-airborne" : "missing-site");
                return;
            }
            if (now >= nextLandingValidation && !validLanding(target)) {
                rejectLanding(now, "site-or-target-moved");
                return;
            }
            if (now >= nextLandingValidation) nextLandingValidation = now + 10;
            // APPROACH is already an accepted landing route, before the landing animation starts.
            if (dragon.getAIMovement().hasActiveLandingTransition() || dragon.isLanding()
                    || dragon.getBrain().hasMemoryValue(DragonMemories.TACTICAL_LANDING_POSITION)) {
                Vec3 reserved = dragon.getBrain().getMemory(DragonMemories.TACTICAL_LANDING_POSITION).orElse(null);
                if (reserved != null && reserved.distanceToSqr(landingPosition) > 1.0D) {
                    rejectLanding(now, "reservation-changed");
                    return;
                }
                if (landingProgressPosition == null
                        || dragon.position().distanceToSqr(landingProgressPosition) >= 0.25D) {
                    landingProgressPosition = dragon.position();
                    landingProgressAt = now;
                }
                if (dragon.getAIMovement().hasFailed()) rejectLanding(now, "route-failed");
                else if (now - landingProgressAt > landing.landingFailureTimeoutTicks()) rejectLanding(now, "approach-stalled");
                return;
            }
            clearMovement();
            if (dragon.getAIMovement().requestGroundTransition(landingPosition,
                    ((DragonAirCombatSettingsProvider) dragon).getAiAirCombatSettings().landingSpeed())) {
                Vec3 accepted = dragon.getAIMovement().getActiveLandingTarget();
                if (accepted == null) {
                    // The transition may complete immediately if contact occurred this tick.
                    if (!dragon.isAerial() && dragon.onGround()) {
                        landingProgressPosition = null;
                        handoff = "grounded:combat-plan";
                    } else {
                        rejectLanding(now, "route-failed-on-start");
                    }
                    return;
                }
                landingPosition = accepted;
                dragon.getBrain().setMemory(DragonMemories.TACTICAL_LANDING_POSITION, landingPosition);
                nextLandingValidation = now + 10;
                landingProgressAt = now;
                landingProgressPosition = dragon.position();
                handoff = "landing:combat-plan";
            } else {
                rejectLanding(now, "no-approach-route");
            }
        } else if (!dragon.isAerial() && dragon.onGround()) {
            dragon.getBrain().eraseMemory(DragonMemories.TACTICAL_LANDING_POSITION);
            dragon.getBrain().eraseMemory(DragonMemories.GROUND_ROUTE_ABANDONED);
        }
    }

    private boolean validLanding(LivingEntity target) {
        double retainedRadius = landing.landingSearchRadius() * (hasLandingReservation() ? 1.5D : 1.0D);
        return landingPosition != null && dragon.getAIMovement().isTacticalGroundTransitionTargetValid(
                landingPosition, DragonTargetingHelper.livingMovementAnchor(target),
                retainedRadius, landing.landingMaxVerticalDelta());
    }

    private boolean hasLandingReservation() {
        return dragon.getBrain().hasMemoryValue(DragonMemories.TACTICAL_LANDING_POSITION)
                || dragon.getAIMovement().hasActiveLandingTransition() || dragon.isLanding();
    }

    private void rejectLanding(long now, String reason) {
        clearMovement();
        dragon.setLanding(false);
        if (!dragon.onGround()) dragon.beginAiFlight();
        landingPosition = null;
        lastLandingFailure = reason;
        nextLandingValidation = 0;
        landingRetryAt = now + 80;
        dragon.getBrain().eraseMemory(DragonMemories.TACTICAL_COMMITMENT);
        revision++;
        handoff = "landing-unavailable:reposition";
    }

    private void clearMovement() {
        handoffRevision++;
        landingProgressPosition = null;
        dragon.getBrain().eraseMemory(DragonMemories.WALK_TARGET);
        dragon.getBrain().eraseMemory(DragonMemories.PATH);
        dragon.getBrain().eraseMemory(DragonMemories.CANT_REACH_WALK_TARGET_SINCE);
        dragon.getBrain().eraseMemory(DragonMemories.TACTICAL_LANDING_POSITION);
        dragon.getBrain().eraseMemory(DragonMemories.GROUND_ROUTE_ABANDONED);
        dragon.getBrain().eraseMemory(DragonMemories.MOVEMENT_INTENT);
        dragon.getAIMovement().brainMovement().discardPending();
        dragon.getAIMovement().clearAllWaypoints();
        dragon.setAccelerating(false);
    }

    public String summary() {
        return handoff + ",targetAir=" + targetNeedsFlight + ",targetSettled=" + targetSettled
                + ",actionCommitted=" + actionCommitted.getAsBoolean()
                + ",rangedReady=" + rangedReady.getAsBoolean()
                + ",rangedDeferred=" + Math.max(0, rangedFlightDeferredUntil - dragon.level().getGameTime())
                + ",mediumAge=" + (dragon.level().getGameTime() - mediumSince)
                + ",lastLandingFailure=" + lastLandingFailure
                + ",landingNoProgress=" + (landingProgressPosition == null ? 0 : dragon.level().getGameTime() - landingProgressAt);
    }

    public record Option(DragonTactic tactic, int score, Vec3 focus, String reason) {}
}
