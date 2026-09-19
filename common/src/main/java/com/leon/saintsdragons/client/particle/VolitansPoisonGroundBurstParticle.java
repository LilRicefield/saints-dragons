package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class VolitansPoisonGroundBurstParticle extends TextureSheetParticle {
    private static final int FRAMES = 12;
    private static final float FRAME_TICKS = 0.75F;
    private final SpriteSet sprites;

    private VolitansPoisonGroundBurstParticle(ClientLevel level, double x, double y, double z,
                                              double scale, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        quadSize = 3.0F * (float) Mth.clamp(scale, 0.25D, 8.0D);
        lifetime = Mth.ceil(FRAMES * FRAME_TICKS);
        hasPhysics = false;
        setSize(quadSize * 2, 0.1F);
        setSprite(sprites.get(0, FRAMES - 1));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) remove();
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float time = age + partialTick;
        if (time >= FRAMES * FRAME_TICKS) return;
        setSprite(sprites.get(Mth.floor(time / FRAME_TICKS), FRAMES - 1));
        Vec3 center = new Vec3(x, y, z).subtract(camera.getPosition());
        float cx = (float) center.x, cy = (float) center.y, cz = (float) center.z;
        vertex(buffer, cx - quadSize, cy, cz - quadSize, getU0(), getV0());
        vertex(buffer, cx - quadSize, cy, cz + quadSize, getU0(), getV1());
        vertex(buffer, cx + quadSize, cy, cz + quadSize, getU1(), getV1());
        vertex(buffer, cx + quadSize, cy, cz - quadSize, getU1(), getV0());
        vertex(buffer, cx + quadSize, cy, cz - quadSize, getU1(), getV0());
        vertex(buffer, cx + quadSize, cy, cz + quadSize, getU1(), getV1());
        vertex(buffer, cx - quadSize, cy, cz + quadSize, getU0(), getV1());
        vertex(buffer, cx - quadSize, cy, cz - quadSize, getU0(), getV0());
    }

    private void vertex(VertexConsumer buffer, float x, float y, float z, float u, float v) {
        buffer.vertex(x, y, z).uv(u, v).color(1, 1, 1, 1.0F).uv2(0xF000F0).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return DragonParticleRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE;
    }

    public record Factory(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double scale, double vy, double vz) {
            return new VolitansPoisonGroundBurstParticle(level, x, y, z, scale, sprites);
        }
    }
}
