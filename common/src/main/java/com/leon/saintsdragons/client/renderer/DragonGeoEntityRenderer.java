package com.leon.saintsdragons.client.renderer;

import com.leon.saintsdragons.client.renderer.vfx.DragonDiveTrailRenderer;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.base.RideableGroundDragon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3d;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

public abstract class DragonGeoEntityRenderer<T extends RideableDragonBase> extends GeoEntityRenderer<T> {
    private static final double DIVE_TRAIL_RENDER_DISTANCE = 256.0D;
    private static final double DIVE_TRAIL_CULL_PADDING = 48.0D;
    protected BakedGeoModel lastBakedModel;
    private boolean renderedModelThisPass;

    protected DragonGeoEntityRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    @Override
    public float getMotionAnimThreshold(T animatable) {
        return 0.000001f;
    }

    @Override
    protected float getDeathMaxRotation(T entity) {
        return 0.0F;
    }

    @Override
    public boolean shouldRender(@NotNull T entity, @NotNull Frustum frustum, double camX, double camY, double camZ) {
        if (super.shouldRender(entity, frustum, camX, camY, camZ)) {
            return true;
        }

        double dx = entity.getX() - camX;
        double dy = entity.getY() - camY;
        double dz = entity.getZ() - camZ;
        double maxDistance = DIVE_TRAIL_RENDER_DISTANCE * DIVE_TRAIL_RENDER_DISTANCE;
        if (dx * dx + dy * dy + dz * dz > maxDistance) {
            return false;
        }
        if (DragonDiveTrailRenderer.getTrailIntensity(entity) <= 0.0F) {
            return false;
        }

        AABB trailBounds = entity.getBoundingBox().inflate(DIVE_TRAIL_CULL_PADDING);
        return frustum.isVisible(trailBounds);
    }

    @Override
    public void preRender(PoseStack poseStack,
                          T entity,
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
        enableTrackingForBones(model);

        float scale = getRenderScale(entity);
        poseStack.scale(scale, scale, scale);
        this.shadowRadius = entity.isBaby() ? getBabyShadowRadius(entity) : getAdultShadowRadius(entity);

        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void render(@NotNull T entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        this.lastBakedModel = null;
        this.renderedModelThisPass = false;
        boolean extractWorldRenderData = !EntityPreviewRenderContext.isRendering();
        try {
            if (extractWorldRenderData) {
                RenderPassContext.beginExtraction(entity.getId());
            }
            try {
                super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            } finally {
                if (extractWorldRenderData) {
                    RenderPassContext.endExtraction();
                }
                getGeoModel().getAnimationProcessor().getRegisteredBones()
                        .forEach(CoreGeoBone::resetStateChanges);
            }

            if (extractWorldRenderData && this.renderedModelThisPass) {
                sampleLocators(entity);
                afterDragonRender(entity, poseStack, bufferSource, partialTick);
            }
        } finally {
            this.lastBakedModel = null;
            this.renderedModelThisPass = false;
        }
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        if (isReRender) {
            return;
        }

        this.renderedModelThisPass = true;

        // GeckoLib restores the pose stack before returning from its recursive call. Reapply the
        // same transform stage it uses for tracked bone matrices so the rider origin remains at
        // the actual .geo.json pivot, including animation-authored position, rotation, and scale.
        // Deliberately do not translate away from the pivot: that would move the origin back into
        // parent space before rendering this bone's cubes and children.
        poseStack.pushPose();
        try {
            RenderUtils.translateMatrixToBone(poseStack, bone);
            RenderUtils.translateToPivotPoint(poseStack, bone);
            RenderUtils.rotateMatrixAroundBone(poseStack, bone);
            RenderUtils.scaleMatrixForBone(poseStack, bone);
            captureRiderCameraIfNeeded(poseStack, animatable, bone);
        } finally {
            poseStack.popPose();
        }
    }

    protected float getRenderScale(T entity) {
        return 1.0f;
    }

    protected abstract float getBabyShadowRadius(T entity);

    protected abstract float getAdultShadowRadius(T entity);

    protected String[] trackedBoneNames() {
        return new String[0];
    }

    protected LocatorSpec[] locatorSpecs(T entity) {
        return new LocatorSpec[0];
    }

    protected void afterDragonRender(T entity, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick) {
    }

    protected Vec3 getBoneWorldPosition(String boneName) {
        if (this.lastBakedModel == null) {
            return null;
        }
        return this.lastBakedModel.getBone(boneName)
                .map(bone -> {
                    Vector3d position = bone.getWorldPosition();
                    return new Vec3(position.x(), position.y(), position.z());
                })
                .orElse(null);
    }

    protected void enableTrackingForBones(BakedGeoModel model) {
        if (model == null) {
            return;
        }
        for (String boneName : trackedBoneNames()) {
            model.getBone(boneName).ifPresent(bone -> bone.setTrackingMatrices(true));
        }
    }

    protected void sampleLocators(T entity) {
        if (this.lastBakedModel == null || entity == null) {
            return;
        }
        for (LocatorSpec spec : locatorSpecs(entity)) {
            trackBoneToLocators(entity, spec.boneName(), spec.x(), spec.y(), spec.z(), spec.locatorNames());
        }
    }

    protected void trackBoneToLocators(T entity, String boneName, float x, float y, float z, String... locatorNames) {
        if (this.lastBakedModel == null || entity == null) {
            return;
        }
        this.lastBakedModel.getBone(boneName).ifPresent(bone -> {
            Vec3 world = transformLocator(bone, x, y, z);
            if (world == null) {
                return;
            }
            for (String locatorName : locatorNames) {
                entity.setClientLocatorPosition(locatorName, world);
            }
        });
    }

    protected void captureRiderCameraIfNeeded(PoseStack poseStack, T animatable, GeoBone bone) {
        RiderConfig.RiderSpec riderSpec = RiderConfig.getSpec(animatable);
        if (riderSpec == null) {
            return;
        }

        int seatIndex = seatIndexForRiderBone(animatable, bone.getName(), riderSpec);
        if (seatIndex < 0 || !RenderPassContext.isExtractionAllowed(animatable.getId())) {
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
        if (!RiderBullcrap.tryLockForFrame(animatable, seatIndex)) {
            return;
        }

        Vector3d boneWorldPosJoml = bone.getWorldPosition();
        Vec3 cameraWorldPos = new Vec3(boneWorldPosJoml.x, boneWorldPosJoml.y, boneWorldPosJoml.z);
        if (!usesGroundedRawFirstPersonBoneAnchor(animatable)) {
            Vector3f firstPersonOffset = RiderConfig.getFirstPersonOffset(animatable, seatIndex);
            Vec3 offsetWorldPos = transformLocator(
                    bone,
                    firstPersonOffset.x(),
                    firstPersonOffset.y(),
                    firstPersonOffset.z()
            );
            if (offsetWorldPos != null) {
                cameraWorldPos = offsetWorldPos;
            }
        }
        RiderBullcrap.store(
                animatable,
                seatIndex,
                viewMatrix,
                cameraWorldPos.subtract(animatable.position())
        );
    }

    private boolean usesGroundedRawFirstPersonBoneAnchor(T animatable) {
        if (animatable instanceof RideableFlyingDragon) {
            return !animatable.isFlying()
                    && !animatable.isTakeoff()
                    && !animatable.isLanding()
                    && !animatable.isHovering();
        }
        return animatable instanceof RideableGroundDragon;
    }

    protected int seatIndexForRiderBone(T animatable, String boneName, RiderConfig.RiderSpec riderSpec) {
        return boneName.equals(riderSpec.boneName) ? 0 : -1;
    }

    protected static Vec3 transformLocator(GeoBone bone, float px, float py, float pz) {
        if (bone == null || bone.getWorldSpaceMatrix() == null) {
            return null;
        }

        float lx = px / 16f;
        float ly = py / 16f;
        float lz = pz / 16f;
        Matrix4f worldMat = new Matrix4f(bone.getWorldSpaceMatrix());
        Vector4f in = new Vector4f(lx, ly, lz, 1f);
        Vector4f out = worldMat.transform(in);
        return new Vec3(out.x(), out.y(), out.z());
    }

    public record LocatorSpec(String boneName, float x, float y, float z, String... locatorNames) {
    }
}
