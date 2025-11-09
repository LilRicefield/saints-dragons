package com.leon.saintsdragons.client.renderer.layer.ignivorus;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Lightweight particle-only layer that spawns vanilla flame/lava particles
 * along the Ignivorus fire breath path.
 */
public class IgnivorusFireBreathLayer extends GeoRenderLayer<Ignivorus> {

    private static final double SEGMENT_SPACING = 0.7D;
    private static final int MIN_SEGMENTS = 8;
    private static final int PARTICLES_PER_SEGMENT = 5;

    public IgnivorusFireBreathLayer() {
        super(null);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, Ignivorus animatable, BakedGeoModel bakedModel,
                       @NotNull RenderType renderType, @NotNull MultiBufferSource bufferSource,
                       @NotNull VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        if (!animatable.isBreathingFire()) {
            return;
        }
        Vec3 start = animatable.getFireBreathStart();
        Vec3 end = animatable.getFireBreathTarget();
        if (start == null || end == null) {
            return;
        }
        if (!(animatable.level() instanceof ClientLevel clientLevel)) {
            return;
        }

        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.25D) {
            return;
        }
        // Limit spawning to every other tick to avoid particle spam
        if ((animatable.tickCount & 1) == 1) {
            return;
        }

        Vec3 direction = delta.normalize();
        Vec3 right = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        Vec3 up = direction.cross(right).normalize();

        int segments = Math.max(MIN_SEGMENTS, (int) Math.ceil(length / SEGMENT_SPACING));
        double step = length / segments;
        RandomSource random = animatable.getRandom();

        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            double coneRadius = 0.45D + t * 1.35D;
            Vec3 basePos = start.add(direction.scale(step * i));
            for (int p = 0; p < PARTICLES_PER_SEGMENT; p++) {
                spawnConeParticle(clientLevel, basePos, direction, right, up, coneRadius, random);
            }
        }
    }

    private static void spawnConeParticle(ClientLevel level,
                                          Vec3 center,
                                          Vec3 forward,
                                          Vec3 right,
                                          Vec3 up,
                                          double coneRadius,
                                          RandomSource random) {

        double radial = coneRadius * (0.5D + random.nextDouble() * 0.5D);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double offsetX = Math.cos(angle) * radial;
        double offsetY = Math.sin(angle) * radial * 0.6D;

        Vec3 offset = right.scale(offsetX).add(up.scale(offsetY));
        Vec3 pos = center.add(offset);

        level.addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, forward.x * 0.02D, forward.y * 0.02D, forward.z * 0.02D);

        if (random.nextFloat() < 0.55F) {
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.x, pos.y, pos.z, 0.0D, 0.0D, 0.0D);
        }
        if (random.nextFloat() < 0.25F) {
            level.addParticle(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 0.0D, 0.015D, 0.0D);
        }
        if (random.nextFloat() < 0.35F) {
            level.addParticle(ParticleTypes.SMOKE, pos.x, pos.y + 0.05D, pos.z, 0.0D, 0.01D, 0.0D);
        }
    }
}
