package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class DragonlordJumpParticle extends TextureSheetParticle {
    private static final int[] FIRE_COLORS = {
            0xfffff3, 0xfdfba5, 0xffc300, 0xff8816, 0xc4422d, 0x8f3527, 0x632618, 0x46201e
    };
    private static final float FRAME_TICKS = 0.5F;
    public enum Kind { FIRE, SPEC, SMOKE, EMITTER, STRIKE, GROUND_IMPACT }

    private final SpriteSet sprites;
    private final ParticleRenderType renderType;
    private final Kind kind;
    private final int frames;
    private final float size;

    private DragonlordJumpParticle(ClientLevel level, double x, double y, double z,
                                    double vx, double vy, double vz, SpriteSet sprites, Kind kind) {
        super(level, x, y, z);
        this.sprites = sprites;
        renderType = ShaderPassCompatibility.isShaderPackInUse()
                ? ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
                : DragonParticleRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE;
        this.kind = kind;
        frames = switch (kind) {
            case FIRE -> 17; case SPEC -> 12; case SMOKE -> 14;
            case EMITTER -> 1; case STRIKE -> 5; case GROUND_IMPACT -> 6;
        };
        lifetime = kind == Kind.EMITTER ? 24 + random.nextInt(5) : Mth.ceil(frames * FRAME_TICKS);
        size = switch (kind) {
            case STRIKE -> 1.7F;
            case GROUND_IMPACT -> 2.5F;
            default -> (kind == Kind.EMITTER ? 0.045F : kind == Kind.SMOKE ? 0.625F : kind == Kind.SPEC ? 0.42F : 0.72F)
                    * (0.85F + random.nextFloat() * 0.3F);
        };
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
        float time = age + partialTick;
        float duration = frames > 1 ? frames * FRAME_TICKS : lifetime;
        if (time >= duration) return;
        float progress = Mth.clamp(time / duration, 0, 1);
        if (frames > 1) {
            setSprite(sprites.get(Math.min(frames - 1, Mth.floor(time / FRAME_TICKS)), frames - 1));
        }
        if (kind == Kind.STRIKE || kind == Kind.GROUND_IMPACT) {
            renderImpact(buffer, camera, partialTick, progress);
            return;
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
        alpha = (kind == Kind.SMOKE ? 0.3F : 1.0F) * fade * fade * (3 - 2 * fade);
        quadSize = size * Mth.lerp(progress, 0.65F, kind == Kind.SMOKE ? 1.6F : 1.15F);
        super.render(buffer, camera, partialTick);
    }

    private void renderImpact(VertexConsumer buffer, Camera camera, float partialTick, float progress) {
        Vec3 center = new Vec3(Mth.lerp(partialTick, xo, x), Mth.lerp(partialTick, yo, y),
                Mth.lerp(partialTick, zo, z)).subtract(camera.getPosition());
        Vec3 right;
        Vec3 up;
        if (kind == Kind.GROUND_IMPACT) {
            right = new Vec3(size, 0, 0);
            up = new Vec3(0, 0, -size);
        } else {
            Vec3 horizontal = new Vec3(center.z, 0, -center.x);
            right = horizontal.lengthSqr() < 1.0E-8 ? new Vec3(size, 0, 0) : horizontal.normalize().scale(size);
            up = new Vec3(0, size, 0);
            center = center.add(up);
        }
        alpha = Mth.clamp((1 - progress) / 0.2F, 0, 1);
        Vec3[] corners = {center.subtract(right).subtract(up), center.subtract(right).add(up),
                center.add(right).add(up), center.add(right).subtract(up)};
        for (int i : new int[] {0, 1, 2, 3, 3, 2, 1, 0}) {
            Vec3 point = corners[i];
            buffer.vertex(point.x, point.y, point.z)
                    .uv(i < 2 ? getU0() : getU1(), i == 0 || i == 3 ? getV1() : getV0())
                    .color(1.0F, 1.0F, 1.0F, alpha).uv2(0xF000F0).endVertex();
        }
    }

    @Override
    public int getLightColor(float partialTick) {
        return kind == Kind.SMOKE ? super.getLightColor(partialTick) : 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return renderType;
    }

    public record Factory(SpriteSet sprites, Kind kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new DragonlordJumpParticle(level, x, y, z, vx, vy, vz, sprites, kind);
        }
    }
}
