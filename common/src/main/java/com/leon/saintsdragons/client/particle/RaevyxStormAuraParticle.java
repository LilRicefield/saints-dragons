package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RaevyxStormAuraParticle extends TextureSheetParticle {
    private static final float FRAME_TICKS = 0.70F;
    private final SpriteSet sprites;
    private final int kind;
    private final int frameCount;
    private final float size;
    private Vec3 alignment = new Vec3(0, 1, 0);

    private RaevyxStormAuraParticle(ClientLevel level, double x, double y, double z,
                                    double vx, double vy, double vz, SpriteSet sprites, int kind) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.kind = kind;
        frameCount = kind < 2 ? 16 : kind == 4 ? 12 : 1;
        xd = vx; yd = vy; zd = vz;
        lifetime = frameCount > 1 ? Mth.ceil(frameCount * FRAME_TICKS) : 6 + random.nextInt(3);
        size = kind == 4 ? 0.35F + random.nextFloat() * 0.25F : kind < 2 ? 0.55F + random.nextFloat() * 0.4F
                : kind == 2 ? 0.035F + random.nextFloat() * 0.025F : 0.5F + random.nextFloat() * 0.10F;
        friction = 0.9F;
        hasPhysics = false;
        roll = oRoll = random.nextFloat() * Mth.TWO_PI;
        if (frameCount > 1) {
            setSprite(sprites.get(0, frameCount - 1));
        } else {
            pickSprite(sprites);
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float time = age + partialTick;
        float progress = Mth.clamp(time / lifetime, 0, 1);
        if (frameCount > 1) setSprite(sprites.get(Math.min(frameCount - 1, (int) (time / FRAME_TICKS)), frameCount - 1));
        float fadeIn = Mth.clamp(progress / 0.1F, 0, 1);
        float fadeOut = Mth.clamp((1 - progress) / 0.65F, 0, 1);
        float envelope = fadeIn * fadeOut * fadeOut * (3 - 2 * fadeOut);
        float twinkle = kind == 4 ? 0.55F + 0.45F * Mth.sin(time * 2.5F) : 1;
        alpha = envelope * twinkle;
        quadSize = size * (kind < 2 ? 1 : envelope);
        if (kind < 2) renderVelocityParallel(buffer, camera, partialTick);
        else super.render(buffer, camera, partialTick);
    }

    private void renderVelocityParallel(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 velocity = new Vec3(xd, yd, zd);
        if (velocity.lengthSqr() > 1.0E-8) alignment = velocity.normalize();
        Vec3 center = new Vec3(Mth.lerp(partialTick, xo, x), Mth.lerp(partialTick, yo, y),
                Mth.lerp(partialTick, zo, z)).subtract(camera.getPosition());
        Vec3 right = alignment.cross(center.scale(-1));
        if (right.lengthSqr() < 1.0E-8) {
            Vec3 reference = Math.abs(alignment.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            right = alignment.cross(reference);
        }
        right = right.normalize().scale(quadSize);
        Vec3 up = alignment.scale(quadSize);
        vertex(buffer, center.subtract(right).subtract(up), getU1(), getV1());
        vertex(buffer, center.add(right).subtract(up), getU0(), getV1());
        vertex(buffer, center.add(right).add(up), getU0(), getV0());
        vertex(buffer, center.subtract(right).add(up), getU1(), getV0());
    }

    private void vertex(VertexConsumer buffer, Vec3 position, float u, float v) {
        buffer.vertex(position.x, position.y, position.z).uv(u, v)
                .color(rCol, gCol, bCol, alpha).uv2(0xF000F0).endVertex();
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
