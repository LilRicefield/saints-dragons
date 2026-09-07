package com.leon.saintsdragons.client.renderer.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class AttachedBillboardFlipbookRenderer {
    private static final long PULSE_SEED = 0x9E3779B97F4A7C15L;

    private AttachedBillboardFlipbookRenderer() {
    }

    public static final class State {
        private final List<Pulse> pulses = new ArrayList<>();
        private float nextSpawnTime;
        private float lastTime = Float.NaN;
        private boolean emitting;
        private long sequence;

        public void update(float time, boolean emit, long seed, int frameCount, Style style) {
            if (!Float.isNaN(lastTime) && time < lastTime) {
                pulses.clear();
                emitting = false;
            }
            float lifetime = frameCount * Math.max(0.05F, style.frameTicks());
            pulses.removeIf(pulse -> time - pulse.birthTime() >= lifetime);
            if (emit && frameCount > 0) {
                if (!emitting || time - nextSpawnTime >= lifetime) {
                    nextSpawnTime = time;
                }
                while (nextSpawnTime <= time) {
                    RandomSource random = RandomSource.create(seed ^ PULSE_SEED * ++sequence);
                    float angle = -Mth.lerp(random.nextFloat(),
                            style.minimumRotationDegrees(), style.maximumRotationDegrees()) * Mth.DEG_TO_RAD;
                    float minimumRadius = Math.max(0.0F, style.minimumSpawnRadius());
                    float maximumRadius = Math.max(minimumRadius, style.maximumSpawnRadius());
                    float radius = Mth.lerp(random.nextFloat(), minimumRadius, maximumRadius);
                    float vertical = random.nextFloat() * 2.0F - 1.0F;
                    float azimuth = random.nextFloat() * Mth.TWO_PI;
                    float horizontal = Mth.sqrt(Math.max(0.0F, 1.0F - vertical * vertical));
                    pulses.add(new Pulse(nextSpawnTime, angle,
                            Mth.cos(azimuth) * horizontal * radius, vertical * radius,
                            Mth.sin(azimuth) * horizontal * radius));
                    nextSpawnTime += Math.max(0.5F, style.emissionIntervalTicks());
                }
            }
            emitting = emit;
            lastTime = time;
        }

        public boolean isActive() {
            return !pulses.isEmpty();
        }
    }

    private record Pulse(float birthTime, float angle, float offsetX, float offsetY, float offsetZ) {
    }

    public static void render(PoseStack entityPose, MultiBufferSource buffers,
                              ResourceLocation[] textures, State state, Style style, float time,
                              float centerX, float centerY, float centerZ,
                              float red, float green, float blue) {
        float frameTicks = Math.max(0.05F, style.frameTicks());
        float lifetime = textures.length * frameTicks;
        Quaternionf worldToBillboard = style.aimAtSource()
                ? new Quaternionf(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation()).conjugate()
                : null;
        for (Pulse pulse : state.pulses) {
            float age = time - pulse.birthTime();
            if (age < 0.0F || age >= lifetime) {
                continue;
            }
            int frame = Math.min(textures.length - 1, Mth.floor(age / frameTicks));
            float alpha = Mth.clamp(style.alpha(), 0.0F, 1.0F);
            float angle = pulse.angle();
            if (worldToBillboard != null) {
                Vector3f inward = worldToBillboard.transform(
                        new Vector3f(-pulse.offsetX(), -pulse.offsetY(), -pulse.offsetZ()));
                if (inward.x() * inward.x() + inward.y() * inward.y() > 1.0E-6F) {
                    angle = (float) Mth.atan2(inward.y(), inward.x());
                }
            }
            BillboardFlashRenderer.renderAnchoredQuad(entityPose, buffers, textures[frame],
                    centerX + pulse.offsetX(), centerY + pulse.offsetY(), centerZ + pulse.offsetZ(),
                    style.halfSize(), angle,
                    red, green, blue, alpha, style.anchorU(), style.anchorV());
        }
    }

    public record Style(float halfSize, float alpha, float frameTicks, float emissionIntervalTicks,
                        float minimumRotationDegrees, float maximumRotationDegrees,
                        float minimumSpawnRadius, float maximumSpawnRadius, boolean aimAtSource,
                        float anchorU, float anchorV) {
    }
}
