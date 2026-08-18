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
    private static final double MOUNT_CLEARANCE_PADDING = 2.0D;
    private static final double MOUNT_STOP_PADDING = 1.25D;
    private static final double MOUNTED_FOLLOW_HYSTERESIS = 2.5D;
    private static final int MAX_GROUND_PROJECTION_DROP = 32;

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

    public static double groundStartDistanceSqr(RideableDragonBase dragon,
                                                 LivingEntity owner,
                                                 double configuredStartDistance,
                                                 double configuredStopDistance) {
        if (!isMounted(owner)) {
            return configuredStartDistance * configuredStartDistance;
        }
        double stopDistance = groundStopDistance(dragon, owner, configuredStopDistance);
        double configuredMountedStart = Math.min(
                configuredStartDistance,
                Math.sqrt(MOUNTED_START_DISTANCE_SQR)
        );
        double startDistance = Math.max(
                configuredMountedStart,
                stopDistance + MOUNTED_FOLLOW_HYSTERESIS
        );
        return startDistance * startDistance;
    }

    public static double groundStopDistance(RideableDragonBase dragon,
                                            LivingEntity owner,
                                            double configuredStopDistance) {
        Entity anchor = anchor(owner);
        return anchor == owner
                ? configuredStopDistance
                : Math.max(
                        configuredStopDistance,
                        mountedClearance(dragon, anchor) + MOUNT_STOP_PADDING
                );
    }

    public static Vec3 groundTarget(RideableDragonBase dragon, LivingEntity owner) {
        Entity anchor = anchor(owner);
        Vec3 anchorPosition = anchor.position();
        if (anchor == owner) {
            return anchorPosition;
        }

        Vec3 awayFromMount = horizontalDirection(anchorPosition, dragon.position());
        if (awayFromMount == null) {
            Vec3 mountMovement = anchor.getDeltaMovement();
            awayFromMount = horizontalDirection(Vec3.ZERO, mountMovement.scale(-1.0D));
        }
        if (awayFromMount == null) {
            Vec3 look = anchor.getLookAngle();
            awayFromMount = horizontalDirection(Vec3.ZERO, look.scale(-1.0D));
        }
        if (awayFromMount == null) {
            double angle = Math.floorMod(dragon.getUUID().hashCode(), 360) * Math.PI / 180.0D;
            awayFromMount = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        }

        double clearance = mountedClearance(dragon, anchor);
        Vec3 preferred = anchorPosition.add(awayFromMount.scale(clearance));
        double projectionStartY = anchor.onGround()
                ? anchorPosition.y + 3.0D
                : Math.min(anchorPosition.y, dragon.getY() + 12.0D);
        Vec3 projected = findStandablePositionBelow(
                dragon,
                new Vec3(preferred.x, projectionStartY, preferred.z),
                MAX_GROUND_PROJECTION_DROP
        );
        return projected != null
                ? projected
                : new Vec3(preferred.x, dragon.getY(), preferred.z);
    }

    public static double groundFollowDistanceToSqr(RideableDragonBase dragon,
                                                    LivingEntity owner,
                                                    Vec3 groundTarget) {
        Entity anchor = anchor(owner);
        if (anchor != owner && (anchor.onGround() || anchor.isInWaterOrBubble())) {
            return dragon.distanceToSqr(anchor.position());
        }
        return dragon.distanceToSqr(groundTarget);
    }

    public static Vec3 groundLookTarget(RideableDragonBase dragon,
                                        LivingEntity owner,
                                        Vec3 groundTarget) {
        if (!isMounted(owner)) {
            return new Vec3(owner.getX(), owner.getEyeY(), owner.getZ());
        }
        Entity anchor = anchor(owner);
        double lookHeight = Mth.clamp(dragon.getBbHeight() * 0.6D, 1.0D, 3.0D);
        return new Vec3(anchor.getX(), groundTarget.y + lookHeight, anchor.getZ());
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

    public static @Nullable Vec3 safeTeleportTarget(RideableDragonBase dragon,
                                                     LivingEntity owner) {
        Vec3 target = groundTarget(dragon, owner);
        int spacing = Math.max(2, Mth.ceil(dragon.getBbWidth() * 0.75D));
        int[][] offsets = {
                {0, 0},
                {spacing, 0}, {-spacing, 0}, {0, spacing}, {0, -spacing},
                {spacing, spacing}, {spacing, -spacing},
                {-spacing, spacing}, {-spacing, -spacing},
                {spacing * 2, 0}, {-spacing * 2, 0},
                {0, spacing * 2}, {0, -spacing * 2}
        };
        for (int[] offset : offsets) {
            Vec3 candidate = findStandablePositionInColumn(
                    dragon,
                    Mth.floor(target.x) + offset[0],
                    Mth.floor(target.z) + offset[1],
                    target.y + 4.0D,
                    MAX_GROUND_PROJECTION_DROP
            );
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private static double mountedClearance(RideableDragonBase dragon, Entity mount) {
        return Math.max(
                4.0D,
                Math.sqrt(2.0D) * (
                        dragon.getBbWidth() * 0.5D
                                + mount.getBbWidth() * 0.5D
                )
                        + MOUNT_CLEARANCE_PADDING
        );
    }

    private static @Nullable Vec3 horizontalDirection(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double lengthSqr = dx * dx + dz * dz;
        if (lengthSqr < 1.0E-6D) {
            return null;
        }
        double inverseLength = 1.0D / Math.sqrt(lengthSqr);
        return new Vec3(dx * inverseLength, 0.0D, dz * inverseLength);
    }

    private static @Nullable Vec3 findStandablePositionBelow(RideableDragonBase dragon,
                                                              Vec3 anchorPosition,
                                                              int maximumDrop) {
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
                    anchorPosition.y,
                    maximumDrop
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
                                                                 double startY,
                                                                 int maximumDrop) {
        Level level = dragon.level();
        BlockPos column = new BlockPos(x, Mth.floor(startY), z);
        if (!level.hasChunkAt(column)) {
            return null;
        }

        int minimumFeetY = Math.max(
                level.getMinBuildHeight() + 1,
                Mth.floor(startY) - Math.max(1, maximumDrop)
        );
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
