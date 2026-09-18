package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
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
    public enum Layer { EXPLOSION, GROUND, SPEC, CHARGE, AURA, SHARP, SWIRL, ABSORB, CIRCLE, TOON }
    private static final float TICKS_PER_FRAME = 2.0F;
    private static final int SPARK_COUNT = 64;
    private final SpriteSet sprites;
    private final boolean ground;
    private final Layer layer;
    private final int frameCount;
    private final float ticksPerFrame;
    private final ParticleRenderType renderType;

    private IgnivorusExplosionLayerParticle(ClientLevel level, double x, double y, double z,
                                            SpriteSet sprites, Layer layer) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.layer = layer;
        this.renderType = (layer == Layer.CHARGE || layer == Layer.AURA || layer == Layer.GROUND || layer == Layer.SHARP || layer == Layer.SWIRL || layer == Layer.ABSORB || layer == Layer.CIRCLE || layer == Layer.TOON)
                && ShaderPassCompatibility.isShaderPackInUse()
                ? ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
                : DragonParticleRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE;
        this.ground = layer == Layer.GROUND;
        this.frameCount = switch (layer) {
            case EXPLOSION -> 7;
            case GROUND -> 6;
            case SPEC -> 12;
            case CHARGE -> 5;
            case AURA -> 10;
            case SHARP -> 8;
            case SWIRL -> 31;
            case ABSORB -> 31;
            case CIRCLE -> 12;
            case TOON -> 8;
        };
        this.ticksPerFrame = switch (layer) {
            case CHARGE, SHARP, SWIRL, GROUND, TOON -> 0.5F;
            case ABSORB, CIRCLE -> 0.5F;
            case AURA -> 16.0F / frameCount;
            default -> TICKS_PER_FRAME;
        };
        lifetime = layer == Layer.AURA ? 16 : Mth.ceil(frameCount * ticksPerFrame);
        hasPhysics = false;
        alpha = 0.0F;
        setColor(1.0F, 0.65F, 0.18F);
        if (layer == Layer.SPEC) setColor(1.0F, 0.82F, 0.38F);
        if (layer == Layer.AURA || layer == Layer.ABSORB) setColor(1.0F, 1.0F, 1.0F);
        if (frameCount == 1) {
            pickSprite(sprites);
        } else {
            setSprite(sprites.get(layer == Layer.SHARP ? frameCount - 1 : 0, frameCount - 1));
        }
        double radius = layer == Layer.AURA ? 70.0D : 56.0D;
        setBoundingBox(new AABB(x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age == 0 && layer == Layer.EXPLOSION) {
            emitSparks();
            emitStars();
        }
        if (layer == Layer.ABSORB && age < 40 && age % 5 == 0) {
            var engine = Minecraft.getInstance().particleEngine;
            for (int i = 0; i < 5; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double vertical = random.nextDouble() * 2.0D - 1.0D;
                double horizontal = Math.sqrt(1.0D - vertical * vertical);
                Vec3 direction = new Vec3(Math.cos(angle) * horizontal, vertical, Math.sin(angle) * horizontal);
                Vec3 point = new Vec3(x, y, z).add(direction.scale(24.0D + random.nextDouble() * 10.0D));
                Vec3 drift = direction.scale(0.08D);
                engine.createParticle(ModParticles.IGNIVORUS_CHARGE_SPARKLE.get(), point.x, point.y, point.z,
                        drift.x, drift.y, drift.z);
            }
        }
        if (++age >= lifetime) remove();
    }

    private void emitStars() {
        var engine = Minecraft.getInstance().particleEngine;
        for (int i = 0; i < 80; i++) {
            double angle = (i + random.nextDouble()) * Math.PI * 2.0D / 80.0D;
            double vertical = random.nextDouble() * 1.5D - 0.5D;
            double horizontal = Math.sqrt(1.0D - vertical * vertical);
            Vec3 direction = new Vec3(Math.cos(angle) * horizontal, vertical, Math.sin(angle) * horizontal);
            Vec3 origin = new Vec3(x, y, z).add(direction.scale(4.0D + random.nextDouble() * 5.0D));
            Vec3 velocity = direction.scale(1.2D + random.nextDouble() * 2.2D);
            engine.createParticle(ModParticles.IGNIVORUS_NOVA_SPARKLE.get(), origin.x, origin.y, origin.z,
                    velocity.x, velocity.y, velocity.z);
        }
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
        if (ground && ShaderPassCompatibility.isIrisShadowPass()) {
            return;
        }
        float elapsed = age + partialTicks;
        if (frameCount > 1) {
            int frame = Math.min((int) (elapsed / ticksPerFrame), frameCount - 1);
            if (layer == Layer.SHARP) frame = frameCount - 1 - frame;
            setSprite(sprites.get(frame, frameCount - 1));
        }
        float progress = Mth.clamp(elapsed / lifetime, 0.0F, 1.0F);
        if (layer == Layer.SPEC) SkyfallFireColors.apply(this, progress);
        float fadeIn = smooth(Mth.clamp(elapsed / 2.0F, 0.0F, 1.0F));
        float fadeOut = 1.0F - smooth(Mth.clamp((progress - 0.3F) / 0.7F, 0.0F, 1.0F));
        alpha = fadeIn * fadeOut;
        quadSize = Mth.lerp(1.0F - (1.0F - progress) * (1.0F - progress),
                ground ? 6.0F : 10.0F, ground ? 8.0F : 16.0F);
        if (layer == Layer.CHARGE) quadSize = Mth.lerp(progress, 24.0F, 36.0F);
        if (layer == Layer.SHARP) quadSize = 16.0F;
        if (layer == Layer.CIRCLE) {
            quadSize = Mth.lerp(1.0F - (1.0F - progress) * (1.0F - progress), 2.0F, 38.0F);
            alpha = smooth(Mth.clamp(elapsed, 0.0F, 1.0F))
                    * (1.0F - smooth(Mth.clamp((elapsed - 4.0F) / 2.0F, 0.0F, 1.0F)));
        }
        if (layer == Layer.TOON) {
            quadSize = Mth.lerp(progress, 8.0F, 16.0F);
            alpha = fadeIn * (1.0F - smooth(Mth.clamp((elapsed - 10.0F) / 6.0F, 0.0F, 1.0F)));
        }
        if (layer == Layer.ABSORB) {
            quadSize = 24.0F;
            alpha = smooth(Mth.clamp(elapsed, 0.0F, 1.0F))
                    * (1.0F - smooth(Mth.clamp((elapsed - 14.0F) / 1.5F, 0.0F, 1.0F)));
        }
        if (layer == Layer.SWIRL) {
            quadSize = 32.0F;
            alpha = fadeIn * (1.0F - smooth(Mth.clamp((elapsed - 27.0F) / 4.0F, 0.0F, 1.0F)));
        }
        if (layer == Layer.AURA) {
            quadSize = Mth.lerp(progress, 18.0F, 24.0F);
            Vec3 center = new Vec3(x, y, z).subtract(camera.getPosition());
            Vec3 towardCamera = camera.getPosition().subtract(x, y, z);
            Vec3 right = new Vec3(towardCamera.z, 0.0D, -towardCamera.x);
            if (right.lengthSqr() < 1.0E-8D) {
                double yaw = Math.toRadians(camera.getYRot());
                right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
            } else {
                right = right.normalize();
            }
            auraCorner(buffer, center, right, -1, -1, getU1(), getV1());
            auraCorner(buffer, center, right, 1, -1, getU0(), getV1());
            auraCorner(buffer, center, right, 1, 1, getU0(), getV0());
            auraCorner(buffer, center, right, -1, 1, getU1(), getV0());
            return;
        }
        if (!ground) {
            super.render(buffer, camera, partialTicks);
            return;
        }
        Vec3 center = new Vec3(x, y + 0.025D, z).subtract(camera.getPosition());
        corner(buffer, center, -quadSize, -quadSize, getU0(), getV0());
        corner(buffer, center, -quadSize, quadSize, getU0(), getV1());
        corner(buffer, center, quadSize, quadSize, getU1(), getV1());
        corner(buffer, center, quadSize, -quadSize, getU1(), getV0());
    }

    private void auraCorner(VertexConsumer buffer, Vec3 center, Vec3 right,
                            float horizontal, float vertical, float u, float v) {
        buffer.vertex(center.x + right.x * horizontal * quadSize, center.y + vertical * quadSize,
                        center.z + right.z * horizontal * quadSize)
                .uv(u, v).color(rCol, gCol, bCol, alpha).uv2(0xF000F0).endVertex();
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
        return renderType;
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
