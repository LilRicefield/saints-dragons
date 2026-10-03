package com.leon.saintsdragons.client.renderer;

import com.leon.saintsdragons.client.renderer.vfx.DragonDiveTrailRenderer;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3d;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;
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
        RiderConfig.RiderSpec riderSpec = RiderConfig.getSpec(entity);
        if (riderSpec != null) {
            riderSpec.seats().forEach((index, seat) -> {
                var bone = model.getBone(seat.boneName());
                if (bone.isPresent()) {
                    bone.get().setTrackingMatrices(true);
                } else if (!isReRender && RenderPassContext.isExtractionAllowed(entity.getId())) {
                    if (seat.locatorName() != null) {
                        entity.clearClientLocatorPosition(seat.locatorName());
                    }
                }
            });
        }

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
        boolean extractWorldRenderData = !EntityPreviewRenderContext.isRendering()
                && !ShaderPassCompatibility.isIrisShadowPass();
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
        RiderConfig.RiderSpec riderSpec = RiderConfig.getSpec(entity);
        if (riderSpec != null) {
            for (RiderConfig.SeatSpec seat : riderSpec.seats().values()) {
                if (seat.locatorName() != null) {
                    Vector3f offset = seat.locatorOffset();
                    trackBoneToLocators(entity, seat.boneName(), offset.x(), offset.y(), offset.z(),
                            seat.locatorName());
                }
            }
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

    /** Evaluate only animation and attachment ancestors; no geometry, layers, or render events. */
    public void prepareRiderAttachments(T entity, float partialTick) {
        RiderConfig.RiderSpec spec = RiderConfig.getSpec(entity);
        if (spec == null) {
            return;
        }
        GeoModel<T> geoModel = getGeoModel();
        ResourceLocation resource = geoModel.getModelResource(entity, this);
        if (!GeckoLibCache.getBakedModels().containsKey(resource)) {
            return;
        }
        BakedGeoModel baked = geoModel.getBakedModel(resource);
        boolean sitting = entity.isPassenger();
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        if (sitting && entity.getVehicle() instanceof LivingEntity vehicle) {
            bodyYaw = Mth.rotLerp(partialTick, vehicle.yBodyRotO, vehicle.yBodyRot);
            float relativeYaw = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -85, 85);
            bodyYaw = headYaw - relativeYaw;
            if (relativeYaw * relativeYaw > 2500) {
                bodyYaw += relativeYaw * 0.2F;
            }
        }
        float limbAmount = !sitting && entity.isAlive() ? Math.min(entity.walkAnimation.speed(partialTick), 1) : 0;
        float limbSwing = !sitting && entity.isAlive() ? entity.walkAnimation.position(partialTick) : 0;
        if (entity.isBaby()) {
            limbSwing *= 3;
        }
        Vec3 velocity = entity.getDeltaMovement();
        float speed = (float) ((Math.abs(velocity.x) + Math.abs(velocity.z)) / 2);
        AnimationState<T> state = new AnimationState<>(entity, limbSwing, limbAmount, partialTick,
                speed >= getMotionAnimThreshold(entity) && limbAmount != 0);
        long instanceId = getInstanceId(entity);
        state.setData(DataTickets.TICK, entity.getTick(entity));
        state.setData(DataTickets.ENTITY, entity);
        state.setData(DataTickets.ENTITY_MODEL_DATA, new EntityModelData(sitting, entity.isBaby(),
                bodyYaw - headYaw, -Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        T previousAnimatable = this.animatable;
        this.animatable = entity;
        try {
            geoModel.addAdditionalStateData(entity, instanceId, state::setData);
            geoModel.handleAnimations(entity, instanceId, state);
            PoseStack root = new PoseStack();
            Vec3 renderOffset = getRenderOffset(entity, partialTick);
            root.translate(renderOffset.x, renderOffset.y, renderOffset.z);
            float scale = getRenderScale(entity);
            root.scale(scale, scale, scale);
            scaleModelForRender(scaleWidth, scaleHeight, root, entity, baked, false, partialTick, 0, 0);
            if (entity.getPose() == Pose.SLEEPING && entity.getBedOrientation() != null) {
                var direction = entity.getBedOrientation();
                float eyeOffset = entity.getEyeHeight(Pose.STANDING) - 0.1F;
                root.translate(-direction.getStepX() * eyeOffset, 0, -direction.getStepZ() * eyeOffset);
            }
            applyRotations(entity, root, entity.tickCount + partialTick, bodyYaw, partialTick);
            root.translate(0, 0.01F, 0);
            spec.seats().forEach((seatIndex, seat) -> baked.getBone(seat.boneName()).ifPresent(bone -> {
                root.pushPose();
                try {
                    applyAttachmentAncestors(root, bone);
                    DragonRiderAttachments.storeSeat(entity, seatIndex, partialTick, root.last().pose());
                } finally {
                    root.popPose();
                }
            }));
        } finally {
            this.animatable = previousAnimatable;
            geoModel.getAnimationProcessor().getRegisteredBones().forEach(CoreGeoBone::resetStateChanges);
        }
    }

    private static void applyAttachmentAncestors(PoseStack poses, GeoBone bone) {
        if (bone.getParent() != null) {
            applyAttachmentAncestors(poses, bone.getParent());
            RenderUtils.translateAwayFromPivotPoint(poses, bone.getParent());
        }
        RenderUtils.translateMatrixToBone(poses, bone);
        RenderUtils.translateToPivotPoint(poses, bone);
        RenderUtils.rotateMatrixAroundBone(poses, bone);
        RenderUtils.scaleMatrixForBone(poses, bone);
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
