package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class DragonDiveTrailRenderer {
    public static final String LEFT_WING_TRAIL_BONE = "leftWingTrailBone";
    public static final String RIGHT_WING_TRAIL_BONE = "rightWingTrailBone";
    public static final String TIP_WING_TRAIL_BONE = "tipWingTrailBone";
    private static final int TRAIL_LENGTH = 18;
    private static final float TRAIL_ALPHA = 1.0F;
    private static final double DIVE_START_SPEED = 0.75D;
    private static final double DIVE_FULL_SPEED = 4.00D;
    private static final double DIVE_START_DOWNWARD_SPEED = 0.10D;
    private static final double DIVE_FULL_DOWNWARD_SPEED = 1.35D;
    private static final Map<RideableDragonBase, TrailPair> TRAILS = new WeakHashMap<>();

    private DragonDiveTrailRenderer() {
    }

    public static void render(RideableDragonBase dragon, Vec3 left, Vec3 right, Vec3 tip,
                              MultiBufferSource bufferSource, PoseStack.Pose pose) {
        TrailPair pair = TRAILS.computeIfAbsent(dragon, ignored -> new TrailPair());
        float intensity = getTrailIntensity(dragon);

        if (pair.lastRecordedTick != dragon.tickCount) {
            pair.lastRecordedTick = dragon.tickCount;
            if (left != null && right != null && tip != null && intensity > 0.0F) {
                pair.left.add(left, TRAIL_ALPHA * intensity);
                pair.right.add(right, TRAIL_ALPHA * intensity);
                pair.tip.add(tip, TRAIL_ALPHA * intensity);
            } else {
                pair.left.decay();
                pair.right.decay();
                pair.tip.decay();
            }
        }

        DragonWingTrailRenderer.render(pair.left, bufferSource, pose);
        DragonWingTrailRenderer.render(pair.right, bufferSource, pose);
        DragonWingTrailRenderer.render(pair.tip, bufferSource, pose);
    }

    public static float getTrailIntensity(RideableDragonBase dragon) {
        if (dragon == null || !dragon.isFlying() || dragon.isInWaterOrBubble()) {
            return 0.0F;
        }

        Vec3 velocity = dragon.getDeltaMovement();
        Vec3 positionDelta = new Vec3(
                dragon.getX() - dragon.xo,
                dragon.getY() - dragon.yo,
                dragon.getZ() - dragon.zo
        );

        double downwardSpeed = Math.max(-velocity.y, -positionDelta.y);
        if (downwardSpeed <= DIVE_START_DOWNWARD_SPEED) {
            return 0.0F;
        }

        double speed = Math.max(velocity.length(), positionDelta.length());
        double speedFactor = normalize(speed, DIVE_START_SPEED, DIVE_FULL_SPEED);
        double downwardFactor = normalize(downwardSpeed, DIVE_START_DOWNWARD_SPEED, DIVE_FULL_DOWNWARD_SPEED);
        return (float) (speedFactor * downwardFactor);
    }

    private static double normalize(double value, double start, double end) {
        return Mth.clamp((value - start) / (end - start), 0.0D, 1.0D);
    }

    private static final class TrailPair {
        private final DragonWingTrail left = new DragonWingTrail(TRAIL_LENGTH);
        private final DragonWingTrail right = new DragonWingTrail(TRAIL_LENGTH);
        private final DragonWingTrail tip = new DragonWingTrail(TRAIL_LENGTH);
        private int lastRecordedTick = -1;
    }
}
