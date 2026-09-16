package com.leon.saintsdragons.server.ai.dragonbrain.tactical;

import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.learning.DragonCombatLearning;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonCombatAim;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

public final class DragonCombatDecisionSupport {
    public enum Failure { ROUTE_FAILED, NO_PROGRESS, BLOCKED_SHOT, BLOCKED_TRAJECTORY, ALIGNMENT_TIMEOUT }

    private final RideableFlyingDragon dragon;
    private final ArrayDeque<FailedPosition> failures = new ArrayDeque<>();
    private UUID opponent;
    private UUID mount;
    private DragonLocomotionMode medium;
    private Vec3 focus;
    private boolean visible;
    private Vec3 route;
    private Vec3 progressPosition;
    private long progressAt;
    private long routeStartedAt;
    private long failedGeneration = -1;
    private long revision;
    private long shotSince;
    private long shotAt;
    private DragonCombatAim.Shot shot;
    private DragonCombatAim.Profile shotProfile;
    private double bestAimError;
    private DragonAbility<?> aimOwner;
    private DragonCombatAim.Profile ownedAim;
    private String movementStatus = "idle";
    private String attackStatus = "none";
    private long selectionUntil;
    private long nextCandidateScan;
    private long selectionRevision;
    private String selectionKey;
    private Vec3 selectionPreferred;
    private Vec3 selectionFocus;
    private Vec3 selectionOrigin;
    private Vec3 selected;
    private Vec3 shotOrigin;
    private Vec3 groundPosition;
    private Vec3 groundFocus;
    private long groundPositionUntil;

    public DragonCombatDecisionSupport(RideableFlyingDragon dragon) {
        this.dragon = dragon;
        dragon.getAIMovement().brainMovement().setActionSupplier(() -> canAct() ? dragon.getActiveAbility() : null);
    }

    public static @Nullable DragonCombatDecisionSupport get(DragonEntity dragon) {
        return dragon instanceof RideableFlyingDragon flying ? flying.getCombatDecisionSupport() : null;
    }

    private long now() { return dragon.level().getGameTime(); }

    public void observe(@Nullable LivingEntity target, boolean canSee) {
        if (!canAct() || target == null || !dragon.isTargetValid(target)) {
            reset();
            return;
        }
        UUID nextMount = DragonTargetingHelper.movementAnchor(target).getUUID();
        if (!target.getUUID().equals(opponent) || !nextMount.equals(mount)
                || medium != dragon.getLocomotionMode()) {
            reset();
            opponent = target.getUUID();
            mount = nextMount;
            medium = dragon.getLocomotionMode();
        }
        visible = canSee;
        if (visible) {
            Vec3 observed = DragonTargetingHelper.movementAnchor(target).getBoundingBox().getCenter();
            if (focus != null && focus.distanceToSqr(observed) > 32 * 32) {
                failures.clear();
                route = null;
                selectionKey = null;
                revision++;
            }
            focus = observed;
        }
        failures.removeIf(failure -> failure.expiresAt <= now());
        synchronizeAction();
        observeMovement();
    }

    private boolean canAct() {
        return !dragon.level().isClientSide && dragon.isAlive() && !dragon.isDying()
                && !dragon.isVehicle() && !dragon.isPassenger() && !dragon.isStayOrSitMuted()
                && !dragon.isSleepLocked() && !dragon.isBaby();
    }

    public void synchronizeAction() {
        DragonAbility<?> active = canAct() ? dragon.getActiveAbility() : null;
        dragon.getAIMovement().brainMovement().coordinateAction(active);
        if (aimOwner != active) {
            aimOwner = active;
            ownedAim = null;
        }
    }

    public boolean claimAim(DragonCombatAim.Profile profile) {
        synchronizeAction();
        if (aimOwner == null) return true;
        if (ownedAim == null) ownedAim = profile;
        return ownedAim == profile;
    }

    public void rememberShotOrigin(Vec3 origin) { shotOrigin = origin; }

    public @Nullable Vec3 groundReposition(LivingEntity target) {
        if (groundPosition != null) {
            if (visible && Objects.equals(opponent, target.getUUID()) && dragon.getActiveAbility() == null
                    && now() < groundPositionUntil && focus.distanceToSqr(groundFocus) < 64
                    && dragon.position().distanceToSqr(groundPosition) > 4
                    && !dragon.getAIMovement().hasFailed()) return groundPosition;
            groundPosition = null;
        }
        if (!needsGroundReposition()) return null;
        Vec3 position = dragon.position();
        Vec3 origin = shotOrigin == null ? dragon.getEyePosition() : shotOrigin;
        Vec3 result = choosePosition("ground-shot", target, position, origin,
                Math.max(16, origin.distanceTo(focus) + 8), candidate -> {
                    Vec3 fitted = dragon.getAIMovement().findGroundWaypointBelow(candidate);
                    return fitted != null && Math.abs(fitted.y - position.y) <= 2 ? fitted : null;
                });
        if (result == null || result.distanceToSqr(position) < 4) return null;
        groundPosition = result;
        groundFocus = focus;
        groundPositionUntil = now() + 40;
        return result;
    }

    private void observeMovement() {
        var movement = dragon.getAIMovement();
        Vec3 destination = movement.getDebugMovementTarget();
        long time = now();
        if (!visible || dragon.isTakeoff() || dragon.isLanding()
                || dragon.getActiveAbility() != null
                || movement.brainMovement().hasRecentHold(movement.getMovementCommandGeneration(), time)) {
            route = null;
            movementStatus = !visible ? "remembered-pursuit" : "action-committed";
            return;
        }
        if (destination == null) {
            route = null;
            movementStatus = "idle";
            return;
        }
        if (route == null || route.distanceToSqr(destination) > 36) {
            route = destination;
            progressPosition = dragon.position();
            progressAt = routeStartedAt = time;
        }
        if (movement.hasArrived()) {
            movementStatus = "arrived";
            progressAt = time;
        } else if (time - routeStartedAt >= 8 && movement.hasFailed()) {
            movementStatus = "route-failed";
            if (failedGeneration != movement.getMovementCommandGeneration()) {
                fail(Failure.ROUTE_FAILED, destination);
                failedGeneration = movement.getMovementCommandGeneration();
            }
        } else if (dragon.position().distanceToSqr(progressPosition) >= 1) {
            progressPosition = dragon.position();
            progressAt = time;
            movementStatus = "progressing";
        } else if (time - progressAt >= 60) {
            fail(Failure.NO_PROGRESS, destination);
            movementStatus = "no-progress";
            progressAt = time;
        } else {
            movementStatus = "approaching";
        }
    }

    public void observeShot(DragonCombatAim.Profile profile, DragonCombatAim.Shot value, double error) {
        if (!canAct() || !visible || opponent == null) return;
        long time = now();
        if (shot != value || shotProfile != profile || time - shotAt > 6) {
            shotSince = time;
            bestAimError = error;
        } else if (error < bestAimError - 3) {
            bestAimError = error;
            shotSince = time;
        }
        shot = value;
        shotProfile = profile;
        shotAt = time;
        if (value == DragonCombatAim.Shot.ALIGNED) {
            failures.removeIf(failure -> (failure.reason == Failure.BLOCKED_SHOT
                    || failure.reason == Failure.ALIGNMENT_TIMEOUT)
                    && failure.position.distanceToSqr(dragon.position()) < 16);
        } else if (time - shotSince >= 40) {
            if (value == DragonCombatAim.Shot.BLOCKED) fail(Failure.BLOCKED_SHOT, dragon.position());
            else if (value.needsAlignment()) fail(Failure.ALIGNMENT_TIMEOUT, dragon.position());
            shotSince = time;
        }
    }

    public void attackResult(UUID target, DragonCombatLearning.Outcome outcome, boolean hit, boolean scored) {
        if (!Objects.equals(target, opponent)) return;
        attackStatus = hit ? "hit" : scored ? "miss" : outcome.name().toLowerCase(Locale.ROOT);
    }

    public void failSetup(@Nullable Vec3 position) {
        if (shot == null || shotAt < now() - 6) return;
        if (shot == DragonCombatAim.Shot.BLOCKED) fail(Failure.BLOCKED_SHOT, position);
        else if (shot.needsAlignment()) fail(Failure.ALIGNMENT_TIMEOUT, position);
    }

    public void fail(Failure reason, @Nullable Vec3 position) {
        if (!canAct() || !visible || focus == null || position == null) return;
        for (FailedPosition failure : failures) {
            if (failure.reason == reason && failure.position.distanceToSqr(position) < 16
                    && failure.focus.distanceToSqr(focus) < 144 && failure.expiresAt > now()) return;
        }
        if (failures.size() >= 8) failures.removeFirst();
        DragonTactic tactic = dragon.getBrain().getMemory(DragonMemories.TACTICAL_COMMITMENT)
                .map(DragonTacticalCommitment::tactic).orElse(DragonTactic.NONE);
        failures.addLast(new FailedPosition(reason, position, dragon.position(), focus, tactic, now() + 160));
        revision++;
        selectionKey = null;
    }

    public double positionPenalty(Vec3 position) {
        if (focus == null) return 0;
        double penalty = 0;
        for (FailedPosition failure : failures) {
            if (failure.expiresAt > now() && failure.focus.distanceToSqr(focus) <= 144) {
                double distance = failure.position.distanceTo(position);
                penalty = Math.max(penalty, Math.max(0, 1 - distance / 10) * 40);
            }
        }
        return penalty;
    }

    public int tacticPenalty(DragonTactic tactic) {
        if (!visible || focus == null || dragon.getActiveAbility() != null) return 0;
        int penalty = 0;
        for (FailedPosition failure : failures) {
            if (failure.tactic == tactic && failure.expiresAt > now()
                    && (failure.reason == Failure.ROUTE_FAILED || failure.reason == Failure.NO_PROGRESS)
                    && failure.focus.distanceToSqr(focus) <= 144
                    && failure.origin.distanceToSqr(dragon.position()) <= 64) penalty += 12;
        }
        return Math.min(24, penalty);
    }

    public boolean needsGroundReposition() {
        return visible && dragon.getActiveAbility() == null && shotAt >= now() - 6
                && positionPenalty(dragon.position()) >= 20;
    }

    public @Nullable Vec3 choosePosition(String key, LivingEntity target, Vec3 preferred, @Nullable Vec3 mouth,
                                         double range, Function<Vec3, Vec3> fit) {
        if (!visible || !Objects.equals(opponent, target.getUUID())) return fit.apply(preferred);
        if (Objects.equals(key, selectionKey) && now() < selectionUntil && selectionRevision == revision
                && selectionPreferred.distanceToSqr(preferred) < 16 && selectionFocus.distanceToSqr(focus) < 16
                && selectionOrigin.distanceToSqr(dragon.position()) < 16) return selected;
        // Fast moving targets may invalidate the cache each tick. Keep the candidate scans bounded.
        if (now() < nextCandidateScan) return fit.apply(preferred);
        nextCandidateScan = now() + 10;
        selected = DragonCombatPositioning.choose(dragon, this, focus, preferred, mouth, range, fit);
        selectionKey = key;
        selectionUntil = now() + 12;
        selectionRevision = revision;
        selectionPreferred = preferred;
        selectionFocus = focus;
        selectionOrigin = dragon.position();
        return selected;
    }

    public long revision() { return revision; }

    public String summary() {
        var active = dragon.getActiveAbility();
        var section = active == null ? null : active.getCurrentSection();
        return "movement=" + movementStatus + ",shot=" + (visible && shotAt >= now() - 6 ? shot : "unknown")
                + ",action=" + (section == null ? "none" : active.getClass().getSimpleName() + ":" + section.sectionType)
                + ",result=" + attackStatus + ",failed_positions=" + failures.size()
                + ",last_failure=" + (failures.isEmpty() ? "none" : failures.getLast().reason);
    }

    private void reset() {
        boolean changed = opponent != null || aimOwner != null || route != null || !failures.isEmpty();
        opponent = mount = null;
        medium = null;
        focus = route = progressPosition = null;
        visible = false;
        failures.clear();
        shot = null;
        shotProfile = null;
        aimOwner = null;
        ownedAim = null;
        shotOrigin = null;
        groundPosition = groundFocus = null;
        selectionKey = null;
        failedGeneration = -1;
        nextCandidateScan = 0;
        movementStatus = "idle";
        attackStatus = "none";
        dragon.getAIMovement().brainMovement().coordinateAction(null);
        if (changed) revision++;
    }

    private record FailedPosition(Failure reason, Vec3 position, Vec3 origin, Vec3 focus,
                                  DragonTactic tactic, long expiresAt) { }
}
