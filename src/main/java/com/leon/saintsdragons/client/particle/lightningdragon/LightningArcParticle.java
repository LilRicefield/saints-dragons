package com.leon.saintsdragons.client.particle.lightningdragon;

import com.leon.saintsdragons.common.particle.lightningdragon.LightningArcData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Lightning arc particle for impact/explosion visuals.
 * Creates layered, duplicated effects for dramatic impact.
 */
public class LightningArcParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected LightningArcParticle(ClientLevel level, double x, double y, double z,
                                  double xSpeed, double ySpeed, double zSpeed,
                                  float size, SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = spriteSet;
        this.setSpriteFromAge(this.sprites);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.quadSize = size;
        this.lifetime = 12; // Shorter lifetime for impact effect
        this.setSize(size * 2.0F, size * 2.0F);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public void render(@Nonnull VertexConsumer buffer, @Nonnull Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        float cx = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cam.x());
        float cy = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cam.y());
        float cz = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cam.z());
        
        Quaternionf camQ = new Quaternionf();
        camQ.rotateY((float) Math.toRadians(-camera.getYRot()));
        
        Vector3f[] corners = new Vector3f[] {
            new Vector3f(-1.0F, -1.0F, 0.0F),
            new Vector3f(-1.0F,  1.0F, 0.0F),
            new Vector3f( 1.0F,  1.0F, 0.0F),
            new Vector3f( 1.0F, -1.0F, 0.0F)
        };
        
        float size = this.getQuadSize(partialTicks);
        
        for (int i = 0; i < 4; ++i) {
            Vector3f v = corners[i];
            v.rotate(camQ);
            v.mul(size);
            v.add(cx, cy, cz);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);

        buffer.vertex(corners[0].x(), corners[0].y(), corners[0].z()).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(corners[1].x(), corners[1].y(), corners[1].z()).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(corners[2].x(), corners[2].y(), corners[2].z()).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(corners[3].x(), corners[3].y(), corners[3].z()).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }

    @Override
    public int getLightColor(float partialTicks) {
        return 240; // Fullbright
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<LightningArcData> {
        private final SpriteSet spriteSet;
        public Factory(SpriteSet spriteSet) { this.spriteSet = spriteSet; }
        
        @Override
        public Particle createParticle(@Nonnull LightningArcData data, @Nonnull ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new LightningArcParticle(world, x, y, z, xSpeed, ySpeed, zSpeed, data.size(), spriteSet);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ChainFactory implements ParticleProvider<com.leon.saintsdragons.common.particle.lightningdragon.LightningChainData> {
        private final SpriteSet spriteSet;
        public ChainFactory(SpriteSet spriteSet) { this.spriteSet = spriteSet; }
        
        @Override
        public Particle createParticle(@Nonnull com.leon.saintsdragons.common.particle.lightningdragon.LightningChainData data, @Nonnull ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new LightningArcParticle(world, x, y, z, xSpeed, ySpeed, zSpeed, data.size(), spriteSet);
        }
    }
}
