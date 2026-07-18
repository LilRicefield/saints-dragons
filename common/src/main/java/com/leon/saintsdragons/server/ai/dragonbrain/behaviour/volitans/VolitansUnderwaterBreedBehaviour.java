package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.volitans;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonBreedBehaviour;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class VolitansUnderwaterBreedBehaviour extends DragonBreedBehaviour<Volitans> {
    private static final int NEST_SEARCH_RADIUS = 8;
    private static final int NEST_SEARCH_DEPTH = 12;
    private static final double COURTSHIP_SWIM_SPEED_SCALE = 0.20D;

    public VolitansUnderwaterBreedBehaviour(double speedModifier,
                                            double partnerRange,
                                            double breedDistanceSqr) {
        super(speedModifier, Volitans.class, partnerRange, breedDistanceSqr);
    }

    @Override
    protected void stop(DragonBrainContext<Volitans> context) {
        super.stop(context);
        Volitans dragon = context.dragon();
        if (dragon.isInWaterOrBubble()) {
            Vec3 velocity = dragon.getDeltaMovement();
            dragon.setDeltaMovement(velocity.x * 0.8D, velocity.y * 0.8D, velocity.z * 0.8D);
        }
    }

    @Override
    protected void tick(DragonBrainContext<Volitans> context) {
        Volitans dragon = context.dragon();
        if (partner == null) {
            return;
        }

        dragon.getLookControl().setLookAt(partner, 10.0F, dragon.getMaxHeadXRot());
        boolean close = closeEnough(dragon);
        if (close) {
            stopMovement(dragon);
        } else if (dragon.isInWaterOrBubble() && partner.isInWaterOrBubble()) {
            dragon.getAIMovement().setWaypoint(
                    partner,
                    speedModifier * dragon.getSwimSpeed() * COURTSHIP_SWIM_SPEED_SCALE,
                    false
            );
        }

        loveTime = Math.min(60, loveTime + 1);
        if (loveTime >= 60 && close) {
            Volitans female = dragon.isFemale() ? dragon : partner;
            if (findEggLayingPosition(context.level(), female) != null) {
                breed(context.level(), dragon);
            }
        }
    }

    @Override
    protected boolean breedingAllowed(Volitans dragon) {
        return super.breedingAllowed(dragon)
                && dragon.isInWaterOrBubble()
                && !dragon.isBurrowing()
                && !dragon.isSleepLocked()
                && !dragon.isVehicle();
    }

    @Override
    protected boolean canBreedInWater(Volitans dragon) {
        return true;
    }

    @Nullable
    @Override
    protected Volitans findMate(ServerLevel level, Volitans dragon) {
        Volitans mate = super.findMate(level, dragon);
        return mate != null && mate.isInWaterOrBubble() ? mate : null;
    }

    @Nullable
    @Override
    protected BlockPos findEggLayingPosition(ServerLevel level, Volitans female) {
        if (!female.isInWaterOrBubble() || partner == null) {
            return null;
        }
        BlockPos midpoint = BlockPos.containing(
                (female.getX() + partner.getX()) * 0.5D,
                (female.getY() + partner.getY()) * 0.5D,
                (female.getZ() + partner.getZ()) * 0.5D
        );
        BlockPos aroundMidpoint = findUnderwaterEggLayingPosition(level, midpoint);
        return aroundMidpoint != null
                ? aroundMidpoint
                : findUnderwaterEggLayingPosition(level, female.blockPosition());
    }

    @Nullable
    private BlockPos findUnderwaterEggLayingPosition(ServerLevel level, BlockPos start) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int x = -NEST_SEARCH_RADIUS; x <= NEST_SEARCH_RADIUS; x++) {
            for (int z = -NEST_SEARCH_RADIUS; z <= NEST_SEARCH_RADIUS; z++) {
                BlockPos candidate = findUnderwaterFloor(level, start.offset(x, 0, z));
                if (candidate == null) {
                    continue;
                }
                double distance = candidate.distSqr(start);
                if (distance < bestDistance) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    @Nullable
    private BlockPos findUnderwaterFloor(ServerLevel level, BlockPos start) {
        for (int depth = 0; depth <= NEST_SEARCH_DEPTH; depth++) {
            BlockPos floor = start.below(depth);
            if (!level.hasChunkAt(floor)) {
                continue;
            }
            BlockPos egg = floor.above();
            BlockState floorState = level.getBlockState(floor);
            if (!floorState.isAir()
                    && floorState.isSolidRender(level, floor)
                    && level.getBlockState(egg).getFluidState().is(FluidTags.WATER)) {
                return egg;
            }
        }
        return null;
    }
}
