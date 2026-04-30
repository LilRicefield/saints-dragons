package com.leon.saintsdragons.client.particle.raevyx;

import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningStormData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RaevyxLightningParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;
    private static final Vector3f[] CORNER_CACHE = new Vector3f[4];
    static {
        for (int i = 0; i < 4; i++) {
            CORNER_CACHE[i] = new Vector3f();
        }
    }

    protected RaevyxLightningParticle(ClientLevel level, double x, double y, double z,
                                      double xSpeed, double ySpeed, double zSpeed,
                                      float size, SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteSet = spriteSet;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.quadSize = size;
        this.lifetime = 8;
        this.setSize(size * 1.5F, size * 1.5F);
        updateSprite();
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            updateSprite();
        }
    }

    private void updateSprite() {
        float agePercent = (float) this.age / (float) this.lifetime;
        int spriteIndex = Math.min((int) (agePercent * 8), 7);
        this.setSprite(this.spriteSet.get(spriteIndex, 7));
    }

    @Override
    public void render(@Nonnull VertexConsumer buffer, @Nonnull Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        float cx = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cam.x());
        float cy = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cam.y());
        float cz = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cam.z());
        Quaternionf camQ = new Quaternionf();
        camQ.rotateY((float) Math.toRadians(-camera.getYRot()));
        float size = this.getQuadSize(partialTicks);
        CORNER_CACHE[0].set(-1.0F, -1.0F, 0.0F);
        CORNER_CACHE[1].set(-1.0F,  1.0F, 0.0F);
        CORNER_CACHE[2].set( 1.0F,  1.0F, 0.0F);
        CORNER_CACHE[3].set( 1.0F, -1.0F, 0.0F);

        for (int i = 0; i < 4; ++i) {
            Vector3f v = CORNER_CACHE[i];
            v.rotate(camQ);
            v.mul(size);
            v.add(cx, cy, cz);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);

        buffer.vertex(CORNER_CACHE[0].x(), CORNER_CACHE[0].y(), CORNER_CACHE[0].z()).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(CORNER_CACHE[1].x(), CORNER_CACHE[1].y(), CORNER_CACHE[1].z()).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(CORNER_CACHE[2].x(), CORNER_CACHE[2].y(), CORNER_CACHE[2].z()).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(CORNER_CACHE[3].x(), CORNER_CACHE[3].y(), CORNER_CACHE[3].z()).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }

    @Override
    public int getLightColor(float partialTicks) {
        return 240;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleProvider<RaevyxLightningStormData> {
        private final SpriteSet spriteSet;
        public Factory(SpriteSet spriteSet) { this.spriteSet = spriteSet; }
        @Override
        public Particle createParticle(@Nonnull RaevyxLightningStormData data, @Nonnull ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new RaevyxLightningParticle(world, x, y, z, xSpeed, ySpeed, zSpeed, data.size(), spriteSet);
        }
    }
}