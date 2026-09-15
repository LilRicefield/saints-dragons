package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
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

public final class IgnivorusNovaSparkleParticle extends TextureSheetParticle {
    private final float size;
    private final float spin;
    private final float phase;
    private final float frequency;
    private final ParticleRenderType renderType;

    private IgnivorusNovaSparkleParticle(ClientLevel level, double x, double y, double z,
                                        double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        xd = vx;
        yd = vy;
        zd = vz;
        hasPhysics = false;
        lifetime = random.nextFloat() < 0.7F ? 55 + random.nextInt(36) : 16 + random.nextInt(10);
        boolean widerStar = random.nextBoolean();
        size = (0.9F + random.nextFloat() * 1.35F) * (widerStar ? 0.4F : 1.0F);
        spin = (random.nextBoolean() ? 1.0F : -1.0F) * (0.04F + random.nextFloat() * 0.09F);
        phase = random.nextFloat() * Mth.TWO_PI;
        frequency = 0.35F + random.nextFloat() * 0.3F;
        roll = oRoll = phase;
        alpha = 0.0F;
        quadSize = size;
        setColor(1.0F, 0.72F + random.nextFloat() * 0.13F, 0.25F);
        setSprite(sprites.get(widerStar ? 1 : 0, 1));
        renderType = ShaderPassCompatibility.isShaderPackInUse()
                ? ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
                : DragonParticleRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE;
        updateBounds();
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        oRoll = roll;
        if (++age >= lifetime) {
            remove();
            return;
        }
        x += xd;
        y += yd;
        z += zd;
        double drag = age < 10 ? 0.92D : 0.78D;
        xd *= drag;
        yd *= drag;
        zd *= drag;
        roll += spin;
        updateBounds();
    }

    private void updateBounds() {
        double radius = size * 2.0D;
        setBoundingBox(new AABB(x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius));
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        float elapsed = age + partialTicks;
        float fadeIn = Mth.clamp(elapsed / 2.0F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((lifetime - elapsed) / 14.0F, 0.0F, 1.0F);
        float wave = 0.5F + 0.5F * Mth.sin(phase + elapsed * frequency);
        float twinkle = wave * wave * wave;
        float settle = Mth.clamp((elapsed - 6.0F) / 8.0F, 0.0F, 1.0F);
        alpha = fadeIn * fadeOut * Mth.lerp(settle, 1.0F, 0.18F + twinkle * 0.82F);
        quadSize = size * (0.75F + 0.35F * twinkle) * (0.65F + 0.35F * fadeOut);
        super.render(buffer, camera, partialTicks);
    }

    @Override
    public int getLightColor(float partialTicks) {
        return 0xF000F0;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return renderType;
    }

    public static final class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new IgnivorusNovaSparkleParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
