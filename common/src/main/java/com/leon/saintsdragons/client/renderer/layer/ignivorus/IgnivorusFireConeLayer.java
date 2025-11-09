package com.leon.saintsdragons.client.renderer.layer.ignivorus;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Geometry-based fire cone renderer for Ignivorus.
 * Renders an expanding cone of fire using vertex buffers (unlimited render distance).
 * Inspired by RaevyxLightningBeamLayer but adapted for cone shape with fire texture.
 */
public class IgnivorusFireConeLayer extends GeoRenderLayer<Ignivorus> {

    // Fire texture - uses custom texture you wired in
    private static final ResourceLocation FIRE_TEXTURE =
        SaintsDragonsCommon.rl("textures/particle/ignivorus/fire_breath_flame.png");

    // Cone geometry constants
    private static final double MAX_VISUAL_DISTANCE = 128.0D;
    private static final float MODEL_SCALE = 1.0F; // Adjust if Ignivorus uses different model scale
    private static final float CONE_START_WIDTH = 0.5F;  // Width at dragon's mouth
    private static final float CONE_END_WIDTH = 1.5F;    // Width at maximum distance (expanding cone)
    private static final float ANIMATION_SPEED = -0.15F;  // UV scroll speed for flame animation (negative = reverse)
    private static final int CONE_SEGMENTS = 8;          // Number of sides (8 = octagon)

    // Per-entity visual state for appear/disappear easing
    private static final class ConeState {
        float appear;      // 0 -> 1 while appearing
        float disappear;   // 0 -> 1 while fading out
        Vec3 lastStart;
        Vec3 lastEnd;
    }
    private static final Map<Ignivorus, ConeState> STATES = new WeakHashMap<>();
    private static final float APPEAR_TICKS = 5f;      // ~0.25s
    private static final float DISAPPEAR_TICKS = 6f;   // ~0.3s

    public IgnivorusFireConeLayer() {
        super(null);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, Ignivorus animatable, BakedGeoModel bakedModel,
                       @NotNull RenderType renderType, @NotNull MultiBufferSource bufferSource,
                       @NotNull VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        ConeState state = STATES.computeIfAbsent(animatable, k -> new ConeState());
        boolean breathing = animatable.isBreathingFire();

        Vec3 startWorld;
        Vec3 endWorld;

        if (breathing) {
            // Ramp in
            state.disappear = 0f;
            state.appear = Mth.clamp(state.appear + (1f / APPEAR_TICKS), 0f, 1f);

            // Get fire start position from bone or fallback
            startWorld = animatable.getFireBreathStartAnchor(partialTick);
            if (startWorld == null) {
                return;
            }

            // Calculate end position using dragon's aim direction
            Vec3 aimDir = Vec3.directionFromRotation(animatable.getXRot(), animatable.yHeadRot);
            Vec3 visualEnd = startWorld.add(aimDir.scale(MAX_VISUAL_DISTANCE));

            // Use server-synced end if available for accuracy
            Vec3 serverEnd = animatable.getFireBreathTarget();
            endWorld = (serverEnd != null) ? serverEnd : visualEnd;

            // Clamp to MAX_VISUAL_DISTANCE
            Vec3 delta = endWorld.subtract(startWorld);
            if (delta.length() > MAX_VISUAL_DISTANCE) {
                endWorld = startWorld.add(delta.normalize().scale(MAX_VISUAL_DISTANCE));
            }

            state.lastStart = startWorld;
            state.lastEnd = endWorld;
        } else {
            // Ramp out using last known positions
            if (state.lastStart == null || state.lastEnd == null ||
                (state.appear <= 0f && state.disappear >= 1f)) {
                return;
            }
            state.disappear = Mth.clamp(state.disappear + (1f / DISAPPEAR_TICKS), 0f, 1f);
            state.appear = 0f;
            startWorld = state.lastStart;
            endWorld = state.lastEnd;
        }

        // Transform into model space relative to entity origin
        double ox = Mth.lerp(partialTick, animatable.xo, animatable.getX());
        double oy = Mth.lerp(partialTick, animatable.yo, animatable.getY());
        double oz = Mth.lerp(partialTick, animatable.zo, animatable.getZ());

        // Calculate cone direction and length
        Vec3 rawConeVector = endWorld.subtract(startWorld);
        float baseLength = (float) (rawConeVector.length() / MODEL_SCALE);
        if (baseLength <= 0.001f) return;

        // Apply progress-based extension (Ice & Fire style)
        int progress = animatable.getFireBreathProgress();
        double progressRatio = Math.min(1.0, progress / 40.0);
        float length = (float) (baseLength * progressRatio);

        if (length <= 0.001f) return;

        // Calculate rotation to point cone in correct direction
        Vec3 direction = rawConeVector.normalize();
        float xRot = (float) Math.acos(direction.y);
        float yRot = (float) Math.atan2(direction.z, direction.x);

        // Transform to start position in model space
        float sx = (float) ((startWorld.x - ox) / MODEL_SCALE);
        float sy = (float) ((startWorld.y - oy) / MODEL_SCALE);
        float sz = (float) ((startWorld.z - oz) / MODEL_SCALE);

        poseStack.pushPose();
        poseStack.translate(sx, sy, sz);
        poseStack.mulPose(Axis.YP.rotationDegrees(((Mth.PI / 2F) - yRot) * Mth.RAD_TO_DEG));
        poseStack.mulPose(Axis.XP.rotationDegrees((-(Mth.PI / 2F) + xRot) * Mth.RAD_TO_DEG));

        // Apply appear/disappear scaling
        float visScale = breathing ? easeOutCubic(state.appear) : (1f - state.disappear);
        visScale = Mth.clamp(visScale, 0f, 1f);
        float scaledLength = Math.max(0.001f, length * visScale);

        // Render the cone
        renderCone(animatable, poseStack, bufferSource, partialTick, scaledLength, visScale);

        poseStack.popPose();
    }

    private void renderCone(Ignivorus entity, PoseStack poseStack, MultiBufferSource source,
                           float partialTicks, float length, float visScale) {
        poseStack.pushPose();

        VertexConsumer vertexConsumer = source.getBuffer(RenderType.entityTranslucent(FIRE_TEXTURE));

        // Animated UV offset for flowing fire effect
        float uvOffset = ((float) entity.tickCount + partialTicks) * ANIMATION_SPEED;

        // Calculate widths at start and end (cone expansion)
        float startWidth = CONE_START_WIDTH * visScale;
        float endWidth = CONE_END_WIDTH * visScale;

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();

        // Build cone mesh as triangle strip around circumference
        for (int i = 0; i <= CONE_SEGMENTS; ++i) {
            float angle = (float) i * ((float) Math.PI * 2F) / (float) CONE_SEGMENTS;
            float nextAngle = (float) (i + 1) * ((float) Math.PI * 2F) / (float) CONE_SEGMENTS;

            float x1 = Mth.cos(angle) * startWidth;
            float y1 = Mth.sin(angle) * startWidth;
            float x2 = Mth.cos(angle) * endWidth;
            float y2 = Mth.sin(angle) * endWidth;

            float u = (float) i / CONE_SEGMENTS;
            float uNext = (float) (i + 1) / CONE_SEGMENTS;

            // Quad: start vertex -> end vertex -> next end vertex -> next start vertex
            // Bottom (start of cone)
            vertexConsumer.vertex(matrix4f, x1, y1, 0.0F)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(u, uvOffset)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240) // Full brightness
                .normal(matrix3f, 0.0F, -1.0F, 0.0F)
                .endVertex();

            // Top (end of cone)
            vertexConsumer.vertex(matrix4f, x2, y2, length)
                .color(1.0F, 1.0F, 1.0F, 0.7F) // Slight fade at tip
                .uv(u, uvOffset + length * 0.2F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(matrix3f, 0.0F, -1.0F, 0.0F)
                .endVertex();

            // Next segment's top
            float x3 = Mth.cos(nextAngle) * endWidth;
            float y3 = Mth.sin(nextAngle) * endWidth;
            vertexConsumer.vertex(matrix4f, x3, y3, length)
                .color(1.0F, 1.0F, 1.0F, 0.7F)
                .uv(uNext, uvOffset + length * 0.2F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(matrix3f, 0.0F, -1.0F, 0.0F)
                .endVertex();

            // Next segment's bottom
            float x4 = Mth.cos(nextAngle) * startWidth;
            float y4 = Mth.sin(nextAngle) * startWidth;
            vertexConsumer.vertex(matrix4f, x4, y4, 0.0F)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(uNext, uvOffset)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(matrix3f, 0.0F, -1.0F, 0.0F)
                .endVertex();
        }

        poseStack.popPose();
    }

    private static float easeOutCubic(float t) {
        float p = 1f - t;
        return 1f - p * p * p;
    }
}
