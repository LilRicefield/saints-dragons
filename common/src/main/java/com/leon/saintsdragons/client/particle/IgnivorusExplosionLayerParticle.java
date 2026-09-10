package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class IgnivorusExplosionLayerParticle extends TextureSheetParticle {
    public enum Layer { EXPLOSION, GROUND, SPEC, CHARGE }
    private static final float TICKS_PER_FRAME = 2.0F;
    private static final int SPARK_COUNT = 64;
    private final SpriteSet sprites;
    private final boolean ground;
    private final Layer layer;
    private final int frameCount;
    private final float ticksPerFrame;

    private IgnivorusExplosionLayerParticle(ClientLevel level, double x, double y, double z,
                                            SpriteSet sprites, Layer layer) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.layer = layer;
        this.ground = layer == Layer.GROUND;
        this.frameCount = switch (layer) {
            case EXPLOSION -> 7;
            case GROUND -> 1;
            case SPEC -> 12;
            case CHARGE -> 5;
        };
        this.ticksPerFrame = layer == Layer.CHARGE ? 1.0F : TICKS_PER_FRAME;
        lifetime = ground ? 24 : (int) (frameCount * ticksPerFrame);
        hasPhysics = false;
        alpha = 0.0F;
        setColor(1.0F, 0.65F, 0.18F);
        if (layer == Layer.SPEC) setColor(1.0F, 0.82F, 0.38F);
        if (ground) {
            pickSprite(sprites);
        } else {
            setSprite(sprites.get(0, frameCount - 1));
        }
        double radius = 56.0D;
        setBoundingBox(new AABB(x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age == 0 && layer == Layer.EXPLOSION) emitSparks();
        if (++age >= lifetime) remove();
    }

    private void emitSparks() {
        var engine = Minecraft.getInstance().particleEngine;
        for (int i = 0; i < SPARK_COUNT; i++) {
            double angle = (i + random.nextDouble()) * Math.PI * 2.0D / SPARK_COUNT;
            double vertical = random.nextDouble() * 1.4D - 0.4D;
            double horizontal = Math.sqrt(1.0D - vertical * vertical);
            Vec3 direction = new Vec3(Math.cos(angle) * horizontal, vertical, Math.sin(angle) * horizontal);
            Vec3 origin = new Vec3(x, y, z).add(direction.scale(3.0D + random.nextDouble() * 5.0D));
            Vec3 emberVelocity = direction.scale(0.35D + random.nextDouble() * 0.55D);
            engine.createParticle(ModParticles.FIRE_BREATH_EMBER.get(), origin.x, origin.y, origin.z,
                    emberVelocity.x, emberVelocity.y, emberVelocity.z);
            Vec3 glowVelocity = direction.scale(1.4D + random.nextDouble() * 1.8D);
            Particle glow = engine.createParticle(ModParticles.GLOWING_EMITTER.get(), origin.x, origin.y, origin.z,
                    glowVelocity.x, glowVelocity.y, glowVelocity.z);
            if (glow != null) glow.setColor(1.0F, 0.55F, 0.12F);
        }
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        float elapsed = age + partialTicks;
        float progress = Mth.clamp(elapsed / lifetime, 0.0F, 1.0F);
        float fadeIn = smooth(Mth.clamp(elapsed / 2.0F, 0.0F, 1.0F));
        float fadeOut = 1.0F - smooth(Mth.clamp((progress - 0.3F) / 0.7F, 0.0F, 1.0F));
        alpha = fadeIn * fadeOut;
        quadSize = Mth.lerp(1.0F - (1.0F - progress) * (1.0F - progress),
                ground ? 10.0F : 12.0F, ground ? 32.0F : 36.0F);
        if (layer == Layer.CHARGE) quadSize = Mth.lerp(progress, 24.0F, 36.0F);
        if (!ground) {
            setSprite(sprites.get(Math.min((int) (elapsed / ticksPerFrame), frameCount - 1), frameCount - 1));
            super.render(buffer, camera, partialTicks);
            return;
        }
        Vec3 center = new Vec3(x, y, z).subtract(camera.getPosition());
        corner(buffer, center, -quadSize, -quadSize, getU0(), getV0());
        corner(buffer, center, -quadSize, quadSize, getU0(), getV1());
        corner(buffer, center, quadSize, quadSize, getU1(), getV1());
        corner(buffer, center, quadSize, -quadSize, getU1(), getV0());
    }

    private void corner(VertexConsumer buffer, Vec3 center, float dx, float dz, float u, float v) {
        buffer.vertex(center.x + dx, center.y, center.z + dz)
                .uv(u, v).color(rCol, gCol, bCol, alpha).uv2(0xF000F0).endVertex();
    }

    private static float smooth(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    @Override
    public int getLightColor(float partialTicks) {
        return 0xF000F0;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return DragonParticleRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE;
    }

    public static final class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Layer layer;

        public Factory(SpriteSet sprites, Layer layer) {
            this.sprites = sprites;
            this.layer = layer;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new IgnivorusExplosionLayerParticle(level, x, y, z, sprites, layer);
        }
    }
}
