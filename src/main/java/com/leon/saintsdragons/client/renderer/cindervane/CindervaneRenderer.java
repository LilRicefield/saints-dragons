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

    private BakedGeoModel lastBakedModel;

    public CindervaneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CindervaneModel());
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
        this.shadowRadius = 0.8f * scale;

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
    }
    @Override
    public void render(@NotNull Cindervane entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
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
