package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourInterruption;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourEligibility;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourSequence;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonDestinationMemory;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementProgress;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonSiteReservations;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncDragonPathfinder;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.interfaces.DrinkingDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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
import java.util.Locale;
import java.util.Map;

public final class DragonDrinkBehaviour<T extends RideableDragonBase & DrinkingDragon>
        extends DragonBehaviour<T> {
    private static final int STABLE_GROUND_TICKS = 3;
    private static final ResourceLocation DRINK_SITE = new ResourceLocation("saintsdragons", "drink_site");
    private static final DragonBehaviourEligibility.Policy DRINK_POLICY = DragonBehaviourEligibility.AMBIENT.also(
            DragonBehaviourEligibility.Blocker.AERIAL, DragonBehaviourEligibility.Blocker.IN_WATER);

    private final Config config;
    private final DragonMovementProgress progress = new DragonMovementProgress();
    private DragonBehaviourSequence<DragonBrainContext<T>> sequence;
    private DragonSiteReservations.Ticket siteTicket;
    private long movementGeneration = -1L;
    private boolean usedRememberedSites;
    private Phase phase = Phase.IDLE;
    private List<DrinkSite> candidates = List.of();
    private int candidateIndex;
    private int routeNodes;
    private int stableGroundTicks;
    private int waterSourcesScanned;
    private int validSitesFound;
    private long phaseEndsAt;
    private boolean completed;
    private boolean drinkingAnimationStarted;
    private String decision = "not-checked";
    @Nullable
    private DrinkSite site;

    public DragonDrinkBehaviour(Config config) {
        this.config = config;
    }

    @Override
    public boolean canYieldTo(DragonBrainContext<T> context, DragonBehaviourInterruption interruption) {
        return super.canYieldTo(context, interruption)
                && (phase != Phase.DRINKING || interruption == DragonBehaviourInterruption.WATER_ESCAPE);
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        String ineligibleReason = ineligibleReason(dragon);
        if (ineligibleReason != null) {
            decision = "ineligible:" + ineligibleReason;
            return false;
        }
        candidates = findSites(context, true);
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
        if (site != null && (!DragonDestinationMemory.loaded(context.level(), site.water())
                || !isDrinkableWater(context.dragon(), site.water()))) {
            decision = "interrupted:site-invalid";
            return false;
        }
        return true;
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        completed = false;
        drinkingAnimationStarted = false;
        candidateIndex = 0;
        routeNodes = 0;
        stableGroundTicks = 0;
        movementGeneration = -1L;
        progress.reset();
        sequence = new DragonBehaviourSequence<>(List.of(
                DragonBehaviourSequence.Step.of("approach", config.approachTimeoutTicks(), this::tickApproachStage),
                DragonBehaviourSequence.Step.of("align", config.alignTicks() + 2, this::tickAlignStage),
                DragonBehaviourSequence.Step.of("drink", Math.max(1, dragon.getDrinkingDurationTicks()) + 2, this::tickDrinkStage)
        ));
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
        sequence.start(context, context.gameTime());
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        if (siteTicket != null && !siteTicket.renew()) {
            phase = Phase.FAILED;
            decision = "interrupted:site-reservation-expired";
        }
        if (sequence == null) return;
        var result = sequence.tick(context, context.gameTime());
        if (result == DragonBehaviourSequence.Result.TIMED_OUT) {
            if (site != null && sequence.stepName().equals("approach")) rejectSite(context, site.water());
            phase = Phase.FAILED;
            decision = "failed:timeout:" + sequence.stepName();
        }
    }

    private DragonBehaviourSequence.Result tickApproachStage(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        switch (phase) {
            case SETTLING -> tickSettling(context);
            case PLANNING -> dragon.getAIMovement().setGroundIdle();
            case APPROACH -> tickApproach(context, dragon);
            default -> {
            }
        }
        return phase == Phase.FAILED ? DragonBehaviourSequence.Result.FAILURE
                : phase == Phase.ALIGN ? DragonBehaviourSequence.Result.SUCCESS : DragonBehaviourSequence.Result.RUNNING;
    }

    private DragonBehaviourSequence.Result tickAlignStage(DragonBrainContext<T> context) {
        if (phase == Phase.ALIGN) tickAlignment(context, context.dragon());
        return phase == Phase.FAILED ? DragonBehaviourSequence.Result.FAILURE
                : phase == Phase.DRINKING ? DragonBehaviourSequence.Result.SUCCESS : DragonBehaviourSequence.Result.RUNNING;
    }

    private DragonBehaviourSequence.Result tickDrinkStage(DragonBrainContext<T> context) {
        if (phase == Phase.DRINKING) tickDrinking(context, context.dragon());
        return phase == Phase.FAILED ? DragonBehaviourSequence.Result.FAILURE
                : phase == Phase.COMPLETE ? DragonBehaviourSequence.Result.SUCCESS : DragonBehaviourSequence.Result.RUNNING;
    }

    private void tickSettling(DragonBrainContext<T> context) {
        T dragon = context.dragon();
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
        requestNextPath(context);
    }

    private void tickApproach(DragonBrainContext<T> context, T dragon) {
        dragon.setGroundMoveStateFromAI(1);
        if (!dragon.getAIMovement().isMovementCommandCurrent(movementGeneration)) {
            phase = Phase.FAILED;
            decision = "interrupted:movement-replaced";
            return;
        }
        boolean arrived = site != null && dragon.getAIMovement().hasArrived()
                && isDryStandingPosition(dragon, dragon.position()) && canDrinkFromCurrentPosition(dragon, site);
        var path = dragon.getNavigation().getPath();
        var result = progress.observe(dragon.position(), context.gameTime(), site != null
                        && DragonDestinationMemory.loaded(context.level(), site.water()) && isDrinkableWater(dragon, site.water()),
                arrived, dragon.getAIMovement().hasFailed(), path == null ? -1 : path.getNextNodeIndex());
        if (result == DragonMovementProgress.Status.IN_PROGRESS) return;
        if (result != DragonMovementProgress.Status.ARRIVED) {
            if (site != null) rejectSite(context, site.water());
            dragon.getAIMovement().stopIfMovementCommandCurrent(movementGeneration);
            phase = Phase.PLANNING;
            decision = "retry:approach:" + result.name().toLowerCase(Locale.ROOT);
            requestNextPath(context);
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
        drinkingAnimationStarted = true;
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
            context.utilities().destinations().remember(DRINK_SITE, context.level(), site.water(),
                    DragonDestinationMemory.Outcome.SUCCESS, 20 * 300);
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
        if (sequence != null) sequence.cancel(context);
        context.dragon().getAIMovement().stop();
        if (drinkingAnimationStarted && !completed) {
            context.dragon().stopDrinkingAnimation();
        }
        drinkingAnimationStarted = false;
        phase = Phase.IDLE;
        candidates = List.of();
        candidateIndex = 0;
        routeNodes = 0;
        stableGroundTicks = 0;
        site = null;
        siteTicket = null;
        movementGeneration = -1L;
        context.dragon().combatManager.recordAiDecision("drink", decision);
    }

    private void requestNextPath(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (siteTicket != null) siteTicket.release();
        siteTicket = null;
        DrinkSite candidate = null;
        while (true) {
            if (candidateIndex >= candidates.size() && usedRememberedSites) {
                candidates = findSites(context, false);
                candidateIndex = 0;
            }
            if (candidateIndex >= candidates.size()) break;
            DrinkSite next = candidates.get(candidateIndex++);
            if (!DragonDestinationMemory.loaded(context.level(), next.water())
                    || !isDrinkableWater(dragon, next.water())
                    || context.utilities().destinations().rejected(DRINK_SITE, context.level(), next.water())) continue;
            var ticket = DragonSiteReservations.claim(context.level(), DRINK_SITE, next.water(), dragon.getUUID(), 60);
            if (ticket == null) continue;
            siteTicket = ticket;
            context.utilities().resources().onStop(this, "drink-site", ticket::release);
            candidate = next;
            break;
        }
        if (candidate == null) {
            phase = Phase.FAILED;
            if (!decision.startsWith("path-rejected:")) {
                decision = "failed:no-route";
            }
            return;
        }

        if (isDryStandingPosition(dragon, dragon.position())
                && canDrinkFromCurrentPosition(dragon, candidate)) {
            site = new DrinkSite(dragon.position(), candidate.water());
            dragon.getAIMovement().setGroundIdle();
            phase = Phase.ALIGN;
            decision = "aligning:nearby-water";
            phaseEndsAt = dragon.level().getGameTime() + config.alignTicks();
            return;
        }

        site = candidate;
        var request = context.utilities().resources().beginTask(this, "drink-path");
        long requestedMovementGeneration = dragon.getAIMovement().getMovementCommandGeneration();
        DrinkSite requestedSite = candidate;
        request.bind(AsyncDragonPathfinder.calculateGroundPathAsync(
                dragon,
                candidate.stance(),
                0,
                true,
                path -> completeTask(dragon, request,
                        () -> acceptPath(context, requestedSite, requestedMovementGeneration, path))
        ));
    }

    private void acceptPath(DragonBrainContext<T> context, DrinkSite candidate,
                            long requestedMovementGeneration, @Nullable Path path) {
        T dragon = context.dragon();
        if (phase != Phase.PLANNING || dragon.isRemoved()) {
            return;
        }
        if (!dragon.getAIMovement().isMovementCommandCurrent(requestedMovementGeneration)) {
            phase = Phase.FAILED;
            decision = "interrupted:movement-replaced";
            return;
        }
        if (!DragonDestinationMemory.loaded(context.level(), candidate.water())) {
            phase = Phase.FAILED;
            decision = "interrupted:water-unloaded";
            return;
        }
        String ineligible = ineligibleReason(dragon);
        if (ineligible != null || siteTicket == null || !siteTicket.renew()) {
            phase = Phase.FAILED;
            decision = "interrupted:" + (ineligible == null ? "site-reservation-expired" : ineligible);
            return;
        }
        String rejection = pathRejection(dragon, candidate, path);
        if (rejection != null) {
            decision = "path-rejected:" + rejection;
            rejectSite(context, candidate.water());
            requestNextPath(context);
            return;
        }
        Vec3 endpoint = path.getEntityPosAtNode(dragon, path.getNodeCount() - 1);
        DrinkSite reachableSite = new DrinkSite(endpoint, candidate.water());
        if (!isSiteValid(dragon, reachableSite)) {
            decision = "path-rejected:unsafe-endpoint";
            rejectSite(context, candidate.water());
            requestNextPath(context);
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
            rejectSite(context, candidate.water());
            requestNextPath(context);
            return;
        }
        phase = Phase.APPROACH;
        movementGeneration = dragon.getAIMovement().getMovementCommandGeneration();
        progress.begin(reachableSite.stance(), dragon.position(), context.gameTime(), config.approachTimeoutTicks(), 80, 0.5D);
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
        if (!dragon.level().hasChunkAt(BlockPos.containing(endpoint))) return "unloaded-endpoint";
        if (!isDryStandingPosition(dragon, endpoint)) {
            return "wet-endpoint";
        }
        return canDrinkFromPosition(dragon, endpoint, candidate.water())
                ? null
                : "endpoint-out-of-reach";
    }

    private List<DrinkSite> findSites(DragonBrainContext<T> context, boolean useMemory) {
        T dragon = context.dragon();
        waterSourcesScanned = 0;
        validSitesFound = 0;
        usedRememberedSites = false;
        if (useMemory) {
            var remembered = context.utilities().destinations().known(DRINK_SITE, context.level(), dragon.blockPosition(),
                    config.searchRadius(), position -> isDrinkableWater(dragon, position));
            if (!remembered.isEmpty()) {
                usedRememberedSites = true;
                validSitesFound = remembered.size();
                return remembered.stream().limit(config.maxCandidateSites())
                        .map(water -> new DrinkSite(Vec3.atBottomCenterOf(water.above()), water)).toList();
            }
        }
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
                    if (!DragonDestinationMemory.loaded(context.level(), water)
                            || !isSourceWaterAccessible(dragon, water)
                            || context.utilities().destinations().rejected(DRINK_SITE, context.level(), water)) {
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

    private void rejectSite(DragonBrainContext<T> context, BlockPos water) {
        context.utilities().destinations().remember(DRINK_SITE, context.level(), water,
                DragonDestinationMemory.Outcome.FAILED, 20 * 30);
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
        String common = DragonBehaviourEligibility.rejection(dragon, DRINK_POLICY);
        if (common != null) return common;
        if (dragon.isBaby()) return "baby";
        if (dragon.wantsToSleep()) return "sleep";
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
        details.put("drink_phase", phase.name().toLowerCase(Locale.ROOT));
        details.put("drink_decision", decision);
        details.put("sequence", sequence == null ? "none" : sequence.stepName() + ":" + sequence.result().name());
        details.put("progress", progress.status().name());
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
