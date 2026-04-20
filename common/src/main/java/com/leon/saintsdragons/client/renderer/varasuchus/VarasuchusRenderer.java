package com.leon.saintsdragons.client.renderer.varasuchus;

import com.leon.saintsdragons.client.model.varasuchus.VarasuchusModel;
import com.leon.saintsdragons.client.renderer.RiderBullcrap;
import com.leon.saintsdragons.client.renderer.RiderConfig;
import com.leon.saintsdragons.client.renderer.RenderPassContext;
import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector4f;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class VarasuchusRenderer extends GeoEntityRenderer<Varasuchus> {
    private BakedGeoModel lastBakedModel;
    public VarasuchusRenderer(EntityRendererProvider.Context context) {
        super(context, new VarasuchusModel());
    }

    @Override
    public float getMotionAnimThreshold(Varasuchus animatable) {
        return 0.000001f;
    }

    @Override
    protected float getDeathMaxRotation(Varasuchus entity) {
        return 0.0F;
    }

    @Override
    public void preRender(@NotNull PoseStack poseStack,
                          Varasuchus entity,
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
        this.shadowRadius = entity.isBaby() ? 1.5F : 2.5f;

        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void render(@NotNull Varasuchus entity, float entityYaw, float partialTick,
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

        sampleAndStashLocatorsAccurate(entity);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, Varasuchus animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        RiderConfig.RiderSpec riderConfig = RiderConfig.getSpec(animatable);
        if (riderConfig == null || !bone.getName().equals(riderConfig.boneName)) {
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
        if (viewSpaceDistance >= riderConfig.maxCaptureDistance) {
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

    @Override
    public RenderType getRenderType(Varasuchus animatable, net.minecraft.resources.ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    private static final float PASSENGER_X = 0.0f, PASSENGER_Y = -3.0f, PASSENGER_Z = 0.0f;

    private void enableTrackingForBones(BakedGeoModel model) {
        if (model == null) return;
        model.getBone("passengerBone").ifPresent(b -> b.setTrackingMatrices(true));
    }

    private void sampleAndStashLocatorsAccurate(Varasuchus entity) {
        if (this.lastBakedModel == null || entity == null) return;

        this.lastBakedModel.getBone("passengerBone").ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, PASSENGER_X, PASSENGER_Y, PASSENGER_Z);
            if (world != null) entity.setClientLocatorPosition("passengerLocator", world);
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
