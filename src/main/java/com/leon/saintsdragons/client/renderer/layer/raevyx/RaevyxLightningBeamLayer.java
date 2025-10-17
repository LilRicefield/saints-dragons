package com.leon.saintsdragons.client.renderer.layer.raevyx;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import static com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxConstantsHandler.*;
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

/**
 * - Draws a multi-layered cylindrical beam with inner core and outer glow
 * Server handles damage; this is visual-only.
 */
public class RaevyxLightningBeamLayer extends GeoRenderLayer<Raevyx> {
    // Core textures: inner (crisp) + outer (soft)
    private static final ResourceLocation INNER_TEX = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/effects/lightning_beam_inner.png");
    private static final ResourceLocation OUTER_TEX = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/effects/lightning_beam_outer.png");
    // Beam tuning constants - adjust these to change beam appearance
    private static final float BASE_BEAM_WIDTH = 0.30F;        // Base width of the beam
    private static final float OUTER_BEAM_BONUS = 0.15F;      // Extra width for outer glow layer
    private static final float INNER_SPEED_MULTIPLIER = 0.25F; // Animation speed for inner beam
    private static final float OUTER_SPEED_MULTIPLIER = 0.25F; // Animation speed for outer beam
    private static final float BEAM_SHAKE_INTENSITY = 0.01F; // Intensity of beam shake effect
    // No end-caps; beam is a tubular mesh only

    // Per-entity visual state for appear/disappear easing
    private static final class BeamState {
        float appear;      // 0 -> 1 while appearing
        float disappear;   // 0 -> 1 while fading out
        net.minecraft.world.phys.Vec3 lastMouth;
        net.minecraft.world.phys.Vec3 lastEnd;
    }
    private static final Map<Raevyx, BeamState> STATES = new WeakHashMap<>();
    private static final float APPEAR_TICKS = 5f;      // ~0.25s
    private static final float DISAPPEAR_TICKS = 6f;   // ~0.3s

    // No beam origin offsets; lock to mouth to avoid drift

    // Local-space fine alignment (applied AFTER rotations so it follows the head)
    // Positive X = beam's right, Positive Y = up, Positive Z = forward along the beam
    private static final float LOCAL_OFFSET_LEFT = 1.5F;  // nudge left a touch
    private static final float LOCAL_OFFSET_UP    =  1.5F;
    private static final float LOCAL_OFFSET_FWD   =  4.0F;  // small forward bias if z-fighting occurs (e.g., 0.01F)

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

            // Use math-based mouth origin to ensure stable following
            mouthWorld = animatable.computeHeadMouthOrigin(partialTick);

            // Predict visual beam end and clamp to neck capability
            net.minecraft.world.phys.Vec3 predictedEnd = predictBeamEnd(animatable, mouthWorld, partialTick);
            // Server-synced end (authoritative for damage)
            net.minecraft.world.phys.Vec3 serverEnd = animatable.getClientBeamEndPosition(partialTick);
            if (serverEnd == null) {
                end = predictedEnd;
            } else {
                double hspeed = animatable.getDeltaMovement().horizontalDistance();
                float turnRate = Math.abs(net.minecraft.util.Mth.degreesDifference(animatable.yHeadRotO, animatable.yHeadRot));
                float weight = net.minecraft.util.Mth.clamp((float) (hspeed * 3.0 + (turnRate / 90.0f)), 0.0f, 1.0f);
                end = lerpVec(serverEnd, predictedEnd, weight);
            }
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
        float scale = MODEL_SCALE;
        
        // Calculate beam direction and prepare transformation
        net.minecraft.world.phys.Vec3 rawBeamPosition = end.subtract(mouthWorld);
        // PoseStack here operates in model space; translation below divides by MODEL_SCALE.
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
        // Apply local-space alignment so offset stays glued to the head orientation
        poseStack.translate(LOCAL_OFFSET_LEFT, LOCAL_OFFSET_UP, LOCAL_OFFSET_FWD);
        
        // Apply appear/disappear scaling
        float visScale = beaming ? easeOutCubic(state.appear) : (1f - state.disappear);
        visScale = Mth.clamp(visScale, 0f, 1f);
        float scaledLength = Math.max(0.001f, length * visScale);
        float scaledWidth = Math.max(0.001f, BASE_BEAM_WIDTH * (0.75f + 0.25f * visScale));

        // Render inner beam
        renderBeam(animatable, poseStack, bufferSource, partialTick, scaledWidth, scaledLength, true);
        // Render outer beam
        renderBeam(animatable, poseStack, bufferSource, partialTick, scaledWidth, scaledLength, false);

        poseStack.popPose();

        // No particle VFX; keep visuals minimal (inner/outer beam only)
    }

    private void renderBeam(Raevyx entity, PoseStack poseStack, MultiBufferSource source, float partialTicks, float width, float length, boolean inner) {
        poseStack.pushPose();
        int vertices;
        VertexConsumer vertexconsumer;
        float speed;
        float startAlpha = 1.0F;
        float endAlpha = 1.0F;
        if (inner) {
            vertices = 4;
            vertexconsumer = source.getBuffer(RenderType.entityTranslucent(INNER_TEX));
            speed = INNER_SPEED_MULTIPLIER;
        } else {
            vertices = 8;
            vertexconsumer = source.getBuffer(RenderType.entityTranslucent(OUTER_TEX));
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
        // Choose aim direction: rider look -> target center -> head look
        net.minecraft.world.phys.Vec3 aimDir;
        net.minecraft.world.entity.Entity cp = dragon.getControllingPassenger();
        if (cp instanceof net.minecraft.world.entity.LivingEntity rider) {
            aimDir = rider.getViewVector(partialTicks).normalize();
        } else {
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

        // Clamp desired aim to what the neck can reasonably achieve
        aimDir = clampAimToNeck(dragon, aimDir, partialTicks);

        final double MAX_DISTANCE = 32.0; // blocks
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

    // Use the same effective limits as the neck-aim system to avoid visuals exceeding neck capability
    private static net.minecraft.world.phys.Vec3 clampAimToNeck(Raevyx dragon, net.minecraft.world.phys.Vec3 desiredDir, float pt) {
        if (desiredDir.lengthSqr() < 1.0e-6) return desiredDir;
        desiredDir = desiredDir.normalize();

        // Convert desired dir to MC yaw/pitch (pitch positive = down)
        float desiredYawDeg = (float)(Math.atan2(-desiredDir.x, desiredDir.z) * (180.0 / Math.PI));
        float desiredPitchDeg = (float)(-Math.atan2(desiredDir.y, Math.sqrt(desiredDir.x * desiredDir.x + desiredDir.z * desiredDir.z)) * (180.0 / Math.PI));

        float headYaw = net.minecraft.util.Mth.lerp(pt, dragon.yHeadRotO, dragon.yHeadRot);
        float headPitch = net.minecraft.util.Mth.lerp(pt, dragon.xRotO, dragon.getXRot());

        float yawErrDeg = net.minecraft.util.Mth.degreesDifference(headYaw, desiredYawDeg);
        float pitchErrDeg = desiredPitchDeg - headPitch;

        // Effective totals from neck weights/clamps in model
        float TOTAL_MAX_YAW_DEG = (float)Math.toDegrees(0.70f * (0.18f + 0.22f + 0.26f + 0.30f));   // ~38.5°
        float TOTAL_MAX_PITCH_DEG = (float)Math.toDegrees(0.90f * (0.18f + 0.22f + 0.26f + 0.30f)); // ~49.5°

        float clampedYawErr = net.minecraft.util.Mth.clamp(yawErrDeg, -TOTAL_MAX_YAW_DEG, TOTAL_MAX_YAW_DEG);
        float clampedPitchErr = net.minecraft.util.Mth.clamp(pitchErrDeg, -TOTAL_MAX_PITCH_DEG, TOTAL_MAX_PITCH_DEG);

        float finalYaw = headYaw + clampedYawErr;
        float finalPitch = headPitch + clampedPitchErr;

        // Reconstruct clamped direction
        return net.minecraft.world.phys.Vec3.directionFromRotation(finalPitch, finalYaw).normalize();
    }
}
