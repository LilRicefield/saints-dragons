package com.leon.saintsdragons.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public final class AtroxiiaSnowShardParticle extends AtroxiiaWindParticle {
    private final float spinSpeed;

    private AtroxiiaSnowShardParticle(ClientLevel level, double x, double y, double z,
                                      double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, 0.012D, 0.012D);
        this.lifetime = 28 + this.random.nextInt(17);
        this.quadSize = 0.23F + this.random.nextFloat() * 0.05F;
        this.spinSpeed = (this.random.nextBoolean() ? 1.0F : -1.0F)
                * (0.12F + this.random.nextFloat() * 0.18F);
        this.roll = this.random.nextFloat() * (float) (Math.PI * 2.0D);
        this.oRoll = this.roll;
        this.alpha = 0.0F;
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

        this.alpha = fadeAlpha(0.92F, 0.08F, 0.72F);
        this.roll += this.spinSpeed;
        moveInWind(0.945F, 0.92F, -0.002D);
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
            return new AtroxiiaSnowShardParticle(
                    level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites
            );
        }
    }
}
