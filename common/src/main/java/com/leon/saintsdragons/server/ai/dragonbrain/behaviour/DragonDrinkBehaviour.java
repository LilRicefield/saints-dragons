package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncDragonPathfinder;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.interfaces.DrinkingDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public final class DragonDrinkBehaviour<T extends RideableDragonBase & DrinkingDragon>
        extends DragonBehaviour<T> {
    private static final int STABLE_GROUND_TICKS = 3;

    private final Config config;
    private Phase phase = Phase.IDLE;
    private List<DrinkSite> candidates = List.of();
    private int candidateIndex;
    private int pathGeneration;
    private int routeNodes;
    private int stableGroundTicks;
    private int waterSourcesScanned;
    private int validSitesFound;
    private long phaseEndsAt;
    private boolean completed;
    private String decision = "not-checked";
    @Nullable
    private DrinkSite site;
    @Nullable
    private Future<?> pathRequest;

    public DragonDrinkBehaviour(Config config) {
        this.config = config;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        String ineligibleReason = ineligibleReason(dragon);
        if (ineligibleReason != null) {
            decision = "ineligible:" + ineligibleReason;
            return false;
        }
        candidates = findSites(dragon);
        if (candidates.isEmpty()) {
            decision = "no-water";
        } else {
            decision = "water-found";
        }
        return true;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        if (phase == Phase.IDLE || phase == Phase.COMPLETE || phase == Phase.FAILED) {
            return false;
        }
        String ineligibleReason = ineligibleReason(context.dragon());
        if (ineligibleReason != null) {
            decision = "interrupted:" + ineligibleReason;
            return false;
        }
        if (context.gameTime() > phaseEndsAt) {
            decision = "failed:timeout";
            return false;
        }
        return site == null || isDrinkableWater(context.dragon(), site.water());
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        completed = false;
        candidateIndex = 0;
        routeNodes = 0;
        stableGroundTicks = 0;
        if (candidates.isEmpty()) {
            phase = Phase.FAILED;
            decision = "failed:no-water";
            return;
        }
        phase = Phase.SETTLING;
        decision = "settling";
        phaseEndsAt = context.gameTime() + config.approachTimeoutTicks();
        dragon.getAIMovement().stop();
        if (dragon instanceof RideableFlyingDragon flyingDragon) {
            flyingDragon.switchToGroundNavigation();
        }
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        switch (phase) {
            case SETTLING -> tickSettling(dragon);
            case PLANNING -> dragon.getAIMovement().setGroundIdle();
            case APPROACH -> tickApproach(context, dragon);
            case ALIGN -> tickAlignment(context, dragon);
            case DRINKING -> tickDrinking(context, dragon);
            default -> {
            }
        }
    }

    private void tickSettling(T dragon) {
        dragon.getAIMovement().setGroundIdle();
        if (!dragon.onGround()) {
            stableGroundTicks = 0;
            decision = "settling:airborne";
            return;
        }
        if (++stableGroundTicks < STABLE_GROUND_TICKS) {
            decision = "settling:" + stableGroundTicks + "/" + STABLE_GROUND_TICKS;
            return;
        }
        phase = Phase.PLANNING;
        decision = "planning";
        requestNextPath(dragon);
    }

    private void tickApproach(DragonBrainContext<T> context, T dragon) {
        dragon.setGroundMoveStateFromAI(1);
        if (dragon.getAIMovement().isPathing()) {
            return;
        }
        if (dragon.getAIMovement().hasFailed()
                || !dragon.getAIMovement().hasArrived()
                || site == null
                || !isDryStandingPosition(dragon, dragon.position())
                || !canDrinkFromCurrentPosition(dragon, site)) {
            phase = Phase.FAILED;
            decision = "failed:approach";
            return;
        }

        dragon.getAIMovement().setGroundIdle();
        phase = Phase.ALIGN;
        decision = "aligning";
        phaseEndsAt = context.gameTime() + config.alignTicks();
    }

    private void tickAlignment(DragonBrainContext<T> context, T dragon) {
        if (site == null) {
            phase = Phase.FAILED;
            decision = "failed:missing-site";
            return;
        }
        dragon.getAIMovement().setGroundIdle();
        faceWater(dragon, site.water());
        if (context.gameTime() < phaseEndsAt) {
            return;
        }

        dragon.startDrinkingAnimation();
        phase = Phase.DRINKING;
        decision = "drinking";
        phaseEndsAt = context.gameTime() + Math.max(1, dragon.getDrinkingDurationTicks());
    }

    private void tickDrinking(DragonBrainContext<T> context, T dragon) {
        if (site == null) {
            phase = Phase.FAILED;
            decision = "failed:missing-site";
            return;
        }
        dragon.getAIMovement().setGroundIdle();
        faceWater(dragon, site.water());
        if (context.gameTime() >= phaseEndsAt) {
            completed = true;
            phase = Phase.COMPLETE;
            decision = "complete";
        }
    }

    @Override
    protected int cooldownForTicks(DragonBrainContext<T> context) {
        if (!completed) {
            return config.failureCooldownTicks();
        }
        int range = config.maxCooldownTicks() - config.minCooldownTicks();
        return config.minCooldownTicks()
                + (range == 0 ? 0 : context.dragon().getRandom().nextInt(range + 1));
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        pathGeneration++;
        if (pathRequest != null) {
            pathRequest.cancel(true);
            pathRequest = null;
        }
        context.dragon().getAIMovement().stop();
        if (phase == Phase.DRINKING && !completed) {
            context.dragon().stopDrinkingAnimation();
        }
        phase = Phase.IDLE;
        candidates = List.of();
        candidateIndex = 0;
        routeNodes = 0;
        stableGroundTicks = 0;
        site = null;
    }

    private void requestNextPath(T dragon) {
        if (candidateIndex >= candidates.size()) {
            phase = Phase.FAILED;
            if (!decision.startsWith("path-rejected:")) {
                decision = "failed:no-route";
            }
            return;
        }

        DrinkSite candidate = candidates.get(candidateIndex++);
        if (isDryStandingPosition(dragon, dragon.position())
                && canDrinkFromCurrentPosition(dragon, candidate)) {
            site = new DrinkSite(dragon.position(), candidate.water());
            dragon.getAIMovement().setGroundIdle();
            phase = Phase.ALIGN;
            decision = "aligning:nearby-water";
            phaseEndsAt = dragon.level().getGameTime() + config.alignTicks();
            return;
        }

        int generation = ++pathGeneration;
        site = candidate;
        pathRequest = AsyncDragonPathfinder.calculateGroundPathAsync(
                dragon,
                candidate.stance(),
                0,
                true,
                path -> acceptPath(dragon, candidate, generation, path)
        );
    }

    private void acceptPath(T dragon, DrinkSite candidate, int generation, @Nullable Path path) {
        if (generation != pathGeneration || phase != Phase.PLANNING || dragon.isRemoved()) {
            return;
        }
        pathRequest = null;
        String rejection = pathRejection(dragon, candidate, path);
        if (rejection != null) {
            decision = "path-rejected:" + rejection;
            requestNextPath(dragon);
            return;
        }
        Vec3 endpoint = path.getEntityPosAtNode(dragon, path.getNodeCount() - 1);
        DrinkSite reachableSite = new DrinkSite(endpoint, candidate.water());
        if (!isSiteValid(dragon, reachableSite)) {
            decision = "path-rejected:unsafe-endpoint";
            requestNextPath(dragon);
            return;
        }

        routeNodes = path.getNodeCount();
        site = reachableSite;
        if (!dragon.getAIMovement().followGroundPath(
                path,
                reachableSite.stance(),
                config.speedModifier(),
                false,
                approachArrivalTolerance(dragon, reachableSite)
        )) {
            decision = "path-rejected:navigation";
            requestNextPath(dragon);
            return;
        }
        phase = Phase.APPROACH;
        decision = "approaching";
    }

    @Nullable
    private String pathRejection(T dragon, DrinkSite candidate, @Nullable Path path) {
        if (path == null) {
            return "no-path";
        }
        if (path.getNodeCount() == 0) {
            return "empty";
        }
        if (path.getNodeCount() > config.maxPathNodes()) {
            return "too-long";
        }
        Vec3 endpoint = path.getEntityPosAtNode(dragon, path.getNodeCount() - 1);
        if (!isDryStandingPosition(dragon, endpoint)) {
            return "wet-endpoint";
        }
        return canDrinkFromPosition(dragon, endpoint, candidate.water())
                ? null
                : "endpoint-out-of-reach";
    }

    private List<DrinkSite> findSites(T dragon) {
        waterSourcesScanned = 0;
        validSitesFound = 0;
        BlockPos origin = dragon.blockPosition();
        List<DrinkSite> sites = new ArrayList<>();
        int radius = config.searchRadius();
        int upwardRange = config.verticalSearchRange();
        int downwardRange = Math.max(upwardRange, Mth.ceil(dragon.getBbHeight()) + 2);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                BlockPos column = origin.offset(dx, 0, dz);
                if (!dragon.level().hasChunkAt(column)) {
                    continue;
                }
                for (int dy = upwardRange; dy >= -downwardRange; dy--) {
                    BlockPos water = column.offset(0, dy, 0);
                    if (!isSourceWaterAccessible(dragon, water)) {
                        continue;
                    }
                    waterSourcesScanned++;
                    sites.add(new DrinkSite(
                            Vec3.atBottomCenterOf(water.above()),
                            water.immutable()
                    ));
                }
            }
        }

        sites.sort(Comparator.comparingDouble(candidate -> candidate.stance().distanceToSqr(dragon.position())));
        validSitesFound = sites.size();
        return sites.size() <= config.maxCandidateSites()
                ? List.copyOf(sites)
                : List.copyOf(sites.subList(0, config.maxCandidateSites()));
    }

    private boolean isSiteValid(T dragon, DrinkSite candidate) {
        return isDrinkableWater(dragon, candidate.water())
                && canDrinkFromPosition(dragon, candidate.stance(), candidate.water());
    }

    private boolean isDryStandingPosition(T dragon, Vec3 position) {
        BlockPos feet = BlockPos.containing(position);
        return dragon.level().getFluidState(feet).isEmpty()
                && isDrySupport(dragon, feet.below());
    }

    private boolean isDrySupport(T dragon, BlockPos support) {
        var state = dragon.level().getBlockState(support);
        return !state.isAir()
                && !state.is(BlockTags.LEAVES)
                && state.getFluidState().isEmpty()
                && state.isFaceSturdy(dragon.level(), support, Direction.UP);
    }

    private boolean isSourceWaterAccessible(T dragon, BlockPos water) {
        var fluid = dragon.level().getFluidState(water);
        BlockPos above = water.above();
        var aboveState = dragon.level().getBlockState(above);
        return fluid.is(FluidTags.WATER)
                && fluid.isSource()
                && !dragon.level().getFluidState(above).is(FluidTags.WATER)
                && aboveState.getCollisionShape(dragon.level(), above).isEmpty();
    }

    private boolean isDrinkableWater(T dragon, BlockPos water) {
        return isSourceWaterAccessible(dragon, water);
    }

    private boolean canDrinkFromCurrentPosition(T dragon, DrinkSite candidate) {
        return canDrinkFromPosition(dragon, dragon.position(), candidate.water());
    }

    private boolean canDrinkFromPosition(T dragon, Vec3 position, BlockPos water) {
        Vec3 waterSurface = Vec3.atBottomCenterOf(water.above());
        return horizontalDistanceSqr(position, waterSurface)
                <= dragon.getDrinkingReach() * dragon.getDrinkingReach()
                && Math.abs(position.y - waterSurface.y) <= 2.5D;
    }

    private double approachArrivalTolerance(T dragon, DrinkSite candidate) {
        double endpointDistance = Math.sqrt(horizontalDistanceSqr(
                candidate.stance(),
                Vec3.atBottomCenterOf(candidate.water().above())
        ));
        double reachMargin = dragon.getDrinkingReach() - endpointDistance;
        return Mth.clamp(reachMargin - 0.1D, 0.15D, 1.0D);
    }

    @Nullable
    private String ineligibleReason(T dragon) {
        if (!dragon.isAlive() || dragon.isDying()) return "dying";
        if (dragon.isBaby()) return "baby";
        if (dragon.isAerial()) return "aerial";
        if (dragon.isInWaterOrBubble()) return "in-water";
        if (dragon.isOrderedToSit() || dragon.isInSittingPose() || dragon.isInSitTransition()) return "sitting";
        if (dragon.isVehicle() || dragon.isPassenger()) return "ridden";
        if (dragon.isInLove()) return "breeding";
        if (dragon.isAggressive() || dragon.getTarget() != null && dragon.getTarget().isAlive()) return "combat";
        if (dragon.getActiveAbility() != null || dragon.areRiderControlsLocked()) return "ability";
        if (dragon.isSleeping() || dragon.isSleepTransitioning() || dragon.wantsToSleep()) return "sleep";
        if (dragon.isTame() && dragon.getCommand() != 2) return "tamed-command";
        return null;
    }

    private void faceWater(T dragon, BlockPos water) {
        double dx = water.getX() + 0.5D - dragon.getX();
        double dz = water.getZ() + 0.5D - dragon.getZ();
        float desiredYaw = (float)(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float yaw = Mth.approachDegrees(dragon.getYRot(), desiredYaw, config.turnDegreesPerTick());
        dragon.setYRot(yaw);
        dragon.yBodyRot = yaw;
        dragon.setYHeadRot(yaw);
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return dx * dx + dz * dz;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("drink_phase", phase.name().toLowerCase(java.util.Locale.ROOT));
        details.put("drink_decision", decision);
        details.put("drink_site", site == null ? "none" : site.water().toShortString());
        details.put("drink_route_nodes", Integer.toString(routeNodes));
        details.put("drink_grounded", stableGroundTicks + "/" + STABLE_GROUND_TICKS);
        details.put("drink_candidates", candidateIndex + "/" + candidates.size());
        details.put("drink_water_sources", Integer.toString(waterSourcesScanned));
        details.put("drink_valid_sites", Integer.toString(validSitesFound));
        return details;
    }

    public record Config(double speedModifier,
                         int searchRadius,
                         int verticalSearchRange,
                         int maxCandidateSites,
                         int maxPathNodes,
                         int approachTimeoutTicks,
                         int alignTicks,
                         float turnDegreesPerTick,
                         int failureCooldownTicks,
                         int minCooldownTicks,
                         int maxCooldownTicks) {
        public Config {
            speedModifier = Math.max(0.01D, speedModifier);
            searchRadius = Math.max(1, searchRadius);
            verticalSearchRange = Math.max(1, verticalSearchRange);
            maxCandidateSites = Math.max(1, maxCandidateSites);
            maxPathNodes = Math.max(1, maxPathNodes);
            approachTimeoutTicks = Math.max(20, approachTimeoutTicks);
            alignTicks = Math.max(1, alignTicks);
            turnDegreesPerTick = Math.max(1.0F, turnDegreesPerTick);
            failureCooldownTicks = Math.max(0, failureCooldownTicks);
            minCooldownTicks = Math.max(0, minCooldownTicks);
            maxCooldownTicks = Math.max(minCooldownTicks, maxCooldownTicks);
        }

        public static Config standard() {
            return new Config(
                    0.65D,
                    12,
                    4,
                    12,
                    64,
                    240,
                    15,
                    10.0F,
                    100,
                    1200,
                    2400
            );
        }

        public Config withSearchRadius(int radius) {
            return new Config(
                    speedModifier,
                    radius,
                    verticalSearchRange,
                    maxCandidateSites,
                    maxPathNodes,
                    approachTimeoutTicks,
                    alignTicks,
                    turnDegreesPerTick,
                    failureCooldownTicks,
                    minCooldownTicks,
                    maxCooldownTicks
            );
        }
    }

    private record DrinkSite(Vec3 stance, BlockPos water) {
    }

    private enum Phase {
        IDLE,
        SETTLING,
        PLANNING,
        APPROACH,
        ALIGN,
        DRINKING,
        COMPLETE,
        FAILED
    }
}
