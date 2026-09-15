package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class VolitansPoisonOrbTrailParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float size;
    private final boolean emitter;
    private VolitansPoisonOrbTrailParticle(ClientLevel level, double x, double y, double z,
                                          double vx, double vy, double vz, SpriteSet sprites, boolean emitter) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.emitter = emitter;
        xd = vx; yd = vy; zd = vz;
        lifetime = emitter ? 18 : 10;
        hasPhysics = false;
        friction = 0.94F;
        size = emitter ? 0.035F + random.nextFloat() * 0.025F : 0.4F + random.nextFloat() * 0.25F;
        setSize(1.6F, 1.6F);
        if (emitter) {
            pickSprite(sprites);
            setColor(0.45F, 1.0F, 0.15F);
        } else setSprite(sprites.get(0, 19));
    }
    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float elapsed = age + partialTick;
        float progress = Mth.clamp(elapsed / lifetime, 0, 1);
        if (!emitter) setSprite(sprites.get(Math.min(19, (int) (elapsed / 0.5F)), 19));
        alpha = Math.min(1, elapsed * 2) * (1 - progress) * (1 - progress);
        quadSize = size * (0.8F + progress * 0.7F);
        super.render(buffer, camera, partialTick);
    }
    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; }
    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    public record Factory(SpriteSet sprites, boolean emitter) implements ParticleProvider<SimpleParticleType> {
        public Factory(SpriteSet sprites) { this(sprites, false); }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new VolitansPoisonOrbTrailParticle(level, x, y, z, vx, vy, vz, sprites, emitter);
        }
    }
}
