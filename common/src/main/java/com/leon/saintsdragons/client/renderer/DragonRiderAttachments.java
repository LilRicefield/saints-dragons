package com.leon.saintsdragons.client.renderer;

import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.client.model.DragonBonePose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Render-thread attachment poses. Won't have any camera matrices or strong entity references are retained (I think) */
public final class DragonRiderAttachments {
    private static final Map<UUID, FramePose> POSES = new HashMap<>();
    private static Object activeLevel;
    private static long frame;

    private DragonRiderAttachments() {}

    public static void beginRenderFrame(@Nullable Object level) {
        if (activeLevel != level) {
            clear();
            activeLevel = level;
        }
        frame++;
        // we keep buffers for mounts used last frame, but release unloaded/hidden mounts promptly
        POSES.values().removeIf(pose -> pose.lastUsedFrame < frame - 1);
    }

    public static void clear() {
        POSES.clear();
        activeLevel = null;
    }

    public static boolean usesPoseCache(RideableDragonBase dragon) {
        return dragon.isVehicle() && !dragon.isRemoved() && RiderConfig.getSpec(dragon) != null
                && dragon.level() == Minecraft.getInstance().level
                && !EntityPreviewRenderContext.isRendering();
    }

    @Nullable
    public static SeatPose resolve(RideableDragonBase dragon, int seat, float partialTick) {
        if (!usesPoseCache(dragon) || !RiderConfig.getSpec(dragon).seats().containsKey(seat)) {
            return null;
        }
        // Shadow passes must not invalidate or overwrite the main view's frame/partial-tick pose.
        if (ShaderPassCompatibility.isIrisShadowPass()) {
            FramePose shadowPose = current(dragon, partialTick);
            return shadowPose == null ? null : shadowPose.seats.get(seat);
        }
        FramePose pose = entry(dragon, partialTick);
        SeatPose attachment = pose.seats.get(seat);
        if (attachment != null || pose.preparing || pose.prepared) {
            return attachment;
        }
        pose.preparing = true;
        try {
            var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(dragon);
            if (renderer instanceof DragonGeoEntityRenderer<?> dragonRenderer) {
                prepare(dragonRenderer, dragon, partialTick);
            }
            pose.prepared = true;
        } finally {
            pose.preparing = false;
        }
        return pose.seats.get(seat);
    }

    @SuppressWarnings("unchecked")
    private static <T extends RideableDragonBase> void prepare(DragonGeoEntityRenderer<T> renderer,
                                                               RideableDragonBase dragon, float partialTick) {
        renderer.prepareRiderAttachments((T) dragon, partialTick);
    }

    public static boolean hasAnimation(RideableDragonBase dragon, float partialTick, BakedGeoModel model) {
        FramePose pose = current(dragon, partialTick);
        return pose != null && pose.animationReady && pose.model == model;
    }

    public static void restoreAnimation(RideableDragonBase dragon, float partialTick) {
        entry(dragon, partialTick).animation.restore();
    }

    public static void captureAnimation(RideableDragonBase dragon, float partialTick, BakedGeoModel model,
                                         Collection<? extends CoreGeoBone> bones) {
        FramePose pose = entry(dragon, partialTick);
        pose.model = model;
        pose.animation.capture(bones);
        pose.animationReady = true;
    }

    public static void storeSeat(RideableDragonBase dragon, int seat, float partialTick, Matrix4f boneTransform) {
        if (!usesPoseCache(dragon) || !boneTransform.isFinite()
                || Math.abs(boneTransform.determinant3x3()) < 1.0E-8F) {
            return;
        }
        FramePose pose = entry(dragon, partialTick);
        if (pose.seats.containsKey(seat)) {
            return;
        }
        // Bone translation includes model scale; the player keeps their own size.
        Matrix4f riderTransform = new Matrix4f(boneTransform).normalize3x3();
        riderTransform.translate(RiderConfig.getSeatOffset(dragon, seat));
        float yaw = Mth.rotLerp(partialTick, dragon.yBodyRotO, dragon.yBodyRot);
        riderTransform.rotateY((yaw + RiderConfig.getYawOffset(dragon, seat)) * Mth.DEG_TO_RAD);
        pose.seats.put(seat, new SeatPose(riderTransform));
    }

    /** Compose with this render pass's view instead of replacing it with a cached view matrix */
    public static void transformRider(Entity rider, float partialTick, PoseStack poses, Vec3 renderOffset) {
        if (!(rider.getVehicle() instanceof RideableDragonBase dragon)) {
            return;
        }
        SeatPose seat = resolve(dragon, dragon.getRiderSeatIndex(rider), partialTick);
        if (seat == null) {
            return;
        }
        Vec3 delta = interpolatedPosition(dragon, partialTick)
                .subtract(interpolatedPosition(rider, partialTick)).subtract(renderOffset);
        poses.translate(delta.x, delta.y, delta.z);
        poses.mulPoseMatrix(seat.riderTransform);
        poses.last().normal().mul(new Matrix3f(seat.riderTransform).invert().transpose());
    }

    public static Vec3 interpolatedPosition(Entity entity, float partialTick) {
        // Match LevelRenderer, including remote entity interpolation.
        return new Vec3(Mth.lerp(partialTick, entity.xOld, entity.getX()),
                Mth.lerp(partialTick, entity.yOld, entity.getY()),
                Mth.lerp(partialTick, entity.zOld, entity.getZ()));
    }

    @Nullable
    private static FramePose current(RideableDragonBase dragon, float partialTick) {
        FramePose pose = POSES.get(dragon.getUUID());
        return pose != null && pose.frame == frame && pose.tick == dragon.tickCount
                && pose.partialTick == partialTick && pose.entityId == dragon.getId() ? pose : null;
    }

    private static FramePose entry(RideableDragonBase dragon, float partialTick) {
        FramePose pose = POSES.computeIfAbsent(dragon.getUUID(), ignored -> new FramePose());
        if (pose.frame != frame || pose.tick != dragon.tickCount || pose.partialTick != partialTick
                || pose.entityId != dragon.getId()) {
            pose.frame = frame;
            pose.tick = dragon.tickCount;
            pose.partialTick = partialTick;
            pose.entityId = dragon.getId();
            pose.animationReady = false;
            pose.prepared = false;
            pose.seats.clear();
        }
        pose.lastUsedFrame = frame;
        return pose;
    }

    public static final class SeatPose {
        private final Matrix4f riderTransform;

        private SeatPose(Matrix4f riderTransform) {
            this.riderTransform = riderTransform;
        }

        public Vec3 eyePosition(RideableDragonBase dragon, Entity rider, float partialTick) {
            Vector3f eye = riderTransform.transformPosition(0, rider.getEyeHeight(), 0, new Vector3f());
            return interpolatedPosition(dragon, partialTick).add(eye.x, eye.y, eye.z);
        }

        /** Keep gameplay yaw/pitch unchanged, and orient the horizon using the seat's actual up axis */
        public float cameraRoll(float yawDegrees, float pitchDegrees) {
            var look = new org.joml.Quaternionf().rotationYXZ(-yawDegrees * Mth.DEG_TO_RAD,
                    pitchDegrees * Mth.DEG_TO_RAD, 0);
            Vector3f up = riderTransform.transformDirection(0, 1, 0, new Vector3f()).normalize();
            Vector3f viewUp = look.transform(new Vector3f(0, 1, 0));
            Vector3f viewLeft = look.transform(new Vector3f(1, 0, 0));
            float x = up.dot(viewLeft);
            float y = up.dot(viewUp);
            if (x * x + y * y < 1.0E-6F) {
                // Looking along the seat's up axis: use its left axis to resolve the horizon.
                Vector3f left = riderTransform.transformDirection(1, 0, 0, new Vector3f()).normalize();
                return (float) Math.atan2(left.dot(viewUp), left.dot(viewLeft)) * Mth.RAD_TO_DEG;
            }
            return (float) Math.atan2(-x, y) * Mth.RAD_TO_DEG;
        }
    }

    private static final class FramePose {
        private long frame = -1;
        private long lastUsedFrame;
        private int tick;
        private int entityId;
        private float partialTick;
        private boolean preparing;
        private boolean prepared;
        private boolean animationReady;
        private BakedGeoModel model;
        private final DragonBonePose animation = new DragonBonePose();
        private final Map<Integer, SeatPose> seats = new HashMap<>();
    }
}
