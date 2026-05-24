package com.leon.saintsdragons.client.renderer.layer.raevyx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import net.minecraft.util.Mth;
import java.util.Map;
import java.util.WeakHashMap;

public class RaevyxLightningBeamLayer extends GeoRenderLayer<Raevyx> {
    private static final ResourceLocation INNER_TEX = SaintsDragonsCommon.rl("textures/entity/raevyx/lightning_beam_inner.png");
    private static final ResourceLocation OUTER_TEX = SaintsDragonsCommon.rl("textures/entity/raevyx/lightning_beam_outer.png");
    private static final ResourceLocation GOLDEN_INNER_TEX = SaintsDragonsCommon.rl("textures/entity/raevyx/golden_lightning_beam_inner.png");
    private static final ResourceLocation GOLDEN_OUTER_TEX = SaintsDragonsCommon.rl("textures/entity/raevyx/golden_lightning_beam_outer.png");
    private static final float BASE_BEAM_WIDTH = 0.45F;
    private static final float OUTER_BEAM_BONUS = 0.15F;
    private static final float INNER_SPEED_MULTIPLIER = 0.25F;
    private static final float OUTER_SPEED_MULTIPLIER = 0.25F;
    private static final float BEAM_SHAKE_INTENSITY = 0.01F;

    private static final class BeamState {
        float appear;
        float disappear;
        net.minecraft.world.phys.Vec3 lastMouth;
        net.minecraft.world.phys.Vec3 lastEnd;
        net.minecraft.world.phys.Vec3 smoothedEnd;
    }
    private static final Map<Raevyx, BeamState> STATES = new WeakHashMap<>();
    private static final float APPEAR_TICKS = 5f;
    private static final float DISAPPEAR_TICKS = 10f;

    public RaevyxLightningBeamLayer() { super(null); }

    @Override
    public void render(@NotNull PoseStack poseStack, Raevyx animatable, BakedGeoModel bakedModel,
                       @NotNull RenderType renderType, @NotNull MultiBufferSource bufferSource, @NotNull VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {

        BeamState state = STATES.computeIfAbsent(animatable, k -> new BeamState());
        boolean beaming = animatable.isBeaming();

        Vec3 mouthWorld;
        Vec3 end;

        if (beaming) {
            state.disappear = 0f;
            state.appear = Mth.clamp(state.appear + (1f / APPEAR_TICKS), 0f, 1f);
            Vec3 bonePos = getBoneWorldPositionInterpolated(bakedModel, "beamBone", animatable, partialTick);
            Vec3 computedPos = animatable.computeBeamStartFallback(partialTick);
            mouthWorld = bonePos != null ? bonePos : computedPos;
            Vec3 predictedEnd = predictBeamEnd(animatable, mouthWorld, partialTick);
            Vec3 serverEnd = animatable.getClientBeamEndPosition(partialTick);
            boolean isRiding = animatable.getControllingPassenger() != null;

            Vec3 targetEnd;
            if (isRiding) {
                targetEnd = predictedEnd;
            } else {
                targetEnd = serverEnd != null ? serverEnd : predictedEnd;
            }

            if (state.smoothedEnd == null) {
                state.smoothedEnd = targetEnd;
            }

            float smoothFactor = isRiding ? 0.3f : 0.65f;
            state.smoothedEnd = lerpVec(state.smoothedEnd, targetEnd, smoothFactor);
            end = state.smoothedEnd;

            state.lastMouth = mouthWorld;
            state.lastEnd = end;
        } else {
            if (state.lastMouth == null || state.lastEnd == null || (state.appear <= 0f && state.disappear >= 1f)) {
                return;
            }
            state.disappear = Mth.clamp(state.disappear + (1f / DISAPPEAR_TICKS), 0f, 1f);
            state.appear = 0f;
            mouthWorld = state.lastMouth;
            end = state.lastEnd;
        }

        double ox = Mth.lerp(partialTick, animatable.xo, animatable.getX());
        double oy = Mth.lerp(partialTick, animatable.yo, animatable.getY());
        double oz = Mth.lerp(partialTick, animatable.zo, animatable.getZ());
        float scale = Raevyx.MODEL_SCALE;
        Vec3 rawBeamPosition = end.subtract(mouthWorld);
        float length = (float) (rawBeamPosition.length() / scale);
        if (length <= 0.001f) return;
        Vec3 vec3 = rawBeamPosition.normalize();
        float xRot = (float) Math.acos(vec3.y);
        float yRot = (float) Math.atan2(vec3.z, vec3.x);
        float ageInTicks = animatable.tickCount + partialTick;
        float shakeByX = (float) Math.sin(ageInTicks * 4F) * BEAM_SHAKE_INTENSITY;
        float shakeByY = (float) Math.sin(ageInTicks * 4F + 1.2F) * BEAM_SHAKE_INTENSITY;
        float shakeByZ = (float) Math.sin(ageInTicks * 4F + 2.4F) * BEAM_SHAKE_INTENSITY;
        float mx = (float) ((mouthWorld.x - ox) / scale);
        float my = (float) ((mouthWorld.y - oy) / scale);
        float mz = (float) ((mouthWorld.z - oz) / scale);
        poseStack.pushPose();
        poseStack.translate(mx + shakeByX, my + shakeByY, mz + shakeByZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(((Mth.PI / 2F) - yRot) * Mth.RAD_TO_DEG));
        poseStack.mulPose(Axis.XP.rotationDegrees((-(Mth.PI / 2F) + xRot) * Mth.RAD_TO_DEG));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45));
        float visScale = beaming ? easeOutCubic(state.appear) : (1f - state.disappear);
        visScale = Mth.clamp(visScale, 0f, 1f);
        float scaledLength = Math.max(0.001f, length * visScale);
        float scaledWidth = Math.max(0.001f, BASE_BEAM_WIDTH * (0.75f + 0.25f * visScale));
        boolean isNightGold = animatable.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD;
        ResourceLocation innerTex = isNightGold ? GOLDEN_INNER_TEX : INNER_TEX;
        ResourceLocation outerTex = isNightGold ? GOLDEN_OUTER_TEX : OUTER_TEX;
        renderBeam(animatable, poseStack, bufferSource, partialTick, scaledWidth, scaledLength, true, innerTex);
        renderBeam(animatable, poseStack, bufferSource, partialTick, scaledWidth, scaledLength, false, outerTex);
        poseStack.popPose();
    }

    private void renderBeam(Raevyx entity, PoseStack poseStack, MultiBufferSource source, float partialTicks, float width, float length, boolean inner, ResourceLocation texture) {
        poseStack.pushPose();
        int vertices;
        VertexConsumer vertexconsumer;
        float speed;
        float startAlpha = 1.0F;
        float endAlpha = 1.0F;
        if (inner) {
            vertices = 4;
            vertexconsumer = source.getBuffer(RenderType.entityTranslucent(texture));
            speed = INNER_SPEED_MULTIPLIER;
        } else {
            vertices = 8;
            vertexconsumer = source.getBuffer(RenderType.entityTranslucent(texture));
            width += OUTER_BEAM_BONUS;
            speed = OUTER_SPEED_MULTIPLIER;
            endAlpha = 0.0F;
        }

        float v = ((float) entity.tickCount + partialTicks) * -0.25F * speed;
        float v1 = v + length * (0.5F);
        float f4 = -width;
        float f5 = 0;
        float f6 = 0.0F;
        PoseStack.Pose posestack$pose = poseStack.last();
        Matrix4f matrix4f = posestack$pose.pose();
        
        for (int j = 0; j <= vertices; ++j) {
            Matrix3f matrix3f = posestack$pose.normal();
            float f7 = Mth.cos((float) Math.PI + (float) j * ((float) Math.PI * 2F) / (float) vertices) * width;
            float f8 = Mth.sin((float) Math.PI + (float) j * ((float) Math.PI * 2F) / (float) vertices) * width;
            float f9 = (float) j + 1;
            vertexconsumer.vertex(matrix4f, f4 * 0.55F, f5 * 0.55F, 0.0F).color(1.0F, 1.0F, 1.0F, startAlpha).uv(f6, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
            vertexconsumer.vertex(matrix4f, f4, f5, length).color(1.0F, 1.0F, 1.0F, endAlpha).uv(f6, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(matrix3f, 0.0F, -1F, 0.0F).endVertex();
            vertexconsumer.vertex(matrix4f, f7, f8, length).color(1.0F, 1.0F, 1.0F, endAlpha).uv(f9, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(matrix3f, 0.0F, -1F, 0.0F).endVertex();
            vertexconsumer.vertex(matrix4f, f7 * 0.55F, f8 * 0.55F, 0.0F).color(1.0F, 1.0F, 1.0F, startAlpha).uv(f9, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
            f4 = f7;
            f5 = f8;
            f6 = f9;
        }
        poseStack.popPose();
    }


    private static float easeOutCubic(float t) {
        float p = 1f - t;
        return 1f - p * p * p;
    }

    private static Vec3 lerpVec(Vec3 a, Vec3 b, float t) {
        t = net.minecraft.util.Mth.clamp(t, 0.0f, 1.0f);
        return a.add(b.subtract(a).scale(t));
    }

    private static Vec3 predictBeamEnd(Raevyx dragon, Vec3 mouthWorld, float partialTicks) {
        Vec3 aimDir;
        Entity cp = dragon.getControllingPassenger();
        if (cp instanceof LivingEntity rider) {
            aimDir = rider.getViewVector(partialTicks).normalize();
        } else {
            aimDir = dragon.getBeamAimDirection();
            if (aimDir == null || aimDir.lengthSqr() < 1.0e-6) {
                dragon.refreshBeamAimDirection(mouthWorld, true);
                aimDir = dragon.getBeamAimDirection();
            }

            if (aimDir == null || aimDir.lengthSqr() < 1.0e-6) {
               LivingEntity tgt = dragon.getTarget();
                if (tgt != null && tgt.isAlive()) {
                    Vec3 aimPoint = tgt.getEyePosition(partialTicks).add(0, -0.25, 0);
                    aimDir = aimPoint.subtract(mouthWorld).normalize();
                } else {
                    float yaw = Mth.lerp(partialTicks, dragon.yHeadRotO, dragon.yHeadRot);
                    float pitch = Mth.lerp(partialTicks, dragon.xRotO, dragon.getXRot());
                    aimDir = Vec3.directionFromRotation(pitch, yaw).normalize();
                }
            }
        }

        final double MAX_DISTANCE = 64;
       Vec3 tentativeEnd = mouthWorld.add(aimDir.scale(MAX_DISTANCE));
        var hit = dragon.level().clip(new ClipContext(
                mouthWorld,
                tentativeEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                dragon
        ));
        return hit.getType() != HitResult.Type.MISS ? hit.getLocation() : tentativeEnd;
    }


    private static Vec3 getBoneWorldPositionInterpolated(BakedGeoModel model, String boneName, Raevyx entity, float partialTick) {
        if (model == null || boneName == null || entity == null) return null;
        var boneOpt = model.getBone(boneName);
        if (boneOpt.isEmpty()) return null;
        var bone = boneOpt.get();
        Matrix4f worldMat = new Matrix4f(bone.getWorldSpaceMatrix());

        Vector4f pivotWorld = new Vector4f(0f, 0f, 0f, 1f);
        worldMat.transform(pivotWorld);

        double entityX = entity.getX();
        double entityY = entity.getY();
        double entityZ = entity.getZ();

        double entityOldX = entity.xo;
        double entityOldY = entity.yo;
        double entityOldZ = entity.zo;

        double interpX = Mth.lerp(partialTick, entityOldX, entityX);
        double interpY = Mth.lerp(partialTick, entityOldY, entityY);
        double interpZ = Mth.lerp(partialTick, entityOldZ, entityZ);

        double correctedX = pivotWorld.x() - entityX + interpX;
        double correctedY = pivotWorld.y() - entityY + interpY;
        double correctedZ = pivotWorld.z() - entityZ + interpZ;

        return new Vec3(correctedX, correctedY, correctedZ);
    }
}