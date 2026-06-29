package com.leon.saintsdragons.server.entity.controller;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.flight.DragonRiderSeat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class GroundDragonRiderControllerHelper {
    private static final float DEFAULT_YAW_BLEND = 0.28F;

    private GroundDragonRiderControllerHelper() {
    }

    @Nullable
    public static Player getRidingPlayer(RideableDragonBase dragon) {
        if (dragon.getFirstPassenger() instanceof Player player) {
            return player;
        }
        return null;
    }

    public static Vec3 standardGroundInput(Player player) {
        float reverseScale = player.zza < 0.0F ? 0.5F : 1.0F;
        return new Vec3(player.xxa * 0.5F, 0.0D, player.zza * 0.9F * reverseScale);
    }

    public static void tickStandardGroundRider(RideableDragonBase dragon, Player player) {
        player.fallDistance = 0.0F;
        dragon.fallDistance = 0.0F;
        dragon.setTarget(null);
        smoothYawToRider(dragon, player, DEFAULT_YAW_BLEND);
    }

    public static void smoothYawToRider(RideableDragonBase dragon, Player player, float blend) {
        float currentYaw = dragon.getYRot();
        float yawDelta = Mth.wrapDegrees(player.getYRot() - currentYaw);
        float newYaw = currentYaw + yawDelta * blend;
        dragon.setYRot(newYaw);
        dragon.yBodyRot = newYaw;
        dragon.yHeadRot = newYaw;
        dragon.setXRot(0.0F);
    }

    public static void positionLocatorRider(RideableDragonBase dragon, Entity passenger,
                                            Entity.MoveFunction moveFunction, double ridingOffset) {
        if (passenger == null) {
            return;
        }
        DragonRiderSeat.positionLocatorRider(
                dragon,
                passenger,
                moveFunction,
                ridingOffset,
                dragon.level().isClientSide ? dragon.getClientLocatorPosition("passengerLocator") : null
        );
    }

    public static Vec3 getDismountLocationForPassenger(RideableDragonBase dragon, LivingEntity passenger) {
        Vec3 rightOffset = getCollisionHorizontalEscapeVector(
                dragon.getBbWidth(),
                passenger.getBbWidth(),
                dragon.getYRot() + (passenger.getMainArm() == HumanoidArm.RIGHT ? 90.0F : -90.0F)
        );
        Vec3 rightSide = getDismountLocationInDirection(dragon, rightOffset, passenger);
        if (rightSide != null) {
            return rightSide;
        }

        Vec3 leftOffset = getCollisionHorizontalEscapeVector(
                dragon.getBbWidth(),
                passenger.getBbWidth(),
                dragon.getYRot() + (passenger.getMainArm() == HumanoidArm.LEFT ? 90.0F : -90.0F)
        );
        Vec3 leftSide = getDismountLocationInDirection(dragon, leftOffset, passenger);
        return leftSide != null ? leftSide : dragon.position();
    }

    private static Vec3 getCollisionHorizontalEscapeVector(double entityWidth, double passengerWidth, float yaw) {
        double offset = (entityWidth + passengerWidth + 1.0E-5F) / 2.0D;
        float sin = -Mth.sin(yaw * Mth.DEG_TO_RAD);
        float cos = Mth.cos(yaw * Mth.DEG_TO_RAD);
        float max = Math.max(Math.abs(sin), Math.abs(cos));
        return new Vec3(sin * offset / max, 0.0D, cos * offset / max);
    }

    @Nullable
    private static Vec3 getDismountLocationInDirection(RideableDragonBase dragon, Vec3 offset, LivingEntity passenger) {
        double targetX = dragon.getX() + offset.x;
        double minY = dragon.getBoundingBox().minY;
        double targetZ = dragon.getZ() + offset.z;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (Pose pose : passenger.getDismountPoses()) {
            pos.set(targetX, minY, targetZ);
            double maxY = dragon.getBoundingBox().maxY + 0.75D;

            while (true) {
                double floorHeight = dragon.level().getBlockFloorHeight(pos);
                if (pos.getY() + floorHeight > maxY) {
                    break;
                }

                if (DismountHelper.isBlockFloorValid(floorHeight)) {
                    AABB bounds = passenger.getLocalBoundsForPose(pose);
                    Vec3 dismountPos = new Vec3(targetX, pos.getY() + floorHeight, targetZ);
                    if (DismountHelper.canDismountTo(dragon.level(), passenger, bounds.move(dismountPos))) {
                        passenger.setPose(pose);
                        return dismountPos;
                    }
                }

                pos.move(Direction.UP);
                if (pos.getY() >= maxY) {
                    break;
                }
            }
        }

        return null;
    }

    @Nullable
    public static Player getOwnedControllingPassenger(RideableDragonBase dragon) {
        Player rider = getRidingPlayer(dragon);
        if (rider == null || !dragon.isTame() || !dragon.isOwnedBy(rider)) {
            return null;
        }
        return rider;
    }
}
