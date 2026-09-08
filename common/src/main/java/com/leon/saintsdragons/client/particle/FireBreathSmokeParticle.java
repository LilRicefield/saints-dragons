package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class FireBreathSmokeParticle extends TextureSheetParticle {
    private static final int FRAMES = 14;
    private static final float FRAME_TICKS = 0.85F;
    private static final float ANIMATION_TICKS = FRAMES * FRAME_TICKS;
    private final SpriteSet sprites;
    private final float peakSize;
    private final float spin;

    private FireBreathSmokeParticle(ClientLevel level, double x, double y, double z,
                                    double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        xd = vx;
        yd = vy;
        zd = vz;
        lifetime = Mth.ceil(ANIMATION_TICKS);
        peakSize = 1.8F + random.nextFloat() * 0.8F;
        spin = (random.nextFloat() - 0.5F) * 0.025F;
        roll = oRoll = random.nextFloat() * Mth.TWO_PI;
        hasPhysics = true;
        setSize(0.2F, 0.2F);
        quadSize = peakSize * 0.45F;
        alpha = 0.25F;
        setSprite(sprites.get(0, FRAMES - 1));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        oRoll = roll;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        roll += spin;
        move(xd, yd, zd);
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        float renderAge = Math.max(0, age - 1 + partialTicks);
        if (renderAge >= ANIMATION_TICKS) return;
        setSprite(sprites.get(Math.min((int) (renderAge / FRAME_TICKS), FRAMES - 1), FRAMES - 1));
        float progress = Mth.clamp(renderAge / ANIMATION_TICKS, 0, 1);
        float grow = smooth(Mth.clamp(progress / 0.35F, 0, 1));
        float shrink = 1 - smooth(Mth.clamp((progress - 0.35F) / 0.65F, 0, 1));
        quadSize = peakSize * Mth.lerp(grow, 0.45F, 1.0F) * shrink * shrink;
        super.render(buffer, camera, partialTicks);
    }

    private static float smooth(float value) {
        return value * value * (3 - 2 * value);
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
            return new FireBreathSmokeParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
