package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CindervaneFireTrailParticle extends TextureSheetParticle {
    public enum Kind { FIRE, DARK_FIRE, BRIGHT_FIRE, ORANGE_SPEC, SPEC, MORE_SPEC, BETTER_FIRE, FIREBALL_FIRE }

    private final SpriteSet sprites;
    private float ticksPerFrame;
    private final int frames;
    private float size;
    private final float aspectRatio;

    private CindervaneFireTrailParticle(ClientLevel level, double x, double y, double z,
                                        double vx, double vy, double vz, SpriteSet sprites, Kind kind) {
        super(level, x, y, z);
        this.sprites = sprites;
        boolean spec = kind == Kind.SPEC || kind == Kind.MORE_SPEC || kind == Kind.ORANGE_SPEC;
        boolean fireball = kind == Kind.FIREBALL_FIRE;
        this.aspectRatio = fireball ? 32.0F / 48.0F : 1.0F;
        this.frames = kind == Kind.MORE_SPEC ? 11 : fireball || kind == Kind.BETTER_FIRE ? 8 : spec ? 12 : 17;
        this.ticksPerFrame = fireball ? 0.25F : kind == Kind.BETTER_FIRE ? 1.0F : 0.5F;
        this.lifetime = (int) Math.ceil(frames * ticksPerFrame);
        this.size = (kind == Kind.BETTER_FIRE ? 0.23F : spec ? 0.36F : 0.46F)
                * (0.8F + random.nextFloat() * 0.4F);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = 0.90F;
        this.hasPhysics = false;
        this.quadSize = size;
        setColor(1.0F, spec ? 0.78F : fireball ? 0.60F : 0.48F, spec ? 0.28F : fireball ? 0.12F : 0.08F);
        if (kind == Kind.DARK_FIRE) setColor(0.75F, 0.25F, 0.035F);
        if (kind == Kind.BRIGHT_FIRE) setColor(1.0F, 0.78F, 0.28F);
        if (kind == Kind.ORANGE_SPEC) setColor(1.0F, 0.48F, 0.08F);
        setSprite(sprites.get(0, frames - 1));
    }

    public void setBodySizeMultiplier(float multiplier) {
        this.size *= multiplier;
        this.quadSize = size;
    }

    public void setAnimationSpeed(float multiplier) {
        if (!Float.isFinite(multiplier) || multiplier <= 0.0F) return;
        ticksPerFrame /= multiplier;
        lifetime = Math.max(1, (int) Math.ceil(frames * ticksPerFrame));
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float time = age + partialTick;
        float progress = Mth.clamp(time / lifetime, 0.0F, 1.0F);
        float fade = Mth.clamp((progress - 0.35F) / 0.65F, 0.0F, 1.0F);
        this.alpha = 0.9F * (1.0F - fade * fade * (3.0F - 2.0F * fade));
        this.quadSize = size * (1.0F + progress * 0.35F);
        setSprite(sprites.get(Math.min(frames - 1, (int) (time / ticksPerFrame)), frames - 1));
        if (aspectRatio == 1.0F) {
            super.render(buffer, camera, partialTick);
            return;
        }
        Vec3 center = new Vec3(Mth.lerp(partialTick, xo, x), Mth.lerp(partialTick, yo, y),
                Mth.lerp(partialTick, zo, z)).subtract(camera.getPosition());
        Quaternionf rotation = new Quaternionf(camera.rotation())
                .rotateZ(Mth.lerp(partialTick, oRoll, roll));
        vertex(buffer, rotation, center, -aspectRatio, -1, getU1(), getV1());
        vertex(buffer, rotation, center, -aspectRatio, 1, getU1(), getV0());
        vertex(buffer, rotation, center, aspectRatio, 1, getU0(), getV0());
        vertex(buffer, rotation, center, aspectRatio, -1, getU0(), getV1());
    }

    private void vertex(VertexConsumer buffer, Quaternionf rotation, Vec3 center,
                        float localX, float localY, float u, float v) {
        Vector3f corner = new Vector3f(localX, localY, 0).mul(quadSize).rotate(rotation);
        buffer.vertex(center.x + corner.x, center.y + corner.y, center.z + corner.z)
                .uv(u, v).color(rCol, gCol, bCol, alpha).uv2(0xF000F0).endVertex();
    }

    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public record Factory(SpriteSet sprites, Kind kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new CindervaneFireTrailParticle(level, x, y, z, vx, vy, vz, sprites, kind);
        }
    }
}
