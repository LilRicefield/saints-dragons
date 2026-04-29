package com.leon.saintsdragons.server.flight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class DragonRiderSeat {
    private DragonRiderSeat() {
    }

    public static void positionCenteredRider(Entity mount, Entity passenger, Entity.MoveFunction moveFunction, double seatYOffset) {
        if (passenger == null || !mount.hasPassenger(passenger)) {
            return;
        }

        double x = mount.getX();
        double y = mount.getY() + seatYOffset + passenger.getMyRidingOffset();
        double z = mount.getZ();
        moveFunction.accept(passenger, x, y, z);
    }

    public static void positionLocatorRider(Entity mount,
                                            Entity passenger,
                                            Entity.MoveFunction moveFunction,
                                            double seatYOffset,
                                            Vec3 locatorWorldPos) {
        positionLocatorRider(mount, passenger, moveFunction, seatYOffset, locatorWorldPos, 0.0D);
    }

    public static void positionLocatorRider(Entity mount,
                                            Entity passenger,
                                            Entity.MoveFunction moveFunction,
                                            double seatYOffset,
                                            Vec3 locatorWorldPos,
                                            double locatorYOffset) {
        if (passenger == null || !mount.hasPassenger(passenger)) {
            return;
        }

        if (locatorWorldPos == null) {
            positionCenteredRider(mount, passenger, moveFunction, seatYOffset);
            return;
        }

        Vec3 mountOldPos = new Vec3(mount.xo, mount.yo, mount.zo);
        Vec3 worldOffset = locatorWorldPos.subtract(mountOldPos);

        double oldYawRad = Math.toRadians(-mount.yRotO);
        double cosOld = Math.cos(oldYawRad);
        double sinOld = Math.sin(oldYawRad);
        double localX = worldOffset.x * cosOld - worldOffset.z * sinOld;
        double localY = worldOffset.y;
        double localZ = worldOffset.x * sinOld + worldOffset.z * cosOld;

        double currentYawRad = Math.toRadians(-mount.getYRot());
        double cosCurrent = Math.cos(currentYawRad);
        double sinCurrent = Math.sin(currentYawRad);
        double currentWorldX = localX * cosCurrent + localZ * sinCurrent;
        double currentWorldZ = -localX * sinCurrent + localZ * cosCurrent;

        Vec3 currentPos = mount.position().add(currentWorldX, localY + locatorYOffset, currentWorldZ);
        moveFunction.accept(passenger, currentPos.x, currentPos.y, currentPos.z);
    }

    public static Vec3 findRadialGroundDismount(LivingEntity passenger, Entity mount,
                                                double[] radii, int[] angles,
                                                int maxDropBelowMount, double fallbackForwardDistance) {
        passenger.fallDistance = 0.0F;
        Level level = mount.level();
        Vec3 base = mount.position();

        for (double radius : radii) {
            for (int angle : angles) {
                double yawRad = Math.toRadians(mount.getYRot() + angle);
                double cx = base.x + Math.cos(yawRad) * radius;
                double cz = base.z + Math.sin(yawRad) * radius;

                int startY = (int) Math.floor(base.y + 1.0D);
                for (int dy = 0; dy <= maxDropBelowMount; dy++) {
                    int y = startY - dy;
                    BlockPos pos = new BlockPos((int) Math.floor(cx), y, (int) Math.floor(cz));
                    BlockPos below = pos.below();
                    var belowState = level.getBlockState(below);
                    var atState = level.getBlockState(pos);
                    boolean solidBelow = !belowState.isAir() && !belowState.getCollisionShape(level, below).isEmpty();
                    boolean spaceFree = atState.getCollisionShape(level, pos).isEmpty();
                    boolean fluidOk = atState.getFluidState().isEmpty();
                    if (solidBelow && spaceFree && fluidOk) {
                        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.05D, pos.getZ() + 0.5D);
                    }
                }
            }
        }

        return forwardDismount(passenger, mount, fallbackForwardDistance);
    }

    public static Vec3 forwardDismount(LivingEntity passenger, Entity mount, double distance) {
        passenger.fallDistance = 0.0F;
        return mount.position().add(mount.getViewVector(1.0F).scale(distance));
    }
}