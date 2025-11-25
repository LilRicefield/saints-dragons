package com.leon.saintsdragons.client.renderer.cindervane;

import com.leon.saintsdragons.client.model.cindervane.CindervaneModel;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CindervaneRenderer extends GeoEntityRenderer<Cindervane> {
    // Offset from bone pivot for rider positioning (adjust as needed)
    private static final float PASSENGER_SEAT0_X = 0.0f, PASSENGER_SEAT0_Y = -3.0f, PASSENGER_SEAT0_Z = 0.0f;
    private static final float PASSENGER_SEAT1_X = 0.0f, PASSENGER_SEAT1_Y = -3.0f, PASSENGER_SEAT1_Z = 0.0f;
    private static final String MOUTH_LOCATOR_NAME = "mouth_origin";
    private static final String MOUTH_BONE = "jawController";
    private static final float MOUTH_OFFSET_X = 0.0f;
    private static final float MOUTH_OFFSET_Y = 1.5f;
    private static final float MOUTH_OFFSET_Z = -9.0f;

    private BakedGeoModel lastBakedModel;

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

    private void enableTrackingForBones(BakedGeoModel model) {
        // Enable tracking for both passenger seat bones
        model.getBone("passengerBone1").ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone("passengerBone2").ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone(MOUTH_BONE).ifPresent(b -> b.setTrackingMatrices(true));
    }
    @Override
    public void render(@NotNull Cindervane entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        this.lastBakedModel = this.getGeoModel().getBakedModel(this.getGeoModel().getModelResource(entity));
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        // Sample passenger bone positions and store in entity's locator cache for RiderController to use
        if (this.lastBakedModel != null) {
            // Sample seat 0 (driver) position from passengerBone1
            this.lastBakedModel.getBone("passengerBone1").ifPresent(b -> {
                net.minecraft.world.phys.Vec3 world = transformLocator(b, PASSENGER_SEAT0_X, PASSENGER_SEAT0_Y, PASSENGER_SEAT0_Z);
                if (world != null) {
                    entity.setClientLocatorPosition("passengerSeat0", world);
                }
            });

            // Sample seat 1 (passenger) position from passengerBone2
            this.lastBakedModel.getBone("passengerBone2").ifPresent(b -> {
                net.minecraft.world.phys.Vec3 world = transformLocator(b, PASSENGER_SEAT1_X, PASSENGER_SEAT1_Y, PASSENGER_SEAT1_Z);
                if (world != null) {
                    entity.setClientLocatorPosition("passengerSeat1", world);
                }
            });

            // Sample accurate mouth locator from jaw controller so bite abilities originate at the model mouth
            this.lastBakedModel.getBone(MOUTH_BONE).ifPresent(b -> {
                net.minecraft.world.phys.Vec3 world = transformLocator(b, MOUTH_OFFSET_X, MOUTH_OFFSET_Y, MOUTH_OFFSET_Z);
                if (world != null) {
                    entity.setClientLocatorPosition(MOUTH_LOCATOR_NAME, world);
                }
            });
        }
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
