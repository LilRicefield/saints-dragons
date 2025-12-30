package com.leon.saintsdragons.client.renderer.layer.ignivorus;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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

    // Fire textures - dual layer system (core + flames)
    private static final ResourceLocation FIRE_CORE_TEXTURE =
        SaintsDragonsCommon.rl("textures/particle/ignivorus/fire_breath_core.png");
    private static final ResourceLocation FIRE_FLAME_TEXTURE =
        SaintsDragonsCommon.rl("textures/particle/ignivorus/fire_breath_flame.png");

    // Cone geometry constants
    private static final double MAX_VISUAL_DISTANCE = 64.0D;
    private static final float MODEL_SCALE = 1.0F; // Adjust if Ignivorus uses different model scale
    private static final float CONE_START_WIDTH = 1.0F;  // Width at dragon's mouth
    private static final float CONE_END_WIDTH = 2.5F;    // Width at maximum distance (expanding cone)
    private static final float CORE_WIDTH_MULTIPLIER = 0.5F; // Core is 50% of outer width
    private static final float OUTER_GLOW_BONUS = 0.3F;  // Extra width for outer flame layer
    private static final float ANIMATION_SPEED = -0.45F;  // UV scroll speed for flame animation (negative = reverse)
    private static final float CORE_ANIMATION_SPEED = -0.6F; // Core scrolls faster for more energy
    private static final int CONE_SEGMENTS = 8;          // Number of sides (8 = octagon, smoother)

    // Per-entity visual state for appear/disappear easing
    private static final class ConeState {
        float appear;      // 0 -> 1 while appearing
        float disappear;   // 0 -> 1 while fading out
        Vec3 lastStart;
        Vec3 lastEnd;
        Vec3 smoothedEnd;  // Smoothly lerped end position for delayed following
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

            // Get fire start position with interpolated bone position
            startWorld = getFireStartInterpolated(animatable, bakedModel, partialTick);
            if (startWorld == null) {
                return;
            }

            // Calculate aim direction - use rider's view vector directly when riding for zero lag
            Vec3 aimDir;
            net.minecraft.world.entity.Entity rider = animatable.getControllingPassenger();
            if (rider instanceof net.minecraft.world.entity.LivingEntity livingRider) {
                aimDir = livingRider.getViewVector(partialTick).normalize();
            } else {
                // AI-controlled: use dragon's aim direction
                aimDir = Vec3.directionFromRotation(animatable.getXRot(), animatable.yHeadRot);
            }

            Vec3 visualEnd = startWorld.add(aimDir.scale(MAX_VISUAL_DISTANCE));

            // When riding, always use predicted end; when AI, blend with server
            Vec3 serverEnd = animatable.getFireBreathTarget();
            Vec3 targetEnd;
            if (rider != null) {
                targetEnd = visualEnd;
            } else {
                targetEnd = (serverEnd != null) ? serverEnd : visualEnd;
            }

            // Clamp to MAX_VISUAL_DISTANCE
            Vec3 delta = targetEnd.subtract(startWorld);
            if (delta.length() > MAX_VISUAL_DISTANCE) {
                targetEnd = startWorld.add(delta.normalize().scale(MAX_VISUAL_DISTANCE));
            }

            // Apply smooth delayed following (fire cone catches up to target)
            if (state.smoothedEnd == null) {
                state.smoothedEnd = targetEnd;
            }

            // Lerp factor: higher = faster catch-up, lower = more delay
            // 0.3 = fire cone follows with noticeable but smooth delay
            float smoothFactor = 0.3f;
            state.smoothedEnd = lerpVec(state.smoothedEnd, targetEnd, smoothFactor);
            endWorld = state.smoothedEnd;

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

        // Add slight pulsing effect for organic feel
        float ageInTicks = animatable.tickCount + partialTick;
        float pulse = 0.85F + Mth.sin(ageInTicks * 0.3F) * 0.15F;

        // Render dual-layer cone (inner core + outer flames)
        renderConeLayer(animatable, poseStack, bufferSource, partialTick, scaledLength, visScale, pulse, true);  // Inner core
        renderConeLayer(animatable, poseStack, bufferSource, partialTick, scaledLength, visScale, pulse, false); // Outer flames

        // Spawn flame particles along the cone path for extra visual flair
        if (breathing && animatable.level() instanceof ClientLevel clientLevel) {
            spawnFlameParticles(animatable, clientLevel, startWorld, endWorld, partialTick);
        }

        poseStack.popPose();
    }

    private void renderConeLayer(Ignivorus entity, PoseStack poseStack, MultiBufferSource source,
                                 float partialTicks, float length, float visScale, float pulse, boolean isCore) {
        poseStack.pushPose();

        // Select texture and properties based on layer type
        ResourceLocation texture = isCore ? FIRE_CORE_TEXTURE : FIRE_FLAME_TEXTURE;
        float animSpeed = isCore ? CORE_ANIMATION_SPEED : ANIMATION_SPEED;

        VertexConsumer vertexConsumer = source.getBuffer(RenderType.entityTranslucent(texture));

        // Animated UV offset for flowing fire effect
        float uvOffset = ((float) entity.tickCount + partialTicks) * animSpeed;

        // Calculate widths at start and end (cone expansion)
        float baseStartWidth = CONE_START_WIDTH * visScale;
        float baseEndWidth = CONE_END_WIDTH * visScale;

        float startWidth, endWidth;
        float startAlpha, endAlpha;
        float colorR, colorG, colorB;

        if (isCore) {
            // Inner core: narrower, bright white/yellow, full opacity
            startWidth = baseStartWidth * CORE_WIDTH_MULTIPLIER;
            endWidth = baseEndWidth * CORE_WIDTH_MULTIPLIER;
            startAlpha = 1.0F;
            endAlpha = 0.8F;
            // Bright white-yellow core
            colorR = 1.0F;
            colorG = 1.0F;
            colorB = 0.9F;
        } else {
            // Outer flames: wider with glow bonus, orange-red tint, pulsing alpha
            startWidth = baseStartWidth + OUTER_GLOW_BONUS;
            endWidth = baseEndWidth + OUTER_GLOW_BONUS;
            startAlpha = 0.9F * pulse;
            endAlpha = 0.5F * pulse;
            // Warm orange-red flames
            colorR = 1.0F;
            colorG = 0.85F;
            colorB = 0.6F;
        }

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
                .color(colorR, colorG, colorB, startAlpha)
                .uv(u, uvOffset)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240) // Full brightness
                .normal(matrix3f, 0.0F, -1.0F, 0.0F)
                .endVertex();

            // Top (end of cone)
            vertexConsumer.vertex(matrix4f, x2, y2, length)
                .color(colorR, colorG, colorB, endAlpha)
                .uv(u, uvOffset + length * 0.2F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(matrix3f, 0.0F, -1.0F, 0.0F)
                .endVertex();

            // Next segment's top
            float x3 = Mth.cos(nextAngle) * endWidth;
            float y3 = Mth.sin(nextAngle) * endWidth;
            vertexConsumer.vertex(matrix4f, x3, y3, length)
                .color(colorR, colorG, colorB, endAlpha)
                .uv(uNext, uvOffset + length * 0.2F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(matrix3f, 0.0F, -1.0F, 0.0F)
                .endVertex();

            // Next segment's bottom
            float x4 = Mth.cos(nextAngle) * startWidth;
            float y4 = Mth.sin(nextAngle) * startWidth;
            vertexConsumer.vertex(matrix4f, x4, y4, 0.0F)
                .color(colorR, colorG, colorB, startAlpha)
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

    private static Vec3 lerpVec(Vec3 a, Vec3 b, float t) {
        t = Mth.clamp(t, 0.0f, 1.0f);
        return a.add(b.subtract(a).scale(t));
    }

    /**
     * Spawns flame particles along the fire breath cone path.
     * Creates a visual trail of flames, embers, and smoke that follows the breath stream.
     */
    private void spawnFlameParticles(Ignivorus dragon, ClientLevel level, Vec3 start, Vec3 end, float partialTick) {
        // Only spawn particles every few ticks to avoid performance issues
        if (dragon.tickCount % 2 != 0) {
            return;
        }

        RandomSource random = dragon.getRandom();
        int progress = dragon.getFireBreathProgress();

        // Calculate current reach based on progress (0-40 → 0.0-1.0)
        double progressRatio = Math.min(1.0, progress / 40.0);
        Vec3 currentEnd = start.add(end.subtract(start).scale(progressRatio));

        // Calculate cone direction for radial particle spread
        Vec3 direction = currentEnd.subtract(start).normalize();

        // Number of particle spawn points along the breath (scaled by distance)
        double distance = start.distanceTo(currentEnd);
        int segments = Math.max(3, (int) (distance / 2.0)); // One segment every 2 blocks
        segments = Math.min(segments, 20); // Cap at 20 to avoid particle spam

        for (int i = 1; i < segments; i++) {
            double ratio = i / (double) segments;
            Vec3 position = start.add(currentEnd.subtract(start).scale(ratio));

            // Cone expands as it travels, so calculate width at this point
            float coneWidth = Mth.lerp((float) ratio, CONE_START_WIDTH, CONE_END_WIDTH);

            // Spawn multiple particles per position for volume
            int particlesPerSegment = 2 + random.nextInt(2); // 2-3 particles per segment

            for (int j = 0; j < particlesPerSegment; j++) {
                // Random offset within the cone's radius at this segment
                double offsetDist = random.nextDouble() * coneWidth * 0.8; // Stay within 80% of cone width
                double offsetAngle = random.nextDouble() * Math.PI * 2.0;

                // Calculate perpendicular vectors for radial offset
                Vec3 perpendicular = direction.cross(new Vec3(0, 1, 0));
                if (perpendicular.lengthSqr() < 0.001) {
                    perpendicular = direction.cross(new Vec3(1, 0, 0));
                }
                perpendicular = perpendicular.normalize();
                Vec3 perpendicular2 = direction.cross(perpendicular).normalize();

                double offsetX = Math.cos(offsetAngle) * offsetDist;
                double offsetY = Math.sin(offsetAngle) * offsetDist;

                Vec3 particlePos = position.add(
                    perpendicular.scale(offsetX).add(perpendicular2.scale(offsetY))
                );

                // Particle velocity: slight forward drift + radial expansion
                Vec3 velocity = direction.scale(0.02 + random.nextDouble() * 0.03);
                Vec3 radialVel = particlePos.subtract(position).normalize().scale(0.01 + random.nextDouble() * 0.02);
                velocity = velocity.add(radialVel);

                // Mix of particle types for variety
                float particleType = random.nextFloat();
                if (particleType < 0.5F) {
                    // Main flame particles
                    level.addParticle(ParticleTypes.FLAME,
                        particlePos.x, particlePos.y, particlePos.z,
                        velocity.x, velocity.y, velocity.z);
                } else if (particleType < 0.8F) {
                    // Small flame particles (embers)
                    level.addParticle(ParticleTypes.SMALL_FLAME,
                        particlePos.x, particlePos.y, particlePos.z,
                        velocity.x * 0.5, velocity.y * 0.5, velocity.z * 0.5);
                } else {
                    // Occasional smoke at the edges
                    level.addParticle(ParticleTypes.LARGE_SMOKE,
                        particlePos.x, particlePos.y, particlePos.z,
                        velocity.x * 0.3, velocity.y + 0.01, velocity.z * 0.3);
                }
            }
        }

        // Add extra embers at the impact point (end of cone)
        if (progressRatio > 0.5) { // Only when breath has extended significantly
            for (int i = 0; i < 3; i++) {
                double spread = 0.5;
                Vec3 emberPos = currentEnd.add(
                    (random.nextDouble() - 0.5) * spread,
                    (random.nextDouble() - 0.5) * spread,
                    (random.nextDouble() - 0.5) * spread
                );
                level.addParticle(ParticleTypes.FLAME,
                    emberPos.x, emberPos.y, emberPos.z,
                    (random.nextDouble() - 0.5) * 0.02,
                    random.nextDouble() * 0.02,
                    (random.nextDouble() - 0.5) * 0.02);
            }
        }
    }

    /**
     * Gets the fire breath start position with interpolated bone position.
     * Reads from fireBone in the model and applies entity position interpolation to prevent lag when moving fast.
     */
    private Vec3 getFireStartInterpolated(Ignivorus entity, BakedGeoModel model, float partialTick) {
        // Try to get bone position
        if (model != null) {
            var boneOpt = model.getBone("fireBone");
            if (boneOpt.isPresent()) {
                var bone = boneOpt.get();

                // Get bone's world-space matrix (includes non-interpolated entity position)
                org.joml.Matrix4f worldMat = new org.joml.Matrix4f(bone.getWorldSpaceMatrix());

                // Transform bone pivot to world space
                org.joml.Vector4f pivotWorld = new org.joml.Vector4f(0f, 0f, 0f, 1f);
                worldMat.transform(pivotWorld);

                // Correct for entity position interpolation
                double entityX = entity.getX();
                double entityY = entity.getY();
                double entityZ = entity.getZ();

                double interpX = Mth.lerp(partialTick, entity.xo, entityX);
                double interpY = Mth.lerp(partialTick, entity.yo, entityY);
                double interpZ = Mth.lerp(partialTick, entity.zo, entityZ);

                // Apply interpolation correction: (bone pos) - (current entity pos) + (interpolated entity pos)
                double correctedX = pivotWorld.x() - entityX + interpX;
                double correctedY = pivotWorld.y() - entityY + interpY;
                double correctedZ = pivotWorld.z() - entityZ + interpZ;

                return new Vec3(correctedX, correctedY, correctedZ);
            }
        }

        // Fallback to entity's fire breath anchor
        return entity.getFireBreathStartAnchor(partialTick);
    }
}
