package com.leon.saintsdragons.server.ai.navigation.async;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.interfaces.SemiAquaticDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public final class DragonLandingSites {
    private DragonLandingSites() { }

    public static @Nullable Vec3 find(Mob dragon, @Nullable LivingEntity target) {
        Vec3 anchor = target != null && target.isAlive() ? target.position() : dragon.position();
        double separation = target == null ? 0.0D
                : Math.max(5.0D, (dragon.getBbWidth() + target.getBbWidth()) * 0.5D + 2.0D);
        return findNear(dragon, anchor, 32, separation, position -> true);
    }

    public static @Nullable Vec3 findNear(Mob dragon, Vec3 target) {
        return findNear(dragon, target, 12, 0.0D, position -> true);
    }

    public static @Nullable Vec3 findForOwner(Mob dragon, Vec3 owner, double stopDistance,
                                             double maxDistance, double maxVerticalDelta) {
        return findNear(dragon, owner, Mth.ceil(maxDistance), stopDistance,
                position -> horizontalDistance(position, owner) <= maxDistance
                        && Math.abs(position.y - owner.y) <= maxVerticalDelta);
    }

    private static @Nullable Vec3 findNear(Mob dragon, Vec3 anchor, int radius,
                                           double separation, Predicate<Vec3> allowed) {
        if (dragon.level().isClientSide) return null;
        DragonFlightSpace space = space(dragon);
        Set<Long> columns = new HashSet<>();
        Vec3 best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        Vec3 bestWater = null;
        double bestWaterScore = Double.POSITIVE_INFINITY;
        boolean allowWater = canLandOnWater(dragon);
        for (int ring = 0; ring <= radius; ring += 4) {
            int samples = ring == 0 ? 1 : 16;
            for (int sample = 0; sample < samples; sample++) {
                double angle = sample * Math.PI * 2.0D / samples;
                BlockPos column = BlockPos.containing(anchor.x + Math.cos(angle) * ring,
                        Math.max(anchor.y, dragon.getY()), anchor.z + Math.sin(angle) * ring);
                if (!columns.add(BlockPos.asLong(column.getX(), 0, column.getZ()))) continue;
                BlockPos ground = DragonFlightSpace.findLandingGround(dragon, column, column.getY());
                if (ground == null) {
                    // Water is a fallback only: finish checking the bounded area for dry land first.
                    if (!allowWater || best != null) continue;
                    Vec3 water = findWaterSurface(dragon, column);
                    if (water == null || !allowed.test(water)) continue;
                    double score = Math.abs(horizontalDistance(water, anchor) - separation) * 2.0D
                            + dragon.position().distanceTo(water) * 0.1D;
                    if (score < bestWaterScore && isValid(dragon, water)
                            && space.corridorClear(water.add(0, FlightLandingMotion.TOUCHDOWN_HEIGHT, 0), water)) {
                        bestWater = water;
                        bestWaterScore = score;
                    }
                    continue;
                }
                Vec3 touchdown = Vec3.atBottomCenterOf(ground.above());
                double score = Math.abs(horizontalDistance(touchdown, anchor) - separation) * 2.0D
                        + dragon.position().distanceTo(touchdown) * 0.1D;
                if (score >= bestScore || !allowed.test(touchdown) || !isValid(dragon, touchdown)) continue;
                // Only the last short descent needs a straight corridor; the route may go around obstacles.
                Vec3 above = touchdown.add(0.0D, FlightLandingMotion.TOUCHDOWN_HEIGHT, 0.0D);
                if (!space.corridorClear(above, touchdown)) continue;
                best = touchdown;
                bestScore = score;
            }
        }
        return best != null ? best : bestWater;
    }

    public static boolean isValid(Mob dragon, Vec3 touchdown) {
        if (touchdown == null || !Double.isFinite(touchdown.x) || !Double.isFinite(touchdown.y)
                || !Double.isFinite(touchdown.z) || !space(dragon).fits(touchdown)) return false;
        return hasDryFootprint(dragon, touchdown) || isWaterSurface(dragon, touchdown);
    }

    private static boolean hasDryFootprint(Mob dragon, Vec3 touchdown) {
        double halfWidth = dragon.getBbWidth() * 0.5D;
        int y = Mth.floor(touchdown.y - 0.05D);
        for (int x = Mth.floor(touchdown.x - halfWidth + 0.05D);
             x <= Mth.floor(touchdown.x + halfWidth - 0.05D); x++) {
            for (int z = Mth.floor(touchdown.z - halfWidth + 0.05D);
                 z <= Mth.floor(touchdown.z + halfWidth - 0.05D); z++) {
                BlockPos support = new BlockPos(x, y, z);
                if (!dragon.level().hasChunkAt(support)) return false;
                var state = dragon.level().getBlockState(support);
                if (!state.getFluidState().isEmpty()
                        || !state.isFaceSturdy(dragon.level(), support, Direction.UP)) return false;
            }
        }
        return true;
    }

    public static boolean canLandOnWater(Mob dragon) {
        return dragon instanceof DragonFlightCapable flight && flight.canAiLandOnWater()
                && dragon instanceof SemiAquaticDragon
                && dragon instanceof RideableDragonBase rideable && rideable.canSwim();
    }

    /** The flight endpoint is just above the water block; only the final descent enters the fluid. */
    private static @Nullable Vec3 findWaterSurface(Mob dragon, BlockPos column) {
        if (!dragon.level().hasChunkAt(column)) return null;
        int top = Math.min(column.getY(), dragon.level().getHeight(
                Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ()) - 1);
        int bottom = Math.max(dragon.level().getMinBuildHeight(), top - 96);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(column.getX(), top, column.getZ());
        for (int y = top; y >= bottom; y--) {
            cursor.setY(y);
            var state = dragon.level().getBlockState(cursor);
            if (!state.getFluidState().isEmpty()) {
                return state.getFluidState().is(FluidTags.WATER)
                        ? Vec3.atBottomCenterOf(cursor.above()) : null;
            }
            if (!state.getCollisionShape(dragon.level(), cursor).isEmpty()) return null;
        }
        return null;
    }

    public static boolean isWaterSurface(Mob dragon, Vec3 touchdown) {
        if (!canLandOnWater(dragon)) return false;
        double halfWidth = dragon.getBbWidth() * 0.5D;
        int surface = Mth.floor(touchdown.y - 0.05D);
        if (Math.abs(touchdown.y - (surface + 1.0D)) > 0.01D) return false;
        for (int x = Mth.floor(touchdown.x - halfWidth + 0.05D);
             x <= Mth.floor(touchdown.x + halfWidth - 0.05D); x++) {
            for (int z = Mth.floor(touchdown.z - halfWidth + 0.05D);
                 z <= Mth.floor(touchdown.z + halfWidth - 0.05D); z++) {
                // Require calm, open water and enough depth to enter without striking the bottom.
                for (int depth = 0; depth < 2; depth++) {
                    BlockPos pos = new BlockPos(x, surface - depth, z);
                    if (!dragon.level().hasChunkAt(pos)) return false;
                    var state = dragon.level().getBlockState(pos);
                    if (!state.getFluidState().is(FluidTags.WATER)
                            || !state.getFluidState().isSource()
                            || !state.getCollisionShape(dragon.level(), pos).isEmpty()) return false;
                }
                BlockPos above = new BlockPos(x, surface + 1, z);
                if (!dragon.level().getFluidState(above).isEmpty()) return false;
            }
        }
        return true;
    }

    private static DragonFlightSpace space(Mob dragon) {
        return dragon instanceof RideableDragonBase rideable
                ? rideable.getAIMovement().flightSpace() : new DragonFlightSpace(dragon);
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        return first.subtract(second).horizontalDistance();
    }
}
