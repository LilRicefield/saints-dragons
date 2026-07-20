package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonSensoryObservation;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

final class DragonPursuitSafety {
    private static final int ROUTE_FAILURE_PRESSURE_LIMIT = 20 * 10;
    private static final int IDLE_PURSUIT_LIMIT_TICKS = 20 * 15;
    private static final int LOST_TARGET_LIMIT_TICKS = 20 * 12;
    private static final int REACQUIRE_COOLDOWN_TICKS = 20 * 10;
    private static final int FRESH_THREAT_TICKS = 10;

    private UUID pursuedTargetId;
    private long pursuitStartedAt;
    private long lastProgressAt;
    private double bestDistance = Double.MAX_VALUE;
    private int routeFailurePressure;
    private int idlePursuitTicks;
    private int lostTargetTicks;
    private UUID abandonedTargetId;
    private long abandonedUntil;
    private long nextAcquisitionScanAt;
    private long debugGameTime;
    private String lastAbandonment = "none";
    private String pursuitState = "idle";

    void beginTick(long gameTime) {
        debugGameTime = gameTime;
        if (abandonedTargetId != null && gameTime >= abandonedUntil) {
            abandonedTargetId = null;
            abandonedUntil = 0L;
            nextAcquisitionScanAt = 0L;
        }
    }

    @Nullable
    String abandonmentReason(DragonBrainContext<? extends RideableDragonBase> context,
                             LivingEntity target) {
        RideableDragonBase dragon = context.dragon();
        long gameTime = context.gameTime();
        double distance = dragon.distanceTo(target);
        if (!target.getUUID().equals(pursuedTargetId)) {
            beginPursuit(target, distance, gameTime);
            return null;
        }

        if (dragon.getActiveAbility() != null
                || dragon.isVehicle()
                || dragon.isPassenger()
                || dragon.isSleeping()
                || dragon.isSleepTransitioning()) {
            lastProgressAt = gameTime;
            pursuitState = "committed";
            return null;
        }

        double progressDistance = Math.max(1.5D, dragon.getBbWidth() * 0.5D);
        boolean exchangedDamage = (dragon.getLastHurtMob() == target
                && dragon.tickCount - dragon.getLastHurtMobTimestamp() <= FRESH_THREAT_TICKS)
                || (dragon.getLastHurtByMob() == target
                && dragon.tickCount - dragon.getLastHurtByMobTimestamp() <= FRESH_THREAT_TICKS);
        if (exchangedDamage || distance <= bestDistance - progressDistance) {
            bestDistance = distance;
            lastProgressAt = gameTime;
            routeFailurePressure = 0;
            idlePursuitTicks = 0;
        }

        boolean routeFailed = hasRouteFailure(context, target);
        routeFailurePressure = routeFailed
                ? Math.min(ROUTE_FAILURE_PRESSURE_LIMIT, routeFailurePressure + 2)
                : Math.max(0, routeFailurePressure - 1);

        double engagementDistance = Math.max(
                6.0D,
                (dragon.getBbWidth() + target.getBbWidth()) * 0.5D + 4.0D
        );
        if (!dragon.getAIMovement().isPathing() && distance > engagementDistance) {
            idlePursuitTicks++;
        } else {
            idlePursuitTicks = Math.max(0, idlePursuitTicks - 2);
        }

        boolean visible = context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false);
        boolean remembered = matchesTarget(
                context.memories().get(DragonMemories.LAST_SEEN_TARGET).orElse(null),
                target
        ) || matchesTarget(
                context.memories().get(DragonMemories.HEARD_TARGET).orElse(null),
                target
        ) || matchesTarget(
                context.memories().get(DragonMemories.INVESTIGATION_TARGET).orElse(null),
                target
        );
        lostTargetTicks = visible || remembered ? 0 : lostTargetTicks + 1;

        long noProgressTicks = Math.max(0L, gameTime - lastProgressAt);
        pursuitState = "tracking";

        if (routeFailurePressure >= ROUTE_FAILURE_PRESSURE_LIMIT && noProgressTicks >= 20L * 5L) {
            return "route-failed";
        }
        if (idlePursuitTicks >= IDLE_PURSUIT_LIMIT_TICKS
                && noProgressTicks >= IDLE_PURSUIT_LIMIT_TICKS) {
            return "movement-idle";
        }
        if (lostTargetTicks >= LOST_TARGET_LIMIT_TICKS) {
            return "target-lost";
        }
        return null;
    }

    void recordAbandonment(long gameTime, LivingEntity target, String reason) {
        abandonedTargetId = target.getUUID();
        abandonedUntil = gameTime + REACQUIRE_COOLDOWN_TICKS;
        nextAcquisitionScanAt = gameTime + 20L;
        lastAbandonment = reason;
        pursuitState = "abandoned:" + reason;
        resetTracking(false);
    }

    boolean canReacquire(RideableDragonBase dragon, LivingEntity target, long gameTime) {
        if (abandonedTargetId == null || !abandonedTargetId.equals(target.getUUID())) {
            return true;
        }
        if (gameTime >= abandonedUntil || isFreshThreat(dragon, target)) {
            abandonedTargetId = null;
            abandonedUntil = 0L;
            nextAcquisitionScanAt = 0L;
            return true;
        }
        return false;
    }

    boolean shouldThrottleAcquisition(RideableDragonBase dragon, long gameTime) {
        if (abandonedTargetId == null) {
            return false;
        }
        if (gameTime < nextAcquisitionScanAt && !hasFreshThreatSignal(dragon)) {
            pursuitState = "reacquire-cooldown";
            return true;
        }
        nextAcquisitionScanAt = gameTime + 20L;
        return false;
    }

    void resetTracking() {
        resetTracking(abandonedTargetId == null);
    }

    String debugSummary() {
        return stateDebugSummary() + ",blocked=" + abandonedDebugSummary();
    }

    String stateDebugSummary() {
        if (!"tracking".equals(pursuitState)) {
            return pursuitState;
        }
        return String.format(
                Locale.ROOT,
                "tracking(age=%dt,noProgress=%dt,failure=%d/%d,idle=%d/%d,lost=%d/%d)",
                Math.max(0L, debugGameTime - pursuitStartedAt),
                Math.max(0L, debugGameTime - lastProgressAt),
                routeFailurePressure,
                ROUTE_FAILURE_PRESSURE_LIMIT,
                idlePursuitTicks,
                IDLE_PURSUIT_LIMIT_TICKS,
                lostTargetTicks,
                LOST_TARGET_LIMIT_TICKS
        );
    }

    String abandonedDebugSummary() {
        return abandonedTargetId == null
                ? "none(last=" + lastAbandonment + ")"
                : abandonedTargetId.toString().substring(0, 8)
                + ":" + lastAbandonment
                + ":" + Math.max(0L, abandonedUntil - debugGameTime) + "t";
    }

    private boolean hasRouteFailure(DragonBrainContext<? extends RideableDragonBase> context,
                                    LivingEntity target) {
        RideableDragonBase dragon = context.dragon();
        if (dragon.isInWaterOrBubble()) {
            AsyncSwimController.DebugSnapshot swim = dragon.getAiSwimController().getDebugSnapshot();
            return swim.rejectedTarget() != null
                    && swim.rejectedCooldown() > 0
                    && swim.rejectedTarget().distanceToSqr(target.position()) <= 64.0D;
        }
        if (dragon instanceof RideableFlyingDragon flyingDragon
                && (flyingDragon.isAerial()
                || flyingDragon.isFlying()
                || flyingDragon.isTakeoff()
                || flyingDragon.isLanding())) {
            return flyingDragon.isFlightControllerStuck();
        }
        if (dragon.getAIMovement().hasFailed() || dragon.getNavigation().isStuck()) {
            return true;
        }
        long cantReachSince = context.memories()
                .get(DragonMemories.CANT_REACH_WALK_TARGET_SINCE)
                .orElse(Long.MAX_VALUE);
        if (cantReachSince != Long.MAX_VALUE && context.gameTime() - cantReachSince >= 20L) {
            return true;
        }
        return false;
    }

    private boolean isFreshThreat(RideableDragonBase dragon, LivingEntity target) {
        if (dragon.getLastHurtByMob() == target
                && dragon.tickCount - dragon.getLastHurtByMobTimestamp() <= FRESH_THREAT_TICKS) {
            return true;
        }
        LivingEntity owner = dragon.getOwner();
        return owner != null && (
                (owner.getLastHurtByMob() == target
                        && owner.tickCount - owner.getLastHurtByMobTimestamp() <= FRESH_THREAT_TICKS)
                        || (owner.getLastHurtMob() == target
                        && owner.tickCount - owner.getLastHurtMobTimestamp() <= FRESH_THREAT_TICKS)
        );
    }

    private boolean hasFreshThreatSignal(RideableDragonBase dragon) {
        LivingEntity attacker = dragon.getLastHurtByMob();
        if (attacker != null
                && dragon.tickCount - dragon.getLastHurtByMobTimestamp() <= FRESH_THREAT_TICKS) {
            return true;
        }
        LivingEntity owner = dragon.getOwner();
        return owner != null && (
                (owner.getLastHurtByMob() != null
                        && owner.tickCount - owner.getLastHurtByMobTimestamp() <= FRESH_THREAT_TICKS)
                        || (owner.getLastHurtMob() != null
                        && owner.tickCount - owner.getLastHurtMobTimestamp() <= FRESH_THREAT_TICKS)
        );
    }

    private void beginPursuit(LivingEntity target, double distance, long gameTime) {
        pursuedTargetId = target.getUUID();
        pursuitStartedAt = gameTime;
        lastProgressAt = gameTime;
        bestDistance = distance;
        routeFailurePressure = 0;
        idlePursuitTicks = 0;
        lostTargetTicks = 0;
        pursuitState = "tracking";
    }

    private boolean matchesTarget(@Nullable DragonSensoryObservation observation,
                                  LivingEntity target) {
        return observation != null && target.getUUID().equals(observation.sourceUuid());
    }

    private void resetTracking(boolean resetState) {
        pursuedTargetId = null;
        pursuitStartedAt = 0L;
        lastProgressAt = 0L;
        bestDistance = Double.MAX_VALUE;
        routeFailurePressure = 0;
        idlePursuitTicks = 0;
        lostTargetTicks = 0;
        if (resetState) {
            pursuitState = "idle";
        }
    }
}
