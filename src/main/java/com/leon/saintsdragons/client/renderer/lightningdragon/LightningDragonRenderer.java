package com.leon.saintsdragons.client.renderer.lightningdragon;

import com.leon.saintsdragons.client.model.lightningdragon.LightningDragonModel;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.leon.saintsdragons.client.renderer.layer.lightningdragon.LightningBeamLayer;

@OnlyIn(Dist.CLIENT)
public class LightningDragonRenderer extends GeoEntityRenderer<LightningDragonEntity> {
    private BakedGeoModel lastBakedModel;
    public LightningDragonRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LightningDragonModel());
        // Attach beam render layer
        this.addRenderLayer(new LightningBeamLayer());
    }

    // Suppress vanilla death flip; use custom death animation instead (method name varies by MC version)
    protected float getDeathMaxRotation(LightningDragonEntity entity) {
        return 0.0F;
    }

    @Override
    public void preRender(PoseStack poseStack,
                          LightningDragonEntity entity,
                          BakedGeoModel model,
                          MultiBufferSource bufferSource,
                          VertexConsumer buffer,
                          boolean isReRender,
                          float partialTick,
                          int packedLight,
                          int packedOverlay,
                          float red, float green, float blue, float alpha) {

        // Scale the dragon
        float scale = 4.5f;
        poseStack.scale(scale, scale, scale);
        this.shadowRadius = 0.8f * scale;

        // Enable matrix tracking for the feet bones we care about
        this.lastBakedModel = model;
        enableTrackingForFootBones(model);

        // Call super.preRender
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);


        // No rider_anchor sampling; seat uses math-based anchor
    }
    @Override
    public void render(@NotNull LightningDragonEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {

        // Call normal rendering first
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        // After bones have been processed, sample accurate world positions for foot locators
        sampleAndStashFootLocatorsAccurate(entity);
    }
    @Override
    public RenderType getRenderType(LightningDragonEntity animatable, ResourceLocation texture,
                                   @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    // --- Helpers ---
    private static final float L_LEFT_X =  2.2f,  L_LEFT_Y = 0.05f, L_LEFT_Z = 2.85f;
    private static final float L_RIGHT_X = -2.2f, L_RIGHT_Y = 0.05f, L_RIGHT_Z = 2.85f;

    private void enableTrackingForFootBones(BakedGeoModel model) {
        if (model == null) return;
        model.getBone("leftfeet").ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone("rightfeet").ifPresent(b -> b.setTrackingMatrices(true));
    }

    private void sampleAndStashFootLocatorsAccurate(LightningDragonEntity entity) {
        if (this.lastBakedModel == null || entity == null) return;
        // Sample left foot
        this.lastBakedModel.getBone("leftfeet").ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, L_LEFT_X, L_LEFT_Y, L_LEFT_Z);
            if (world != null) entity.setClientLocatorPosition("leftfeetLocator", world);
        });
        // Sample right foot
        this.lastBakedModel.getBone("rightfeet").ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, L_RIGHT_X, L_RIGHT_Y, L_RIGHT_Z);
            if (world != null) entity.setClientLocatorPosition("rightfeetLocator", world);
        });
        // No beam_origin sampling required; beam uses computeHeadMouthOrigin()
    }

    private static net.minecraft.world.phys.Vec3 transformLocator(software.bernie.geckolib.cache.object.GeoBone bone,
                                                                  float px, float py, float pz) {
        if (bone == null) return null;
        // Convert pixels to model units (blocks)
        float lx = px / 16f;
        float ly = py / 16f;
        float lz = pz / 16f;
        org.joml.Matrix4f worldMat = new org.joml.Matrix4f(bone.getWorldSpaceMatrix());
        org.joml.Vector4f in = new org.joml.Vector4f(lx, ly, lz, 1f);
        org.joml.Vector4f out = worldMat.transform(in);
        return new net.minecraft.world.phys.Vec3(out.x(), out.y(), out.z());
    }
}
