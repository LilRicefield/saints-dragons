package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class DragonOwnerFollowTarget {
    private static final double MOUNTED_START_DISTANCE_SQR = 12.0D * 12.0D;

    private DragonOwnerFollowTarget() {
    }

    public static Entity anchor(LivingEntity owner) {
        Entity rootVehicle = owner.getRootVehicle();
        return rootVehicle != owner && rootVehicle.isAlive() ? rootVehicle : owner;
    }

    public static boolean isMounted(LivingEntity owner) {
        return anchor(owner) != owner;
    }

    public static Vec3 anchorPosition(LivingEntity owner) {
        return anchor(owner).position();
    }

    public static double anchorDistanceToSqr(Entity follower, LivingEntity owner) {
        return follower.distanceToSqr(anchorPosition(owner));
    }

    public static double startDistanceSqr(LivingEntity owner, double configuredDistanceSqr) {
        return isMounted(owner)
                ? Math.min(configuredDistanceSqr, MOUNTED_START_DISTANCE_SQR)
                : configuredDistanceSqr;
    }

    public static Vec3 groundTarget(RideableDragonBase dragon, LivingEntity owner) {
        Entity anchor = anchor(owner);
        Vec3 anchorPosition = anchor.position();
        if (anchor == owner || anchor.onGround() || anchor.isInWaterOrBubble()) {
            return anchorPosition;
        }

        Vec3 projected = findStandablePositionBelow(dragon, anchorPosition);
        return projected != null
                ? projected
                : new Vec3(anchorPosition.x, dragon.getY(), anchorPosition.z);
    }

    public static Vec3 groundLookTarget(RideableDragonBase dragon,
                                        LivingEntity owner,
                                        Vec3 groundTarget) {
        if (!isMounted(owner)) {
            return new Vec3(owner.getX(), owner.getEyeY(), owner.getZ());
        }
        double lookHeight = Mth.clamp(dragon.getBbHeight() * 0.6D, 1.0D, 3.0D);
        return groundTarget.add(0.0D, lookHeight, 0.0D);
    }

    public static Vec3 visualTarget(LivingEntity owner) {
        Entity anchor = anchor(owner);
        if (anchor == owner) {
            return new Vec3(owner.getX(), owner.getEyeY(), owner.getZ());
        }
        return anchor.getBoundingBox().getCenter();
    }

    public static Vec3 airFormationTarget(LivingEntity owner,
                                          double hoverHeight,
                                          double followOffset,
                                          double verticalOffset) {
        Entity anchor = anchor(owner);
        Vec3 look = anchor.getLookAngle();
        double targetY = anchor == owner
                ? owner.getY() + owner.getBbHeight() + hoverHeight
                : anchor.getBoundingBox().getCenter().y + hoverHeight;
        return new Vec3(
                anchor.getX() - look.x * followOffset,
                targetY + verticalOffset,
                anchor.getZ() - look.z * followOffset
        );
    }

    public static Vec3 swimTarget(RideableDragonBase dragon, LivingEntity owner) {
        Entity anchor = anchor(owner);
        if (anchor.isInWaterOrBubble()) {
            return anchor.position().add(0.0D, anchor.getBbHeight() * 0.35D, 0.0D);
        }
        return new Vec3(
                anchor.getX(),
                dragon.getY() + dragon.getBbHeight() * 0.35D,
                anchor.getZ()
        );
    }

    private static @Nullable Vec3 findStandablePositionBelow(RideableDragonBase dragon,
                                                              Vec3 anchorPosition) {
        int spacing = Math.max(2, Mth.ceil(dragon.getBbWidth() * 0.75D));
        int[][] offsets = {
                {0, 0},
                {spacing, 0}, {-spacing, 0}, {0, spacing}, {0, -spacing},
                {spacing, spacing}, {spacing, -spacing},
                {-spacing, spacing}, {-spacing, -spacing}
        };
        for (int[] offset : offsets) {
            Vec3 candidate = findStandablePositionInColumn(
                    dragon,
                    Mth.floor(anchorPosition.x) + offset[0],
                    Mth.floor(anchorPosition.z) + offset[1],
                    anchorPosition.y
            );
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private static @Nullable Vec3 findStandablePositionInColumn(RideableDragonBase dragon,
                                                                 int x,
                                                                 int z,
                                                                 double startY) {
        Level level = dragon.level();
        BlockPos column = new BlockPos(x, Mth.floor(startY), z);
        if (!level.hasChunkAt(column)) {
            return null;
        }

        int minimumFeetY = level.getMinBuildHeight() + 1;
        int maximumFeetY = level.getMaxBuildHeight() - Mth.ceil(dragon.getBbHeight()) - 1;
        int firstFeetY = Mth.clamp(Mth.floor(startY) + 1, minimumFeetY, maximumFeetY);
        for (int feetY = firstFeetY; feetY >= minimumFeetY; feetY--) {
            BlockPos feet = new BlockPos(x, feetY, z);
            BlockPos floorPosition = feet.below();
            BlockState floor = level.getBlockState(floorPosition);
            if (!floor.isFaceSturdy(level, floorPosition, Direction.UP)) {
                continue;
            }

            AABB translatedBounds = dragon.getBoundingBox().move(
                    x + 0.5D - dragon.getX(),
                    feetY - dragon.getY(),
                    z + 0.5D - dragon.getZ()
            );
            if (level.noCollision(dragon, translatedBounds)) {
                return new Vec3(x + 0.5D, feetY, z + 0.5D);
            }
        }
        return null;
    }
}
