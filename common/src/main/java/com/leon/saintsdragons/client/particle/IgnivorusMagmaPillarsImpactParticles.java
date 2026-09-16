package com.leon.saintsdragons.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;


public class IgnivorusMagmaPillarsImpactParticles extends TextureSheetParticle {
    private final SpriteSet sprites;
    private static final int FRAMES = 6;
    private static final float TICKS_PER_FRAME = 0.85F;
    private static final float SIZE = 8.0F;

    protected IgnivorusMagmaPillarsImpactParticles(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        lifetime = Mth.ceil(FRAMES * TICKS_PER_FRAME);
        hasPhysics = false;
        xd = yd = zd = 0.0D;
        quadSize = SIZE;
        setSprite(sprites.get(0, FRAMES - 1));
        setBoundingBox(new AABB(x - SIZE * 1.25D, y - 0.1D, z - SIZE * 1.25D,
                x + SIZE * 1.25D, y + 0.1D, z + SIZE * 1.25D));
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
        float elapsed = age + partialTick;
        int frame = Math.min(FRAMES - 1, (int) (elapsed / TICKS_PER_FRAME));
        setSprite(sprites.get(frame, FRAMES - 1));
        float progress = Mth.clamp(elapsed / lifetime, 0.0F, 1.0F);
        float fade = Mth.clamp((progress - 0.65F) / 0.35F, 0.0F, 1.0F);
        alpha = 1.0F - fade * fade * (3.0F - 2.0F * fade);
        float size = SIZE * (0.75F + 0.5F * progress);
        Vec3 center = new Vec3(x, y, z).subtract(camera.getPosition());
        float cx = (float) center.x, cy = (float) center.y, cz = (float) center.z;
        vertex(buffer, cx - size, cy, cz - size, getU0(), getV0());
        vertex(buffer, cx - size, cy, cz + size, getU0(), getV1());
        vertex(buffer, cx + size, cy, cz + size, getU1(), getV1());
        vertex(buffer, cx + size, cy, cz - size, getU1(), getV0());
    }

    private void vertex(VertexConsumer buffer, float x, float y, float z, float u, float v) {
        buffer.vertex(x, y, z).uv(u, v).color(rCol, gCol, bCol, alpha).uv2(0xF000F0).endVertex();
    }

    @Override
    public int getLightColor(float partialTick) { return 0xF000F0; }

    public static final class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double ax, double by, double cz) {
            return new IgnivorusMagmaPillarsImpactParticles(level, x, y, z, sprites);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ShaderPassCompatibility.isShaderPackInUse()
                ? ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
                : DragonParticleRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE;
    }
}
