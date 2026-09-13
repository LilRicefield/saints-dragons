package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class CindervaneFireBodyStarParticle extends TextureSheetParticle {
    private final float size;
    private final float phase;

    private CindervaneFireBodyStarParticle(ClientLevel level, double x, double y, double z,
                                          double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        xd = vx; yd = vy; zd = vz;
        lifetime = 16 + random.nextInt(9);
        size = 0.10F + random.nextFloat() * 0.05F;
        phase = random.nextFloat() * Mth.TWO_PI;
        roll = oRoll = random.nextFloat() * Mth.TWO_PI;
        friction = 0.94F;
        hasPhysics = false;
        quadSize = 0;
        alpha = 0;
        setColor(1.0F, 0.78F, 0.3F);
        pickSprite(sprites);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float time = age + partialTick;
        float progress = Mth.clamp(time / lifetime, 0, 1);
        float envelope = Mth.sin(progress * Mth.PI);
        float twinkle = 0.55F + 0.45F * Mth.sin(time * 0.8F + phase);
        alpha = envelope * twinkle;
        quadSize = size * envelope * (0.7F + 0.3F * twinkle);
        super.render(buffer, camera, partialTick);
    }

    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public record Factory(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new CindervaneFireBodyStarParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
