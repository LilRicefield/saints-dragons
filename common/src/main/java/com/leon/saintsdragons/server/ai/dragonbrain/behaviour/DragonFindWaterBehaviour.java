package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonOwnerFollowTarget;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviourEligibility;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonDestinationMemory;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementProgress;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonRetryState;
import com.leon.saintsdragons.server.ai.navigation.PathNavigateGround;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class DragonFindWaterBehaviour<T extends RideableDragonBase & SemiAquaticDragon>
        extends DragonBehaviour<T> {
    private static final int EXECUTION_CHANCE = 30;
    private static final int TARGET_ATTEMPTS = 15;
    private static final ResourceLocation WATER_ENTRY = new ResourceLocation("saintsdragons", "water_entry");

    private final double speedModifier;
    private final DragonMovementProgress progress = new DragonMovementProgress();
    private final DragonRetryState retries = new DragonRetryState(3, 10, 40);
    private long movementGeneration = -1L;
    private boolean finished;
    private String decision = "idle";
    @Nullable
    private BlockPos target;

    public DragonFindWaterBehaviour(double speedModifier) {
        this.speedModifier = speedModifier;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (DragonBehaviourEligibility.rejection(dragon, DragonBehaviourEligibility.AMBIENT) != null
                || !dragon.onGround()
                || dragon.isInWaterOrBubble()
                || dragon.isInLove()
                || !dragon.shouldEnterWater()
                || isFollowingDryOwner(dragon)
                || (dragon.getTarget() == null && dragon.getRandom().nextInt(EXECUTION_CHANCE) != 0)) {
            return false;
        }
        target = findWater(context);
        return target != null;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (dragon.isInWaterOrBubble()) {
            if (target != null && validWater(context, target)) {
                context.utilities().destinations().remember(WATER_ENTRY, context.level(), target,
                        DragonDestinationMemory.Outcome.SUCCESS, 20 * 300);
            }
            decision = "arrived";
            finished = true;
            return false;
        }
        if (movementGeneration >= 0 && !dragon.getAIMovement().isMovementCommandCurrent(movementGeneration)) {
            decision = "superseded";
            return false;
        }
        return !finished && !isFollowingDryOwner(dragon)
                && DragonBehaviourEligibility.rejection(dragon, DragonBehaviourEligibility.AMBIENT) == null;
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        finished = false;
        retries.reset();
        progress.reset();
        movementGeneration = -1L;
        if (context.dragon().getNavigation() instanceof PathNavigateGround navigation) {
            navigation.setWaterEntryAllowed(true);
        }
        beginAttempt(context);
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        if (target == null) {
            beginAttempt(context);
            return;
        }
        var path = context.dragon().getNavigation().getPath();
        var status = progress.observe(context.dragon().position(), context.gameTime(),
                validWater(context, target), context.dragon().isInWaterOrBubble(),
                context.dragon().getAIMovement().hasFailed(), path == null ? -1 : path.getNextNodeIndex());
        if (status == DragonMovementProgress.Status.BLOCKED || status == DragonMovementProgress.Status.TIMED_OUT
                || status == DragonMovementProgress.Status.INVALID) {
            failAttempt(context, status.name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        if (movementGeneration >= 0) context.dragon().getAIMovement().stopIfMovementCommandCurrent(movementGeneration);
        if (context.dragon().getNavigation() instanceof PathNavigateGround navigation) {
            navigation.setWaterEntryAllowed(false);
        }
        target = null;
        movementGeneration = -1L;
        context.dragon().combatManager.recordAiDecision("find-water", decision);
    }

    @Override
    protected int cooldownForTicks(DragonBrainContext<T> context) {
        return finished && !"arrived".equals(decision) ? 100 : 0;
    }

    private void beginAttempt(DragonBrainContext<T> context) {
        if (!retries.beginAttempt(context.gameTime())) return;
        if (target == null) target = findWater(context);
        T dragon = context.dragon();
        if (target == null || !validWater(context, target)) {
            failAttempt(context, "no-destination");
            return;
        }
        Vec3 destination = Vec3.atBottomCenterOf(target);
        if (!dragon.getAIMovement().moveToGroundPosition(destination, speedModifier, false)) {
            failAttempt(context, "route-rejected");
            return;
        }
        movementGeneration = dragon.getAIMovement().getMovementCommandGeneration();
        progress.begin(destination, dragon.position(), context.gameTime(), 240, 80, 0.5D);
        decision = "approaching";
    }

    private void failAttempt(DragonBrainContext<T> context, String reason) {
        if (target != null && validWater(context, target)) {
            context.utilities().destinations().remember(WATER_ENTRY, context.level(), target,
                    DragonDestinationMemory.Outcome.FAILED, 20 * 30);
        }
        if (movementGeneration >= 0) context.dragon().getAIMovement().stopIfMovementCommandCurrent(movementGeneration);
        movementGeneration = -1L;
        target = null;
        progress.reset();
        retries.failed(context.gameTime());
        finished = retries.exhausted();
        decision = (finished ? "failed:" : "retry:") + reason;
    }

    @Nullable
    private BlockPos findWater(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        RandomSource random = dragon.getRandom();
        int range = Math.max(1, dragon.getWaterSearchRange());
        var remembered = context.utilities().destinations().known(WATER_ENTRY, context.level(), dragon.blockPosition(),
                range, position -> validWater(context, position));
        if (!remembered.isEmpty()) return remembered.get(0);
        int halfRange = Math.max(1, range / 2);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int attempt = 0; attempt < TARGET_ATTEMPTS; attempt++) {
            BlockPos candidate = dragon.blockPosition().offset(
                    random.nextInt(range) - halfRange,
                    3,
                    random.nextInt(range) - halfRange
            );
            if (!DragonDestinationMemory.loaded(context.level(), candidate)) continue;
            while (dragon.level().isEmptyBlock(candidate)
                    && candidate.getY() > dragon.level().getMinBuildHeight()) {
                candidate = candidate.below();
            }
            if (validWater(context, candidate)
                    && !context.utilities().destinations().rejected(WATER_ENTRY, context.level(), candidate)) {
                double distance = candidate.distSqr(dragon.blockPosition());
                if (distance < bestDistance) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private boolean validWater(DragonBrainContext<T> context, BlockPos position) {
        return DragonDestinationMemory.loaded(context.level(), position)
                && context.level().getFluidState(position).is(FluidTags.WATER);
    }

    private boolean isFollowingDryOwner(T dragon) {
        if (!dragon.isTame() || dragon.getCommand() != 0) {
            return false;
        }
        LivingEntity owner = dragon.getOwner();
        return owner != null
                && owner.isAlive()
                && owner.level() == dragon.level()
                && !DragonOwnerFollowTarget.anchor(owner).isInWaterOrBubble();
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of("water_target", target == null ? "none" : target.toShortString(),
                "decision", decision, "attempts", Integer.toString(retries.attempts()),
                "progress", progress.status().name());
    }
}
