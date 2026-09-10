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
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public final class IgnivorusExplosionStarParticle extends TextureSheetParticle {
    private static final float HALF_SIZE = 48.0F;
    private static final float FADE_IN_TICKS = 2.0F;

    private IgnivorusExplosionStarParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        lifetime = 7;
        hasPhysics = false;
        quadSize = HALF_SIZE;
        alpha = 0.0F;
        setColor(1.0F, 0.65F, 0.12F);
        pickSprite(sprites);
        double radius = HALF_SIZE * Math.sqrt(2.0D);
        setBoundingBox(new AABB(x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) remove();
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        float elapsed = age + partialTicks;
        float fade = elapsed < FADE_IN_TICKS
                ? elapsed / FADE_IN_TICKS
                : (lifetime - elapsed) / (lifetime - FADE_IN_TICKS);
        fade = Mth.clamp(fade, 0.0F, 1.0F);
        alpha = fade * fade * (3.0F - 2.0F * fade);
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
            return new IgnivorusExplosionStarParticle(level, x, y, z, sprites);
        }
    }
}
