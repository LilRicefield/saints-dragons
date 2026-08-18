package com.leon.saintsdragons.client.renderer.layer.raevyx;

import com.leon.saintsdragons.client.renderer.vfx.RaevyxBeamLightningRenderer;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import net.minecraft.util.Mth;
import java.util.Map;
import java.util.WeakHashMap;

public class RaevyxLightningBeamLayer extends GeoRenderLayer<Raevyx> {
    private static final float BEAM_SHAKE_INTENSITY = 0.01F;

    private static final class BeamState {
        float visibility;
        float lastRenderTime = Float.NaN;
        Vec3 lastMouth;
        Vec3 lastEnd;
        Vec3 smoothedEnd;
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
        float ageInTicks = animatable.tickCount + partialTick;
        float elapsedTicks = elapsedTicks(state, ageInTicks);

        if (beaming) {
            state.visibility = Mth.clamp(state.visibility + elapsedTicks / APPEAR_TICKS, 0.0F, 1.0F);
        } else {
            state.visibility = Mth.clamp(state.visibility - elapsedTicks / DISAPPEAR_TICKS, 0.0F, 1.0F);
        }

        Vec3 mouthWorld;
        Vec3 end;

        if (beaming) {
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

            float smoothFactor = timeAdjustedSmoothing(isRiding ? 0.48F : 0.72F, elapsedTicks);
            state.smoothedEnd = lerpVec(state.smoothedEnd, targetEnd, smoothFactor);
            end = state.smoothedEnd;

            state.lastMouth = mouthWorld;
            state.lastEnd = end;
        } else {
            if (state.lastMouth == null || state.lastEnd == null || state.visibility <= 0.001F) {
                state.lastMouth = null;
                state.lastEnd = null;
                state.smoothedEnd = null;
                return;
            }
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
        float visScale = beaming ? easeOutCubic(state.visibility) : state.visibility;
        visScale = Mth.clamp(visScale, 0f, 1f);
        float renderLength = beaming
                ? Math.max(0.001F, length * visScale)
                : length;
        boolean isNightGold = animatable.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD;
        RaevyxBeamLightningRenderer.render(animatable, poseStack, bufferSource,
                renderLength, visScale, ageInTicks, isNightGold, !beaming);
        poseStack.popPose();
    }

    private static float elapsedTicks(BeamState state, float renderTime) {
        if (Float.isNaN(state.lastRenderTime) || renderTime < state.lastRenderTime) {
            state.lastRenderTime = renderTime;
            return 0.0F;
        }

        float elapsed = Mth.clamp(renderTime - state.lastRenderTime, 0.0F, 20.0F);
        state.lastRenderTime = renderTime;
        return elapsed;
    }

    private static float timeAdjustedSmoothing(float smoothingPerTick, float elapsedTicks) {
        if (elapsedTicks <= 0.0F) {
            return 0.0F;
        }
        return 1.0F - (float) Math.pow(1.0F - smoothingPerTick, elapsedTicks);
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
