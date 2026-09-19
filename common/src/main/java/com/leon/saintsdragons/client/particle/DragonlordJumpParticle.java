package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class DragonlordJumpParticle extends TextureSheetParticle {
    // Same hot-to-charred palette as Ignivorus's fire stream, driven by age here.
    private static final int[] FIRE_COLORS = {
            0xfffff3, 0xfdfba5, 0xffc300, 0xff8816, 0xc4422d, 0x8f3527, 0x632618, 0x46201e
    };
    public enum Kind { FIRE, SPEC, SMOKE, EMITTER }

    private final SpriteSet sprites;
    private final Kind kind;
    private final int frames;
    private final float size;

    private DragonlordJumpParticle(ClientLevel level, double x, double y, double z,
                                    double vx, double vy, double vz, SpriteSet sprites, Kind kind) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.kind = kind;
        frames = switch (kind) { case FIRE -> 17; case SPEC -> 12; case SMOKE -> 14; case EMITTER -> 1; };
        lifetime = (kind == Kind.SMOKE ? 26 : kind == Kind.EMITTER ? 24 : kind == Kind.SPEC ? 16 : 20) + random.nextInt(5);
        size = (kind == Kind.EMITTER ? 0.045F : kind == Kind.SMOKE ? 0.625F : kind == Kind.SPEC ? 0.275F : 0.475F)
                * (0.85F + random.nextFloat() * 0.3F);
        xd = vx;
        yd = vy;
        zd = vz;
        friction = 0.94F;
        hasPhysics = false;
        roll = oRoll = random.nextFloat() * Mth.TWO_PI;
        setSize(size * 2, size * 2);
        if (frames > 1) {
            setSprite(sprites.get(0, frames - 1));
        } else {
            pickSprite(sprites);
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float progress = Mth.clamp((age + partialTick) / lifetime, 0, 1);
        if (frames > 1) {
            setSprite(sprites.get(Math.min(frames - 1, Mth.floor(progress * frames)), frames - 1));
        }
        if (kind == Kind.SMOKE) {
            float shade = Mth.lerp(progress, 0.38F, 0.07F);
            setColor(shade, shade * 0.9F, shade * 0.85F);
        } else {
            float colorProgress = progress * (FIRE_COLORS.length - 1);
            int index = Math.min(FIRE_COLORS.length - 2, Mth.floor(colorProgress));
            float blend = colorProgress - index;
            int from = FIRE_COLORS[index], to = FIRE_COLORS[index + 1];
            setColor(Mth.lerp(blend, (from >> 16) & 255, (to >> 16) & 255) / 255.0F,
                    Mth.lerp(blend, (from >> 8) & 255, (to >> 8) & 255) / 255.0F,
                    Mth.lerp(blend, from & 255, to & 255) / 255.0F);
        }
        float fade = Mth.clamp((1 - progress) / 0.4F, 0, 1);
        alpha = (kind == Kind.SMOKE ? 0.7F : 1.0F) * fade * fade * (3 - 2 * fade);
        quadSize = size * Mth.lerp(progress, 0.65F, kind == Kind.SMOKE ? 1.6F : 1.15F);
        super.render(buffer, camera, partialTick);
    }

    @Override
    public int getLightColor(float partialTick) {
        return kind == Kind.SMOKE ? super.getLightColor(partialTick) : 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return DragonParticleRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE;
    }

    public record Factory(SpriteSet sprites, Kind kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new DragonlordJumpParticle(level, x, y, z, vx, vy, vz, sprites, kind);
        }
    }
}
