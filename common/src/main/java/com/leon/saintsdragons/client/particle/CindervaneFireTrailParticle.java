package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class CindervaneFireTrailParticle extends TextureSheetParticle {
    public enum Kind { FIRE, SPEC, BETTER_FIRE, FIREBALL_FIRE }

    private final SpriteSet sprites;
    private final float ticksPerFrame;
    private final int frames;
    private final float size;

    private CindervaneFireTrailParticle(ClientLevel level, double x, double y, double z,
                                        double vx, double vy, double vz, SpriteSet sprites, Kind kind) {
        super(level, x, y, z);
        this.sprites = sprites;
        boolean spec = kind == Kind.SPEC;
        boolean fireball = kind == Kind.FIREBALL_FIRE;
        this.frames = fireball || kind == Kind.BETTER_FIRE ? 8 : spec ? 12 : 17;
        this.ticksPerFrame = fireball ? 0.25F : kind == Kind.BETTER_FIRE ? 1.0F : 0.5F;
        this.lifetime = (int) Math.ceil(frames * ticksPerFrame);
        this.size = (spec ? 0.36F : 0.46F) * (0.8F + random.nextFloat() * 0.4F);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = 0.90F;
        this.hasPhysics = false;
        this.quadSize = size;
        setColor(1.0F, spec ? 0.78F : fireball ? 0.60F : 0.48F, spec ? 0.28F : fireball ? 0.12F : 0.08F);
        setSprite(sprites.get(0, frames - 1));
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float time = age + partialTick;
        float progress = Mth.clamp(time / lifetime, 0.0F, 1.0F);
        float fade = Mth.clamp((progress - 0.35F) / 0.65F, 0.0F, 1.0F);
        this.alpha = 0.9F * (1.0F - fade * fade * (3.0F - 2.0F * fade));
        this.quadSize = size * (1.0F + progress * 0.35F);
        setSprite(sprites.get(Math.min(frames - 1, (int) (time / ticksPerFrame)), frames - 1));
        super.render(buffer, camera, partialTick);
    }

    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public record Factory(SpriteSet sprites, Kind kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new CindervaneFireTrailParticle(level, x, y, z, vx, vy, vz, sprites, kind);
        }
    }
}
