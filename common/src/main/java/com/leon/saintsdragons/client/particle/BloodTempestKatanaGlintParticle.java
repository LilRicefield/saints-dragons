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

public final class BloodTempestKatanaGlintParticle extends TextureSheetParticle {
    private static final int DELAY_TICKS = 4;
    private static final int ACTIVE_TICKS = 7;
    private static final float START_SIZE = 8.0F;

    private final float spinSpeed;

    private BloodTempestKatanaGlintParticle(ClientLevel level, double x, double y, double z,
                                            SpriteSet sprites) {
        super(level, x, y, z);
        this.lifetime = DELAY_TICKS + ACTIVE_TICKS;
        this.quadSize = START_SIZE;
        this.alpha = 0.0F;
        this.hasPhysics = false;
        this.roll = this.random.nextFloat() * (float)(Math.PI * 2.0D);
        this.oRoll = this.roll;
        this.spinSpeed = (this.random.nextBoolean() ? 1.0F : -1.0F) * 0.42F;
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

        if (this.age > DELAY_TICKS) {
            this.roll += this.spinSpeed;
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float activeAge = this.age + partialTicks - DELAY_TICKS;
        if (activeAge <= 0.0F) {
            return;
        }

        float progress = Mth.clamp(activeAge / ACTIVE_TICKS, 0.0F, 1.0F);
        float fade = progress < 0.22F
                ? progress / 0.22F
                : 1.0F - (progress - 0.22F) / 0.78F;
        fade = Mth.clamp(fade, 0.0F, 1.0F);
        this.alpha = fade * fade * (3.0F - 2.0F * fade);
        float shrink = 1.0F - progress;
        this.quadSize = START_SIZE * Math.max(0.08F, shrink * shrink);
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
            return new BloodTempestKatanaGlintParticle(level, x, y, z, this.sprites);
        }
    }
}
