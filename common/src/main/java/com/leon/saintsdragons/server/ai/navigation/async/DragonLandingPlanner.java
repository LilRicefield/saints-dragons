package com.leon.saintsdragons.server.ai.navigation.async;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class DragonLandingPlanner {
    private static final int MAX_SEARCH_RADIUS = 32;
    private static final int EXPLICIT_SEARCH_RADIUS = 12;
    private static final int RADIUS_STEP = 4;
    private static final int ANGLE_SAMPLES = 16;
    private static final double MAX_ENTRY_VERTICAL_SLOPE = 0.55D;
    private static final double MAX_ENTRY_RUNWAY = 24.0D;
    private static final double[] APPROACH_ANGLE_OFFSETS = {
            0.0D, 30.0D, -30.0D, 60.0D, -60.0D, 90.0D, -90.0D, 180.0D
    };

    private DragonLandingPlanner() {
    }

    public static @Nullable DragonLandingPlan findPlan(Mob dragon, @Nullable LivingEntity target) {
        if (target != null && target.isAlive()) {
            double desiredTargetDistance = Math.max(
                    5.0D,
                    dragon.getBbWidth() * 0.65D + target.getBbWidth() * 0.5D + 2.0D
            );
            return findBestPlan(
                    dragon,
                    target.position(),
                    MAX_SEARCH_RADIUS,
                    desiredTargetDistance,
                    preferredHeading(dragon)
            );
        }

        Vec3 heading = preferredHeading(dragon);
        double forwardLead = Math.max(18.0D, dragon.getBbWidth() * 3.5D);
        Vec3 preferredTouchdown = dragon.position().add(heading.scale(forwardLead));
        return findBestPlan(dragon, preferredTouchdown, MAX_SEARCH_RADIUS, 0.0D, heading);
    }

    public static @Nullable DragonLandingPlan findPlanNear(Mob dragon, Vec3 requestedTouchdown) {
        if (requestedTouchdown == null) {
            return null;
        }
        return findBestPlan(
                dragon,
                requestedTouchdown,
                EXPLICIT_SEARCH_RADIUS,
                0.0D,
                preferredHeading(dragon)
        );
    }

    static boolean isTouchdownStillValid(Mob dragon, DragonLandingPlan plan) {
        if (plan == null || dragon.level().isClientSide) {
            return false;
        }
        Vec3 touchdown = plan.touchdown();
        BlockPos ground = BlockPos.containing(
                touchdown.x,
                touchdown.y - 0.05D,
                touchdown.z
        );
        return dragon.level().hasChunkAt(ground) && hasLandingFootprint(dragon, ground);
    }

    private static @Nullable DragonLandingPlan findBestPlan(Mob dragon,
                                                             Vec3 anchor,
                                                             int maxRadius,
                                                             double desiredAnchorDistance,
                                                             Vec3 preferredHeading) {
        if (dragon.level().isClientSide) {
            return null;
        }

        Set<Long> sampledColumns = new HashSet<>();
        DragonLandingPlan bestPlan = null;
        double bestScore = Double.POSITIVE_INFINITY;
        double baseAngle = Math.atan2(preferredHeading.z, preferredHeading.x);

        for (int radius = 0; radius <= maxRadius; radius += RADIUS_STEP) {
            int samples = radius == 0 ? 1 : ANGLE_SAMPLES;
            for (int sample = 0; sample < samples; sample++) {
                int signedSample = sample == 0
                        ? 0
                        : ((sample + 1) / 2) * (sample % 2 == 1 ? 1 : -1);
                double angle = baseAngle + signedSample * (Math.PI * 2.0D / samples);
                int x = Mth.floor(anchor.x + Math.cos(angle) * radius);
                int z = Mth.floor(anchor.z + Math.sin(angle) * radius);
                BlockPos column = new BlockPos(x, Mth.floor(anchor.y), z);
                long columnKey = BlockPos.asLong(x, 0, z);
                if (!sampledColumns.add(columnKey) || !dragon.level().hasChunkAt(column)) {
                    continue;
                }

                BlockPos ground = findGround(dragon, column, Mth.floor(anchor.y));
                if (ground == null) {
                    continue;
                }

                Vec3 touchdown = new Vec3(x + 0.5D, ground.getY() + 1.0D, z + 0.5D);
                if (!hasMinimumRunway(dragon, touchdown) || !hasLandingFootprint(dragon, ground)) {
                    continue;
                }

                double anchorDistance = horizontalDistance(touchdown, anchor);
                double anchorPenalty = Math.abs(anchorDistance - desiredAnchorDistance) * 2.0D;
                double travelDistance = horizontalDistance(dragon.position(), touchdown);
                Vec3 toTouchdown = horizontalDirection(dragon.position(), touchdown, preferredHeading);
                double forwardAlignment = Mth.clamp(toTouchdown.dot(preferredHeading), -1.0D, 1.0D);
                double alignmentPenalty = (1.0D - forwardAlignment) * 5.0D;
                double verticalPenalty = Math.abs(dragon.getY() - touchdown.y) * 0.04D;
                double baseScore = anchorPenalty
                        + travelDistance * 0.08D
                        + alignmentPenalty
                        + verticalPenalty;
                if (baseScore >= bestScore) {
                    continue;
                }

                DirectionPlan directionPlan = findClearDirectionPlan(dragon, touchdown, preferredHeading);
                if (directionPlan == null) {
                    continue;
                }
                double score = baseScore + directionPlan.directionPenalty();
                if (score < bestScore) {
                    bestScore = score;
                    bestPlan = directionPlan.plan();
                }
            }
        }
        return bestPlan;
    }

    private static @Nullable DirectionPlan findClearDirectionPlan(Mob dragon,
                                                                   Vec3 touchdown,
                                                                   Vec3 preferredHeading) {
        Vec3 naturalIncoming = horizontalDirection(dragon.position(), touchdown, preferredHeading);
        double approachDistance = Math.max(12.0D, dragon.getBbWidth() * 3.0D);
        double glideDistance = Math.max(5.0D, approachDistance * 0.45D);
        double flareDistance = Math.max(1.75D, dragon.getBbWidth() * 0.40D);
        double approachHeight = Math.max(6.0D, dragon.getBbHeight() * 1.5D);
        double glideHeight = Math.max(3.0D, dragon.getBbHeight() * 0.72D);
        double flareHeight = Math.max(0.8D, dragon.getBbHeight() * 0.20D);

        DirectionPlan best = null;
        for (double angleOffset : APPROACH_ANGLE_OFFSETS) {
            Vec3 incoming = rotateHorizontal(naturalIncoming, angleOffset);
            Vec3 approach = touchdown.subtract(incoming.scale(approachDistance))
                    .add(0.0D, approachHeight, 0.0D);
            Vec3 glide = touchdown.subtract(incoming.scale(glideDistance))
                    .add(0.0D, glideHeight, 0.0D);
            Vec3 flare = touchdown.subtract(incoming.scale(flareDistance))
                    .add(0.0D, flareHeight, 0.0D);
            DragonLandingPlan plan = new DragonLandingPlan(approach, glide, flare, touchdown);
            if (horizontalDistance(dragon.position(), approach) < requiredEntryRunway(dragon, touchdown)
                    || !isLoaded(dragon, approach)
                    || !isLoaded(dragon, glide)
                    || !isLoaded(dragon, flare)
                    || !isLandingCorridorClear(dragon, plan)) {
                continue;
            }

            Vec3 toApproach = horizontalDirection(dragon.position(), approach, preferredHeading);
            double entryAlignment = Mth.clamp(toApproach.dot(preferredHeading), -1.0D, 1.0D);
            double directionPenalty = Math.abs(angleOffset) / 30.0D
                    + (1.0D - entryAlignment) * 2.0D;
            DirectionPlan candidate = new DirectionPlan(plan, directionPenalty);
            if (best == null || candidate.directionPenalty() < best.directionPenalty()) {
                best = candidate;
            }
        }
        return best;
    }

    private static boolean hasMinimumRunway(Mob dragon, Vec3 touchdown) {
        double approachDistance = Math.max(12.0D, dragon.getBbWidth() * 3.0D);
        return horizontalDistance(dragon.position(), touchdown)
                >= approachDistance + requiredEntryRunway(dragon, touchdown);
    }

    private static double requiredEntryRunway(Mob dragon, Vec3 touchdown) {
        double approachHeight = Math.max(6.0D, dragon.getBbHeight() * 1.5D);
        double currentHeightAboveTouchdown = dragon.getY() - touchdown.y;
        double requiredAltitudeChange = Math.abs(currentHeightAboveTouchdown - approachHeight);
        double altitudeRunway = Math.min(
                MAX_ENTRY_RUNWAY,
                requiredAltitudeChange / MAX_ENTRY_VERTICAL_SLOPE
        );
        double entryMargin = Math.max(
                Math.max(2.0D, dragon.getBbWidth() * 0.5D),
                altitudeRunway
        );
        return entryMargin;
    }

    private static boolean isLandingCorridorClear(Mob dragon, DragonLandingPlan plan) {
        AABB relativeBounds = dragon.getBoundingBox().move(
                -dragon.getX(),
                -dragon.getY(),
                -dragon.getZ()
        );
        return isSegmentClear(dragon, relativeBounds, plan.approach(), plan.glide())
                && isSegmentClear(dragon, relativeBounds, plan.glide(), plan.flare())
                && isSegmentClear(dragon, relativeBounds, plan.flare(), plan.touchdown());
    }

    private static boolean isSegmentClear(Mob dragon, AABB relativeBounds, Vec3 from, Vec3 to) {
        return VoxelAabbSweeper.isClear(
                dragon.level(),
                dragon,
                relativeBounds.move(from),
                to.subtract(from)
        );
    }

    private static boolean hasLandingFootprint(Mob dragon, BlockPos ground) {
        double halfWidth = dragon.getBbWidth() * 0.5D;
        double centerX = ground.getX() + 0.5D;
        double centerZ = ground.getZ() + 0.5D;
        int minX = Mth.floor(centerX - halfWidth + 0.05D);
        int maxX = Mth.floor(centerX + halfWidth - 0.05D);
        int minZ = Mth.floor(centerZ - halfWidth + 0.05D);
        int maxZ = Mth.floor(centerZ + halfWidth - 0.05D);
        int clearanceHeight = Math.max(2, Mth.ceil(dragon.getBbHeight()));

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos support = new BlockPos(x, ground.getY(), z);
                if (!dragon.level().hasChunkAt(support)) {
                    return false;
                }
                var supportState = dragon.level().getBlockState(support);
                if (supportState.isAir()
                        || !supportState.getFluidState().isEmpty()
                        || !supportState.isFaceSturdy(dragon.level(), support, Direction.UP)) {
                    return false;
                }

                for (int dy = 1; dy <= clearanceHeight; dy++) {
                    BlockPos clearance = support.above(dy);
                    var clearanceState = dragon.level().getBlockState(clearance);
                    if (!clearanceState.getCollisionShape(dragon.level(), clearance).isEmpty()
                            || !clearanceState.getFluidState().isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static @Nullable BlockPos findGround(Mob dragon, BlockPos column, int originY) {
        if (!dragon.level().dimensionType().hasCeiling()) {
            int surfaceY = dragon.level().getHeight(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    column.getX(),
                    column.getZ()
            );
            return new BlockPos(column.getX(), surfaceY - 1, column.getZ());
        }

        int minY = dragon.level().getMinBuildHeight();
        int maxY = dragon.level().getMaxBuildHeight() - 1;
        int startY = Math.min(maxY, Math.max(minY, originY + 8));
        for (int y = startY; y >= minY; y--) {
            BlockPos ground = new BlockPos(column.getX(), y, column.getZ());
            if (hasLandingFootprint(dragon, ground)) {
                return ground;
            }
        }
        return null;
    }

    private static Vec3 preferredHeading(Mob dragon) {
        Vec3 velocity = dragon.getDeltaMovement();
        Vec3 horizontalVelocity = new Vec3(velocity.x, 0.0D, velocity.z);
        if (horizontalVelocity.lengthSqr() > 0.04D) {
            return horizontalVelocity.normalize();
        }
        float yawRadians = dragon.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yawRadians), 0.0D, Mth.cos(yawRadians)).normalize();
    }

    private static Vec3 horizontalDirection(Vec3 from, Vec3 to, Vec3 fallback) {
        Vec3 horizontal = new Vec3(to.x - from.x, 0.0D, to.z - from.z);
        return horizontal.lengthSqr() > 1.0D ? horizontal.normalize() : fallback;
    }

    private static Vec3 rotateHorizontal(Vec3 direction, double degrees) {
        double radians = Math.toRadians(degrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return new Vec3(
                direction.x * cosine - direction.z * sine,
                0.0D,
                direction.x * sine + direction.z * cosine
        ).normalize();
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static boolean isLoaded(Mob dragon, Vec3 position) {
        return dragon.level().hasChunkAt(BlockPos.containing(position));
    }

    private record DirectionPlan(DragonLandingPlan plan, double directionPenalty) {
    }
}
