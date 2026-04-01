package com.leon.saintsdragons.server.entity.controller.stegonaut;

import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
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


public record StegonautRiderController(Stegonaut drake) {
    private static final double SEAT_BASE_FACTOR = 0.0D;

    @Nullable
    public Player getRidingPlayer() {
        if (drake.getFirstPassenger() instanceof Player player) {
            return player;
        }
        return null;
    }

    public Vec3 getRiddenInput(Player player, @SuppressWarnings("unused") Vec3 deltaIn) {
        float f = player.zza < 0.0F ? 0.5F : 1.0F;
        return new Vec3(player.xxa * 0.5F, 0.0D, player.zza * 0.9F * f);
    }

    public void tickRidden(Player player, @SuppressWarnings("unused") Vec3 travelVector) {
        player.fallDistance = 0.0F;
        drake.fallDistance = 0.0F;
        drake.setTarget(null);

        float currentYaw = drake.getYRot();
        float targetYaw = player.getYRot();
        float rawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float blend = 0.28f;
        float newYaw = currentYaw + (rawDiff * blend);

        drake.setYRot(newYaw);
        drake.yBodyRot = newYaw;
        drake.yHeadRot = newYaw;
        drake.setXRot(0.0F);
    }

    public float getRiddenSpeed(Player player) {
        double speed = drake.isAccelerating() ? Stegonaut.RIDER_RUN_SPEED : Stegonaut.RIDER_WALK_SPEED;
        return (float) (speed * drake.getHappinessSpeedMultiplier());
    }

    public double getPassengersRidingOffset() {
        return (drake.getBbHeight() * SEAT_BASE_FACTOR);
    }

    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (passenger == null) {
            return;
        }
        DragonRiderSeat.positionLocatorRider(
                drake,
                passenger,
                moveFunction,
                getPassengersRidingOffset(),
                drake.level().isClientSide ? drake.getClientLocatorPosition("passengerLocator") : null
        );
    }

    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 vec3 = getCollisionHorizontalEscapeVector(
            drake.getBbWidth(),
            passenger.getBbWidth(),
            drake.getYRot() + (passenger.getMainArm() == HumanoidArm.RIGHT ? 90.0F : -90.0F)
        );
        Vec3 rightSide = getDismountLocationInDirection(vec3, passenger);
        if (rightSide != null) {
            return rightSide;
        }

        Vec3 vec32 = getCollisionHorizontalEscapeVector(
            drake.getBbWidth(),
            passenger.getBbWidth(),
            drake.getYRot() + (passenger.getMainArm() == HumanoidArm.LEFT ? 90.0F : -90.0F)
        );
        Vec3 leftSide = getDismountLocationInDirection(vec32, passenger);
        return leftSide != null ? leftSide : drake.position();
    }

    private static Vec3 getCollisionHorizontalEscapeVector(double entityWidth, double passengerWidth, float yaw) {
        double d0 = (entityWidth + passengerWidth + 1.0E-5F) / 2.0D;
        float f = -Mth.sin(yaw * ((float)Math.PI / 180F));
        float f1 = Mth.cos(yaw * ((float)Math.PI / 180F));
        float f2 = Math.max(Math.abs(f), Math.abs(f1));
        return new Vec3((double)f * d0 / (double)f2, 0.0D, (double)f1 * d0 / (double)f2);
    }

    @Nullable
    private Vec3 getDismountLocationInDirection(Vec3 offset, LivingEntity passenger) {
        double targetX = drake.getX() + offset.x;
        double minY = drake.getBoundingBox().minY;
        double targetZ = drake.getZ() + offset.z;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (Pose pose : passenger.getDismountPoses()) {
            pos.set(targetX, minY, targetZ);
            double maxY = drake.getBoundingBox().maxY + 0.75D;

            while (true) {
                double floorHeight = drake.level().getBlockFloorHeight(pos);
                if ((double)pos.getY() + floorHeight > maxY) {
                    break;
                }

                if (DismountHelper.isBlockFloorValid(floorHeight)) {
                    AABB aabb = passenger.getLocalBoundsForPose(pose);
                    Vec3 dismountPos = new Vec3(targetX, (double)pos.getY() + floorHeight, targetZ);
                    if (DismountHelper.canDismountTo(drake.level(), passenger, aabb.move(dismountPos))) {
                        passenger.setPose(pose);
                        return dismountPos;
                    }
                }

                pos.move(Direction.UP);
                if (!((double)pos.getY() < maxY)) {
                    break;
                }
            }
        }

        return null;
    }

    @Nullable
    public Player getControllingPassenger() {
        Player rider = getRidingPlayer();
        if (rider == null) {
            return null;
        }
        if (!drake.isTame()) {
            return null;
        }
        if (!drake.isOwnedBy(rider)) {
            return null;
        }
        return rider;
    }
}
