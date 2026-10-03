package com.leon.saintsdragons.client.renderer;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class DragonSeatAnchoredCamera {
    private DragonSeatAnchoredCamera() {}

    public static int getSeatIndex(RideableDragonBase dragon, Entity rider) {
        return dragon.getRiderSeatIndex(rider);
    }

    public static boolean supports(RideableDragonBase dragon) {
        RiderConfig.RiderSpec spec = RiderConfig.getSpec(dragon);
        return spec != null && spec.cameraEnabled();
    }

    @Nullable
    public static DragonRiderAttachments.SeatPose resolve(RideableDragonBase dragon, Entity rider, float partialTick) {
        return supports(dragon)
                ? DragonRiderAttachments.resolve(dragon, getSeatIndex(dragon, rider), partialTick) : null;
    }

    public static Vec3 computePivot(RideableDragonBase dragon, Entity rider,
                                    DragonRiderAttachments.SeatPose seat, Vector3f up,
                                    Vector3f forwards, Vector3f left, float partialTick,
                                    double leanX, double leanY, double leanZ) {
        // The eye offset belongs to the seated player, never to the direction the camera is looking
        return seat.eyePosition(dragon, rider, partialTick).add(
                forwards.x() * leanZ + up.x() * leanY + left.x() * leanX,
                forwards.y() * leanZ + up.y() * leanY + left.y() * leanX,
                forwards.z() * leanZ + up.z() * leanY + left.z() * leanX);
    }
}
