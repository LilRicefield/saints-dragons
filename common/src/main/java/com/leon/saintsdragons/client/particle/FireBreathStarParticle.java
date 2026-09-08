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

/** Short camera-facing flashes along the backblast, using the beam's star texture. */
public final class FireBreathStarParticle extends TextureSheetParticle {
    private final float peakSize;
    private final float spin;

    private FireBreathStarParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        lifetime = 5 + random.nextInt(4);
        peakSize = 0.6F + random.nextFloat() * 0.5F;
        spin = (random.nextBoolean() ? 1 : -1) * 0.24F;
        roll = oRoll = random.nextFloat() * Mth.TWO_PI;
        hasPhysics = false;
        setSize(2.2F, 2.2F);
        setColor(1.0F, 0.65F, 0.12F);
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        oRoll = roll;
        roll += spin;
        if (age++ >= lifetime) remove();
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        float progress = Mth.clamp(Math.max(0, age - 1 + partialTicks) / (lifetime - 1.0F), 0, 1);
        float fade = 1 - progress * progress * (3 - 2 * progress);
        quadSize = peakSize * fade;
        alpha = 0.9F * Mth.clamp(progress / 0.15F, 0, 1) * fade;
        super.render(buffer, camera, partialTicks);
    }

    @Override
    public int getLightColor(float partialTicks) {
        return 0xF000F0;
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
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new FireBreathStarParticle(level, x, y, z, sprites);
        }
    }
}
