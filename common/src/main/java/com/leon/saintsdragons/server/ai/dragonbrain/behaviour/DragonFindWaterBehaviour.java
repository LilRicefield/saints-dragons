package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonOwnerFollowTarget;
import com.leon.saintsdragons.server.ai.navigation.PathNavigateGround;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.core.BlockPos;
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

    private final double speedModifier;
    @Nullable
    private BlockPos target;

    public DragonFindWaterBehaviour(double speedModifier) {
        this.speedModifier = speedModifier;
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (!dragon.onGround()
                || dragon.isInWaterOrBubble()
                || dragon.isInLove()
                || !dragon.shouldEnterWater()
                || isFollowingDryOwner(dragon)
                || (dragon.getTarget() == null && dragon.getRandom().nextInt(EXECUTION_CHANCE) != 0)) {
            return false;
        }
        target = findWater(dragon);
        return target != null;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        return target != null
                && !dragon.isInWaterOrBubble()
                && !dragon.isInLove()
                && !isFollowingDryOwner(dragon)
                && dragon.getAIMovement().isPathing();
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        if (context.dragon().getNavigation() instanceof PathNavigateGround navigation) {
            navigation.setWaterEntryAllowed(true);
        }
        moveToTarget(context.dragon());
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        moveToTarget(context.dragon());
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        context.dragon().getAIMovement().stop();
        if (context.dragon().getNavigation() instanceof PathNavigateGround navigation) {
            navigation.setWaterEntryAllowed(false);
        }
        target = null;
    }

    private void moveToTarget(T dragon) {
        if (target != null) {
            dragon.getAIMovement().moveToGroundPosition(Vec3.atBottomCenterOf(target), speedModifier, false);
        }
    }

    @Nullable
    private BlockPos findWater(T dragon) {
        RandomSource random = dragon.getRandom();
        int range = dragon.getWaterSearchRange();
        int halfRange = Math.max(1, range / 2);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int attempt = 0; attempt < TARGET_ATTEMPTS; attempt++) {
            BlockPos candidate = dragon.blockPosition().offset(
                    random.nextInt(range) - halfRange,
                    3,
                    random.nextInt(range) - halfRange
            );
            while (dragon.level().isEmptyBlock(candidate)
                    && candidate.getY() > dragon.level().getMinBuildHeight()) {
                candidate = candidate.below();
            }
            if (dragon.level().getFluidState(candidate).is(FluidTags.WATER)) {
                double distance = candidate.distSqr(dragon.blockPosition());
                if (distance < bestDistance) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        return best;
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
        return Map.of("water_target", target == null ? "none" : target.toShortString());
    }
}
