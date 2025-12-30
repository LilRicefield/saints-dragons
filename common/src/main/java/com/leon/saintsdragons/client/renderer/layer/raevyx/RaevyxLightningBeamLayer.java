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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import net.minecraft.util.Mth;
import java.util.Map;
import java.util.WeakHashMap;
import org.joml.Vector3f;

/**
 * - Draws a multi-layered cylindrical beam with inner core and outer glow
 * Server handles damage; this is visual-only.
 */
public class RaevyxLightningBeamLayer extends GeoRenderLayer<Raevyx> {
    // Core textures: inner (crisp) + outer (soft)
    private static final ResourceLocation INNER_TEX = SaintsDragonsCommon.rl("textures/effects/raevyx/lightning_beam_inner.png");
    private static final ResourceLocation OUTER_TEX = SaintsDragonsCommon.rl("textures/effects/raevyx/lightning_beam_outer.png");
    private static final ResourceLocation FEMALE_INNER_TEX = SaintsDragonsCommon.rl("textures/effects/raevyx/female_lightning_beam_inner.png");
    private static final ResourceLocation FEMALE_OUTER_TEX = SaintsDragonsCommon.rl("textures/effects/raevyx/female_lightning_beam_outer.png");
    private static final ResourceLocation[] MALE_STORM_FRAMES = buildStormFrames("lightning_storm_", 8);
    // Female export only shipped a subset of frames, so build a looping sequence that reuses what exists.
    private static final ResourceLocation[] FEMALE_STORM_SEQUENCE = new ResourceLocation[]{
            particle("female_lightning_storm_0"),
            particle("female_lightning_storm_3"),
            particle("female_lightning_storm_5"),
            particle("female_lightning_storm_6"),
            particle("female_lightning_storm_7"),
            particle("female_lightning_storm_6"),
            particle("female_lightning_storm_5"),
            particle("female_lightning_storm_3")
    };
    // Beam tuning constants - adjust these to change beam appearance
    private static final float BASE_BEAM_WIDTH = 0.45F;        // Base width of the beam
    private static final float OUTER_BEAM_BONUS = 0.15F;      // Extra width for outer glow layer
    private static final float INNER_SPEED_MULTIPLIER = 0.25F; // Animation speed for inner beam
    private static final float OUTER_SPEED_MULTIPLIER = 0.25F; // Animation speed for outer beam
    private static final float BEAM_SHAKE_INTENSITY = 0.01F; // Intensity of beam shake effect
    private static final float LIGHTNING_SQUARE_PADDING = 0.1F;
    private static final float LIGHTNING_SEGMENT_LENGTH = 1.6F;
    private static final float LIGHTNING_SEGMENT_GAP = 0.45F;
    private static final float LIGHTNING_FRAME_INTERVAL_TICKS = 2.0F;

    // No end-caps; beam is a tubular mesh only

    // Per-entity visual state for appear/disappear easing
    private static final class BeamState {
        float appear;      // 0 -> 1 while appearing
        float disappear;   // 0 -> 1 while fading out
        net.minecraft.world.phys.Vec3 lastMouth;
        net.minecraft.world.phys.Vec3 lastEnd;
        net.minecraft.world.phys.Vec3 smoothedEnd; // Smoothly lerped end position for delayed following
    }
    private static final Map<Raevyx, BeamState> STATES = new WeakHashMap<>();
    private static final float APPEAR_TICKS = 5f;
    private static final float DISAPPEAR_TICKS = 10f;

    // No local offsets needed - beamBone position from model is already accurate

    public RaevyxLightningBeamLayer() { super(null); }

    @Override
    public void render(@NotNull PoseStack poseStack, Raevyx animatable, BakedGeoModel bakedModel,
                       @NotNull RenderType renderType, @NotNull MultiBufferSource bufferSource, @NotNull VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {

        BeamState state = STATES.computeIfAbsent(animatable, k -> new BeamState());
        boolean beaming = animatable.isBeaming();

        net.minecraft.world.phys.Vec3 mouthWorld;
        net.minecraft.world.phys.Vec3 end;

        if (beaming) {
            // ramp in
            state.disappear = 0f;
            state.appear = Mth.clamp(state.appear + (1f / APPEAR_TICKS), 0f, 1f);

            // Get mouth position from bone, but we need to interpolate the entity position
            net.minecraft.world.phys.Vec3 bonePos = getBoneWorldPositionInterpolated(bakedModel, "beamBone", animatable, partialTick);
            net.minecraft.world.phys.Vec3 computedPos = animatable.computeHeadMouthOrigin(partialTick);

            // Always prefer bone position when available - it's the visual source of truth
            mouthWorld = bonePos != null ? bonePos : computedPos;

            // Predict visual beam end and clamp to neck capability
            net.minecraft.world.phys.Vec3 predictedEnd = predictBeamEnd(animatable, mouthWorld, partialTick);
            // Server-synced end (authoritative for damage)
            net.minecraft.world.phys.Vec3 serverEnd = animatable.getClientBeamEndPosition(partialTick);

            // When riding, ALWAYS use predicted end (rider's camera is source of truth, no lag)
            // When not riding (AI), blend with server position based on movement
            boolean isRiding = animatable.getControllingPassenger() != null;

            net.minecraft.world.phys.Vec3 targetEnd;
            if (isRiding) {
                targetEnd = predictedEnd;
            } else if (serverEnd == null) {
                targetEnd = predictedEnd;
            } else {
                double hspeed = animatable.getDeltaMovement().horizontalDistance();
                float turnRate = Math.abs(net.minecraft.util.Mth.degreesDifference(animatable.yHeadRotO, animatable.yHeadRot));
                float weight = net.minecraft.util.Mth.clamp((float) (hspeed * 3.0 + (turnRate / 90.0f)), 0.0f, 1.0f);
                targetEnd = lerpVec(serverEnd, predictedEnd, weight);
            }

            // Apply smooth delayed following (beam catches up to target)
            if (state.smoothedEnd == null) {
                state.smoothedEnd = targetEnd;
            }

            // Lerp factor: higher = faster catch-up, lower = more delay
            // 0.3 = beam follows with noticeable but smooth delay
            float smoothFactor = 0.3f;
            state.smoothedEnd = lerpVec(state.smoothedEnd, targetEnd, smoothFactor);
            end = state.smoothedEnd;

            state.lastMouth = mouthWorld;
            state.lastEnd = end;
        } else {
            // ramp out using last known segment
            if (state.lastMouth == null || state.lastEnd == null || (state.appear <= 0f && state.disappear >= 1f)) {
                return;
            }
            state.disappear = Mth.clamp(state.disappear + (1f / DISAPPEAR_TICKS), 0f, 1f);
            state.appear = 0f;
            mouthWorld = state.lastMouth;
            end = state.lastEnd;
        }

        // Transform into model space relative to entity origin for poseStack use
        double ox = net.minecraft.util.Mth.lerp(partialTick, animatable.xo, animatable.getX());
        double oy = net.minecraft.util.Mth.lerp(partialTick, animatable.yo, animatable.getY());
        double oz = net.minecraft.util.Mth.lerp(partialTick, animatable.zo, animatable.getZ());
        float scale = Raevyx.MODEL_SCALE;

        // Calculate beam direction and prepare transformation
        net.minecraft.world.phys.Vec3 rawBeamPosition = end.subtract(mouthWorld);
        // PoseStack here operates in model space; translation below divides by Raevyx.MODEL_SCALE.
        // Do the same for beam length so visuals match server/world distance.
        float length = (float) (rawBeamPosition.length() / scale);
        if (length <= 0.001f) return;
        
        net.minecraft.world.phys.Vec3 vec3 = rawBeamPosition.normalize();
        float xRot = (float) Math.acos(vec3.y);
        float yRot = (float) Math.atan2(vec3.z, vec3.x);
        // configurable beam width

        // Small shake effect for visual flair
        float ageInTicks = animatable.tickCount + partialTick;
        float shakeByX = (float) Math.sin(ageInTicks * 4F) * BEAM_SHAKE_INTENSITY;
        float shakeByY = (float) Math.sin(ageInTicks * 4F + 1.2F) * BEAM_SHAKE_INTENSITY;
        float shakeByZ = (float) Math.sin(ageInTicks * 4F + 2.4F) * BEAM_SHAKE_INTENSITY;

        // Transform to mouth position in model space
        float mx = (float) ((mouthWorld.x - ox) / scale);
        float my = (float) ((mouthWorld.y - oy) / scale);
        float mz = (float) ((mouthWorld.z - oz) / scale);

        poseStack.pushPose();
        poseStack.translate(mx + shakeByX, my + shakeByY, mz + shakeByZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(((Mth.PI / 2F) - yRot) * Mth.RAD_TO_DEG));
        poseStack.mulPose(Axis.XP.rotationDegrees((-(Mth.PI / 2F) + xRot) * Mth.RAD_TO_DEG));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45));

        // Apply appear/disappear scaling
        float visScale = beaming ? easeOutCubic(state.appear) : (1f - state.disappear);
        visScale = Mth.clamp(visScale, 0f, 1f);
        float scaledLength = Math.max(0.001f, length * visScale);
        float scaledWidth = Math.max(0.001f, BASE_BEAM_WIDTH * (0.75f + 0.25f * visScale));

        // Render inner beam
        ResourceLocation innerTex = animatable.isFemale() ? FEMALE_INNER_TEX : INNER_TEX;
        ResourceLocation outerTex = animatable.isFemale() ? FEMALE_OUTER_TEX : OUTER_TEX;
        renderBeam(animatable, poseStack, bufferSource, partialTick, scaledWidth, scaledLength, true, innerTex);
        // Render outer beam
        renderBeam(animatable, poseStack, bufferSource, partialTick, scaledWidth, scaledLength, false, outerTex);
        renderLightningStormLayer(animatable, poseStack, bufferSource, partialTick, scaledWidth, scaledLength, visScale);

        poseStack.popPose();

        // No particle VFX; keep visuals minimal (inner/outer beam only)
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
            width += OUTER_BEAM_BONUS; // configurable outer beam bonus width
            speed = OUTER_SPEED_MULTIPLIER;
            endAlpha = 0.0F;
        }

        float v = ((float) entity.tickCount + partialTicks) * -0.25F * speed;
        float v1 = v + length * (inner ? 0.5F : 0.15F);
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
        // Beam mesh done
        poseStack.popPose();
    }

    // No end-cap rendering

    private static float easeOutCubic(float t) {
        float p = 1f - t;
        return 1f - p * p * p;
    }

    private static net.minecraft.world.phys.Vec3 lerpVec(net.minecraft.world.phys.Vec3 a, net.minecraft.world.phys.Vec3 b, float t) {
        t = net.minecraft.util.Mth.clamp(t, 0.0f, 1.0f);
        return a.add(b.subtract(a).scale(t));
    }

    private static net.minecraft.world.phys.Vec3 predictBeamEnd(Raevyx dragon, net.minecraft.world.phys.Vec3 mouthWorld, float partialTicks) {
        net.minecraft.world.phys.Vec3 aimDir;

        // When riding, ALWAYS use rider's view vector directly (zero lag)
        // When AI-controlled, use server-calculated beam aim direction
        net.minecraft.world.entity.Entity cp = dragon.getControllingPassenger();
        if (cp instanceof net.minecraft.world.entity.LivingEntity rider) {
            aimDir = rider.getViewVector(partialTicks).normalize();
        } else {
            // AI-controlled: prefer the dragon's smoothed beam aim direction
            aimDir = dragon.getBeamAimDirection();
            if (aimDir == null || aimDir.lengthSqr() < 1.0e-6) {
                dragon.refreshBeamAimDirection(mouthWorld, true);
                aimDir = dragon.getBeamAimDirection();
            }

            if (aimDir == null || aimDir.lengthSqr() < 1.0e-6) {
                net.minecraft.world.entity.LivingEntity tgt = dragon.getTarget();
                if (tgt != null && tgt.isAlive()) {
                    net.minecraft.world.phys.Vec3 aimPoint = tgt.getEyePosition(partialTicks).add(0, -0.25, 0);
                    aimDir = aimPoint.subtract(mouthWorld).normalize();
                } else {
                    float yaw = net.minecraft.util.Mth.lerp(partialTicks, dragon.yHeadRotO, dragon.yHeadRot);
                    float pitch = net.minecraft.util.Mth.lerp(partialTicks, dragon.xRotO, dragon.getXRot());
                    aimDir = net.minecraft.world.phys.Vec3.directionFromRotation(pitch, yaw).normalize();
                }
            }
        }

        final double MAX_DISTANCE = 64; // blocks
        net.minecraft.world.phys.Vec3 tentativeEnd = mouthWorld.add(aimDir.scale(MAX_DISTANCE));
        var hit = dragon.level().clip(new net.minecraft.world.level.ClipContext(
                mouthWorld,
                tentativeEnd,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                dragon
        ));
        return hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS ? hit.getLocation() : tentativeEnd;
    }

    private void renderLightningStormLayer(Raevyx entity, PoseStack poseStack, MultiBufferSource source,
                                           float partialTicks, float baseWidth, float length, float visibility) {
        if (visibility <= 0.0F) {
            return;
        }
        float width = baseWidth + LIGHTNING_SQUARE_PADDING;
        float totalLength = Math.max(0.001F, length);
        float segmentLen = Math.max(0.25F, Math.min(LIGHTNING_SEGMENT_LENGTH, totalLength));
        float cursor = 0.0F;
        int segmentIndex = 0;
        while (cursor < totalLength) {
            float remaining = totalLength - cursor;
            float currentLen = Math.min(segmentLen, remaining);
            float alphaPulse = 0.65F + 0.35F * Mth.sin((entity.tickCount + partialTicks + segmentIndex * 3.5F) * 0.45F);
            float alpha = Mth.clamp(visibility * alphaPulse, 0.05F, 1.0F);
            ResourceLocation texture = getStormFrameTexture(entity, partialTicks + segmentIndex * 0.25F);
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, cursor);
            if ((segmentIndex & 1) == 1) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }
            renderLightningSegment(texture, poseStack, source, width, currentLen, alpha);
            poseStack.popPose();
            cursor += currentLen + LIGHTNING_SEGMENT_GAP;
            segmentIndex++;
        }
    }

    private void renderLightningSegment(ResourceLocation texture, PoseStack poseStack, MultiBufferSource source,
                                        float width, float length, float alpha) {
        VertexConsumer consumer = source.getBuffer(RenderType.entityTranslucent(texture));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        renderLightningPlane(consumer, matrix4f, normalMatrix, width, 0.0F, length, alpha, true, true);
        renderLightningPlane(consumer, matrix4f, normalMatrix, -width, 0.0F, length, alpha, true, false);
        renderLightningPlane(consumer, matrix4f, normalMatrix, width, 0.0F, length, alpha, false, true);
        renderLightningPlane(consumer, matrix4f, normalMatrix, -width, 0.0F, length, alpha, false, false);
    }

    private static ResourceLocation getStormFrameTexture(Raevyx entity, float partialTicks) {
        float ticks = (entity.tickCount + partialTicks) / LIGHTNING_FRAME_INTERVAL_TICKS;
        if (entity.isFemale()) {
            int idx = (int) (ticks % FEMALE_STORM_SEQUENCE.length);
            return FEMALE_STORM_SEQUENCE[idx];
        }
        int idx = (int) (ticks % MALE_STORM_FRAMES.length);
        return MALE_STORM_FRAMES[idx];
    }

    private void renderLightningPlane(VertexConsumer consumer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                                      float fixedCoord, float zStart, float zEnd, float alpha, boolean axisX, boolean positive) {
        float half = Math.max(0.001F, Math.abs(fixedCoord));
        float u0 = 0.0F;
        float u1 = 1.0F;
        float v0 = 0.0F;
        float v1 = 1.0F;

        Vector3f normalVec;
        if (axisX) {
            normalVec = new Vector3f(positive ? 1.0F : -1.0F, 0.0F, 0.0F);
            normalMatrix.transform(normalVec);
            float x = fixedCoord;
            float yMin = -half;
            float yMax = half;
            consumer.vertex(poseMatrix, x, yMin, zStart).color(1.0F, 1.0F, 1.0F, alpha).uv(u0, v0)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(normalVec.x(), normalVec.y(), normalVec.z()).endVertex();
            consumer.vertex(poseMatrix, x, yMin, zEnd).color(1.0F, 1.0F, 1.0F, alpha).uv(u0, v1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(normalVec.x(), normalVec.y(), normalVec.z()).endVertex();
            consumer.vertex(poseMatrix, x, yMax, zEnd).color(1.0F, 1.0F, 1.0F, alpha).uv(u1, v1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(normalVec.x(), normalVec.y(), normalVec.z()).endVertex();
            consumer.vertex(poseMatrix, x, yMax, zStart).color(1.0F, 1.0F, 1.0F, alpha).uv(u1, v0)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(normalVec.x(), normalVec.y(), normalVec.z()).endVertex();
        } else {
            normalVec = new Vector3f(0.0F, positive ? 1.0F : -1.0F, 0.0F);
            normalMatrix.transform(normalVec);
            float y = fixedCoord;
            float xMin = -half;
            float xMax = half;
            consumer.vertex(poseMatrix, xMin, y, zStart).color(1.0F, 1.0F, 1.0F, alpha).uv(u0, v0)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(normalVec.x(), normalVec.y(), normalVec.z()).endVertex();
            consumer.vertex(poseMatrix, xMin, y, zEnd).color(1.0F, 1.0F, 1.0F, alpha).uv(u0, v1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(normalVec.x(), normalVec.y(), normalVec.z()).endVertex();
            consumer.vertex(poseMatrix, xMax, y, zEnd).color(1.0F, 1.0F, 1.0F, alpha).uv(u1, v1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(normalVec.x(), normalVec.y(), normalVec.z()).endVertex();
            consumer.vertex(poseMatrix, xMax, y, zStart).color(1.0F, 1.0F, 1.0F, alpha).uv(u1, v0)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240).normal(normalVec.x(), normalVec.y(), normalVec.z()).endVertex();
        }
    }

    private static ResourceLocation[] buildStormFrames(String prefix, int count) {
        ResourceLocation[] frames = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            frames[i] = particle(prefix + i);
        }
        return frames;
    }

    private static ResourceLocation particle(String path) {
        return SaintsDragonsCommon.rl("textures/particle/raevyx/" + path + ".png");
    }

    /**
     * Gets the world-space position of a bone with interpolated entity position.
     * The bone matrix from GeckoLib uses non-interpolated entity position, so we need to correct it.
     */
    private static net.minecraft.world.phys.Vec3 getBoneWorldPositionInterpolated(BakedGeoModel model, String boneName,
                                                                                   Raevyx entity, float partialTick) {
        if (model == null || boneName == null || entity == null) return null;

        var boneOpt = model.getBone(boneName);
        if (boneOpt.isEmpty()) return null;

        var bone = boneOpt.get();

        // Get the bone's world-space matrix (includes entity position, but NOT interpolated)
        org.joml.Matrix4f worldMat = new org.joml.Matrix4f(bone.getWorldSpaceMatrix());

        // Transform the bone's pivot point to get its world position
        org.joml.Vector4f pivotWorld = new org.joml.Vector4f(0f, 0f, 0f, 1f);
        worldMat.transform(pivotWorld);

        // The bone position includes the entity's NON-interpolated position
        // We need to subtract the non-interpolated entity pos and add the interpolated one
        double entityX = entity.getX();
        double entityY = entity.getY();
        double entityZ = entity.getZ();

        double entityOldX = entity.xo;
        double entityOldY = entity.yo;
        double entityOldZ = entity.zo;

        double interpX = net.minecraft.util.Mth.lerp(partialTick, entityOldX, entityX);
        double interpY = net.minecraft.util.Mth.lerp(partialTick, entityOldY, entityY);
        double interpZ = net.minecraft.util.Mth.lerp(partialTick, entityOldZ, entityZ);

        // Calculate the offset: (bone world pos) - (entity non-interp pos) + (entity interp pos)
        double correctedX = pivotWorld.x() - entityX + interpX;
        double correctedY = pivotWorld.y() - entityY + interpY;
        double correctedZ = pivotWorld.z() - entityZ + interpZ;

        return new net.minecraft.world.phys.Vec3(correctedX, correctedY, correctedZ);
    }
}
