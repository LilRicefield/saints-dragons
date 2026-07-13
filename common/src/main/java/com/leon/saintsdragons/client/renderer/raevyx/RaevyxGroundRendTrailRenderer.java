package com.leon.saintsdragons.client.renderer.raevyx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.effect.LightningVisualEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class RaevyxGroundRendTrailRenderer extends EntityRenderer<LightningVisualEntity> {
    private static final float WIDTH_MULTIPLIER = 1.40F;
    private static final int SLASH_FRAME_COUNT = 7;
    private static final float SLASH_TICKS_PER_FRAME = 2.0F;
    private static final int STORM_FRAME_COUNT = 8;
    private static final float STORM_TICKS_PER_FRAME = 2.0F;
    private static final ResourceLocation[] SLASH_TEXTURES = new ResourceLocation[SLASH_FRAME_COUNT];
    private static final ResourceLocation[] STORM_TEXTURES = new ResourceLocation[STORM_FRAME_COUNT];

    static {
        for (int frame = 0; frame < SLASH_FRAME_COUNT; frame++) {
            SLASH_TEXTURES[frame] = SaintsDragonsCommon.rl("textures/particle/slash_line" + frame + ".png");
        }
        for (int frame = 0; frame < STORM_FRAME_COUNT; frame++) {
            STORM_TEXTURES[frame] = SaintsDragonsCommon.rl(
                    "textures/particle/katana_lightning_storm" + frame + ".png");
        }
    }

    public RaevyxGroundRendTrailRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(LightningVisualEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        double dx = entity.getX() - camX;
        double dy = entity.getY() - camY;
        double dz = entity.getZ() - camZ;
        return entity.shouldRenderAtSqrDistance(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public void render(@NotNull LightningVisualEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        Vec3 start = entity.getStartOffset();
        Vec3 end = entity.getEndOffset();
        if (start.distanceToSqr(end) < 1.0E-6D) {
            return;
        }

        if (entity.getVisualStyle() == LightningVisualEntity.VisualStyle.BLOOD_TEMPEST_SLASH) {
            renderAnimatedTrail(entity, partialTick, poseStack, bufferSource, start, end,
                    SLASH_TEXTURES, SLASH_TICKS_PER_FRAME, 1.1D);
            return;
        }

        if (entity.getVisualStyle() == LightningVisualEntity.VisualStyle.BLOOD_TEMPEST_STORM) {
            renderAnimatedTrail(entity, partialTick, poseStack, bufferSource, start, end,
                    STORM_TEXTURES, STORM_TICKS_PER_FRAME, 1.55D);
            return;
        }

        boolean bloodTempest = entity.getVisualStyle()
                == LightningVisualEntity.VisualStyle.BLOOD_TEMPEST;
        float alpha = bloodTempest ? 1.0F : entity.getRenderAlpha(partialTick);
        if (alpha <= 0.01F) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        long seed = Integer.toUnsignedLong(entity.getRenderSeed())
                + (bloodTempest ? 0L : (long)entity.tickCount * 31L);

        if (bloodTempest) {
            RenderWindow window = getBloodTempestWindow(entity, partialTick);
            if (window.end() <= window.start()) {
                return;
            }
            float pulse = window.flashing() ? 1.32F : 1.0F;
            renderLayer(matrix, consumer, start, end, entity.getVisualScale() * pulse, alpha, seed,
                    window.start(), window.end(),
                    0.17F, 0.020F, 0.36F, 0.01F, 0.025F, 0.52F);
            renderLayer(matrix, consumer, start, end, entity.getVisualScale() * 0.72F * pulse, alpha, seed + 31L,
                    window.start(), window.end(),
                    0.11F, 0.013F, 1.0F, 0.035F, 0.085F, 0.72F);
            renderLayer(matrix, consumer, start, end, entity.getVisualScale() * 0.38F * pulse, alpha, seed + 63L,
                    window.start(), window.end(),
                    0.065F, 0.007F, 1.0F, 0.78F, 0.82F, 0.92F);
        } else {
            renderLayer(matrix, consumer, start, end, entity.getVisualScale(), alpha, seed,
                    0.0F, 1.0F,
                    0.18F, 0.012F, 0.45F, 0.45F, 0.50F, 0.26F);
            renderLayer(matrix, consumer, start, end, entity.getVisualScale() * 0.74F, alpha, seed + 31L,
                    0.0F, 1.0F,
                    0.12F, 0.008F, 0.66F, 0.77F, 0.98F, 0.31F);
            renderLayer(matrix, consumer, start, end, entity.getVisualScale() * 0.46F, alpha, seed + 63L,
                    0.0F, 1.0F,
                    0.07F, 0.004F, 0.96F, 0.98F, 1.0F, 0.36F);
        }
    }

    private void renderAnimatedTrail(LightningVisualEntity entity, float partialTick, PoseStack poseStack,
                                     MultiBufferSource bufferSource, Vec3 start, Vec3 end,
                                     ResourceLocation[] textures, float ticksPerFrame, double halfWidth) {
        int frame = Mth.clamp(
                (int)Math.floor((entity.tickCount + partialTick) / ticksPerFrame),
                0,
                textures.length - 1
        );
        float alpha = entity.getRenderAlpha(partialTick);
        if (alpha <= 0.01F) {
            return;
        }

        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-4D) {
            return;
        }

        Vec3 direction = delta.scale(1.0D / length);
        Vec3 cameraLocal = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()
                .subtract(entity.position());
        Vec3 widthAxis = direction.cross(cameraLocal);
        if (widthAxis.lengthSqr() < 1.0E-6D) {
            widthAxis = direction.cross(Math.abs(direction.y) < 0.9D
                    ? new Vec3(0.0D, 1.0D, 0.0D)
                    : new Vec3(1.0D, 0.0D, 0.0D));
        }
        widthAxis = widthAxis.normalize().scale(halfWidth * entity.getVisualScale());

        Vec3 normal = direction.cross(widthAxis).normalize();
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        Vector3f transformedNormal = new Vector3f((float)normal.x, (float)normal.y, (float)normal.z);
        normalMatrix.transform(transformedNormal);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.eyes(textures[frame]));

        emitSlashQuad(consumer, matrix, transformedNormal, start, end, widthAxis, alpha, false);
        emitSlashQuad(consumer, matrix, transformedNormal, start, end, widthAxis, alpha, true);
    }

    private void emitSlashQuad(VertexConsumer consumer, Matrix4f matrix, Vector3f normal,
                               Vec3 start, Vec3 end, Vec3 widthAxis, float alpha, boolean reverse) {
        Vec3 startTop = start.add(widthAxis);
        Vec3 startBottom = start.subtract(widthAxis);
        Vec3 endBottom = end.subtract(widthAxis);
        Vec3 endTop = end.add(widthAxis);
        if (reverse) {
            addSlashVertex(consumer, matrix, normal, endTop, 0.0F, 0.0F, alpha);
            addSlashVertex(consumer, matrix, normal, endBottom, 0.0F, 1.0F, alpha);
            addSlashVertex(consumer, matrix, normal, startBottom, 1.0F, 1.0F, alpha);
            addSlashVertex(consumer, matrix, normal, startTop, 1.0F, 0.0F, alpha);
            return;
        }

        addSlashVertex(consumer, matrix, normal, startTop, 1.0F, 0.0F, alpha);
        addSlashVertex(consumer, matrix, normal, startBottom, 1.0F, 1.0F, alpha);
        addSlashVertex(consumer, matrix, normal, endBottom, 0.0F, 1.0F, alpha);
        addSlashVertex(consumer, matrix, normal, endTop, 0.0F, 0.0F, alpha);
    }

    private void addSlashVertex(VertexConsumer consumer, Matrix4f matrix, Vector3f normal,
                                Vec3 position, float u, float v, float alpha) {
        consumer.vertex(matrix, (float)position.x, (float)position.y, (float)position.z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal.x(), normal.y(), normal.z())
                .endVertex();
    }

    private void renderLayer(Matrix4f matrix, VertexConsumer consumer, Vec3 start, Vec3 end, float scale, float alpha,
                             long seed, float visibleStart, float visibleEnd,
                             float widthFactor, float jitterFactor,
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

        Vec3[] points = new Vec3[8];
        for (int i = 0; i < 8; ++i) {
            float progress = i / 7.0F;
            points[i] = start.lerp(end, progress)
                    .add(axisA.scale(offsetsA[i] * jitter))
                    .add(axisB.scale(offsetsB[i] * jitter));
        }

        for (int i = 0; i < 7; ++i) {
            float t0 = i / 7.0F;
            float t1 = (i + 1) / 7.0F;
            float clippedT0 = Math.max(t0, visibleStart);
            float clippedT1 = Math.min(t1, visibleEnd);
            if (clippedT1 <= clippedT0) {
                continue;
            }
            float localT0 = (clippedT0 - t0) / (t1 - t0);
            float localT1 = (clippedT1 - t0) / (t1 - t0);
            Vec3 p0 = points[i].lerp(points[i + 1], localT0);
            Vec3 p1 = points[i].lerp(points[i + 1], localT1);
            float width0 = baseWidth * (1.08F - clippedT0 * 0.22F);
            float width1 = baseWidth * (1.08F - clippedT1 * 0.22F);

            emitRibbon(matrix, consumer, p0, p1, axisA, width0, width1, red, green, blue, layerAlpha);
            emitRibbon(matrix, consumer, p0, p1, axisB, width0, width1, red, green, blue, layerAlpha);
            emitRibbon(matrix, consumer, p0, p1, diagA, width0 * 0.82F, width1 * 0.82F, red, green, blue, layerAlpha * 0.92F);
            emitRibbon(matrix, consumer, p0, p1, diagB, width0 * 0.82F, width1 * 0.82F, red, green, blue, layerAlpha * 0.92F);
        }
    }

    private RenderWindow getBloodTempestWindow(LightningVisualEntity entity, float partialTick) {
        float age = entity.tickCount + partialTick;
        float growTicks = Math.max(2.0F, entity.getMaxAge() * 0.34F);
        float flashTicks = 0.75F;
        float retractStart = growTicks + flashTicks;
        float retractTicks = Math.max(1.5F, Math.min(2.75F, entity.getMaxAge() - retractStart));
        float head = easeOutCubic(Mth.clamp(age / growTicks, 0.0F, 1.0F));
        float tail = age <= retractStart
                ? 0.0F
                : smoothStep(Mth.clamp((age - retractStart) / retractTicks, 0.0F, 1.0F));
        return new RenderWindow(tail, head, age >= growTicks && age < retractStart);
    }

    private float easeOutCubic(float value) {
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse;
    }

    private float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
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
    public @NotNull ResourceLocation getTextureLocation(@NotNull LightningVisualEntity entity) {
        if (entity.getVisualStyle() == LightningVisualEntity.VisualStyle.BLOOD_TEMPEST_SLASH) {
            return SLASH_TEXTURES[0];
        }
        if (entity.getVisualStyle() == LightningVisualEntity.VisualStyle.BLOOD_TEMPEST_STORM) {
            return STORM_TEXTURES[0];
        }
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private record RenderWindow(float start, float end, boolean flashing) {
    }
}
