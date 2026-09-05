package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class ImpactGlowParticle extends TextureSheetParticle {
    private static final int LIFETIME_TICKS = 40;

    private final float maximumAlpha;

    private ImpactGlowParticle(ClientLevel level, double x, double y, double z,
                               SpriteSet sprites, float size, float maximumAlpha) {
        super(level, x, y, z);
        this.lifetime = LIFETIME_TICKS;
        this.quadSize = size;
        this.maximumAlpha = maximumAlpha;
        this.alpha = maximumAlpha;
        this.hasPhysics = false;
        this.setSprite(sprites.get(this.random));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (++this.age >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float progress = Mth.clamp((this.age + partialTicks) / this.lifetime, 0.0F, 1.0F);
        float remaining = 1.0F - progress;
        this.alpha = this.maximumAlpha * remaining * remaining * (3.0F - 2.0F * remaining);
        super.render(buffer, camera, partialTicks);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240 | super.getLightColor(partialTick) & 0xFF0000;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return DragonParticleRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE;
    }

    public static final class RedGlowFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public RedGlowFactory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new ImpactGlowParticle(level, x, y, z, this.sprites, 4.5F, 0.2F);
        }
    }

    public static final class RainbowFlareFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public RainbowFlareFactory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new ImpactGlowParticle(level, x, y, z, this.sprites, 3.7F, 0.05F);
        }
    }
}
