package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class RaevyxBeamLightningRenderer {
    private static final long PHASE_SEED = 0x9E3779B97F4A7C15L;
    private static final long SECONDARY_SEED = 0x632BE59BD9B4E019L;
    private static final Vec3 BOLT_START = Vec3.ZERO;

    private RaevyxBeamLightningRenderer() {
    }

    public static void render(Raevyx raevyx, PoseStack poseStack, MultiBufferSource bufferSource,
                              float beamLength, float visibility, boolean nightGold) {
        if (raevyx == null || beamLength <= 0.05F || visibility <= 0.01F) {
            return;
        }

        long entitySeed = raevyx.getUUID().getMostSignificantBits()
                ^ raevyx.getUUID().getLeastSignificantBits();
        long phase = raevyx.tickCount;
        long seed = entitySeed ^ phase * PHASE_SEED;
        Vec3 end = new Vec3(0.0D, 0.0D, beamLength);
        float spread = (float) Mth.clamp(beamLength * 0.014F, 0.12F, 0.62F);
        int segments = Mth.clamp(5 + (int) (beamLength * 0.15F), 6, 14);
        float visibleWidth = 0.7F + visibility * 0.3F;

        BoltStyle primary = nightGold
                ? new BoltStyle(1.0F, 0.48F, 0.03F, 0.88F * visibility, 0.10F * visibleWidth)
                : new BoltStyle(1.0F, 0.14F, 0.02F, 0.92F * visibility, 0.10F * visibleWidth);
        BoltStyle secondary = nightGold
                ? new BoltStyle(1.0F, 0.94F, 0.48F, 0.88F * visibility, 0.28F * visibleWidth)
                : new BoltStyle(0.95F, 0.025F, 0.01F, 0.90F * visibility, 0.28F * visibleWidth);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        renderBolt(matrix, consumer, BOLT_START, end, seed ^ SECONDARY_SEED,
                segments, spread * 1.05F, secondary, true);
        renderBolt(matrix, consumer, BOLT_START, end, seed, segments, spread, primary, true);
        renderImpactArcs(matrix, consumer, end, seed ^ PHASE_SEED,
                beamLength, visibility, primary, secondary);
    }

    private static void renderImpactArcs(Matrix4f matrix, VertexConsumer consumer, Vec3 impact,
                                         long seed, float beamLength, float visibility,
                                         BoltStyle primary, BoltStyle secondary) {
        RandomSource random = RandomSource.create(seed);
        int arcCount = 5;
        double impactScale = Mth.clamp(beamLength * 0.018D, 0.55D, 1.35D)
                * (0.65D + visibility * 0.35D);

        for (int i = 0; i < arcCount; i++) {
            Vec3 direction = new Vec3(
                    random.nextDouble() * 2.0D - 1.0D,
                    random.nextDouble() * 2.0D - 1.0D,
                    -0.2D - random.nextDouble() * 0.8D
            ).normalize();
            double length = impactScale * (0.55D + random.nextDouble() * 0.45D);
            Vec3 end = impact.add(direction.scale(length));
            BoltStyle baseStyle = (i & 1) == 0 ? primary : secondary;
            BoltStyle impactStyle = new BoltStyle(baseStyle.red(), baseStyle.green(), baseStyle.blue(),
                    baseStyle.alpha(), baseStyle.width() * 0.9F);
            renderBolt(matrix, consumer, impact, end, random.nextLong(),
                    3 + random.nextInt(3), 0.14F, impactStyle, false);
        }
    }

    private static void renderBolt(Matrix4f matrix, VertexConsumer consumer,
                                   Vec3 start, Vec3 end, long seed, int segments, float spread,
                                   BoltStyle style, boolean allowBranch) {
        RandomSource random = RandomSource.create(seed);
        Vec3[] points = generateCenterline(start, end, segments, spread, random);
        renderPrism(matrix, consumer, points, style);

        if (allowBranch && segments >= 6) {
            Vec3 mainDirection = end.subtract(start);
            Vec3 mainDirectionNormalized = mainDirection.normalize();
            int branchCount = 3 + random.nextInt(2);
            int mouthSegmentCount = Math.max(1, segments / 3);

            for (int branch = 0; branch < branchCount; branch++) {
                int branchIndex = branch == 0
                        ? random.nextInt(mouthSegmentCount)
                        : 1 + random.nextInt(segments - 1);
                Vec3 branchStart = points[branchIndex];
                double forwardBias = random.nextDouble() * 0.55D - 0.20D;
                Vec3 branchDirection = randomOrthogonal(mainDirection, random)
                        .add(mainDirectionNormalized.scale(forwardBias))
                        .normalize();
                double branchLength = Mth.clamp(
                        mainDirection.length() * (0.10D + random.nextDouble() * 0.07D),
                        0.5D,
                        2.5D
                );
                Vec3 branchEnd = branchStart.add(branchDirection.scale(branchLength));
                BoltStyle branchStyle = new BoltStyle(style.red(), style.green(), style.blue(),
                        style.alpha() * 0.76F, style.width() * 0.68F);
                renderBolt(matrix, consumer, branchStart, branchEnd, random.nextLong(),
                        Mth.clamp(segments / 2 + random.nextInt(2), 4, 8),
                        Math.max(0.12F, spread * 0.75F), branchStyle, false);
            }
        }
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
            previousOffset = previousOffset.scale(0.58D).add(randomOffset.scale(0.42D));
            points[i] = start.add(direction.scale(progress)).add(previousOffset);
        }

        points[segments] = end;
        return points;
    }

    private static void renderPrism(Matrix4f matrix, VertexConsumer consumer,
                                    Vec3[] points, BoltStyle style) {
        int segmentCount = points.length - 1;
        Vec3[][] rings = new Vec3[points.length][4];
        Vec3 previousAxis = null;

        for (int i = 0; i < points.length; i++) {
            Vec3 tangent = centerlineTangent(points, i);
            Vec3 firstAxis = transportAxis(previousAxis, tangent);
            Vec3 secondAxis = tangent.cross(firstAxis).normalize();
            firstAxis = secondAxis.cross(tangent).normalize();
            previousAxis = firstAxis;

            float taper = 1.0F - (i / (float) segmentCount) * 0.45F;
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
            for (int face = 0; face < 4; face++) {
                int nextFace = (face + 1) & 3;
                vertex(matrix, consumer, rings[i][face], style);
                vertex(matrix, consumer, rings[i + 1][face], style);
                vertex(matrix, consumer, rings[i + 1][nextFace], style);
                vertex(matrix, consumer, rings[i][nextFace], style);
            }
        }
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

    private record BoltStyle(float red, float green, float blue, float alpha, float width) {
    }
}
