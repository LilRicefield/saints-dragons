package com.leon.saintsdragons.client.renderer;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.base.RideableGroundDragon;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class DragonSeatAnchoredCamera {
    private static final double MAX_REASONABLE_OFFSET = 20.0D;
    private static final double GROUNDED_RIDEABLE_CAMERA_LIFT = 1.2D;

    private DragonSeatAnchoredCamera() {
    }

    public static boolean isValidSeatOffset(Vec3 offset) {
        return offset != null
                && Math.abs(offset.x) < MAX_REASONABLE_OFFSET
                && Math.abs(offset.y) < MAX_REASONABLE_OFFSET
                && Math.abs(offset.z) < MAX_REASONABLE_OFFSET;
    }

    public static Vec3 computePivot(RideableDragonBase dragon,
                                    Entity rider,
                                    Vec3 seatOffset,
                                    Vector3f up,
                                    Vector3f forwards,
                                    Vector3f left,
                                    float partialTick,
                                    double leanX,
                                    double leanY,
                                    double leanZ) {
        double interpX = Mth.lerp(partialTick, dragon.xo, dragon.getX());
        double interpY = Mth.lerp(partialTick, dragon.yo, dragon.getY());
        double interpZ = Mth.lerp(partialTick, dragon.zo, dragon.getZ());

        boolean useGroundedBoneCamera = usesGroundedRideableBoneCamera(dragon);
        float eyeHeight = useGroundedBoneCamera ? 0.0F : rider.getEyeHeight();
        Vec3 pivot = new Vec3(
                interpX + seatOffset.x + up.x() * eyeHeight,
                interpY + seatOffset.y + up.y() * eyeHeight,
                interpZ + seatOffset.z + up.z() * eyeHeight
        );
        if (useGroundedBoneCamera) {
            pivot = pivot.add(0.0D, GROUNDED_RIDEABLE_CAMERA_LIFT, 0.0D);
        }

        if (Math.abs(leanX) > 0.001D || Math.abs(leanY) > 0.001D || Math.abs(leanZ) > 0.001D) {
            pivot = pivot.add(
                    forwards.x() * leanZ + up.x() * leanY + left.x() * leanX,
                    forwards.y() * leanZ + up.y() * leanY + left.y() * leanX,
                    forwards.z() * leanZ + up.z() * leanY + left.z() * leanX
            );
        }
        return pivot;
    }

    private static boolean usesGroundedRideableBoneCamera(RideableDragonBase dragon) {
        if (dragon instanceof RideableFlyingDragon) {
            return !dragon.isFlying()
                    && !dragon.isTakeoff()
                    && !dragon.isLanding()
                    && !dragon.isHovering();
        }
        return dragon instanceof RideableGroundDragon;
    }
}
