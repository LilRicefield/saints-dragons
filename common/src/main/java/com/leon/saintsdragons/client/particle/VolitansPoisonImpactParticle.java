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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class VolitansPoisonImpactParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final boolean cloud;
    private final float scale;
    private final int frames;
    private final float frameTicks;

    private VolitansPoisonImpactParticle(ClientLevel level, double x, double y, double z,
                                         double scale, SpriteSet sprites, boolean cloud) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.cloud = cloud;
        this.scale = (float) Mth.clamp(scale, 0.25D, 8.0D);
        frames = cloud ? 15 : 17;
        frameTicks = cloud ? 2.0F : 0.75F;
        lifetime = Mth.ceil(frames * frameTicks);
        hasPhysics = false;
        setSize(10.0F * this.scale, 10.0F * this.scale);
        setSprite(sprites.get(0, frames - 1));
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
        float time = age + partialTicks;
        float duration = frames * frameTicks;
        if (time >= duration) return;
        float progress = Mth.clamp(time / duration, 0, 1);
        setSprite(sprites.get(Math.min(frames - 1, Mth.floor(time / frameTicks)), frames - 1));
        if (cloud) {
            float grow = 1.0F - (1.0F - progress) * (1.0F - progress);
            quadSize = Mth.lerp(grow, 1.8F, 5.0F) * scale;
            float fade = Mth.clamp((progress - 0.2F) / 0.8F, 0, 1);
            alpha = 0.9F * (1.0F - fade * fade * (3.0F - 2.0F * fade));
        } else {
            quadSize = 3.0F * scale;
            alpha = 1.0F;
        }
        Vec3 behind = cloud ? new Vec3(x, y, z).subtract(camera.getPosition()).normalize().scale(0.04D) : Vec3.ZERO;
        double savedX = x, savedY = y, savedZ = z;
        double savedXo = xo, savedYo = yo, savedZo = zo;
        x += behind.x;
        y += behind.y;
        z += behind.z;
        xo += behind.x;
        yo += behind.y;
        zo += behind.z;
        try {
            super.render(buffer, camera, partialTicks);
        } finally {
            x = savedX;
            y = savedY;
            z = savedZ;
            xo = savedXo;
            yo = savedYo;
            zo = savedZo;
        }
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean cloud;

        public Factory(SpriteSet sprites, boolean cloud) {
            this.sprites = sprites;
            this.cloud = cloud;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double scale, double vy, double vz) {
            return new VolitansPoisonImpactParticle(level, x, y, z, scale, sprites, cloud);
        }
    }
}
