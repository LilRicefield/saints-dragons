package com.leon.saintsdragons.client.renderer.layer.raevyx;

import com.leon.saintsdragons.client.renderer.vfx.RaevyxBeamLightningRenderer;
import com.leon.saintsdragons.client.renderer.vfx.AttachedWindRenderer;
import com.leon.saintsdragons.client.renderer.vfx.AttachedBillboardFlipbookRenderer;
import com.leon.saintsdragons.client.renderer.vfx.BillboardFlashRenderer;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraft.resources.ResourceLocation;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
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
    private static final double FIRST_PERSON_START_OFFSET = 1.0D;
    private static final double ORIGIN_STAR_FORWARD_OFFSET = 2.0D;
    private static final long ORIGIN_STAR_SEED_SALT = 0x3C6EF372FE94F82BL;
    private static final ResourceLocation ORIGIN_STAR_TEXTURE = SaintsDragonsCommon.rl("textures/particle/star.png");
    private static final BillboardFlashRenderer.Style ORIGIN_STAR_STYLE =
            new BillboardFlashRenderer.Style(5.0F, 4.0F, 1.0F, 0.85F, 0.3F);
    private static final long MOUTH_SWIRL_SEED_SALT = 0x510E527FADE682D1L;
    private static final ResourceLocation[] MOUTH_SWIRL_TEXTURES = new ResourceLocation[16];
    private static final double MOUTH_SWIRL_FORWARD_OFFSET = 1.5D;
    private static final float MOUTH_SWIRL_ANCHOR_U = 30.5F / 32.0F;
    private static final float MOUTH_SWIRL_ANCHOR_V = 30.5F / 32.0F;
    private static final AttachedBillboardFlipbookRenderer.Style MOUTH_SWIRL_STYLE =
            new AttachedBillboardFlipbookRenderer.Style(1.5F, 1.0F, 0.5F, 3.0F, -1000.0F, 1000.0F,
                    0.0F, 0.0F, true, MOUTH_SWIRL_ANCHOR_U, MOUTH_SWIRL_ANCHOR_V);

    static {
        for (int frame = 0; frame < MOUTH_SWIRL_TEXTURES.length; frame++) {
            MOUTH_SWIRL_TEXTURES[frame] = SaintsDragonsCommon.rl("textures/particle/swirl" + frame + ".png");
        }
    }
    private static final ResourceLocation[] WIND_TEXTURES = {
            SaintsDragonsCommon.rl("textures/particle/wind.png"),
            SaintsDragonsCommon.rl("textures/particle/wind2.png")
    };

    private static final class BeamState {
        final AttachedWindRenderer.State wind = new AttachedWindRenderer.State();
        final AttachedBillboardFlipbookRenderer.State mouthSwirl = new AttachedBillboardFlipbookRenderer.State();
        float visibility;
        float lastRenderTime = Float.NaN;
        Vec3 lastMouth;
        Vec3 lastEnd;
        Vec3 smoothedEnd;
        Vec3 liveMouth;
        Matrix4f starBeamPose;
        float starBeamLength;
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
        // Never reuse geometry from an earlier frame when this render exits early.
        state.starBeamPose = null;
        boolean beaming = animatable.isBeaming();
        float ageInTicks = animatable.tickCount + partialTick;
        float elapsedTicks = elapsedTicks(state, ageInTicks);
        state.mouthSwirl.update(ageInTicks, beaming,
                animatable.getUUID().getMostSignificantBits() ^ animatable.getUUID().getLeastSignificantBits()
                        ^ MOUTH_SWIRL_SEED_SALT,
                MOUTH_SWIRL_TEXTURES.length, MOUTH_SWIRL_STYLE);
        state.wind.update(ageInTicks, beaming,
                animatable.getUUID().getMostSignificantBits() ^ animatable.getUUID().getLeastSignificantBits(),
                WIND_TEXTURES.length);

        if (beaming) {
            state.visibility = Mth.clamp(state.visibility + elapsedTicks / APPEAR_TICKS, 0.0F, 1.0F);
        } else {
            state.visibility = Mth.clamp(state.visibility - elapsedTicks / DISAPPEAR_TICKS, 0.0F, 1.0F);
        }

        Vec3 mouthWorld;
        Vec3 end;
        Vec3 liveMouth = null;
        if (beaming || state.wind.isActive() || state.mouthSwirl.isActive() || state.visibility > 0.001F) {
            liveMouth = getBoneWorldPositionInterpolated(bakedModel, "beamBone", animatable, partialTick);
            if (liveMouth == null) {
                liveMouth = animatable.computeBeamStartFallback(partialTick);
            }
        }

        double ox = Mth.lerp(partialTick, animatable.xo, animatable.getX());
        double oy = Mth.lerp(partialTick, animatable.yo, animatable.getY());
        double oz = Mth.lerp(partialTick, animatable.zo, animatable.getZ());
        float scale = Raevyx.MODEL_SCALE;
        float visScale = Mth.clamp(beaming ? easeOutCubic(state.visibility) : state.visibility, 0f, 1f);
        // Draw the flash later, after GeckoLib restores the unrotated entity pose.
        state.liveMouth = liveMouth;

        if (beaming) {
            mouthWorld = liveMouth;
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
            if (state.lastMouth == null || state.lastEnd == null
                    || (state.visibility <= 0.001F && !state.wind.isActive())) {
                state.lastMouth = null;
                state.lastEnd = null;
                state.smoothedEnd = null;
                return;
            }
            mouthWorld = state.lastMouth;
            end = state.lastEnd;
        }

        Vec3 rawBeamPosition = end.subtract(mouthWorld);
        float length = (float) (rawBeamPosition.length() / scale);
        if (length <= 0.001f) return;
        Vec3 vec3 = rawBeamPosition.normalize();
        float xRot = (float) Math.acos(vec3.y);
        float yRot = (float) Math.atan2(vec3.z, vec3.x);
        if (state.wind.isActive() && liveMouth != null) {
            poseStack.pushPose();
            poseStack.translate((liveMouth.x - ox) / scale, (liveMouth.y - oy) / scale, (liveMouth.z - oz) / scale);
            poseStack.mulPose(Axis.YP.rotationDegrees(((Mth.PI / 2F) - yRot) * Mth.RAD_TO_DEG));
            poseStack.mulPose(Axis.XP.rotationDegrees((-(Mth.PI / 2F) + xRot) * Mth.RAD_TO_DEG));
            AttachedWindRenderer.render(poseStack, bufferSource, WIND_TEXTURES, state.wind, ageInTicks);
            poseStack.popPose();
        }
        float shakeByX = (float) Math.sin(ageInTicks * 4F) * BEAM_SHAKE_INTENSITY;
        float shakeByY = (float) Math.sin(ageInTicks * 4F + 1.2F) * BEAM_SHAKE_INTENSITY;
        float shakeByZ = (float) Math.sin(ageInTicks * 4F + 2.4F) * BEAM_SHAKE_INTENSITY;
        float mx = (float) ((mouthWorld.x - ox) / scale);
        float my = (float) ((mouthWorld.y - oy) / scale);
        float mz = (float) ((mouthWorld.z - oz) / scale);
        float renderLength = beaming
                ? Math.max(0.001F, length * visScale)
                : length;

        Minecraft minecraft = Minecraft.getInstance();
        boolean localRiderFirstPerson = minecraft.player != null
                && animatable.getControllingPassenger() == minecraft.player
                && minecraft.options.getCameraType() == CameraType.FIRST_PERSON;
        double renderedWorldLength = renderLength * scale;
        double startOffsetWorld = localRiderFirstPerson
                ? Math.min(FIRST_PERSON_START_OFFSET,
                Math.max(0.0D, renderedWorldLength - 0.05D))
                : 0.0D;
        float startOffsetLocal = (float) (startOffsetWorld / scale);
        float visualRenderLength = Math.max(0.001F, renderLength - startOffsetLocal);
        Vec3 visualStartWorld = mouthWorld.add(vec3.scale(startOffsetWorld));
        Vec3 renderedEndWorld = mouthWorld.add(vec3.scale(renderedWorldLength));

        poseStack.pushPose();
        poseStack.translate(mx + shakeByX, my + shakeByY, mz + shakeByZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(((Mth.PI / 2F) - yRot) * Mth.RAD_TO_DEG));
        poseStack.mulPose(Axis.XP.rotationDegrees((-(Mth.PI / 2F) + xRot) * Mth.RAD_TO_DEG));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45));
        poseStack.translate(0.0F, 0.0F, startOffsetLocal);
        // Retain the exact beam placement for moving star centers, not their orientation.
        state.starBeamPose = new Matrix4f(poseStack.last().pose());
        state.starBeamLength = visualRenderLength;
        RaevyxBeamLightningRenderer.render(animatable, poseStack, bufferSource,
                visualRenderLength, visScale, ageInTicks,
                visualStartWorld, renderedEndWorld, localRiderFirstPerson);
        poseStack.popPose();
    }

    public static void renderFlashes(Raevyx entity, PoseStack entityPose,
                                        MultiBufferSource buffers, float partialTick) {
        BeamState state = STATES.get(entity);
        float time = entity.tickCount + partialTick;
        if (state == null || state.liveMouth == null || state.lastRenderTime != time) {
            return;
        }
        float visibility = entity.isBeaming() ? easeOutCubic(state.visibility) : state.visibility;
        if (state.starBeamPose != null) {
            // Cancel the outer pose only to recover entity-relative particle positions.
            // The billboard itself receives the clean pose and camera quaternion.
            Matrix4f beamToEntity = new Matrix4f(entityPose.last().pose()).invert().mul(state.starBeamPose);
            RaevyxBeamLightningRenderer.renderStars(entity, entityPose, buffers, beamToEntity,
                    state.starBeamLength, visibility, time);
        }
        double x = Mth.lerp(partialTick, entity.xo, entity.getX());
        double y = Mth.lerp(partialTick, entity.yo, entity.getY());
        double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
        Vec3 swirlWorld = state.liveMouth;
        if (state.lastMouth != null && state.lastEnd != null) {
            Vec3 beamDirection = state.lastEnd.subtract(state.lastMouth).normalize();
            swirlWorld = swirlWorld.add(beamDirection.scale(MOUTH_SWIRL_FORWARD_OFFSET));
        }
        // Anchored to beamBone, independent of the star offset, beam shake and camera mode.
        // Existing pulses finish their sprite sequence after emission stops.
        boolean gold = entity.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD;
        AttachedBillboardFlipbookRenderer.render(entityPose, buffers, MOUTH_SWIRL_TEXTURES,
                state.mouthSwirl, MOUTH_SWIRL_STYLE, time,
                (float) (swirlWorld.x - x), (float) (swirlWorld.y - y), (float) (swirlWorld.z - z),
                1.0F, gold ? 0.75F : 0.0F, gold ? 0.15F : 0.0F);
        long seed = entity.getUUID().getMostSignificantBits()
                ^ entity.getUUID().getLeastSignificantBits() ^ ORIGIN_STAR_SEED_SALT;
        Vec3 flashWorld = state.liveMouth;
        if (state.lastMouth != null && state.lastEnd != null) {
            Vec3 beamDirection = state.lastEnd.subtract(state.lastMouth).normalize();
            flashWorld = flashWorld.add(beamDirection.scale(ORIGIN_STAR_FORWARD_OFFSET));
        }
        BillboardFlashRenderer.render(entityPose, buffers, ORIGIN_STAR_TEXTURE,
                (float) (flashWorld.x - x), (float) (flashWorld.y - y),
                (float) (flashWorld.z - z), visibility, time, seed, ORIGIN_STAR_STYLE,
                1.0F, gold ? 0.75F : 0.0F, gold ? 0.15F : 0.0F);
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
