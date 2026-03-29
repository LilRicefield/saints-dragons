package com.leon.saintsdragons.client.renderer;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class DragonRiderCameraSync {
    private DragonRiderCameraSync() {
    }

    @FunctionalInterface
    public interface CameraPositionSink {
        void setPosition(double x, double y, double z);
    }

    public static boolean applyFirstPersonBoneAnchor(RideableDragonBase dragon, float partialTick, float rollDegrees, CameraPositionSink cameraSink) {
        return applyFirstPersonBoneAnchor(dragon, 0, partialTick, rollDegrees, cameraSink);
    }

    public static boolean applyFirstPersonBoneAnchor(RideableDragonBase dragon, int seatIndex, float partialTick, float rollDegrees, CameraPositionSink cameraSink) {
        RiderConfig.RiderSpec riderSpec = RiderConfig.getSpec(dragon);
        if (riderSpec == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        long lastRender = RiderBullcrap.getLastRenderTime(dragon.getId());
        long lastUpdate = RiderBullcrap.getTimestamp(dragon.getId(), seatIndex);
        if (now - lastRender > riderSpec.staleMs || now - lastUpdate > riderSpec.staleMs) {
            return false;
        }

        Vec3 saddleOffset = RiderBullcrap.getCameraOffset(dragon.getId(), seatIndex);
        if (saddleOffset == null) {
            return false;
        }
        if (Math.abs(saddleOffset.x) >= 20.0 || Math.abs(saddleOffset.y) >= 20.0 || Math.abs(saddleOffset.z) >= 20.0) {
            return false;
        }

        double interpX = Mth.lerp(partialTick, dragon.xo, dragon.getX());
        double interpY = Mth.lerp(partialTick, dragon.yo, dragon.getY());
        double interpZ = Mth.lerp(partialTick, dragon.zo, dragon.getZ());

        Vector3f firstPersonOffset = RiderConfig.getFirstPersonOffset(dragon, seatIndex);
        Vec3 rotatedFirstPersonOffset = rotateByDragon(
                new Vec3(firstPersonOffset.x(), firstPersonOffset.y(), firstPersonOffset.z()),
                (float) Math.toRadians(Mth.rotLerp(partialTick, dragon.yBodyRotO, dragon.yBodyRot)),
                (float) Math.toRadians(Mth.lerp(partialTick, dragon.xRotO, dragon.getXRot())),
                (float) Math.toRadians(rollDegrees)
        );

        cameraSink.setPosition(
                interpX + saddleOffset.x + rotatedFirstPersonOffset.x,
                interpY + saddleOffset.y + rotatedFirstPersonOffset.y,
                interpZ + saddleOffset.z + rotatedFirstPersonOffset.z
        );
        return true;
    }

    private static Vec3 rotateByDragon(Vec3 value, float yawRad, float pitchRad, float rollRad) {
        double x = value.x;
        double y = value.y;
        double z = value.z;

        double cosRoll = Math.cos(rollRad);
        double sinRoll = Math.sin(rollRad);
        double xRoll = x * cosRoll - y * sinRoll;
        double yRoll = x * sinRoll + y * cosRoll;
        double zRoll = z;

        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);
        double xPitch = xRoll;
        double yPitch = yRoll * cosPitch - zRoll * sinPitch;
        double zPitch = yRoll * sinPitch + zRoll * cosPitch;

        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        double xYaw = xPitch * cosYaw - zPitch * sinYaw;
        double yYaw = yPitch;
        double zYaw = xPitch * sinYaw + zPitch * cosYaw;

        return new Vec3(xYaw, yYaw, zYaw);
    }
}
