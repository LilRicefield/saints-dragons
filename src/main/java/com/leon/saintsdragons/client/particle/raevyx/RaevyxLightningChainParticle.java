package com.leon.saintsdragons.client.particle.raevyx;

import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningChainData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;

/**
 * Animated lightning chain particle that traces a path between two points.
 * Creates the visual effect of lightning jumping from target to target.
 */
public class RaevyxLightningChainParticle extends TextureSheetParticle {
    private final TextureAtlasSprite[] frames;
    private final Vec3 startPos;
    private final Vec3 endPos;
    private final float totalDistance;
    private final float speed;
    private float progress = 0.0f;

    protected RaevyxLightningChainParticle(ClientLevel level, double x, double y, double z,
                                           double xSpeed, double ySpeed, double zSpeed,
                                           float size, SpriteSet spriteSet, Vec3 startPos, Vec3 endPos, boolean female) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        TextureAtlasSprite[] resolved = RaevyxParticleSprites.arc(female);
        if (resolved.length == 0) {
            resolved = new TextureAtlasSprite[]{spriteSet.get(0, 1)};
        }
        this.frames = resolved;
        this.startPos = startPos;
        this.endPos = endPos;
        this.totalDistance = (float) startPos.distanceTo(endPos);
        this.speed = 0.15f; // How fast the lightning travels along the path
        this.quadSize = size;
        this.lifetime = (int) (totalDistance / speed) + 10; // Extra frames for cleanup
        this.setSize(size * 2.0F, size * 2.0F);
        this.setSprite(this.frames[0]);
        
        // Set initial position to start
        this.setPos(startPos.x, startPos.y, startPos.z);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            // Animate along the path
            progress = Math.min(1.0f, (float) this.age / Math.max(1, this.lifetime - 10));
            
            // Interpolate position along the path
            Vec3 currentPos = startPos.lerp(endPos, progress);
            this.setPos(currentPos.x, currentPos.y, currentPos.z);
            
            // Update sprite for animation (loop through frames)
            updateSprite();
            
            // Fade out at the end
            if (progress > 0.8f) {
                this.alpha = 1.0f - ((progress - 0.8f) / 0.2f);
            }
        }
    }

    private void updateSprite() {
        if (this.frames.length == 0) {
            return;
        }
        int frameIndex = (this.age / 2) % this.frames.length;
        this.setSprite(this.frames[frameIndex]);
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
    public static class Factory implements ParticleProvider<RaevyxLightningChainData> {
        private final SpriteSet spriteSet;
        public Factory(SpriteSet spriteSet) { this.spriteSet = spriteSet; }
        
        @Override
        public Particle createParticle(@Nonnull RaevyxLightningChainData data, @Nonnull ClientLevel world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new RaevyxLightningChainParticle(world, x, y, z, xSpeed, ySpeed, zSpeed, data.size(), spriteSet, data.startPos(), data.endPos(), data.female());
        }
    }
}
