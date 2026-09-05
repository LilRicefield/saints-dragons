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

public final class BloodTempestKatanaXMarkParticle extends TextureSheetParticle {
    private static final int LIFETIME_TICKS = 4;
    private static final float MAX_ALPHA = 0.95F;
    private static final float SIZE = 5.5F;

    private BloodTempestKatanaXMarkParticle(ClientLevel level, double x, double y, double z,
                                            SpriteSet sprites) {
        super(level, x, y, z);
        this.lifetime = LIFETIME_TICKS;
        this.quadSize = SIZE;
        this.alpha = 0.0F;
        this.hasPhysics = false;
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
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float progress = Mth.clamp((this.age + partialTicks) / this.lifetime, 0.0F, 1.0F);
        float fade = progress < 0.34F
                ? progress / 0.34F
                : 1.0F - (progress - 0.34F) / 0.66F;
        fade = Mth.clamp(fade, 0.0F, 1.0F);
        this.alpha = MAX_ALPHA * fade * fade * (3.0F - 2.0F * fade);
        super.render(buffer, camera, partialTicks);
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
            return new BloodTempestKatanaXMarkParticle(level, x, y, z, this.sprites);
        }
    }
}
