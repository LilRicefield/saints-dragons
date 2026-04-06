package com.leon.saintsdragons.client.renderer.ignivorus;

import com.leon.saintsdragons.client.renderer.RiderBullcrap;
import com.leon.saintsdragons.client.renderer.RiderConfig;
import com.leon.saintsdragons.client.renderer.RenderPassContext;
import com.leon.saintsdragons.client.model.ignivorus.IgnivorusModel;
import com.leon.saintsdragons.client.renderer.layer.ignivorus.IgnivorusGlowLayer;
import com.leon.saintsdragons.client.renderer.layer.ignivorus.IgnivorusMouthSmokeLayer;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class IgnivorusRenderer extends GeoEntityRenderer<Ignivorus> {
    private static final float PASSENGER_X = 0.0f, PASSENGER_Y = -3.0f, PASSENGER_Z = 0.0f;
    private static final String FIRE_BONE = "fireBone";
    private static final String PASSENGER_BONE = "passengerBone";
    private static final String MOUTH_LOCATOR_BONE = "mouth_origin";

    // Bones for hitbox parts
    private static final String HEAD_BONE = "headController";
    private static final String NECK_BONE = "neck3Controller";
    private static final String HIP_BONE = "hip";
    private static final String LEFT_WING_BONE = "leftwing";
    private static final String RIGHT_WING_BONE = "rightwing";
    private static final String LEFT_WING_JOINT_BONE = "leftwingjoint";
    private static final String RIGHT_WING_JOINT_BONE = "rightwingjoint";
    private static final String TAIL1_BONE = "tail1";
    private static final String TAIL2_BONE = "tail2";
    private static final String TAIL3_BONE = "tail3";
    private static final String TAIL4_BONE = "tail4";
    private static final String LEFT_FRONT_LEG_BONE = "leftfrontleg";
    private static final String RIGHT_FRONT_LEG_BONE = "rightfrontleg";
    private static final String LEFT_BACK_LEG_BONE = "leftbackleg";
    private static final String RIGHT_BACK_LEG_BONE = "rightbackleg";
    private static final int SYNC_INTERVAL_TICKS = 2;
    private static final double SNAPSHOT_PRECISION = 1000.0D;

    private BakedGeoModel lastBakedModel;
    private final java.util.Map<Integer, Integer> lastBoneSnapshotHashes = new java.util.HashMap<>();

    public IgnivorusRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new IgnivorusModel());
        this.addRenderLayer(new IgnivorusMouthSmokeLayer());
        this.addRenderLayer(new IgnivorusGlowLayer(this));
    }

    @Override
    public float getMotionAnimThreshold(Ignivorus animatable) {
        return 0.000001f;
    }

    @Override
    protected float getDeathMaxRotation(Ignivorus entity) {
        return 0.0F;
    }

    @Override
    public void preRender(PoseStack poseStack,
                          Ignivorus entity,
                          BakedGeoModel model,
                          MultiBufferSource bufferSource,
                          VertexConsumer buffer,
                          boolean isReRender,
                          float partialTick,
                          int packedLight,
                          int packedOverlay,
                          float red, float green, float blue, float alpha) {

        float scale = 1.0f;
        poseStack.scale(scale, scale, scale);
        this.shadowRadius = entity.isBaby() ? 1.5F : 5.0f;

        this.lastBakedModel = model;
        enableTrackingForBones(model);

        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private void enableTrackingForBones(BakedGeoModel model) {
        if (model == null) {
            return;
        }
        model.getBone(PASSENGER_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(FIRE_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(MOUTH_LOCATOR_BONE).ifPresent(b -> b.setTrackingMatrices(true));

        // Enable tracking for hitbox bones
        model.getBone(HEAD_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(NECK_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(HIP_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(LEFT_WING_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(RIGHT_WING_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(LEFT_WING_JOINT_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(RIGHT_WING_JOINT_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(TAIL1_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(TAIL2_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(TAIL3_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(TAIL4_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(LEFT_FRONT_LEG_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(RIGHT_FRONT_LEG_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(LEFT_BACK_LEG_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(RIGHT_BACK_LEG_BONE).ifPresent(b -> b.setTrackingMatrices(true));
    }

    @Override
    public void render(@NotNull Ignivorus entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        RenderPassContext.beginExtraction(entity.getId());
        RiderBullcrap.notifyRendered(entity.getId());
        try {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            RenderPassContext.endExtraction();
        }

        if (this.lastBakedModel == null) {
            return;
        }

        if (entity.isBaby()) {
            this.lastBakedModel.getBone(PASSENGER_BONE).ifPresent(b -> {
                net.minecraft.world.phys.Vec3 world = transformLocator(b, PASSENGER_X, PASSENGER_Y, PASSENGER_Z);
                if (world != null) {
                    entity.setClientLocatorPosition("passengerLocator", world);
                }
            });
            return;
        }

        this.lastBakedModel.getBone(PASSENGER_BONE).ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, PASSENGER_X, PASSENGER_Y, PASSENGER_Z);
            if (world != null) {
                entity.setClientLocatorPosition("passengerLocator", world);
            }
        });

        this.lastBakedModel.getBone(FIRE_BONE).ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, 0f, 0f, 0f);
            if (world != null) {
                entity.setClientLocatorPosition("fireBoneOrigin", world);
            }
        });

        this.lastBakedModel.getBone(MOUTH_LOCATOR_BONE).ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, 0f, 0f, 0f);
            if (world != null) {
                entity.setClientLocatorPosition("mouth_origin", world);
            }
        });

        // Track hitbox bone positions and sync to server
        trackBone(HEAD_BONE, "headController", entity);
        trackBone(NECK_BONE, "neck3Controller", entity);
        trackBone(HIP_BONE, "hip", entity);
        trackBone(LEFT_WING_BONE, "leftwing", entity);
        trackBone(RIGHT_WING_BONE, "rightwing", entity);
        trackBone(LEFT_WING_JOINT_BONE, "leftwingjoint", entity);
        trackBone(RIGHT_WING_JOINT_BONE, "rightwingjoint", entity);
        trackBone(TAIL1_BONE, "tail1", entity);
        trackBone(TAIL2_BONE, "tail2", entity);
        trackBone(TAIL3_BONE, "tail3", entity);
        trackBone(TAIL4_BONE, "tail4", entity);
        trackBone(LEFT_FRONT_LEG_BONE, "leftfrontleg", entity);
        trackBone(RIGHT_FRONT_LEG_BONE, "rightfrontleg", entity);
        trackBone(LEFT_BACK_LEG_BONE, "leftbackleg", entity);
        trackBone(RIGHT_BACK_LEG_BONE, "rightbackleg", entity);

        // Send bone positions to server for hitbox sync (every few frames to reduce network load)
        sendBonePositionsToServer(entity);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, Ignivorus animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        RiderConfig.RiderSpec riderSpec = RiderConfig.getSpec(animatable);
        if (riderSpec == null || !bone.getName().equals(riderSpec.boneName)) {
            return;
        }
        if (!RenderPassContext.isExtractionAllowed(animatable.getId())) {
            return;
        }

        Matrix4f viewMatrix = new Matrix4f((Matrix4fc) poseStack.last().pose());
        Vector4f boneViewPos4 = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f).mul((Matrix4fc) viewMatrix);
        double viewSpaceDistance = Math.sqrt(
                boneViewPos4.x() * boneViewPos4.x()
                        + boneViewPos4.y() * boneViewPos4.y()
                        + boneViewPos4.z() * boneViewPos4.z()
        );
        if (viewSpaceDistance >= riderSpec.maxCaptureDistance) {
            return;
        }
        if (!RiderBullcrap.tryLockForFrame(animatable.getId())) {
            return;
        }

        RiderBullcrap.store(animatable.getId(), viewMatrix);
        Vector3d boneWorldPosJoml = bone.getWorldPosition();
        Vec3 boneWorldPos = new Vec3(boneWorldPosJoml.x, boneWorldPosJoml.y, boneWorldPosJoml.z);
        RiderBullcrap.storeCameraOffset(animatable.getId(), boneWorldPos.subtract(animatable.position()));
    }

    private void sendBonePositionsToServer(Ignivorus entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !entity.isAlive()) {
            return;
        }

        // Keep server-side multipart hitboxes aligned even when nobody is riding.
        // Limit to nearby entities to avoid unnecessary traffic.
        if (minecraft.player.distanceToSqr(entity) > 96.0D * 96.0D) {
            return;
        }

        // Per-entity cadence so one dragon's render calls do not throttle another's.
        if ((entity.tickCount + entity.getId()) % SYNC_INTERVAL_TICKS != 0) {
            return;
        }

        java.util.Map<String, net.minecraft.world.phys.Vec3> positions =
                new java.util.HashMap<>(com.leon.saintsdragons.common.network.MessageDragonBonePositions.SYNCED_BONES.length);
        for (String boneName : com.leon.saintsdragons.common.network.MessageDragonBonePositions.SYNCED_BONES) {
            net.minecraft.world.phys.Vec3 pos = entity.getClientLocatorPosition(boneName);
            if (pos != null) {
                positions.put(boneName, pos);
            }
        }

        if (positions.isEmpty()) {
            return;
        }

        int snapshotHash = computeSnapshotHash(positions);
        Integer previousHash = lastBoneSnapshotHashes.put(entity.getId(), snapshotHash);
        if (previousHash != null && previousHash == snapshotHash) {
            return;
        }

        if (!positions.isEmpty()) {
            com.leon.saintsdragons.common.network.NetworkHandler.sendToServer(
                new com.leon.saintsdragons.common.network.MessageDragonBonePositions(entity.getId(), positions)
            );
        }
    }

    private static int computeSnapshotHash(java.util.Map<String, net.minecraft.world.phys.Vec3> positions) {
        int hash = 1;
        for (String boneName : com.leon.saintsdragons.common.network.MessageDragonBonePositions.SYNCED_BONES) {
            net.minecraft.world.phys.Vec3 pos = positions.get(boneName);
            if (pos == null) {
                continue;
            }
            hash = 31 * hash + boneName.hashCode();
            hash = 31 * hash + quantize(pos.x);
            hash = 31 * hash + quantize(pos.y);
            hash = 31 * hash + quantize(pos.z);
        }
        return hash;
    }

    private static int quantize(double value) {
        return (int) Math.round(value * SNAPSHOT_PRECISION);
    }

    private void trackBone(String boneName, String locatorName, Ignivorus entity) {
        this.lastBakedModel.getBone(boneName).ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, 0f, 0f, 0f);
            if (world != null) {
                entity.setClientLocatorPosition(locatorName, world);
            }
        });
    }

    private net.minecraft.world.phys.Vec3 transformLocator(GeoBone bone, float px, float py, float pz) {
        if (bone == null || bone.getWorldSpaceMatrix() == null) return null;

        float lx = px / 16f;
        float ly = py / 16f;
        float lz = pz / 16f;
        org.joml.Matrix4f worldMat = new org.joml.Matrix4f(bone.getWorldSpaceMatrix());
        org.joml.Vector4f in = new org.joml.Vector4f(lx, ly, lz, 1f);
        org.joml.Vector4f out = worldMat.transform(in);
        return new net.minecraft.world.phys.Vec3(out.x(), out.y(), out.z());
    }
}
