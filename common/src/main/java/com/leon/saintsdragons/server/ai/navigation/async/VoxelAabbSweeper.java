package com.leon.saintsdragons.server.ai.navigation.async;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Predicate;

/** Continuous swept-AABB collision queries for both live and snapshotted voxel worlds. */
public final class VoxelAabbSweeper {
    private static final double EPSILON = 1.0E-7D;

    private VoxelAabbSweeper() {
    }

    public static boolean isClear(CollisionGetter level, Entity entity, AABB startBox, Vec3 movement) {
        AABB broadPhase = sweptBounds(startBox, movement);
        return isClear(startBox, movement, level.getCollisions(entity, broadPhase));
    }

    static boolean isClear(ImmutableBlockSnapshot snapshot, AABB startBox, Vec3 movement) {
        return isClear(snapshot, startBox, movement, state -> false);
    }

    static boolean isClear(ImmutableBlockSnapshot snapshot,
                           AABB startBox,
                           Vec3 movement,
                           Predicate<BlockState> ignoredBlocks) {
        AABB broadPhase = sweptBounds(startBox, movement);
        int minX = Mth.floor(broadPhase.minX + EPSILON);
        int minY = Mth.floor(broadPhase.minY + EPSILON);
        int minZ = Mth.floor(broadPhase.minZ + EPSILON);
        int maxX = Mth.floor(broadPhase.maxX - EPSILON);
        int maxY = Mth.floor(broadPhase.maxY - EPSILON);
        int maxZ = Mth.floor(broadPhase.maxZ - EPSILON);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    if (ignoredBlocks.test(snapshot.getBlockState(cursor))) {
                        continue;
                    }
                    for (AABB obstacle : snapshot.collisionBoxes(cursor)) {
                        if (collidesDuringSweep(startBox, movement, obstacle)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    static boolean isClear(AABB startBox, Vec3 movement, Iterable<VoxelShape> collisionShapes) {
        for (VoxelShape shape : collisionShapes) {
            for (AABB obstacle : shape.toAabbs()) {
                if (collidesDuringSweep(startBox, movement, obstacle)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static AABB sweptBounds(AABB startBox, Vec3 movement) {
        return startBox.minmax(startBox.move(movement)).inflate(EPSILON);
    }

    private static boolean collidesDuringSweep(AABB moving, Vec3 movement, AABB obstacle) {
        if (moving.intersects(obstacle)) {
            return true;
        }
        if (separatedOnStaticAxis(moving.minX, moving.maxX, obstacle.minX, obstacle.maxX, movement.x)
                || separatedOnStaticAxis(moving.minY, moving.maxY, obstacle.minY, obstacle.maxY, movement.y)
                || separatedOnStaticAxis(moving.minZ, moving.maxZ, obstacle.minZ, obstacle.maxZ, movement.z)) {
            return false;
        }

        double entry = Math.max(
                entryTime(moving.minX, moving.maxX, obstacle.minX, obstacle.maxX, movement.x),
                Math.max(
                        entryTime(moving.minY, moving.maxY, obstacle.minY, obstacle.maxY, movement.y),
                        entryTime(moving.minZ, moving.maxZ, obstacle.minZ, obstacle.maxZ, movement.z)
                )
        );
        double exit = Math.min(
                exitTime(moving.minX, moving.maxX, obstacle.minX, obstacle.maxX, movement.x),
                Math.min(
                        exitTime(moving.minY, moving.maxY, obstacle.minY, obstacle.maxY, movement.y),
                        exitTime(moving.minZ, moving.maxZ, obstacle.minZ, obstacle.maxZ, movement.z)
                )
        );

        // Touching while moving away (exit == 0), or only touching at the destination
        // (entry == 1), is not penetration and therefore is not a collision.
        return entry <= exit + EPSILON
                && exit > EPSILON
                && entry < 1.0D - EPSILON;
    }

    private static boolean separatedOnStaticAxis(double movingMin,
                                                  double movingMax,
                                                  double obstacleMin,
                                                  double obstacleMax,
                                                  double velocity) {
        return Math.abs(velocity) <= EPSILON
                && (movingMax <= obstacleMin + EPSILON || movingMin >= obstacleMax - EPSILON);
    }

    private static double entryTime(double movingMin,
                                    double movingMax,
                                    double obstacleMin,
                                    double obstacleMax,
                                    double velocity) {
        if (Math.abs(velocity) <= EPSILON) {
            return Double.NEGATIVE_INFINITY;
        }
        double first = (obstacleMin - movingMax) / velocity;
        double second = (obstacleMax - movingMin) / velocity;
        return Math.min(first, second);
    }

    private static double exitTime(double movingMin,
                                   double movingMax,
                                   double obstacleMin,
                                   double obstacleMax,
                                   double velocity) {
        if (Math.abs(velocity) <= EPSILON) {
            return Double.POSITIVE_INFINITY;
        }
        double first = (obstacleMin - movingMax) / velocity;
        double second = (obstacleMax - movingMin) / velocity;
        return Math.max(first, second);
    }
}
