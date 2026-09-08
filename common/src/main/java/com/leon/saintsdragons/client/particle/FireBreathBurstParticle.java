package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import com.leon.saintsdragons.common.particle.FireBreathBurstData;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class FireBreathBurstParticle extends TextureSheetParticle {
    private static final float WIDE_DELAY = 3.0F;
    private static final double FORWARD_OFFSET = 2.0;
    private static final int LAST_SPRITE = 7;
    private static final float RING_FRAME_TICKS = 0.25F;
    private static final int RING_FRAMES = 6;
    private static final float RING_LIFETIME_TICKS = 12.0F;
    private final SpriteSet sprites;
    private final Ignivorus dragon;

    private FireBreathBurstParticle(ClientLevel level, double x, double y, double z,
                                   SpriteSet sprites, Ignivorus dragon) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.dragon = dragon;
        lifetime = Mth.ceil(RING_LIFETIME_TICKS + 2.0F) + 1;
        hasPhysics = false;
        setSize(24.0F, 24.0F);
        setColor(1.0F, 0.45F, 0.06F);
        setSprite(sprites.get(0, LAST_SPRITE));
    }

    @Override
    public void tick() {
        if (!dragon.isAlive() || dragon.isRemoved()) {
            remove();
            return;
        }
        anchorToMouth(1.0F);
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) remove();
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        if (!dragon.isAlive() || dragon.isRemoved()) return;
        anchorToMouth(partialTicks);
        float time = Math.max(0, age - 1 + partialTicks);
        // Submit the rings before overlapping translucent star faces write depth.
        renderRings(buffer, camera, partialTicks, time);
        if (time < 9.0F) {
            setSprite(sprites.get(0, LAST_SPRITE));
            roll = oRoll = 0;
            quadSize = 8.0F * Mth.lerp(smooth(Mth.clamp(time, 0, 1)), 0.7F, 1.0F);
            alpha = 1 - smooth(Mth.clamp((time - 4.0F) / 5.0F, 0, 1));
            super.render(buffer, camera, partialTicks);
        }
        float wideAge = time - WIDE_DELAY;
        if (wideAge >= 0 && wideAge < 5.0F) {
            setSprite(sprites.get(1, LAST_SPRITE));
            roll = oRoll = wideAge * 0.55F;
            float grow = smooth(Mth.clamp(wideAge, 0, 1));
            quadSize = 5.5F * Mth.lerp(grow, 0.4F, 1.0F);
            alpha = grow * (1 - smooth(Mth.clamp((wideAge - 1.0F) / 4.0F, 0, 1)));
            super.render(buffer, camera, partialTicks);
        }
    }

    private void renderRings(VertexConsumer buffer, Camera camera, float partialTicks, float time) {
        Vec3 mouth = dragon.getFireBreathStartAnchor(partialTicks);
        if (mouth == null) return;
        Vec3 forward = dragon.getFireBreathVisualDirection(partialTicks);
        Vec3 reference = Math.abs(forward.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = forward.cross(reference).normalize();
        Vec3 up = right.cross(forward).normalize();
        for (int i = 0; i < 3; i++) {
            float ringAge = time - i;
            float duration = RING_LIFETIME_TICKS;
            if (ringAge < 0 || ringAge >= duration) continue;
            setSprite(sprites.get(2 + (int) (ringAge / RING_FRAME_TICKS) % RING_FRAMES, LAST_SPRITE));
            float progress = ringAge / duration;
            double size = Mth.lerp(smooth(progress), 1.5F, 8.0F + i);
            Vec3 center = mouth.add(forward.scale(FORWARD_OFFSET + i * 2.0 + ringAge * 0.6))
                    .subtract(camera.getPosition());
            Vec3 across = right;
            Vec3 a = center.add(across.scale(-size)).add(up.scale(-size));
            Vec3 b = center.add(across.scale(-size)).add(up.scale(size));
            Vec3 c = center.add(across.scale(size)).add(up.scale(size));
            Vec3 d = center.add(across.scale(size)).add(up.scale(-size));
            float opacity = 1 - smooth(Mth.clamp((progress - 0.65F) / 0.35F, 0, 1));
            ringVertex(buffer, a, getU1(), getV1(), opacity);
            ringVertex(buffer, b, getU1(), getV0(), opacity);
            ringVertex(buffer, c, getU0(), getV0(), opacity);
            ringVertex(buffer, d, getU0(), getV1(), opacity);
            ringVertex(buffer, d, getU0(), getV1(), opacity);
            ringVertex(buffer, c, getU0(), getV0(), opacity);
            ringVertex(buffer, b, getU1(), getV0(), opacity);
            ringVertex(buffer, a, getU1(), getV1(), opacity);
        }
    }

    private static void ringVertex(VertexConsumer buffer, Vec3 point, float u, float v, float opacity) {
        buffer.vertex(point.x, point.y, point.z).uv(u, v)
                .color(1.0F, 0.65F, 0.08F, opacity).uv2(0xF000F0).endVertex();
    }

    private void anchorToMouth(float partialTicks) {
        Vec3 mouth = dragon.getFireBreathStartAnchor(partialTicks);
        if (mouth == null) return;
        Vec3 forward = dragon.getFireBreathVisualDirection(partialTicks);
        Vec3 center = mouth.add(forward.scale(FORWARD_OFFSET));
        setPos(center.x, center.y, center.z);
        xo = x;
        yo = y;
        zo = z;
    }

    private static float smooth(float value) {
        return value * value * (3 - 2 * value);
    }

    @Override
    public int getLightColor(float partialTicks) {
        return 0xF000F0;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Factory implements ParticleProvider<FireBreathBurstData> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull FireBreathBurstData type, @NotNull ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            if (!(level.getEntity(type.dragonId()) instanceof Ignivorus dragon)) return null;
            return new FireBreathBurstParticle(level, x, y, z, sprites, dragon);
        }
    }
}
