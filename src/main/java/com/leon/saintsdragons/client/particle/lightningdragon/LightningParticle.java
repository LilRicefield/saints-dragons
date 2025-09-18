package com.leon.saintsdragons.client.particle.lightningdragon;

import com.leon.saintsdragons.common.particle.lightningdragon.LightningStormData;
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

public class LightningParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected LightningParticle(ClientLevel level, double x, double y, double z,
                                double xSpeed, double ySpeed, double zSpeed,
                                float size, SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = spriteSet;
        this.setSpriteFromAge(this.sprites);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.quadSize = size;
        this.lifetime = 8; // Match 8-frame texture list
        this.setSize(size * 1.5F, size * 1.5F);
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

        // Create a billboard that's oriented along the lightning direction for better connection
        Quaternionf camQ = new Quaternionf(camera.rotation());
        float size = this.getQuadSize(partialTicks);
        
        Vector3f[] corners = new Vector3f[] {
            new Vector3f(-1.0F, -1.0F, 0.0F),
            new Vector3f(-1.0F,  1.0F, 0.0F),
            new Vector3f( 1.0F,  1.0F, 0.0F),
            new Vector3f( 1.0F, -1.0F, 0.0F)
        };

        // For lightning particles, orient them along the direction vector for better connection
        Vector3f dir = new Vector3f((float)this.xd, (float)this.yd, (float)this.zd);
        if (dir.lengthSquared() > 1.0e-6f) {
            dir.normalize();
            
            // Create a rotation that aligns the particle with the lightning direction
            // This helps create a more connected appearance
            Vector3f up = new Vector3f(0, 1, 0);
            Vector3f right = new Vector3f();
            dir.cross(up, right);
            if (right.lengthSquared() < 0.1f) {
                // If direction is nearly vertical, use a different up vector
                up.set(1, 0, 0);
                dir.cross(up, right);
            }
            right.normalize();
            up.cross(right, up);
            up.normalize();
            
            // Apply the orientation to each corner
            for (int i = 0; i < 4; ++i) {
                Vector3f v = corners[i];
                // Transform the corner to align with the lightning direction
                Vector3f transformed = new Vector3f();
                transformed.add(right.mul(v.x()));
                transformed.add(up.mul(v.y()));
                transformed.add(dir.mul(v.z()));
                
                // Apply camera rotation and position
                transformed.rotate(camQ);
                transformed.mul(size);
                transformed.add(cx, cy, cz);
                
                corners[i] = transformed;
            }
        } else {
            // Fallback to camera-facing billboard
            for (int i = 0; i < 4; ++i) {
                Vector3f v = corners[i];
                v.rotate(camQ);
                v.mul(size);
                v.add(cx, cy, cz);
            }
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
    public static class Factory implements ParticleProvider<LightningStormData> {
        private final SpriteSet spriteSet;
        public Factory(SpriteSet spriteSet) { this.spriteSet = spriteSet; }
        @Override
        public Particle createParticle(@Nonnull LightningStormData data, @Nonnull ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new LightningParticle(world, x, y, z, xSpeed, ySpeed, zSpeed, data.size(), spriteSet);
        }
    }

    // Secondary factory to reuse the same renderer with a different ParticleOptions type
    public static class FactoryArc implements ParticleProvider<LightningArcData> {
        private final SpriteSet spriteSet;
        public FactoryArc(SpriteSet spriteSet) { this.spriteSet = spriteSet; }
        @Override
        public Particle createParticle(@Nonnull LightningArcData data, @Nonnull ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new LightningParticle(world, x, y, z, xSpeed, ySpeed, zSpeed, data.size(), spriteSet);
        }
    }
}
