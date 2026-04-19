package com.leon.saintsdragons.client.renderer.nulljaw;

import com.leon.saintsdragons.client.model.nulljaw.NulljawModel;
import com.leon.saintsdragons.client.renderer.RenderPassContext;
import com.leon.saintsdragons.client.renderer.RiderBullcrap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

@Environment(EnvType.CLIENT)
public final class NulljawRenderer extends GeoEntityRenderer<Nulljaw> {
    private BakedGeoModel lastBakedModel;
    private static final String PASSENGER_BONE = "passengerBone";
    private static final float PASSENGER_X = -0.5f;
    private static final float PASSENGER_Y = 1.0f;
    private static final float PASSENGER_Z = 0.0f;

    public NulljawRenderer(EntityRendererProvider.Context context) {
        super(context, new NulljawModel());
        this.shadowRadius = 1.3F;
    }

    @Override
    public float getMotionAnimThreshold(Nulljaw animatable) {
        return 0.000001f;
    }

    @Override
    protected float getDeathMaxRotation(Nulljaw entity) {
        return 0.0F;
    }

    @Override
    public void preRender(PoseStack poseStack,
                          Nulljaw entity,
                          BakedGeoModel model,
                          MultiBufferSource bufferSource,
                          VertexConsumer buffer,
                          boolean isReRender,
                          float partialTick,
                          int packedLight,
                          int packedOverlay,
                          float red,
                          float green,
                          float blue,
                          float alpha) {
        this.lastBakedModel = model;
        if (model != null) {
            model.getBone(PASSENGER_BONE).ifPresent(b -> b.setTrackingMatrices(true));
        }

        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void render(@NotNull Nulljaw entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        RenderPassContext.beginExtraction(entity.getId());
        RiderBullcrap.notifyRendered(entity.getId());
        try {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } finally {
            RenderPassContext.endExtraction();
        }
        sampleAndStashLocators(entity);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, Nulljaw animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        if (!bone.getName().equals(PASSENGER_BONE)) {
            return;
        }

        if (RenderPassContext.isExtractionAllowed(animatable.getId())) {
            Matrix4f viewMatrix = new Matrix4f((Matrix4fc) poseStack.last().pose());
            Vector4f boneViewPos4 = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f).mul((Matrix4fc) viewMatrix);
            double viewSpaceDistance = Math.sqrt(
                    boneViewPos4.x() * boneViewPos4.x()
                            + boneViewPos4.y() * boneViewPos4.y()
                            + boneViewPos4.z() * boneViewPos4.z()
            );
            if (viewSpaceDistance < 80.0D && RiderBullcrap.tryLockForFrame(animatable.getId())) {
                RiderBullcrap.store(animatable.getId(), viewMatrix);
                Vector3d boneWorldPosJoml = bone.getWorldPosition();
                net.minecraft.world.phys.Vec3 boneWorldPos = new net.minecraft.world.phys.Vec3(
                        boneWorldPosJoml.x,
                        boneWorldPosJoml.y,
                        boneWorldPosJoml.z
                );
                RiderBullcrap.storeCameraOffset(animatable.getId(), boneWorldPos.subtract(animatable.position()));
            }
        }

        net.minecraft.world.phys.Vec3 world = transformLocator(bone, PASSENGER_X, PASSENGER_Y, PASSENGER_Z);
        if (world != null) {
            animatable.setClientLocatorPosition("passengerLocator", world);
            animatable.setClientLocatorPosition("passengerSeat0", world);
        }
    }

    private void sampleAndStashLocators(Nulljaw entity) {
        if (this.lastBakedModel == null || entity == null) {
            return;
        }
        this.lastBakedModel.getBone(PASSENGER_BONE).ifPresent(b -> {
            net.minecraft.world.phys.Vec3 world = transformLocator(b, PASSENGER_X, PASSENGER_Y, PASSENGER_Z);
            if (world != null) {
                entity.setClientLocatorPosition("passengerLocator", world);
                entity.setClientLocatorPosition("passengerSeat0", world);
            }
        });
    }

    private static net.minecraft.world.phys.Vec3 transformLocator(GeoBone bone, float px, float py, float pz) {
        if (bone == null || bone.getWorldSpaceMatrix() == null) {
            return null;
        }

        float lx = px / 16f;
        float ly = py / 16f;
        float lz = pz / 16f;
        org.joml.Matrix4f worldMat = new org.joml.Matrix4f(bone.getWorldSpaceMatrix());
        org.joml.Vector4f in = new org.joml.Vector4f(lx, ly, lz, 1f);
        org.joml.Vector4f out = worldMat.transform(in);
        return new net.minecraft.world.phys.Vec3(out.x(), out.y(), out.z());
    }
}
