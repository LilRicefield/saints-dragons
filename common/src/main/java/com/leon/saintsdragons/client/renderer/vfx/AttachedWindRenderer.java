package com.leon.saintsdragons.client.renderer.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class AttachedWindRenderer {
    private static final float EMISSION_INTERVAL_TICKS = 3.0F;
    private static final float LIFETIME_TICKS = 8.0F;
    private static final float MAX_ALPHA = 0.85F;
    private static final float START_HALF_SIZE = 2.0F;
    private static final float END_HALF_SIZE = 5.0F;
    private static final float FORWARD_OFFSET = 1.0F;
    private static final long PULSE_SEED = 0x9E3779B97F4A7C15L;

    private AttachedWindRenderer() {
    }

    public static final class State {
        private final List<Pulse> pulses = new ArrayList<>();
        private float nextSpawnTime;
        private float lastTime = Float.NaN;
        private boolean emitting;
        private long sequence;

        public void update(float time, boolean emit, long seed, int textureCount) {
            if (!Float.isNaN(lastTime) && time < lastTime) {
                pulses.clear();
                emitting = false;
            }
            pulses.removeIf(pulse -> time - pulse.birthTime() >= LIFETIME_TICKS);
            if (emit) {
                if (!emitting || time - nextSpawnTime >= LIFETIME_TICKS) {
                    nextSpawnTime = time;
                }
                while (nextSpawnTime <= time) {
                    RandomSource random = RandomSource.create(seed ^ PULSE_SEED * ++sequence);
                    for (int texture = 0; texture < textureCount; texture++) {
                        pulses.add(new Pulse(nextSpawnTime, texture,
                                random.nextFloat() * Mth.TWO_PI,
                                Mth.lerp(random.nextFloat(), 0.9F, 1.1F)));
                    }
                    nextSpawnTime += EMISSION_INTERVAL_TICKS;
                }
            }
            emitting = emit;
            lastTime = time;
        }

        public boolean isActive() {
            return !pulses.isEmpty();
        }
    }

    private record Pulse(float birthTime, int textureIndex, float angle, float scale) {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers,
                              ResourceLocation[] textures, State state, float time) {
        Matrix4f matrix = poseStack.last().pose();
        Vector3f normal = poseStack.last().normal().transform(new Vector3f(0, 0, 1)).normalize();
        for (Pulse pulse : state.pulses) {
            float life = Mth.clamp((time - pulse.birthTime()) / LIFETIME_TICKS, 0.0F, 1.0F);
            float alpha = MAX_ALPHA * smoothStep(life / 0.2F)
                    * (1.0F - smoothStep((life - 0.4F) / 0.6F));
            if (alpha <= 0.001F || pulse.textureIndex() >= textures.length) {
                continue;
            }
            float halfSize = Mth.lerp(1.0F - (1.0F - life) * (1.0F - life),
                    START_HALF_SIZE, END_HALF_SIZE) * pulse.scale();
            float cos = Mth.cos(pulse.angle());
            float sin = Mth.sin(pulse.angle());
            float z = FORWARD_OFFSET + pulse.textureIndex() * 0.025F;
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(textures[pulse.textureIndex()]));
            vertex(consumer, matrix, normal, -halfSize, -halfSize, z, cos, sin, 0, 1, alpha);
            vertex(consumer, matrix, normal, halfSize, -halfSize, z, cos, sin, 1, 1, alpha);
            vertex(consumer, matrix, normal, halfSize, halfSize, z, cos, sin, 1, 0, alpha);
            vertex(consumer, matrix, normal, -halfSize, halfSize, z, cos, sin, 0, 0, alpha);
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vector3f normal,
                               float x, float y, float z, float cos, float sin,
                               float u, float v, float alpha) {
        consumer.vertex(matrix, x * cos - y * sin, x * sin + y * cos, z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal.x(), normal.y(), normal.z())
                .endVertex();
    }

    private static float smoothStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
