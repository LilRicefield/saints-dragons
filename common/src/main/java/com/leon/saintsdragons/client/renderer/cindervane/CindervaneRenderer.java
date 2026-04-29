package com.leon.saintsdragons.client.renderer.cindervane;

import com.leon.saintsdragons.client.renderer.RiderBullcrap;
import com.leon.saintsdragons.client.renderer.RiderConfig;
import com.leon.saintsdragons.client.renderer.RenderPassContext;
import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.client.model.cindervane.CindervaneModel;
import com.leon.saintsdragons.common.network.MessageDragonBonePositions;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CindervaneRenderer extends GeoEntityRenderer<Cindervane> {
    // Offset from bone pivot for rider positioning (adjust as needed)
    private static final float PASSENGER_SEAT0_X = 0.0f, PASSENGER_SEAT0_Y = -3.0f, PASSENGER_SEAT0_Z = 0.0f;
    private static final float PASSENGER_SEAT1_X = 0.0f, PASSENGER_SEAT1_Y = -3.0f, PASSENGER_SEAT1_Z = 0.0f;
    private static final String AUTO_MOUNT_BONE = "automountBoneRight";
    private static final String AUTO_MOUNT_LOCATOR = "automountBoneRight";
    private static final float AUTO_MOUNT_OFFSET_X = 0.0f;
    private static final float AUTO_MOUNT_OFFSET_Y = 0.0f;
    private static final float AUTO_MOUNT_OFFSET_Z = 0.0f;
    private static final int SYNC_INTERVAL_TICKS = 2;
    private static final double SNAPSHOT_PRECISION = 1000.0;

    private BakedGeoModel lastBakedModel;
    private final java.util.Map<Integer, Integer> lastBoneSnapshotHashes = new java.util.HashMap<>();

    public CindervaneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CindervaneModel());
    }

    @Override
    public float getMotionAnimThreshold(Cindervane animatable) {
        return 0.000001f;
    }

    @Override
    protected float getDeathMaxRotation(Cindervane entity) {
        // Keep Amphithere upright so custom death animation plays without vanilla flop
        return 0.0F;
    }

    @Override
    public void preRender(PoseStack poseStack,
                          Cindervane entity,
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

        if (entity.isBaby()) {
            this.shadowRadius = 0.8f;
        } else {
            this.shadowRadius = 2.0f * scale;
        }

        // Store the model for later use in render()
        this.lastBakedModel = model;

        // Enable matrix tracking for passenger bones
        enableTrackingForBones(model);

        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private void enableTrackingForBones(BakedGeoModel model) {
        // Enable tracking for both passenger seat bones
        model.getBone("passengerBone1").ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone("passengerBone2").ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(AUTO_MOUNT_BONE).ifPresent(b -> b.setTrackingMatrices(true));
    }
    @Override
    public void render(@NotNull Cindervane entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        if (ShaderPassCompatibility.isIrisShadowPass()) {
            return;
        }
        RenderPassContext.beginExtraction(entity.getId());
        RiderBullcrap.notifyRendered(entity.getId());
        try {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            RenderPassContext.endExtraction();
        }

        // Sample passenger bone positions and store in entity's locator cache for RiderController to use
        if (this.lastBakedModel != null) {
            // Sample seat 0 (driver) position from passengerBone1
            this.lastBakedModel.getBone("passengerBone1").ifPresent(b -> {
                net.minecraft.world.phys.Vec3 world = transformLocator(b, PASSENGER_SEAT0_X, PASSENGER_SEAT0_Y, PASSENGER_SEAT0_Z);
                if (world != null) {
                    entity.setClientLocatorPosition("passengerSeat0", world);
                    // Alias key so single-seat lookup paths still work in compatibility mode.
                    entity.setClientLocatorPosition("passengerLocator", world);
                }
            });

            // Sample seat 1 (passenger) position from passengerBone2
            this.lastBakedModel.getBone("passengerBone2").ifPresent(b -> {
                net.minecraft.world.phys.Vec3 world = transformLocator(b, PASSENGER_SEAT1_X, PASSENGER_SEAT1_Y, PASSENGER_SEAT1_Z);
                if (world != null) {
                    entity.setClientLocatorPosition("passengerSeat1", world);
                }
            });

            this.lastBakedModel.getBone(AUTO_MOUNT_BONE).ifPresent(b -> {
                net.minecraft.world.phys.Vec3 world = transformLocator(b, AUTO_MOUNT_OFFSET_X, AUTO_MOUNT_OFFSET_Y, AUTO_MOUNT_OFFSET_Z);
                if (world != null) {
                    entity.setClientLocatorPosition(AUTO_MOUNT_LOCATOR, world);
                }
            });

        }

        sendBonePositionsToServer(entity);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, Cindervane animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        int seatIndex = -1;
        RiderConfig.RiderSpec riderSpec = RiderConfig.getSpec(animatable);
        if (riderSpec == null) {
            return;
        }
        if (bone.getName().equals(RiderConfig.getSeatBoneName(animatable, 0))) {
            seatIndex = 0;
        } else if (bone.getName().equals(RiderConfig.getSeatBoneName(animatable, 1))) {
            seatIndex = 1;
        }
        if (seatIndex < 0) {
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
        if (!RiderBullcrap.tryLockForFrame(animatable.getId(), seatIndex)) {
            return;
        }

        RiderBullcrap.store(animatable.getId(), seatIndex, viewMatrix);
        Vector3d boneWorldPosJoml = bone.getWorldPosition();
        net.minecraft.world.phys.Vec3 boneWorldPos = new net.minecraft.world.phys.Vec3(
                boneWorldPosJoml.x,
                boneWorldPosJoml.y,
                boneWorldPosJoml.z
        );
        RiderBullcrap.storeCameraOffset(animatable.getId(), seatIndex, boneWorldPos.subtract(animatable.position()));
    }

    private void sendBonePositionsToServer(Cindervane entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || entity.getControllingPassenger() != minecraft.player) {
            return;
        }

        if ((entity.tickCount + entity.getId()) % SYNC_INTERVAL_TICKS != 0) {
            return;
        }

        java.util.Map<String, net.minecraft.world.phys.Vec3> positions = new java.util.HashMap<>(1);
        net.minecraft.world.phys.Vec3 autoMount = entity.getClientLocatorPosition(AUTO_MOUNT_LOCATOR);
        if (autoMount != null) {
            positions.put(AUTO_MOUNT_LOCATOR, autoMount);
        }

        if (positions.isEmpty()) {
            return;
        }

        int snapshotHash = computeSnapshotHash(positions);
        Integer previousHash = lastBoneSnapshotHashes.put(entity.getId(), snapshotHash);
        if (previousHash != null && previousHash == snapshotHash) {
            return;
        }

        NetworkHandler.sendToServer(new MessageDragonBonePositions(entity.getId(), positions));
    }

    private static int computeSnapshotHash(java.util.Map<String, net.minecraft.world.phys.Vec3> positions) {
        int hash = 1;
        net.minecraft.world.phys.Vec3 autoMount = positions.get(AUTO_MOUNT_LOCATOR);
        if (autoMount != null) {
            hash = 31 * hash + AUTO_MOUNT_LOCATOR.hashCode();
            hash = 31 * hash + quantize(autoMount.x);
            hash = 31 * hash + quantize(autoMount.y);
            hash = 31 * hash + quantize(autoMount.z);
        }
        return hash;
    }

    private static int quantize(double value) {
        return (int) Math.round(value * SNAPSHOT_PRECISION);
    }

    /**
     * Transform a local offset relative to a bone into world space.
     * This is used to sample bone positions for rider placement.
     */
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
