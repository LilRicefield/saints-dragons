package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class AtroxiiaIceBurstParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float angle;

    private AtroxiiaIceBurstParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        lifetime = 14;
        hasPhysics = false;
        angle = random.nextFloat() * Mth.TWO_PI;
        setSprite(sprites.get(0, 13));
        setColor(1, 1, 1);
        setBoundingBox(new AABB(x - 24, y - 1, z - 24, x + 24, y + 1, z + 24));
    }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        if (age == 0) {
            var engine = Minecraft.getInstance().particleEngine;
            for (int i = 0; i < 64; i++) {
                double direction = (i + random.nextDouble()) * Math.PI * 2 / 64;
                double radius = 2 + Math.sqrt(random.nextDouble()) * 10;
                double dx = Math.cos(direction), dz = Math.sin(direction);
                var particle = engine.createParticle(ModParticles.IGNIVORUS_CHARGED_STAR_TRAIL.get(),
                        x + dx * radius, y + 0.3 + random.nextDouble() * 2, z + dz * radius,
                        dx * 0.3, 0.02 + random.nextDouble() * 0.08, dz * 0.3);
                if (particle instanceof IgnivorusChargedFireballTrailParticle star) {
                    star.lingerAfterImpact();
                    star.setStarColor(0.8F, 0.93F, 1.0F);
                }
            }
            for (int i = 0; i < 48; i++) {
                double direction = (i + random.nextDouble()) * Math.PI * 2 / 48;
                engine.createParticle(ModParticles.ATROXIIA_QUAKE_SMOKE.get(), x, y + 0.25, z,
                        Math.cos(direction), 0, Math.sin(direction));
            }
            for (int i = 0; i < 320; i++) {
                double direction = (i + random.nextDouble()) * Math.PI * 2 / 320;
                double dx = Math.cos(direction), dz = Math.sin(direction);
                double speed = 0.7 + random.nextDouble() * 1.4;
                double radius = 0.5 + random.nextDouble() * 2;
                var emitter = engine.createParticle(ModParticles.CINDERVANE_MOUTH_EMITTER.get(),
                        x + dx * radius, y + 0.15, z + dz * radius,
                        dx * speed, 0.05 + random.nextDouble() * 0.3, dz * speed);
                if (emitter != null) {
                    float tint = random.nextFloat();
                    emitter.setColor(0.65F + tint * 0.35F, 0.85F + tint * 0.15F, 1);
                }
                engine.createParticle(ParticleTypes.SNOWFLAKE,
                        x + dx * radius, y + 0.3, z + dz * radius,
                        dx * speed * 0.65, 0.1 + random.nextDouble() * 0.2, dz * speed * 0.65);
            }
        }
        if (++age >= lifetime) remove();
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float time = age + partialTick;
        float progress = Mth.clamp(time / lifetime, 0, 1);
        setSprite(sprites.get(Math.min(13, (int) time), 13));
        float fade = Mth.clamp((1 - progress) / 0.45F, 0, 1);
        alpha = Math.min(1, time * 2) * fade * fade * (3 - 2 * fade);
        quadSize = Mth.lerp(1 - (1 - progress) * (1 - progress), 5, 16);
        Vec3 center = new Vec3(x, y, z).subtract(camera.getPosition());
        corner(buffer, center, -1, -1, getU0(), getV0());
        corner(buffer, center, -1, 1, getU0(), getV1());
        corner(buffer, center, 1, 1, getU1(), getV1());
        corner(buffer, center, 1, -1, getU1(), getV0());
    }

    private void corner(VertexConsumer buffer, Vec3 center, float dx, float dz, float u, float v) {
        double px = (dx * Math.cos(angle) - dz * Math.sin(angle)) * quadSize;
        double pz = (dx * Math.sin(angle) + dz * Math.cos(angle)) * quadSize;
        buffer.vertex(center.x + px, center.y, center.z + pz).uv(u, v)
                .color(rCol, gCol, bCol, alpha).uv2(0xF000F0).endVertex();
    }

    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public record Factory(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new AtroxiiaIceBurstParticle(level, x, y, z, sprites);
        }
    }
}
