package com.leon.saintsdragons.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public final class GlowingEmitterParticle extends TextureSheetParticle {
    private final double swayAxisX;
    private final double swayAxisZ;
    private final double swayPhase;
    private final double swayFrequency;
    private final double swayStrength;
    private final float startSize;

    private GlowingEmitterParticle(ClientLevel level, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.hasPhysics = false;
        this.lifetime = 24 + this.random.nextInt(13);
        this.startSize = 0.025F + this.random.nextFloat() * 0.025F;
        this.quadSize = this.startSize;
        this.alpha = 0.92F;

        double horizontalSpeed = Math.sqrt(xSpeed * xSpeed + zSpeed * zSpeed);
        if (horizontalSpeed > 1.0E-4D) {
            this.swayAxisX = -zSpeed / horizontalSpeed;
            this.swayAxisZ = xSpeed / horizontalSpeed;
        } else {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            this.swayAxisX = Math.cos(angle);
            this.swayAxisZ = Math.sin(angle);
        }
        this.swayPhase = this.random.nextDouble() * Math.PI * 2.0D;
        this.swayFrequency = 0.42D + this.random.nextDouble() * 0.22D;
        this.swayStrength = 0.009D + this.random.nextDouble() * 0.009D;
        this.setSprite(sprites.get(this.random));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float progress = this.age / (float)this.lifetime;
        double wave = Math.sin(this.swayPhase + this.age * this.swayFrequency) * this.swayStrength;
        this.move(
                this.xd + this.swayAxisX * wave,
                this.yd + Math.cos(this.swayPhase + this.age * 0.37D) * this.swayStrength * 0.45D,
                this.zd + this.swayAxisZ * wave
        );
        this.xd *= 0.92D;
        this.yd *= 0.92D;
        this.zd *= 0.92D;
        this.alpha = 0.92F * Math.max(0.0F, 1.0F - progress);
        this.quadSize = this.startSize * (0.75F + 0.25F * (1.0F - progress));
    }

    public void enableTerrainCollision() {
        this.hasPhysics = true;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240 | super.getLightColor(partialTick) & 0xFF0000;
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
            return new GlowingEmitterParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
