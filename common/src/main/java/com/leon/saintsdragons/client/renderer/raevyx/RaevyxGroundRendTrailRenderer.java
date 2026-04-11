package com.leon.saintsdragons.client.renderer.raevyx;

import com.leon.saintsdragons.server.entity.effect.raevyx.RaevyxGroundRendTrailEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class RaevyxGroundRendTrailRenderer extends EntityRenderer<RaevyxGroundRendTrailEntity> {
    private static final float WIDTH_MULTIPLIER = 1.40F;

    public RaevyxGroundRendTrailRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(RaevyxGroundRendTrailEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        double dx = entity.getX() - camX;
        double dy = entity.getY() - camY;
        double dz = entity.getZ() - camZ;
        return entity.shouldRenderAtSqrDistance(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public void render(@NotNull RaevyxGroundRendTrailEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        Vec3 start = entity.getStartOffset();
        Vec3 end = entity.getEndOffset();
        if (start.distanceToSqr(end) < 1.0E-6D) {
            return;
        }

        float alpha = entity.getRenderAlpha(partialTick);
        if (alpha <= 0.01F) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        long seed = Integer.toUnsignedLong(entity.getRenderSeed()) + (long)entity.tickCount * 31L;

        renderLayer(matrix, consumer, start, end, entity.getVisualScale(), alpha, seed,
                0.18F, 0.012F, 0.45F, 0.45F, 0.50F, 0.26F);
        renderLayer(matrix, consumer, start, end, entity.getVisualScale() * 0.74F, alpha, seed + 31L,
                0.12F, 0.008F, 0.66F, 0.77F, 0.98F, 0.31F);
        renderLayer(matrix, consumer, start, end, entity.getVisualScale() * 0.46F, alpha, seed + 63L,
                0.07F, 0.004F, 0.96F, 0.98F, 1.0F, 0.36F);
    }

    private void renderLayer(Matrix4f matrix, VertexConsumer consumer, Vec3 start, Vec3 end, float scale, float alpha,
                             long seed, float widthFactor, float jitterFactor,
                             float red, float green, float blue, float alphaFactor) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-4D) {
            return;
        }

        Vec3 direction = delta.scale(1.0D / length);
        Vec3 axisA = direction.cross(Math.abs(direction.y) > 0.85D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D));
        if (axisA.lengthSqr() < 1.0E-6D) {
            axisA = new Vec3(1.0D, 0.0D, 0.0D);
        }
        axisA = axisA.normalize();
        Vec3 axisB = direction.cross(axisA).normalize();
        Vec3 diagA = axisA.add(axisB).normalize();
        Vec3 diagB = axisA.subtract(axisB).normalize();

        float[] offsetsA = new float[8];
        float[] offsetsB = new float[8];
        RandomSource random = RandomSource.create(seed);
        float runningA = 0.0F;
        float runningB = 0.0F;

        for (int i = 7; i >= 0; --i) {
            offsetsA[i] = runningA;
            offsetsB[i] = runningB;
            runningA += random.nextInt(11) - 5;
            runningB += random.nextInt(11) - 5;
        }

        float baseWidth = (0.02F + scale * widthFactor) * WIDTH_MULTIPLIER;
        float jitter = 0.006F + scale * jitterFactor;
        float layerAlpha = alpha * alphaFactor;

        for (int i = 0; i < 7; ++i) {
            float t0 = i / 7.0F;
            float t1 = (i + 1) / 7.0F;
            Vec3 p0 = start.lerp(end, t0)
                    .add(axisA.scale(offsetsA[i] * jitter))
                    .add(axisB.scale(offsetsB[i] * jitter));
            Vec3 p1 = start.lerp(end, t1)
                    .add(axisA.scale(offsetsA[i + 1] * jitter))
                    .add(axisB.scale(offsetsB[i + 1] * jitter));
            float width0 = baseWidth * (1.08F - t0 * 0.22F);
            float width1 = baseWidth * (1.08F - t1 * 0.22F);

            emitRibbon(matrix, consumer, p0, p1, axisA, width0, width1, red, green, blue, layerAlpha);
            emitRibbon(matrix, consumer, p0, p1, axisB, width0, width1, red, green, blue, layerAlpha);
            emitRibbon(matrix, consumer, p0, p1, diagA, width0 * 0.82F, width1 * 0.82F, red, green, blue, layerAlpha * 0.92F);
            emitRibbon(matrix, consumer, p0, p1, diagB, width0 * 0.82F, width1 * 0.82F, red, green, blue, layerAlpha * 0.92F);
        }
    }

    private void emitRibbon(Matrix4f matrix, VertexConsumer consumer, Vec3 start, Vec3 end, Vec3 axis,
                            float startWidth, float endWidth, float red, float green, float blue, float alpha) {
        Vec3 startOffset = axis.scale(startWidth);
        Vec3 endOffset = axis.scale(endWidth);

        addVertex(matrix, consumer, start.add(startOffset), red, green, blue, alpha);
        addVertex(matrix, consumer, end.add(endOffset), red, green, blue, alpha);
        addVertex(matrix, consumer, end.subtract(endOffset), red, green, blue, alpha);
        addVertex(matrix, consumer, start.subtract(startOffset), red, green, blue, alpha);
    }

    private void addVertex(Matrix4f matrix, VertexConsumer consumer, Vec3 pos, float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
                .color(red, green, blue, alpha)
                .endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull RaevyxGroundRendTrailEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
