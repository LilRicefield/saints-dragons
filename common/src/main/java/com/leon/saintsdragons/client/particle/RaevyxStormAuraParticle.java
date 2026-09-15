package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class RaevyxStormAuraParticle extends TextureSheetParticle {
    private static final float FRAME_TICKS = 0.70F;
    private final SpriteSet sprites;
    private final int kind;
    private final float size;

    private RaevyxStormAuraParticle(ClientLevel level, double x, double y, double z,
                                    double vx, double vy, double vz, SpriteSet sprites, int kind) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.kind = kind;
        xd = vx; yd = vy; zd = vz;
        lifetime = kind < 2 ? (int) (16 * FRAME_TICKS) : 6 + random.nextInt(3);
        size = kind < 2 ? 0.55F + random.nextFloat() * 0.4F
                : kind == 2 ? 0.035F + random.nextFloat() * 0.025F : 0.15F + random.nextFloat() * 0.10F;
        friction = 0.9F;
        hasPhysics = false;
        roll = oRoll = random.nextFloat() * Mth.TWO_PI;
        if (kind < 2) {
            setSprite(sprites.get(0, 15));
        } else {
            pickSprite(sprites);
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float time = age + partialTick;
        float progress = Mth.clamp(time / lifetime, 0, 1);
        if (kind < 2) setSprite(sprites.get(Math.min(15, (int) (time / FRAME_TICKS)), 15));
        float fadeIn = Mth.clamp(progress / 0.1F, 0, 1);
        float fadeOut = Mth.clamp((1 - progress) / 0.65F, 0, 1);
        float envelope = fadeIn * fadeOut * fadeOut * (3 - 2 * fadeOut);
        float twinkle = kind == 3 ? 0.55F + 0.45F * Mth.sin(time * 2.5F) : 1;
        alpha = envelope * twinkle;
        quadSize = size * (kind < 2 ? 1 : envelope);
        super.render(buffer, camera, partialTick);
    }

    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public record Factory(SpriteSet sprites, int kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new RaevyxStormAuraParticle(level, x, y, z, vx, vy, vz, sprites, kind);
        }
    }
}
