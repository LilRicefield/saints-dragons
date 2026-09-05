package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;


public final class SecondImpactRingParticle extends TextureSheetParticle {
    private static final int FRAME_COUNT = 8;
    private static final float FRAME_DURATION_TICKS = 0.75F;
    private static final float SIZE = 6.0F;

    private final SpriteSet sprites;

    private SecondImpactRingParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = Mth.ceil(FRAME_COUNT * FRAME_DURATION_TICKS);
        this.quadSize = SIZE;
        this.alpha = 1.0F;
        this.hasPhysics = false;
        updateSprite(0.0F);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (++this.age >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        updateSprite(partialTicks);

        Vec3 cameraPos = camera.getPosition();
        double worldX = Mth.lerp(partialTicks, this.xo, this.x);
        double worldY = Mth.lerp(partialTicks, this.yo, this.y);
        double worldZ = Mth.lerp(partialTicks, this.zo, this.z);
        float x = (float)(worldX - cameraPos.x());
        float y = (float)(worldY - cameraPos.y());
        float z = (float)(worldZ - cameraPos.z());

        double facingX = cameraPos.x() - worldX;
        double facingZ = cameraPos.z() - worldZ;
        double horizontalLength = Math.sqrt(facingX * facingX + facingZ * facingZ);
        float forwardX;
        float forwardZ;
        if (horizontalLength > 1.0E-5D) {
            forwardX = (float)(facingX / horizontalLength);
            forwardZ = (float)(facingZ / horizontalLength);
        } else {
            forwardX = 0.0F;
            forwardZ = 1.0F;
        }
        float rightX = -forwardZ;
        float rightZ = forwardX;
        float size = this.quadSize;

        Vector3f[] vertices = new Vector3f[]{
                new Vector3f(x - rightX * size - forwardX * size, y, z - rightZ * size - forwardZ * size),
                new Vector3f(x - rightX * size + forwardX * size, y, z - rightZ * size + forwardZ * size),
                new Vector3f(x + rightX * size + forwardX * size, y, z + rightZ * size + forwardZ * size),
                new Vector3f(x + rightX * size - forwardX * size, y, z + rightZ * size - forwardZ * size)
        };

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);
        drawQuad(buffer, vertices, u0, u1, v0, v1, light);
        drawReverseQuad(buffer, vertices, u0, u1, v0, v1, light);
    }

    private void updateSprite(float partialTicks) {
        float animationAge = Math.max(0.0F, this.age + partialTicks);
        int frame = Math.min((int)(animationAge / FRAME_DURATION_TICKS), FRAME_COUNT - 1);
        this.setSprite(this.sprites.get(frame, FRAME_COUNT - 1));
    }

    private void drawQuad(VertexConsumer buffer, Vector3f[] vertices,
                          float u0, float u1, float v0, float v1, int light) {
        vertex(buffer, vertices[0], u1, v1, light);
        vertex(buffer, vertices[1], u1, v0, light);
        vertex(buffer, vertices[2], u0, v0, light);
        vertex(buffer, vertices[3], u0, v1, light);
    }

    private void drawReverseQuad(VertexConsumer buffer, Vector3f[] vertices,
                                 float u0, float u1, float v0, float v1, int light) {
        vertex(buffer, vertices[3], u0, v1, light);
        vertex(buffer, vertices[2], u0, v0, light);
        vertex(buffer, vertices[1], u1, v0, light);
        vertex(buffer, vertices[0], u1, v1, light);
    }

    private void vertex(VertexConsumer buffer, Vector3f vertex, float u, float v, int light) {
        buffer.vertex(vertex.x(), vertex.y(), vertex.z())
                .uv(u, v)
                .color(1.0F, 1.0F, 1.0F, this.alpha)
                .uv2(light)
                .endVertex();
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240 | super.getLightColor(partialTick) & 0xFF0000;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return DragonParticleRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE;
    }

    public static final class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new SecondImpactRingParticle(level, x, y, z, this.sprites);
        }
    }
}
