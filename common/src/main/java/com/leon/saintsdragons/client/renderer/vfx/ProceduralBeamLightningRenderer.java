package com.leon.saintsdragons.client.renderer.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class ProceduralBeamLightningRenderer {
    private static final int STRANDS = 2;
    private static final float FLASH_TICKS = 4.0F;
    private static final float MAX_RADIUS = 1.2F;
    private static final float CORE_HALF_WIDTH = 0.035F;
    private static final float OUTER_HALF_WIDTH = 0.10F;
    private static final long SEED_SALT = 0x94D049BB133111EBL;

    private ProceduralBeamLightningRenderer() {
    }

    public static void render(PoseStack poses, MultiBufferSource buffers, float length,
                              float visibility, float ageInTicks, long seed,
                              float red, float green, float blue) {
        if (length <= 0.05F || visibility <= 0.01F) {
            return;
        }

        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = poses.last().pose();
        for (int strand = 0; strand < STRANDS; strand++) {
            float clock = ageInTicks + strand * FLASH_TICKS / STRANDS;
            int generation = Mth.floor(clock / FLASH_TICKS);
            float age = clock - generation * FLASH_TICKS;
            float flash = Math.min(1.0F, age / 0.25F)
                    * (1.0F - Mth.clamp((age - 0.75F) / 3.25F, 0.0F, 1.0F));
            float alpha = flash * visibility;
            if (alpha <= 0.01F) {
                continue;
            }
            long boltSeed = seed ^ (SEED_SALT * (strand + 1))
                    ^ (0x9E3779B97F4A7C15L * generation);
            emitBolt(vertices, matrix, length, boltSeed,
                    (0.35F + flash * 0.65F) * visibility, alpha, red, green, blue);
        }
    }

    static void emitBolt(VertexConsumer vertices, Matrix4f matrix, float length, long seed,
                         float widthScale, float alpha, float red, float green, float blue) {
        int segments = Mth.clamp(Mth.ceil(length / 2.0F), 6, 40);
        float radius = Math.min(MAX_RADIUS, length * 0.11F);
        float x = 0.0F;
        float y = 0.0F;
        float z = 0.0F;
        for (int point = 1; point <= segments; point++) {
            float progress = point / (float) segments;
            float envelope = point == segments ? 0.0F
                    : Math.min(1.0F, Math.min(progress, 1.0F - progress) * 6.0F);
            float nextX = noise(seed + point * 2L) * radius * envelope;
            float nextY = noise(seed + point * 2L + 1L) * radius * envelope;
            float nextZ = progress * length;
            emitCross(vertices, matrix, x, y, z, nextX, nextY, nextZ,
                    OUTER_HALF_WIDTH * widthScale, red, green, blue, alpha * 0.35F);
            emitCross(vertices, matrix, x, y, z, nextX, nextY, nextZ,
                    CORE_HALF_WIDTH * widthScale,
                    0.8F + red * 0.2F, 0.8F + green * 0.2F, 0.8F + blue * 0.2F, alpha);
            x = nextX;
            y = nextY;
            z = nextZ;
        }
    }

    private static float noise(long seed) {
        long value = (seed ^ (seed >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * SEED_SALT;
        value ^= value >>> 31;
        return (value >>> 40) / 8388607.5F - 1.0F;
    }

    private static void emitCross(VertexConsumer vertices, Matrix4f matrix,
                                  float x, float y, float z, float endX, float endY, float endZ,
                                  float width, float red, float green, float blue, float alpha) {
        for (int plane = 0; plane < 2; plane++) {
            float dx = plane == 0 ? width : 0.0F;
            float dy = plane == 1 ? width : 0.0F;
            for (int side = -1; side <= 1; side += 2) {
                float sx = dx * side;
                float sy = dy * side;
                vertex(vertices, matrix, x - sx, y - sy, z, red, green, blue, alpha);
                vertex(vertices, matrix, endX - sx, endY - sy, endZ, red, green, blue, alpha);
                vertex(vertices, matrix, endX + sx, endY + sy, endZ, red, green, blue, alpha);
                vertex(vertices, matrix, x + sx, y + sy, z, red, green, blue, alpha);
            }
        }
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix,
                               float x, float y, float z,
                               float red, float green, float blue, float alpha) {
        vertices.vertex(matrix, x, y, z).color(red, green, blue, alpha).endVertex();
    }
}
