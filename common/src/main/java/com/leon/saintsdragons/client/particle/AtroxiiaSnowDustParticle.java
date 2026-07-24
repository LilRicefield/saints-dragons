package com.leon.saintsdragons.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public final class AtroxiiaSnowDustParticle extends TextureSheetParticle {
    private static final int LIFETIME_TICKS = 36;
    private static final float PARTICLE_SCALE = 0.86F;

    private final float spinSpeed;

    private AtroxiiaSnowDustParticle(ClientLevel level, double x, double y, double z,
                                     double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.hasPhysics = false;
        this.lifetime = LIFETIME_TICKS;
        this.quadSize = PARTICLE_SCALE;
        this.spinSpeed = (this.random.nextBoolean() ? 1.0F : -1.0F)
                * (0.07F + this.random.nextFloat() * 0.07F);
        this.roll = this.random.nextFloat() * (float) (Math.PI * 2.0D);
        this.oRoll = this.roll;
        this.alpha = 0.0F;

        // The four dust sprites are static variants, not animation frames.
        this.setSprite(sprites.get(this.random));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float progress = this.age / (float) this.lifetime;
        if (progress < 0.08F) {
            this.alpha = 0.88F * progress / 0.08F;
        } else if (progress > 0.64F) {
            this.alpha = 0.88F * Math.max(0.0F, 1.0F - (progress - 0.64F) / 0.36F);
        } else {
            this.alpha = 0.88F;
        }

        this.roll += this.spinSpeed;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.94F;
        this.yd = this.yd * 0.93F + 0.0004D;
        this.zd *= 0.94F;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new AtroxiiaSnowDustParticle(
                    level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites
            );
        }
    }
}
