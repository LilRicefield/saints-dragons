package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class DragonWaterEntryTargeting {
    private static final int SEARCH_RADIUS = 32;
    private static final int MAX_SHORE_STEP = 2;
    private static final double MAX_ENTRY_DETOUR = 4.0D;

    private DragonWaterEntryTargeting() {
    }

    @Nullable
    public static <T extends RideableDragonBase> Target find(DragonBrainContext<T> context,
                                                              Vec3 destination,
                                                              Set<BlockPos> rejectedLandPositions) {
        T dragon = context.dragon();
        BlockPos origin = dragon.blockPosition();
        BlockPos destinationSurface = findDestinationSurfaceWater(context, destination);
        if (destinationSurface == null) {
            return null;
        }
        int bodyMargin = Math.max(1, Mth.ceil(dragon.getBbWidth() * 0.5F));

        for (int radius = 1; radius <= SEARCH_RADIUS; radius++) {
            Target best = null;
            double bestScore = Double.MAX_VALUE;
            for (int dx = -radius; dx <= radius; dx++) {
                Target north = evaluate(context, origin.offset(dx, 0, -radius), destinationSurface.getY(), bodyMargin, rejectedLandPositions);
                double northScore = score(dragon, north, destination);
                if (northScore < bestScore) {
                    best = north;
                    bestScore = northScore;
                }

                Target south = evaluate(context, origin.offset(dx, 0, radius), destinationSurface.getY(), bodyMargin, rejectedLandPositions);
                double southScore = score(dragon, south, destination);
                if (southScore < bestScore) {
                    best = south;
                    bestScore = southScore;
                }
            }
            for (int dz = -radius + 1; dz <= radius - 1; dz++) {
                Target west = evaluate(context, origin.offset(-radius, 0, dz), destinationSurface.getY(), bodyMargin, rejectedLandPositions);
                double westScore = score(dragon, west, destination);
                if (westScore < bestScore) {
                    best = west;
                    bestScore = westScore;
                }

                Target east = evaluate(context, origin.offset(radius, 0, dz), destinationSurface.getY(), bodyMargin, rejectedLandPositions);
                double eastScore = score(dragon, east, destination);
                if (eastScore < bestScore) {
                    best = east;
                    bestScore = eastScore;
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    public static boolean isCloseEnoughToEnter(RideableDragonBase dragon, Target target) {
        double radius = Math.max(2.5D, dragon.getBbWidth() * 0.75D + 1.0D);
        return dragon.position().distanceToSqr(target.landPosition()) <= radius * radius;
    }

    public static void moveIntoWater(RideableDragonBase dragon, Target target) {
        Vec3 toWater = target.waterPosition().subtract(dragon.position());
        Vec3 horizontal = new Vec3(toWater.x, 0.0D, toWater.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return;
        }

        Vec3 direction = horizontal.normalize();
        Vec3 velocity = dragon.getDeltaMovement();
        double horizontalBoost = dragon.horizontalCollision ? 0.45D : 0.34D;
        double upward = dragon.onGround() ? 0.20D : velocity.y;
        dragon.setDeltaMovement(
                velocity.x * 0.4D + direction.x * horizontalBoost,
                Math.max(velocity.y, upward),
                velocity.z * 0.4D + direction.z * horizontalBoost
        );
        dragon.getMoveControl().setWantedPosition(
                target.waterPosition().x,
                target.waterPosition().y - 0.35D,
                target.waterPosition().z,
                1.1D
        );
        dragon.hasImpulse = true;
    }

    @Nullable
    private static <T extends RideableDragonBase> Target evaluate(DragonBrainContext<T> context,
                                                                   BlockPos column,
                                                                   int waterSurfaceY,
                                                                   int bodyMargin,
                                                                   Set<BlockPos> rejectedLandPositions) {
        if (!context.level().hasChunkAt(column)) {
            return null;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos adjacentWater = findSurfaceWater(context, column.relative(direction), waterSurfaceY);
            if (adjacentWater == null) {
                continue;
            }
            BlockPos edgeLand = findStandableLand(
                    context,
                    column.getX(),
                    column.getZ(),
                    adjacentWater.getY() + 1,
                    MAX_SHORE_STEP
            );
            if (edgeLand == null) {
                continue;
            }
            BlockPos approachWater = findSurfaceWater(
                    context,
                    edgeLand.relative(direction, bodyMargin + 1),
                    adjacentWater.getY()
            );
            if (approachWater == null
                    || Math.abs(approachWater.getY() - adjacentWater.getY()) > MAX_SHORE_STEP) {
                continue;
            }
            BlockPos landPosition = findStandableLand(
                    context,
                    edgeLand.relative(direction.getOpposite(), bodyMargin).getX(),
                    edgeLand.relative(direction.getOpposite(), bodyMargin).getZ(),
                    edgeLand.getY(),
                    MAX_SHORE_STEP
            );
            if (landPosition == null || rejectedLandPositions.contains(landPosition)) {
                continue;
            }
            Vec3 waterPosition = Vec3.atCenterOf(approachWater);
            if (!hasBodyClearance(context, waterPosition)) {
                continue;
            }
            return new Target(Vec3.atBottomCenterOf(landPosition), waterPosition);
        }
        return null;
    }

    private static double score(RideableDragonBase dragon, @Nullable Target target, Vec3 destination) {
        if (target == null) {
            return Double.MAX_VALUE;
        }
        double currentDistance = Math.sqrt(dragon.position().distanceToSqr(destination));
        double entryDistance = Math.sqrt(target.waterPosition().distanceToSqr(destination));
        if (entryDistance > currentDistance + MAX_ENTRY_DETOUR) {
            return Double.MAX_VALUE;
        }
        return entryDistance * entryDistance * 4.0D
                + target.landPosition().distanceToSqr(dragon.position());
    }

    @Nullable
    private static <T extends RideableDragonBase> BlockPos findDestinationSurfaceWater(DragonBrainContext<T> context,
                                                                                        Vec3 destination) {
        BlockPos destinationPos = BlockPos.containing(destination);
        int highestY = Math.min(
                context.level().getMaxBuildHeight() - 1,
                Math.max(context.dragon().blockPosition().getY(), destinationPos.getY()) + 8
        );
        int lowestY = Math.max(
                context.level().getMinBuildHeight(),
                Math.min(context.dragon().blockPosition().getY(), destinationPos.getY()) - 16
        );
        for (int y = highestY; y >= lowestY; y--) {
            BlockPos water = new BlockPos(destinationPos.getX(), y, destinationPos.getZ());
            if (context.level().getFluidState(water).is(FluidTags.WATER)
                    && !context.level().getFluidState(water.above()).is(FluidTags.WATER)
                    && context.level().getBlockState(water).getCollisionShape(context.level(), water).isEmpty()) {
                return water;
            }
        }
        return null;
    }

    @Nullable
    private static <T extends RideableDragonBase> BlockPos findStandableLand(DragonBrainContext<T> context,
                                                                              int x,
                                                                              int z,
                                                                              int referenceY,
                                                                              int verticalRange) {
        for (int offset = 0; offset <= verticalRange; offset++) {
            BlockPos above = new BlockPos(x, referenceY + offset, z);
            if (isStandableLand(context, above)) {
                return above;
            }
            if (offset > 0) {
                BlockPos below = new BlockPos(x, referenceY - offset, z);
                if (isStandableLand(context, below)) {
                    return below;
                }
            }
        }
        return null;
    }

    private static <T extends RideableDragonBase> boolean isStandableLand(DragonBrainContext<T> context,
                                                                           BlockPos feet) {
        return context.level().getFluidState(feet).isEmpty()
                && !context.level().getBlockState(feet.below()).getCollisionShape(context.level(), feet.below()).isEmpty()
                && hasVerticalClearance(context, feet);
    }

    private static <T extends RideableDragonBase> boolean hasVerticalClearance(DragonBrainContext<T> context,
                                                                                BlockPos feet) {
        int requiredHeight = Math.max(2, Mth.ceil(context.dragon().getBbHeight()));
        for (int dy = 0; dy < requiredHeight; dy++) {
            BlockPos check = feet.above(dy);
            if (!context.level().getFluidState(check).isEmpty()
                    || !context.level().getBlockState(check).getCollisionShape(context.level(), check).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static <T extends RideableDragonBase> BlockPos findSurfaceWater(DragonBrainContext<T> context,
                                                                             BlockPos column,
                                                                             int referenceY) {
        for (int y = referenceY + MAX_SHORE_STEP + 2; y >= referenceY - MAX_SHORE_STEP - 2; y--) {
            BlockPos water = new BlockPos(column.getX(), y, column.getZ());
            if (context.level().getFluidState(water).is(FluidTags.WATER)
                    && !context.level().getFluidState(water.above()).is(FluidTags.WATER)
                    && context.level().getBlockState(water).getCollisionShape(context.level(), water).isEmpty()) {
                return water;
            }
        }
        return null;
    }

    private static <T extends RideableDragonBase> boolean hasBodyClearance(DragonBrainContext<T> context,
                                                                            Vec3 feetPosition) {
        T dragon = context.dragon();
        double halfWidth = dragon.getBbWidth() * 0.5D;
        AABB bounds = new AABB(
                feetPosition.x - halfWidth,
                feetPosition.y,
                feetPosition.z - halfWidth,
                feetPosition.x + halfWidth,
                feetPosition.y + dragon.getBbHeight(),
                feetPosition.z + halfWidth
        );
        return context.level().noCollision(dragon, bounds);
    }

    public record Target(Vec3 landPosition, Vec3 waterPosition) {
    }
}
