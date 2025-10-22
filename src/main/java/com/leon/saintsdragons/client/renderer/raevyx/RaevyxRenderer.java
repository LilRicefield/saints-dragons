package com.leon.saintsdragons.client.renderer.raevyx;

import com.leon.saintsdragons.client.model.raevyx.RaevyxModel;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.leon.saintsdragons.client.renderer.layer.raevyx.RaevyxLightningBeamLayer;

@OnlyIn(Dist.CLIENT)
public class RaevyxRenderer extends GeoEntityRenderer<Raevyx> {
    private BakedGeoModel lastBakedModel;
    private static final ResourceLocation TEXTURE_MALE = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/raevyx/raevyx.png");
    private static final ResourceLocation TEXTURE_FEMALE =  ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/raevyx/raevyx_female.png");
    private static final ResourceLocation TEXTURE_BABY = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/raevyx/baby_raevyx.png");

    public RaevyxRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RaevyxModel());
        // Attach beam render layer
        this.addRenderLayer(new RaevyxLightningBeamLayer());
        // TODO: Re-enable once vanilla positioning is confirmed working
        // this.addRenderLayer(new RaevyxRiderLayer());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull Raevyx entity) {
        if (entity.isBaby()) {
            return TEXTURE_BABY;
        }
        return entity.isFemale() ? TEXTURE_FEMALE : TEXTURE_MALE;
    }

    // Suppress vanilla death flip; use custom death animation instead (method name varies by MC version)
    protected float getDeathMaxRotation(Raevyx entity) {
        return 0.0F;
    }

    @Override
    public void preRender(PoseStack poseStack,
                          Raevyx entity,
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

        // Enable matrix tracking for the feet bones we care about
        this.lastBakedModel = model;
        enableTrackingForBones(model);

        // Call super.preRender
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
    @Override
    public void render(@NotNull Raevyx entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {

        // Call normal rendering first
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        // After bones have been processed, sample accurate world positions for foot locators
        sampleAndStashLocatorsAccurate(entity);
    }
    @Override
    public RenderType getRenderType(Raevyx animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    // --- Helpers ---
    private static final float L_LEFT_X =  2.2f,  L_LEFT_Y = 0.05f, L_LEFT_Z = 2.85f;
    private static final float L_RIGHT_X = -2.2f, L_RIGHT_Y = 0.05f, L_RIGHT_Z = 2.85f;
    private static final float MOUTH_X = 0.1f, MOUTH_Y = 8.7f, MOUTH_Z = -17.4f;
    private static final float BODY_X = 0.0f, BODY_Y = 10.0f, BODY_Z = 0.0f;
    // Passenger bone offsets (in pixels, divided by 16 to convert to blocks)
    // X = left/right, Y = up/down (negative pushes down), Z = forward/back (negative = forward)
    private static final float PASSENGER_X = 0.0f, PASSENGER_Y = -3.0f, PASSENGER_Z = 0.0f;

    private void enableTrackingForBones(BakedGeoModel model) {
        if (model == null) return;
        model.getBone("leftfeet").ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone("rightfeet").ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone("head").ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone("heightController").ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone("passengerBone").ifPresent(b -> b.setTrackingMatrices(true));
    }

    private void sampleAndStashLocatorsAccurate(Raevyx entity) {
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
        // Sample mouth origin
        this.lastBakedModel.getBone("head").ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, MOUTH_X, MOUTH_Y, MOUTH_Z);
            if (world != null) entity.setClientLocatorPosition("mouth_origin", world);
        });
        // Sample body locator
        this.lastBakedModel.getBone("heightController").ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, BODY_X, BODY_Y, BODY_Z);
            if (world != null) entity.setClientLocatorPosition("bodyLocator", world);
        });
        // Sample passenger bone position for rider placement
        this.lastBakedModel.getBone("passengerBone").ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, PASSENGER_X, PASSENGER_Y, PASSENGER_Z);
            if (world != null) entity.setClientLocatorPosition("passengerLocator", world);
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
