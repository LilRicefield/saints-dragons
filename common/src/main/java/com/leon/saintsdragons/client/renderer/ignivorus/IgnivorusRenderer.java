package com.leon.saintsdragons.client.renderer.ignivorus;

import com.leon.saintsdragons.client.model.ignivorus.IgnivorusModel;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class IgnivorusRenderer extends GeoEntityRenderer<Ignivorus> {
    private static final float PASSENGER_X = 0.0f, PASSENGER_Y = -3.0f, PASSENGER_Z = 0.0f;

    private BakedGeoModel lastBakedModel;

    public IgnivorusRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new IgnivorusModel());
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
        this.shadowRadius = 2.0f * scale;

        this.lastBakedModel = model;
        enableTrackingForBones(model);

        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private void enableTrackingForBones(BakedGeoModel model) {
        model.getBone("passengerBone").ifPresent(b -> b.setTrackingMatrices(true));
    }

    @Override
    public void render(@NotNull Ignivorus entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        if (this.lastBakedModel != null) {
            this.lastBakedModel.getBone("passengerBone").ifPresent(b -> {
                net.minecraft.world.phys.Vec3 world = transformLocator(b, PASSENGER_X, PASSENGER_Y, PASSENGER_Z);
                if (world != null) {
                    entity.setClientLocatorPosition("passengerLocator", world);
                }
            });
        }
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
