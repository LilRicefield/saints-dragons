package com.leon.saintsdragons.client.renderer.stegonaut;

import com.leon.saintsdragons.client.renderer.RiderBullcrap;
import com.leon.saintsdragons.client.renderer.RiderConfig;
import com.leon.saintsdragons.client.renderer.RenderPassContext;
import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.client.model.stegonaut.StegonautModel;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector4f;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class StegonautRenderer extends GeoEntityRenderer<Stegonaut> {
    private BakedGeoModel lastBakedModel;
    
    private static final String PASSENGER_BONE = "passengerBone";
    private static final float PASSENGER_X = 0.0f;
    private static final float PASSENGER_Y = -3.0f;
    private static final float PASSENGER_Z = 0.0f;
    
    public StegonautRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new StegonautModel());
    }

    @Override
    public float getMotionAnimThreshold(Stegonaut animatable) {
        return 0.000001f;
    }

    @Override
    protected float getDeathMaxRotation(Stegonaut entity) {
        return 0.0F;
    }

    @Override
    public void preRender(PoseStack poseStack,
                          Stegonaut entity,
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
        this.shadowRadius = entity.isBaby() ? 1.0F : 2.25f;

        this.lastBakedModel = model;
        enableTrackingForBones(model);

        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
    
    @Override
    public void render(@NotNull Stegonaut entity, float entityYaw, float partialTick,
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
    public RenderType getRenderType(Stegonaut animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, Stegonaut animatable, GeoBone bone, RenderType renderType,
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
        if (!RiderBullcrap.tryLockForFrame(animatable.getId(), 0)) {
            return;
        }
        RiderBullcrap.store(animatable.getId(), 0, viewMatrix);
        Vector3d boneWorldPosJoml = bone.getWorldPosition();
        Vec3 boneWorldPos = new Vec3(boneWorldPosJoml.x, boneWorldPosJoml.y, boneWorldPosJoml.z);
        RiderBullcrap.storeCameraOffset(animatable.getId(), 0, boneWorldPos.subtract(animatable.position()));
    }

    private void enableTrackingForBones(BakedGeoModel model) {
        if (model == null) {
            return;
        }
        model.getBone(PASSENGER_BONE).ifPresent(b -> b.setTrackingMatrices(true));
    }
    
    private void sampleAndStashLocatorsAccurate(Stegonaut entity) {
        if (this.lastBakedModel == null || entity == null) return;

        this.lastBakedModel.getBone(PASSENGER_BONE).ifPresent(b -> {
            Vec3 world = transformLocator(b, PASSENGER_X, PASSENGER_Y, PASSENGER_Z);
            if (world != null) entity.setClientLocatorPosition("passengerLocator", world);
        });
    }
    
    private static Vec3 transformLocator(GeoBone bone, float px, float py, float pz) {
        if (bone == null) return null;
        float lx = px / 16f;
        float ly = py / 16f;
        float lz = pz / 16f;
        Matrix4f worldMat = new Matrix4f(bone.getWorldSpaceMatrix());
        Vector4f in = new Vector4f(lx, ly, lz, 1f);
        Vector4f out = worldMat.transform(in);
        
        return new Vec3(out.x(), out.y(), out.z());
    }
}
