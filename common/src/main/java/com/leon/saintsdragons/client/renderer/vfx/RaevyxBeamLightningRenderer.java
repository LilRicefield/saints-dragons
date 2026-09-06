package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class RaevyxBeamLightningRenderer {
    private static final boolean RIBBON_ONLY_TEST = true;
    private static final RenderType GLOW_RENDER_TYPE = BeamRenderType.TRANSLUCENT;
    private static final RenderType CORE_RENDER_TYPE = BeamRenderType.OPAQUE;
    private static final ResourceLocation STAR_TEXTURE =
            SaintsDragonsCommon.rl("textures/particle/star.png");
    private static final ResourceLocation LIGHTNING_BEAM_TEXTURE =
            SaintsDragonsCommon.rl("textures/particle/lightning_beam.png");
    private static final ResourceLocation LIGHTNING_BEAM_AURA_TEXTURE =
            SaintsDragonsCommon.rl("textures/particle/lightning_beam_aura.png");
    private static final ResourceLocation LIGHTNING_BEAM_SWIRL_TEXTURE =
            SaintsDragonsCommon.rl("textures/particle/lightning_beam_swirl.png");
    private static final ResourceLocation LIGHTNING_BEAM_SECOND_SWIRL_TEXTURE =
            SaintsDragonsCommon.rl("textures/particle/lightning_beam_second_swirl.png");
    private static final BeamRibbonRenderer.Style LIGHTNING_BEAM_STYLE =
            new BeamRibbonRenderer.Style(
                    0.75F,
                    1.0F,
                    0.075F,
                    0.12F,
                    0.28F,
                    1.0F,
                    true);
    private static final BeamRibbonRenderer.Style LIGHTNING_BEAM_AURA_STYLE =
            new BeamRibbonRenderer.Style(
                    0.92F,
                    1.0F,
                    0.11F,
                    0.12F,
                    0.28F,
                    0.72F,
                    true);
    private static final BeamRibbonRenderer.Style LIGHTNING_BEAM_FAINT_AURA_STYLE =
            new BeamRibbonRenderer.Style(
                    1.16F,
                    1.0F,
                    0.14F,
                    0.12F,
                    0.28F,
                    0.05F,
                    true);
    private static final BeamRibbonRenderer.Style LIGHTNING_BEAM_SWIRL_STYLE =
            new BeamRibbonRenderer.Style(
                    0.88F,
                    1.0F,
                    0.09F,
                    0.5F,
                    0.28F,
                    0.85F,
                    true);
    private static final BeamRibbonRenderer.Style LIGHTNING_BEAM_SECOND_SWIRL_STYLE =
            new BeamRibbonRenderer.Style(
                    0.62F,
                    1.0F,
                    0.10F,
                    0.5F,
                    0.28F,
                    0.60F,
                    true);
    private static final BeamStarFlashRenderer.Style BEAM_STAR_STYLE =
            new BeamStarFlashRenderer.Style(
                    32, 2.5F, 3.0F,
                    1.0F, 6.0F,
                    0.2F, 0.42F, 0.15F,
                    0.70F, 1.35F,
                    0.75F, 1.75F,
                    0.92F, 0.85F);
    private static final ResourceLocation[] FAST_LINE_TEXTURES = new ResourceLocation[16];
    private static final BeamGrainRenderer.RibbonEmitterStyle FAST_LINE_EMITTER_STYLE =
            new BeamGrainRenderer.RibbonEmitterStyle(
                    32, 8.0F, 16.0F,
                    3.0F, 3.0F,
                    0.5F, 0.5F, 3.0F,
                    0.5F, 0.25F, 0.75F, 0.85F,
                    0.75F, 0.25F);
    private static final long PHASE_SEED = 0x9E3779B97F4A7C15L;
    private static final long SECONDARY_SEED = 0x632BE59BD9B4E019L;
    private static final long BRANCH_SEED = 0xD1B54A32D192ED03L;
    private static final Vec3 BOLT_START = Vec3.ZERO;
    private static final float BOLT_MORPHS_PER_TICK = 0.65F;

    static {
        for (int frame = 0; frame < FAST_LINE_TEXTURES.length; frame++) {
            FAST_LINE_TEXTURES[frame] = SaintsDragonsCommon.rl(
                    "textures/particle/fast_lines" + frame + ".png");
        }
    }

    private RaevyxBeamLightningRenderer() {
    }

    public static void render(Raevyx raevyx, PoseStack poseStack, MultiBufferSource bufferSource,
                              float beamLength, float visibility, float ageInTicks,
                              boolean nightGold, boolean ending,
                              Vec3 beamStartWorld, Vec3 beamEndWorld) {
        if (raevyx == null || beamLength <= 0.05F || visibility <= 0.01F) {
            return;
        }

        long entitySeed = raevyx.getUUID().getMostSignificantBits()
                ^ raevyx.getUUID().getLeastSignificantBits();
        BeamRibbonRenderer.render(poseStack, bufferSource, LIGHTNING_BEAM_TEXTURE,
                beamLength, visibility, ageInTicks, beamStartWorld, beamEndWorld,
                LIGHTNING_BEAM_STYLE,
                1.0F, 0.0F, 0.0F);
        BeamRibbonRenderer.render(poseStack, bufferSource, LIGHTNING_BEAM_AURA_TEXTURE,
                beamLength, visibility, ageInTicks, beamStartWorld, beamEndWorld,
                LIGHTNING_BEAM_AURA_STYLE,
                1.0F, 0.0F, 0.0F);
        BeamRibbonRenderer.render(poseStack, bufferSource, LIGHTNING_BEAM_AURA_TEXTURE,
                beamLength, visibility, ageInTicks, beamStartWorld, beamEndWorld,
                LIGHTNING_BEAM_FAINT_AURA_STYLE,
                1.0F, 0.0F, 0.0F);
        BeamRibbonRenderer.render(poseStack, bufferSource, LIGHTNING_BEAM_SWIRL_TEXTURE,
                beamLength, visibility, ageInTicks, beamStartWorld, beamEndWorld,
                LIGHTNING_BEAM_SWIRL_STYLE,
                1.0F, 0.0F, 0.0F);
        BeamRibbonRenderer.render(poseStack, bufferSource, LIGHTNING_BEAM_SECOND_SWIRL_TEXTURE,
                beamLength, visibility, ageInTicks, beamStartWorld, beamEndWorld,
                LIGHTNING_BEAM_SECOND_SWIRL_STYLE,
                0.0F, 0.0F, 0.0F);
        BeamGrainRenderer.renderEmitter(poseStack, bufferSource, FAST_LINE_TEXTURES,
                beamLength, visibility, ageInTicks, entitySeed ^ BRANCH_SEED,
                FAST_LINE_EMITTER_STYLE, beamStartWorld, beamEndWorld,
                0.0F, 0.0F, 0.0F);
        if (!RIBBON_ONLY_TEST) {
            renderLightning(poseStack, bufferSource, beamLength, visibility,
                    ageInTicks, entitySeed, nightGold, ending);
        }
    }

    private static void renderLightning(PoseStack poseStack, MultiBufferSource bufferSource,
                                        float beamLength,
                                        float visibility, float ageInTicks,
                                        long entitySeed, boolean nightGold, boolean ending) {
        float phaseTime = ageInTicks * BOLT_MORPHS_PER_TICK;
        long phase = Mth.floor(phaseTime);
        float morph = smoothStep(phaseTime - Mth.floor(phaseTime));
        long seed = entitySeed ^ phase * PHASE_SEED;
        long nextSeed = entitySeed ^ (phase + 1L) * PHASE_SEED;
        Vec3 end = new Vec3(0.0D, 0.0D, beamLength);
        float spread = (float) Mth.clamp(beamLength * 0.012F, 0.10F, 0.48F);
        int segments = Mth.clamp(7 + (int) (beamLength * 0.20F), 8, 18);

        // Width stays fixed while alpha falls; shutdown should dissolve, not thin out.
        BoltStyle glow = nightGold
                ? new BoltStyle(0.96F, 0.54F, 0.025F, 0.74F * visibility, 0.19F)
                : new BoltStyle(0.82F, 0.085F, 0.035F, 0.76F * visibility, 0.19F);
        BoltStyle core = nightGold
                ? new BoltStyle(1.0F, 0.86F, 0.28F, 1.0F, 0.065F)
                : new BoltStyle(1.0F, 0.50F, 0.36F, 1.0F, 0.065F);
        BoltStyle crawler = nightGold
                ? new BoltStyle(1.0F, 0.70F, 0.065F, 0.62F * visibility, 0.055F)
                : new BoltStyle(0.92F, 0.16F, 0.075F, 0.64F * visibility, 0.055F);

        Matrix4f matrix = poseStack.last().pose();
        float coreCoverage = ending ? visibility : 1.0F;

        VertexConsumer glowConsumer = bufferSource.getBuffer(GLOW_RENDER_TYPE);
        renderMorphingBolt(matrix, glowConsumer, BOLT_START, end, seed, nextSeed,
                morph, segments, spread, glow, true, 1.0F, entitySeed ^ PHASE_SEED);
        renderMorphingBolt(matrix, glowConsumer, BOLT_START, end,
                seed ^ SECONDARY_SEED, nextSeed ^ SECONDARY_SEED,
                morph, segments, spread * 1.35F, crawler, false,
                1.0F, entitySeed ^ SECONDARY_SEED);

        BoltStyle impactGlow = new BoltStyle(glow.red(), glow.green(), glow.blue(),
                glow.alpha() * 0.78F, glow.width() * 0.72F);
        renderImpactArcs(matrix, glowConsumer, end, seed ^ BRANCH_SEED,
                beamLength, visibility, impactGlow, 1.0F, entitySeed ^ BRANCH_SEED);

        VertexConsumer coreConsumer = bufferSource.getBuffer(CORE_RENDER_TYPE);
        renderMorphingBolt(matrix, coreConsumer, BOLT_START, end, seed, nextSeed,
                morph, segments, spread, core, true,
                coreCoverage, entitySeed ^ BRANCH_SEED);

        BoltStyle impactCore = new BoltStyle(core.red(), core.green(), core.blue(),
                1.0F, core.width() * 0.76F);
        renderImpactArcs(matrix, coreConsumer, end, seed ^ BRANCH_SEED,
                beamLength, visibility, impactCore, coreCoverage,
                entitySeed ^ SECONDARY_SEED);

        float starRed = 1.0F;
        float starGreen = nightGold ? 0.72F : 0.02F;
        float starBlue = nightGold ? 0.08F : 0.01F;
        BeamStarFlashRenderer.render(poseStack, bufferSource, STAR_TEXTURE,
                beamLength, visibility, ageInTicks, entitySeed ^ PHASE_SEED,
                BEAM_STAR_STYLE, starRed, starGreen, starBlue);
    }

    private static float smoothStep(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }

    private static void renderImpactArcs(Matrix4f matrix, VertexConsumer consumer, Vec3 impact,
                                         long seed, float beamLength, float visibility,
                                         BoltStyle style, float coverage, long dissolveSeed) {
        RandomSource random = RandomSource.create(seed);
        int arcCount = 5;
        double impactScale = Mth.clamp(beamLength * 0.018D, 0.50D, 1.30D)
                * (0.58D + visibility * 0.42D);

        for (int i = 0; i < arcCount; i++) {
            Vec3 direction = new Vec3(
                    random.nextDouble() * 2.0D - 1.0D,
                    random.nextDouble() * 2.0D - 1.0D,
                    -0.15D - random.nextDouble() * 0.85D
            ).normalize();
            double length = impactScale * (0.50D + random.nextDouble() * 0.50D);
            Vec3 arcEnd = impact.add(direction.scale(length));
            int arcSegments = 3 + random.nextInt(3);
            float arcSpread = 0.10F + random.nextFloat() * 0.07F;
            long arcSeed = random.nextLong();

            renderStaticBolt(matrix, consumer, impact, arcEnd, arcSeed,
                    arcSegments, arcSpread, style, coverage, dissolveSeed ^ arcSeed);
        }
    }

    private static void renderMorphingBolt(Matrix4f matrix, VertexConsumer consumer,
                                            Vec3 start, Vec3 end,
                                            long seed, long nextSeed, float morph,
                                            int segments, float spread,
                                            BoltStyle style, boolean allowBranch,
                                            float coverage, long dissolveSeed) {
        Vec3[] points = generateMorphingCenterline(start, end, segments, spread, seed, nextSeed, morph);
        renderPrism(matrix, consumer, points, style, coverage, dissolveSeed);

        if (!allowBranch || segments < 6) {
            return;
        }

        RandomSource random = RandomSource.create(seed ^ BRANCH_SEED);
        Vec3 mainDirection = end.subtract(start);
        Vec3 mainDirectionNormalized = mainDirection.normalize();
        int branchCount = 2 + random.nextInt(2);
        int mouthSegmentCount = Math.max(1, segments / 3);

        for (int branch = 0; branch < branchCount; branch++) {
            int branchIndex = branch == 0
                    ? random.nextInt(mouthSegmentCount)
                    : 1 + random.nextInt(segments - 1);
            Vec3 branchStart = points[branchIndex];
            double forwardBias = random.nextDouble() * 0.50D - 0.16D;
            Vec3 branchDirection = randomOrthogonal(mainDirection, random)
                    .add(mainDirectionNormalized.scale(forwardBias))
                    .normalize();
            double branchLength = Mth.clamp(
                    mainDirection.length() * (0.08D + random.nextDouble() * 0.07D),
                    0.40D,
                    2.20D
            );
            Vec3 branchEnd = branchStart.add(branchDirection.scale(branchLength));
            float branchAlpha = style.alpha() >= 0.999F
                    ? 1.0F
                    : style.alpha() * 0.72F;
            BoltStyle branchStyle = new BoltStyle(style.red(), style.green(), style.blue(),
                    branchAlpha, style.width() * 0.62F);
            renderStaticBolt(matrix, consumer, branchStart, branchEnd, random.nextLong(),
                    Mth.clamp(segments / 2, 4, 8), Math.max(0.10F, spread * 0.68F),
                    branchStyle, coverage, dissolveSeed ^ branch);
        }
    }

    private static void renderStaticBolt(Matrix4f matrix, VertexConsumer consumer,
                                         Vec3 start, Vec3 end, long seed,
                                         int segments, float spread, BoltStyle style,
                                         float coverage, long dissolveSeed) {
        Vec3[] points = generateCenterline(start, end, segments, spread, RandomSource.create(seed));
        renderPrism(matrix, consumer, points, style, coverage, dissolveSeed);
    }

    private static Vec3[] generateMorphingCenterline(Vec3 start, Vec3 end, int segments,
                                                     float spread, long seed, long nextSeed,
                                                     float morph) {
        Vec3[] current = generateCenterline(start, end, segments, spread, RandomSource.create(seed));
        Vec3[] next = generateCenterline(start, end, segments, spread, RandomSource.create(nextSeed));
        Vec3[] result = new Vec3[current.length];

        for (int i = 0; i < current.length; i++) {
            result[i] = current[i].add(next[i].subtract(current[i]).scale(morph));
        }
        return result;
    }

    private static Vec3[] generateCenterline(Vec3 start, Vec3 end, int segments,
                                             float spread, RandomSource random) {
        Vec3[] points = new Vec3[segments + 1];
        Vec3 direction = end.subtract(start);
        Vec3 previousOffset = Vec3.ZERO;
        points[0] = start;

        for (int i = 1; i < segments; i++) {
            double progress = i / (double) segments;
            double envelope = Math.sin(Math.PI * progress);
            Vec3 randomOffset = randomOrthogonal(direction, random)
                    .scale(spread * envelope * (0.45D + random.nextDouble() * 0.55D));
            previousOffset = previousOffset.scale(0.52D).add(randomOffset.scale(0.48D));
            points[i] = start.add(direction.scale(progress)).add(previousOffset);
        }

        points[segments] = end;
        return points;
    }

    private static void renderPrism(Matrix4f matrix, VertexConsumer consumer,
                                    Vec3[] points, BoltStyle style,
                                    float coverage, long dissolveSeed) {
        int segmentCount = points.length - 1;
        Vec3[][] rings = new Vec3[points.length][4];
        Vec3 previousAxis = null;

        for (int i = 0; i < points.length; i++) {
            Vec3 tangent = centerlineTangent(points, i);
            Vec3 firstAxis = transportAxis(previousAxis, tangent);
            Vec3 secondAxis = tangent.cross(firstAxis).normalize();
            firstAxis = secondAxis.cross(tangent).normalize();
            previousAxis = firstAxis;

            float progress = i / (float) segmentCount;
            float taper = 0.72F + Mth.sin(Mth.PI * progress) * 0.28F;
            double width = style.width() * taper;
            Vec3 firstOffset = firstAxis.scale(width);
            Vec3 secondOffset = secondAxis.scale(width);
            Vec3 point = points[i];
            rings[i][0] = point.add(firstOffset).add(secondOffset);
            rings[i][1] = point.subtract(firstOffset).add(secondOffset);
            rings[i][2] = point.subtract(firstOffset).subtract(secondOffset);
            rings[i][3] = point.add(firstOffset).subtract(secondOffset);
        }

        for (int i = 0; i < segmentCount; i++) {
            if (!isSegmentVisible(coverage, dissolveSeed, i)) {
                continue;
            }
            for (int face = 0; face < 4; face++) {
                int nextFace = (face + 1) & 3;
                vertex(matrix, consumer, rings[i][face], style);
                vertex(matrix, consumer, rings[i + 1][face], style);
                vertex(matrix, consumer, rings[i + 1][nextFace], style);
                vertex(matrix, consumer, rings[i][nextFace], style);
            }
        }
    }

    private static boolean isSegmentVisible(float coverage, long seed, int segment) {
        if (coverage >= 0.999F) {
            return true;
        }
        if (coverage <= 0.001F) {
            return false;
        }

        long mixed = seed + PHASE_SEED * (segment + 1L);
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        double sample = (mixed >>> 11) * 0x1.0p-53;
        return sample < coverage;
    }

    private static Vec3 centerlineTangent(Vec3[] points, int index) {
        Vec3 tangent;
        if (index == 0) {
            tangent = points[1].subtract(points[0]);
        } else if (index == points.length - 1) {
            tangent = points[index].subtract(points[index - 1]);
        } else {
            tangent = points[index + 1].subtract(points[index - 1]);
        }

        if (tangent.lengthSqr() < 1.0E-6D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return tangent.normalize();
    }

    private static Vec3 transportAxis(Vec3 previousAxis, Vec3 tangent) {
        if (previousAxis != null) {
            Vec3 transported = previousAxis.subtract(tangent.scale(previousAxis.dot(tangent)));
            if (transported.lengthSqr() >= 1.0E-6D) {
                return transported.normalize();
            }
        }

        Vec3 reference = Math.abs(tangent.y) < 0.9D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 axis = reference.cross(tangent);
        return axis.lengthSqr() < 1.0E-6D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : axis.normalize();
    }

    private static void vertex(Matrix4f matrix, VertexConsumer consumer, Vec3 position, BoltStyle style) {
        consumer.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(style.red(), style.green(), style.blue(), style.alpha())
                .endVertex();
    }

    private static Vec3 randomOrthogonal(Vec3 direction, RandomSource random) {
        Vec3 orthogonal = direction.cross(randomUnit(random));
        if (orthogonal.lengthSqr() < 1.0E-6D) {
            orthogonal = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }
        return orthogonal.lengthSqr() < 1.0E-6D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : orthogonal.normalize();
    }

    private static Vec3 randomUnit(RandomSource random) {
        Vec3 vector;
        do {
            vector = new Vec3(
                    random.nextDouble() * 2.0D - 1.0D,
                    random.nextDouble() * 2.0D - 1.0D,
                    random.nextDouble() * 2.0D - 1.0D
            );
        } while (vector.lengthSqr() < 1.0E-6D);
        return vector.normalize();
    }

    private static final class BeamRenderType extends RenderType {
        private static final RenderType TRANSLUCENT = create(
                "saintsdragons_raevyx_beam_glow",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                2048,
                false,
                true,
                CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .setOutputState(TRANSLUCENT_TARGET)
                        .createCompositeState(false)
        );

        private static final RenderType OPAQUE = create(
                "saintsdragons_raevyx_beam_core",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                1024,
                false,
                false,
                CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(NO_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .setOutputState(MAIN_TARGET)
                        .createCompositeState(false)
        );

        private BeamRenderType(String name, VertexFormat format, VertexFormat.Mode mode,
                               int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                               Runnable setupState, Runnable clearState) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload,
                    setupState, clearState);
        }
    }

    private record BoltStyle(float red, float green, float blue, float alpha, float width) {
    }
}
