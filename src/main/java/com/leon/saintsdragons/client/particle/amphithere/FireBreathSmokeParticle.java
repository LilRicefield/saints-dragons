package com.leon.saintsdragons.client.particle.amphithere;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class FireBreathSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private float prevAlpha = 0.0F;

    protected FireBreathSmokeParticle(ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet spriteSet) {
        super(world, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = spriteSet;
        this.setSpriteFromAge(this.sprites);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.setSize(0.4F, 0.4F);
        this.quadSize = 0.8F + world.random.nextFloat() * 0.4F;
        this.lifetime = 20 + world.random.nextInt(20);
        this.friction = 0.96F;
        
        // Smoke colors - dark gray to black
        this.rCol = 0.2F + random.nextFloat() * 0.2F;
        this.gCol = 0.2F + random.nextFloat() * 0.2F;
        this.bCol = 0.2F + random.nextFloat() * 0.2F;
    }

    public void tick() {
        this.setSpriteFromAge(this.sprites);
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        float ageProgress = this.age / (float) lifetime;
        float f = ageProgress - 0.2F;
        float f1 = 1.0F - f * 1.25F;
        
        // Fade out in the last 80% of lifetime
        if (ageProgress > 0.2F) {
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
            this.yd += 0.005D;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public int getLightColor(float partialTicks) {
        return 0; // No light emission for smoke
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(@org.jetbrains.annotations.NotNull SimpleParticleType typeIn, @org.jetbrains.annotations.NotNull ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            FireBreathSmokeParticle particle = new FireBreathSmokeParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet);
            return particle;
        }
    }
}
