package com.leon.saintsdragons.client.renderer.raevyx;

import com.leon.saintsdragons.client.model.raevyx.RaevyxModel;
import com.leon.saintsdragons.client.renderer.RenderPassContext;
import com.leon.saintsdragons.client.renderer.RiderConfig;
import com.leon.saintsdragons.client.renderer.RiderBullcrap;
import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.leon.saintsdragons.client.renderer.layer.raevyx.RaevyxLightningBeamLayer;
import com.leon.saintsdragons.client.renderer.layer.raevyx.RaevyxGlowLayer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector4f;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class RaevyxRenderer extends GeoEntityRenderer<Raevyx> {
    private BakedGeoModel lastBakedModel;
    private static final ResourceLocation TEXTURE_MALE = SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx.png");
    private static final ResourceLocation TEXTURE_FEMALE = SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_female.png");
    private static final ResourceLocation TEXTURE_NIGHT_GOLD_MALE = SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_night_gold.png");
    private static final ResourceLocation TEXTURE_NIGHT_GOLD_FEMALE = SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_night_gold_female.png");
    private static final ResourceLocation TEXTURE_BABY_MALE = SaintsDragonsCommon.rl("textures/entity/raevyx/baby_raevyx.png");
    private static final ResourceLocation TEXTURE_BABY_FEMALE = SaintsDragonsCommon.rl("textures/entity/raevyx/baby_raevyx_female.png");

    public RaevyxRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RaevyxModel());
        // Attach beam render layers (glow + beam)
        this.addRenderLayer(new RaevyxGlowLayer(this));
        this.addRenderLayer(new RaevyxLightningBeamLayer());
    }

    @Override
    public float getMotionAnimThreshold(Raevyx animatable) {
        return 0.000001f;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull Raevyx entity) {
        if (entity.isBaby()) {
            return entity.isFemale() ? TEXTURE_BABY_FEMALE : TEXTURE_BABY_MALE;
        }
        if (entity.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD) {
            return entity.isFemale() ? TEXTURE_NIGHT_GOLD_FEMALE : TEXTURE_NIGHT_GOLD_MALE;
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

        // Baby dragons have smaller shadows
        this.shadowRadius = entity.isBaby() ? 1.25F : 3.0f;

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
    public void renderRecursively(PoseStack poseStack, Raevyx animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        RiderConfig.RiderSpec riderConfig = RiderConfig.getSpec(animatable);
        if (riderConfig == null) {
            return;
        }
        if (!bone.getName().equals(riderConfig.boneName)) {
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
    public RenderType getRenderType(Raevyx animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    // --- Helpers ---
    // Passenger bone offsets (in pixels, divided by 16 to convert to blocks)
    // X = left/right, Y = up/down (negative pushes down), Z = forward/back (negative = forward)
    private static final float PASSENGER_X = 0.0f, PASSENGER_Y = -3.0f, PASSENGER_Z = 0.0f;

    private void enableTrackingForBones(BakedGeoModel model) {
        if (model == null) return;
        model.getBone("passengerBone").ifPresent(b -> b.setTrackingMatrices(true));
        model.getBone("beamBone").ifPresent(b -> b.setTrackingMatrices(true));
    }

    private void sampleAndStashLocatorsAccurate(Raevyx entity) {
        if (this.lastBakedModel == null || entity == null) return;
        // Sample passenger bone position for rider placement
        this.lastBakedModel.getBone("passengerBone").ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, PASSENGER_X, PASSENGER_Y, PASSENGER_Z);
            if (world != null) {
                entity.setClientLocatorPosition("passengerLocator", world);
                entity.setClientLocatorPosition("passengerSeat0", world);
            }
        });
        // Sample beam bone position for accurate beam origin (follows head/neck animations perfectly)
        this.lastBakedModel.getBone("beamBone").ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, 0f, 0f, 0f); // Use bone pivot directly
            if (world != null) entity.setClientLocatorPosition("beamBoneOrigin", world);
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
