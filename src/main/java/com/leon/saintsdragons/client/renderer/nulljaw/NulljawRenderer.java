package com.leon.saintsdragons.client.renderer.nulljaw;

import com.leon.saintsdragons.client.model.nulljaw.NulljawModel;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class NulljawRenderer extends GeoEntityRenderer<Nulljaw> {
    private BakedGeoModel lastBakedModel;
    public NulljawRenderer(EntityRendererProvider.Context context) {
        super(context, new NulljawModel());
    }

    @Override
    public void preRender(@NotNull PoseStack poseStack,
                          Nulljaw entity,
                          BakedGeoModel model,
                          MultiBufferSource bufferSource,
                          com.mojang.blaze3d.vertex.VertexConsumer buffer,
                          boolean isReRender,
                          float partialTick,
                          int packedLight,
                          int packedOverlay,
                          float red, float green, float blue, float alpha) {

        // Store model and enable matrix tracking for bones with locators
        this.lastBakedModel = model;
        enableTrackingForBones(model);

        // Scale the drake - females are slightly smaller (85% scale)
        float scale = 1.0f;
        poseStack.scale(scale, scale, scale);
        this.shadowRadius = 2.5f * scale;

        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void render(@NotNull Nulljaw entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        // Call normal rendering first
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        // After bones have been processed, sample accurate world positions for locators
        sampleAndStashLocatorsAccurate(entity);
    }

    // --- Locator coordinates from rift_drake.geo.json (in pixels) ---
    private static final float FRONT_X = 0f, FRONT_Y = 0f, FRONT_Z = -5f;
    private static final float BACK_X = 0f, BACK_Y = 0f, BACK_Z = 10f;
    private static final float MOUTH_X = 0f, MOUTH_Y = 10.25f, MOUTH_Z = -24f;
    private static final float LEFT_FEET_X = 4.2f, LEFT_FEET_Y = 0f, LEFT_FEET_Z = -5f;
    private static final float RIGHT_FEET_X = -4.2f, RIGHT_FEET_Y = 0f, RIGHT_FEET_Z = -5f;

    private void enableTrackingForBones(BakedGeoModel model) {
        if (model == null) return;
        model.getBone("body").ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone("jawController").ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone("leftfrontfeetController").ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone("rightfrontfeetController").ifPresent(b -> b.setTrackingMatrices(true));
    }

    private void sampleAndStashLocatorsAccurate(Nulljaw entity) {
        if (this.lastBakedModel == null || entity == null) return;

        // Sample front and back locators from body bone
        this.lastBakedModel.getBone("body").ifPresent(b -> {
            net.minecraft.world.phys.Vec3 front = transformLocator(b, FRONT_X, FRONT_Y, FRONT_Z);
            if (front != null) entity.setClientLocatorPosition("frontLocator", front);

            net.minecraft.world.phys.Vec3 back = transformLocator(b, BACK_X, BACK_Y, BACK_Z);
            if (back != null) entity.setClientLocatorPosition("backLocator", back);
        });

        // Sample mouth origin from jawController bone
        this.lastBakedModel.getBone("jawController").ifPresent(b -> {
            net.minecraft.world.phys.Vec3 mouth = transformLocator(b, MOUTH_X, MOUTH_Y, MOUTH_Z);
            if (mouth != null) entity.setClientLocatorPosition("mouth_origin", mouth);
        });

        // Sample left front feet locator
        this.lastBakedModel.getBone("leftfrontfeetController").ifPresent(b -> {
            net.minecraft.world.phys.Vec3 leftFeet = transformLocator(b, LEFT_FEET_X, LEFT_FEET_Y, LEFT_FEET_Z);
            if (leftFeet != null) entity.setClientLocatorPosition("leftfrontfeetLocator", leftFeet);
        });

        // Sample right front feet locator
        this.lastBakedModel.getBone("rightfrontfeetController").ifPresent(b -> {
            net.minecraft.world.phys.Vec3 rightFeet = transformLocator(b, RIGHT_FEET_X, RIGHT_FEET_Y, RIGHT_FEET_Z);
            if (rightFeet != null) entity.setClientLocatorPosition("rightfrontfeetLocator", rightFeet);
        });
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
