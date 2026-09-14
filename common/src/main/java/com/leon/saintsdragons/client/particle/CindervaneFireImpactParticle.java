package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CindervaneFireImpactParticle extends TextureSheetParticle {
    public enum Kind { FIRE, SMALL, GROUND, CRASH_FIRE, CRASH_GROUND, CRASH_CIRCLE, CRASH_SPLATTER }
    private final SpriteSet sprites;
    private final Kind kind;
    private final int frames;
    private final float ticksPerFrame;
    private final float size;

    private CindervaneFireImpactParticle(ClientLevel level, double x, double y, double z,
                                         SpriteSet sprites, Kind kind) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.kind = kind;
        this.frames = switch (kind) {
            case FIRE, CRASH_FIRE -> 7;
            case SMALL -> 5;
            case CRASH_CIRCLE -> 12;
            case CRASH_SPLATTER -> 10;
            default -> 6;
        };
        this.size = switch (kind) {
            case FIRE -> 4.375F;
            case SMALL -> 2.75F;
            case GROUND -> 6.75F;
            case CRASH_FIRE -> 9.0F;
            case CRASH_GROUND -> 12.0F;
            case CRASH_CIRCLE -> 13.0F;
            case CRASH_SPLATTER -> 10.0F;
        };
        this.ticksPerFrame = switch (kind) {
            case GROUND, CRASH_GROUND -> 2.0F;
            case CRASH_FIRE -> 1.5F;
            case CRASH_CIRCLE -> 0.75F;
            default -> 1.0F;
        };
        if (kind == Kind.CRASH_CIRCLE) setColor(1.0F, 0.48F, 0.08F);
        this.lifetime = (int) Math.ceil(frames * ticksPerFrame);
        this.hasPhysics = false;
        this.xd = this.yd = this.zd = 0;
        this.quadSize = size;
        setSprite(sprites.get(0, frames - 1));
        double radius = size * 1.6;
        setBoundingBox(new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius));
    }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        if (++age >= lifetime) remove();
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float time = age + partialTick;
        float progress = Mth.clamp(time / lifetime, 0, 1);
        float fade = Mth.clamp((progress - 0.6F) / 0.4F, 0, 1);
        alpha = 1.0F - fade * fade * (3.0F - 2.0F * fade);
        quadSize = size * (0.75F + 0.5F * progress);
        setSprite(sprites.get(Math.min(frames - 1, (int) (time / ticksPerFrame)), frames - 1));
        if (kind != Kind.GROUND && kind != Kind.CRASH_GROUND && kind != Kind.CRASH_CIRCLE) {
            super.render(buffer, camera, partialTick);
            return;
        }
        Vec3 center = new Vec3(x, y, z).subtract(camera.getPosition());
        float cx = (float) center.x, cy = (float) center.y, cz = (float) center.z;
        vertex(buffer, cx - quadSize, cy, cz - quadSize, getU0(), getV0());
        vertex(buffer, cx - quadSize, cy, cz + quadSize, getU0(), getV1());
        vertex(buffer, cx + quadSize, cy, cz + quadSize, getU1(), getV1());
        vertex(buffer, cx + quadSize, cy, cz - quadSize, getU1(), getV0());
    }

    private void vertex(VertexConsumer buffer, float x, float y, float z, float u, float v) {
        buffer.vertex(x, y, z).uv(u, v).color(rCol, gCol, bCol, alpha).uv2(0xF000F0).endVertex();
    }

    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public record Factory(SpriteSet sprites, Kind kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new CindervaneFireImpactParticle(level, x, y, z, sprites, kind);
        }
    }
}
