package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class CindervaneImpactEmitterParticle extends TextureSheetParticle {
    private final float size;

    private CindervaneImpactEmitterParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double rise = 0.1 + random.nextDouble() * 0.75;
        double horizontal = Math.sqrt(1.0 - rise * rise);
        double speed = 0.22 + random.nextDouble() * 0.28;
        this.xd = Math.cos(angle) * horizontal * speed;
        this.yd = rise * speed;
        this.zd = Math.sin(angle) * horizontal * speed;
        this.size = 0.035F + random.nextFloat() * 0.025F;
        this.quadSize = size;
        this.lifetime = 24 + random.nextInt(13);
        this.friction = 0.93F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        setColor(1.0F, 0.72F, 0.16F);
        pickSprite(sprites);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float progress = Mth.clamp((age + partialTick) / lifetime, 0.0F, 1.0F);
        float fade = Mth.clamp((progress - 0.15F) / 0.85F, 0.0F, 1.0F);
        alpha = 0.9F * (1.0F - fade * fade * (3.0F - 2.0F * fade));
        quadSize = size * (1.0F - progress * 0.35F);
        super.render(buffer, camera, partialTick);
    }

    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public record MouthFactory(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            var particle = new CindervaneImpactEmitterParticle(level, x, y, z, sprites);
            particle.xd = vx;
            particle.yd = vy;
            particle.zd = vz;
            return particle;
        }
    }

    public record Factory(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new CindervaneImpactEmitterParticle(level, x, y, z, sprites);
        }
    }
}
