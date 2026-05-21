package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.client.camera.DragonDiveEffectIntensity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class DragonDiveTrailRenderer {
    public static final String LEFT_WING_TRAIL_BONE = "leftWingTrailBone";
    public static final String RIGHT_WING_TRAIL_BONE = "rightWingTrailBone";
    public static final String TIP_WING_TRAIL_BONE = "tipWingTrailBone";
    private static final int TRAIL_LENGTH = 18;
    private static final float TRAIL_ALPHA = 1.0F;
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
        return DragonDiveEffectIntensity.get(dragon);
    }

    private static final class TrailPair {
        private final DragonWingTrail left = new DragonWingTrail(TRAIL_LENGTH);
        private final DragonWingTrail right = new DragonWingTrail(TRAIL_LENGTH);
        private final DragonWingTrail tip = new DragonWingTrail(TRAIL_LENGTH);
        private int lastRecordedTick = -1;
    }
}
