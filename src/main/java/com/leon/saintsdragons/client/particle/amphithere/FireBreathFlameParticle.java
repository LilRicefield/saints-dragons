package com.leon.saintsdragons.client.particle.amphithere;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class FireBreathFlameParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private float prevAlpha = 0.0F;
    private boolean spinning;
    private float spinIncrement;

    protected FireBreathFlameParticle(ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet spriteSet, boolean spinning) {
        super(world, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = spriteSet;
        this.setSpriteFromAge(this.sprites);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.setSize(0.3F, 0.3F);
        this.quadSize = 0.5F + world.random.nextFloat() * 0.3F;
        this.lifetime = 15 + world.random.nextInt(15);
        this.friction = 0.98F;
        this.spinning = spinning;
        if (spinning) {
            this.roll = (float) Math.toRadians(360F * random.nextFloat());
            this.oRoll = roll;
            spinIncrement = (random.nextBoolean() ? -1 : 1) * random.nextFloat() * 0.3F;
        }
        
        // Fire colors - bright orange to red
        this.rCol = 1.0F;
        this.gCol = 0.6F + random.nextFloat() * 0.3F;
        this.bCol = 0.1F + random.nextFloat() * 0.2F;
    }

    public void tick() {
        this.setSpriteFromAge(this.sprites);
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        float ageProgress = this.age / (float) lifetime;
        float f = ageProgress - 0.3F;
        float f1 = 1.0F - f * 1.5F;
        
        // Fade out in the last 70% of lifetime
        if (ageProgress > 0.3F) {
            prevAlpha = alpha;
            this.setAlpha(prevAlpha + (f1 - prevAlpha) * 0.1F); // Use fixed partial tick
        }
        
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
            this.xd *= (double) this.friction;
            this.yd *= (double) this.friction;
            this.zd *= (double) this.friction;
            
            // Add some upward drift
            this.yd += 0.01D;
        }
        
        if (spinning) {
            this.oRoll = roll;
            this.roll += f1 * spinIncrement;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public int getLightColor(float partialTicks) {
        return 240; // Full brightness for fire
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(@org.jetbrains.annotations.NotNull SimpleParticleType typeIn, @org.jetbrains.annotations.NotNull ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            FireBreathFlameParticle particle = new FireBreathFlameParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet, true);
            return particle;
        }
    }
}
