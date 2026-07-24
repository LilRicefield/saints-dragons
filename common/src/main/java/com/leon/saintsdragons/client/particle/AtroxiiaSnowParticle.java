package com.leon.saintsdragons.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public final class AtroxiiaSnowParticle extends AtroxiiaWindParticle {
    private static final int SNOW_FRAME_COUNT = 4;
    private static final int SPARK_FRAME_COUNT = 6;
    private static final int SNOW_FRAME_TICKS = 6;
    private static final int SPARK_FRAME_TICKS = 6;

    private final SpriteSet sprites;
    private final Style style;
    private final float spinSpeed;

    private enum Style {
        FLAKE,
        SPARK
    }

    private AtroxiiaSnowParticle(ClientLevel level, double x, double y, double z,
                                 double xSpeed, double ySpeed, double zSpeed,
                                 SpriteSet sprites, Style style) {
        super(
                level, x, y, z, xSpeed, ySpeed, zSpeed,
                style == Style.SPARK ? 0.018D : 0.026D,
                style == Style.SPARK ? 0.016D : 0.024D
        );
        this.sprites = sprites;
        this.style = style;
        if (style == Style.SPARK) {
            this.lifetime = 18 + this.random.nextInt(11);
            this.quadSize = 0.34F + this.random.nextFloat() * 0.06F;
            this.spinSpeed = (this.random.nextBoolean() ? 1.0F : -1.0F)
                    * (0.055F + this.random.nextFloat() * 0.07F);
            this.roll = this.random.nextFloat() * (float) (Math.PI * 2.0D);
        } else {
            this.lifetime = 34 + this.random.nextInt(21);
            this.quadSize = 0.34F + this.random.nextFloat() * 0.08F;
            this.spinSpeed = (this.random.nextBoolean() ? 1.0F : -1.0F)
                    * (0.012F + this.random.nextFloat() * 0.025F);
            this.roll = this.random.nextFloat() * (float) (Math.PI * 2.0D);
        }
        this.oRoll = this.roll;
        this.alpha = 0.0F;
        setAnimatedSprite();
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

        setAnimatedSprite();
        this.roll += this.spinSpeed;
        if (this.style == Style.SPARK) {
            this.alpha = fadeAlpha(0.95F, 0.08F, 0.58F);
            moveInWind(0.94F, 0.91F, 0.0015D);
        } else {
            this.alpha = fadeAlpha(0.86F, 0.12F, 0.68F);
            moveInWind(0.965F, 0.94F, 0.0006D);
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    private void setAnimatedSprite() {
        int frameCount = this.style == Style.SPARK ? SPARK_FRAME_COUNT : SNOW_FRAME_COUNT;
        int frameTicks = this.style == Style.SPARK ? SPARK_FRAME_TICKS : SNOW_FRAME_TICKS;
        int frame = Math.min(this.age / frameTicks, frameCount - 1);
        this.setSprite(this.sprites.get(frame, frameCount - 1));
    }

    public static final class FlakeFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public FlakeFactory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new AtroxiiaSnowParticle(
                    level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, Style.FLAKE
            );
        }
    }

    public static final class SparkFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SparkFactory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new AtroxiiaSnowParticle(
                    level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, Style.SPARK
            );
        }
    }

}
