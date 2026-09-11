package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public final class IgnivorusAftermathFlameParticle extends TextureSheetParticle {
    public enum Style { FIRE, SPEC, SMOKE, BETTER_FIRE }
    private final SpriteSet sprites;
    private final Style style;
    private final int frames;
    private final float size;
    private final float frameOffset;
    private final ParticleRenderType renderType;
    private int nextSparkAge;

    private IgnivorusAftermathFlameParticle(ClientLevel level, double x, double y, double z,
                                           double vx, double vy, double vz, SpriteSet sprites, Style style) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.style = style;
        nextSparkAge = 2 + random.nextInt(5);
        frames = switch (style) {
            case FIRE -> 17;
            case SPEC -> 12;
            case SMOKE -> 14;
            case BETTER_FIRE -> 8;
        };
        frameOffset = style == Style.SMOKE ? 0.0F : random.nextInt(frames);
        lifetime = style == Style.SMOKE ? 28 : 45 + random.nextInt(36);
        size = style == Style.SMOKE ? 4.0F + random.nextFloat() * 3.0F : 1.4F + random.nextFloat() * 1.5F;
        xd = vx;
        yd = vy;
        zd = vz;
        hasPhysics = false;
        alpha = 0.0F;
        quadSize = size;
        if (style == Style.FIRE || style == Style.BETTER_FIRE) setColor(1.0F, 0.55F, 0.12F);
        if (style == Style.SPEC) setColor(1.0F, 0.82F, 0.38F);
        if (style == Style.SMOKE) setColor(0.7F, 0.65F, 0.6F);
        setSprite(sprites.get((int) frameOffset, frames - 1));
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
        if (++age >= lifetime) {
            remove();
            return;
        }
        x += xd;
        y += yd;
        z += zd;
        xd *= 0.9D;
        zd *= 0.9D;
        yd = yd * 0.9D + 0.004D;
        if (style != Style.SMOKE && age >= nextSparkAge && age < lifetime * 0.65F) {
            emitSparks();
            nextSparkAge = age + 10 + random.nextInt(7);
        }
        updateBounds();
    }

    private void emitSparks() {
        var engine = Minecraft.getInstance().particleEngine;
        for (int i = 0; i < 5; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = Math.sqrt(random.nextDouble()) * size * 0.65D;
            double px = x + Math.cos(angle) * radius;
            double py = y + random.nextDouble() * size * 0.7D;
            double pz = z + Math.sin(angle) * radius;
            boolean ember = i >= 3;
            double speed = ember ? 0.04D + random.nextDouble() * 0.1D
                    : 0.12D + random.nextDouble() * 0.24D;
            double rise = ember ? 0.06D + random.nextDouble() * 0.12D
                    : 0.15D + random.nextDouble() * 0.3D;
            Particle spark = engine.createParticle(ember ? ModParticles.FIRE_BREATH_EMBER.get()
                            : ModParticles.GLOWING_EMITTER.get(), px, py, pz,
                    Math.cos(angle) * speed + xd * 0.25D, rise, Math.sin(angle) * speed + zd * 0.25D);
            if (spark != null && !ember) spark.setColor(1.0F, 0.55F, 0.12F);
        }
    }

    private void updateBounds() {
        double radius = size * 3.0D;
        setBoundingBox(new AABB(x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius));
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        float elapsed = age + partialTicks;
        float progress = Mth.clamp(elapsed / lifetime, 0.0F, 1.0F);
        int frame = style == Style.SMOKE ? Math.min((int) (elapsed / 2.0F), frames - 1)
                : (int) (elapsed / (style == Style.FIRE ? 0.75F : 1.0F) + frameOffset) % frames;
        setSprite(sprites.get(frame, frames - 1));
        float fadeIn = Mth.clamp(elapsed / 4.0F, 0.0F, 1.0F);
        float fadeOut = 1.0F - smooth(Mth.clamp((progress - 0.55F) / 0.45F, 0.0F, 1.0F));
        alpha = fadeIn * fadeOut * (style == Style.SMOKE ? 0.35F : 0.85F);
        quadSize = size * (style == Style.SMOKE ? 0.6F + progress * 1.4F : 0.8F + progress * 0.35F);
        super.render(buffer, camera, partialTicks);
    }

    private static float smooth(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    @Override
    public int getLightColor(float partialTicks) {
        return style == Style.SMOKE ? super.getLightColor(partialTicks) : 0xF000F0;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return renderType;
    }

    public static final class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Style style;

        public Factory(SpriteSet sprites, Style style) {
            this.sprites = sprites;
            this.style = style;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new IgnivorusAftermathFlameParticle(level, x, y, z, vx, vy, vz, sprites, style);
        }
    }
}
