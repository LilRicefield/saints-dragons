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

public final class IgnivorusChargedFireballTrailParticle extends TextureSheetParticle {
    public enum Kind { FIRE, SPEC, MORE_SPEC, EMBER, EMITTER, STAR }
    private static final int[] FIRE_COLORS = {0xffffff, 0xffeefa, 0xffd6f4, 0xffb5ec, 0xff8ee5, 0xc77cf3};
    private static final int[] SPEC_COLORS = {0x936eff, 0x744eff, 0x663bdd, 0x663bdd, 0x3a2178};
    private final SpriteSet sprites;
    private final Kind kind;
    private final int frames;
    private float size;

    private IgnivorusChargedFireballTrailParticle(ClientLevel level, double x, double y, double z,
                                                 double vx, double vy, double vz, SpriteSet sprites, Kind kind) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.kind = kind;
        frames = switch (kind) { case FIRE -> 17; case SPEC -> 12; case MORE_SPEC -> 11; default -> 1; };
        lifetime = frames > 1 ? (int) Math.ceil(frames * 0.5F) : kind == Kind.STAR ? 20 : 18;
        size = switch (kind) {
            case FIRE -> (0.65F + random.nextFloat() * 0.45F) * 1.5F;
            case SPEC, MORE_SPEC -> 0.4F + random.nextFloat() * 0.35F;
            case STAR -> 0.16F + random.nextFloat() * 0.10F;
            default -> 0.06F + random.nextFloat() * 0.045F;
        };
        xd = vx;
        yd = vy;
        zd = vz;
        friction = 0.92F;
        hasPhysics = false;
        quadSize = size;
        if (kind == Kind.EMBER) roll = oRoll = random.nextFloat() * Mth.TWO_PI;
        if (frames > 1) setSprite(sprites.get(0, frames - 1));
        else pickSprite(sprites);
    }

    public void setBurstSizeMultiplier(float multiplier) {
        size *= multiplier;
        quadSize = size;
    }

    public void lingerAfterImpact() {
        if (kind != Kind.STAR) return;
        lifetime = 40 + random.nextInt(21);
        friction = 0.88F;
        setBurstSizeMultiplier(1.8F);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float time = age + partialTick;
        float progress = Mth.clamp(time / lifetime, 0, 1);
        float fade = smooth(progress / 0.10F) * (1 - smooth((progress - 0.45F) / 0.55F));
        boolean spark = frames == 1;
        if (kind == Kind.STAR) {
            setColor(1.0F, 214 / 255.0F, 244 / 255.0F);
            fade *= 0.65F + 0.35F * Mth.sin(lifetime > 20
                    ? time * Mth.TWO_PI / 10.0F : progress * Mth.TWO_PI * 2);
        } else {
            int[] palette = kind == Kind.SPEC || kind == Kind.MORE_SPEC ? SPEC_COLORS : FIRE_COLORS;
            float position = progress * (palette.length - 1);
            int index = Math.min(palette.length - 2, Mth.floor(position));
            float blend = position - index;
            setColor(channel(palette[index], palette[index + 1], 16, blend),
                    channel(palette[index], palette[index + 1], 8, blend),
                    channel(palette[index], palette[index + 1], 0, blend));
        }
        alpha = fade * (kind == Kind.FIRE ? 0.8F : 0.95F);
        quadSize = size * (spark ? fade : 0.8F + progress * 0.4F);
        if (frames > 1) setSprite(sprites.get(Math.min(frames - 1, Mth.floor(time / 0.5F)), frames - 1));
        if (kind != Kind.EMBER) {
            super.render(buffer, camera, partialTick);
            return;
        }
        Vec3 center = new Vec3(Mth.lerp(partialTick, xo, x), Mth.lerp(partialTick, yo, y),
                Mth.lerp(partialTick, zo, z)).subtract(camera.getPosition());
        Quaternionf rotation = new Quaternionf(camera.rotation()).rotateZ(roll + progress);
        vertex(buffer, rotation, center, -0.5F, -1, getU1(), getV1());
        vertex(buffer, rotation, center, -0.5F, 1, getU1(), getV0());
        vertex(buffer, rotation, center, 0.5F, 1, getU0(), getV0());
        vertex(buffer, rotation, center, 0.5F, -1, getU0(), getV1());
    }

    private void vertex(VertexConsumer buffer, Quaternionf rotation, Vec3 center, float x, float y, float u, float v) {
        Vector3f corner = new Vector3f(x, y, 0).mul(quadSize).rotate(rotation);
        buffer.vertex(center.x + corner.x, center.y + corner.y, center.z + corner.z)
                .uv(u, v).color(rCol, gCol, bCol, alpha).uv2(0xF000F0).endVertex();
    }

    public static void applyFireColor(Particle particle, float progress) {
        float position = Mth.clamp(progress, 0, 1) * (FIRE_COLORS.length - 1);
        int index = Math.min(FIRE_COLORS.length - 2, Mth.floor(position));
        float blend = position - index;
        int from = FIRE_COLORS[index];
        int to = FIRE_COLORS[index + 1];
        particle.setColor(channel(from, to, 16, blend), channel(from, to, 8, blend), channel(from, to, 0, blend));
    }

    private static float channel(int from, int to, int shift, float blend) {
        return Mth.lerp(blend, (from >> shift) & 255, (to >> shift) & 255) / 255.0F;
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; }

    @Override
    public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public record Factory(SpriteSet sprites, Kind kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new IgnivorusChargedFireballTrailParticle(level, x, y, z, vx, vy, vz, sprites, kind);
        }
    }
}
