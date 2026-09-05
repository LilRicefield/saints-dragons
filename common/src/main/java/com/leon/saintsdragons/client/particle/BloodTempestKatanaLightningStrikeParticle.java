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

public final class BloodTempestKatanaLightningStrikeParticle extends TextureSheetParticle {
    private static final int FRAME_COUNT = 17;
    private static final float FRAME_DURATION_TICKS = 0.75F;
    private static final float HALF_WIDTH = 4.0F;
    private static final float HEIGHT = 10.0F;

    private final SpriteSet sprites;

    private BloodTempestKatanaLightningStrikeParticle(ClientLevel level, double x, double y, double z,
                                                      SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = Mth.ceil(FRAME_COUNT * FRAME_DURATION_TICKS);
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
        Vec3 cameraPos = camera.getPosition();
        double worldX = Mth.lerp(partialTicks, this.xo, this.x);
        double worldY = Mth.lerp(partialTicks, this.yo, this.y);
        double worldZ = Mth.lerp(partialTicks, this.zo, this.z);
        float x = (float)(worldX - cameraPos.x);
        float y = (float)(worldY - cameraPos.y);
        float z = (float)(worldZ - cameraPos.z);

        double facingX = cameraPos.x - worldX;
        double facingZ = cameraPos.z - worldZ;
        double horizontalLength = Math.sqrt(facingX * facingX + facingZ * facingZ);
        float rightX;
        float rightZ;
        if (horizontalLength > 1.0E-5D) {
            rightX = (float)(-facingZ / horizontalLength);
            rightZ = (float)(facingX / horizontalLength);
        } else {
            rightX = 1.0F;
            rightZ = 0.0F;
        }

        Vector3f bottomLeft = new Vector3f(
                x - rightX * HALF_WIDTH,
                y,
                z - rightZ * HALF_WIDTH
        );
        Vector3f topLeft = new Vector3f(
                x - rightX * HALF_WIDTH,
                y + HEIGHT,
                z - rightZ * HALF_WIDTH
        );
        Vector3f topRight = new Vector3f(
                x + rightX * HALF_WIDTH,
                y + HEIGHT,
                z + rightZ * HALF_WIDTH
        );
        Vector3f bottomRight = new Vector3f(
                x + rightX * HALF_WIDTH,
                y,
                z + rightZ * HALF_WIDTH
        );

        int light = this.getLightColor(partialTicks);
        float animationFrame = Math.max(0.0F, this.age + partialTicks) / FRAME_DURATION_TICKS;
        int currentFrame = Math.min((int)Math.floor(animationFrame), FRAME_COUNT - 1);
        int nextFrame = Math.min(currentFrame + 1, FRAME_COUNT - 1);
        float blend = nextFrame == currentFrame ? 0.0F : smoothStep(animationFrame - currentFrame);

        this.setSprite(this.sprites.get(currentFrame, FRAME_COUNT - 1));
        renderFrame(buffer, bottomLeft, topLeft, topRight, bottomRight, light,
                this.alpha * (1.0F - blend));
        if (nextFrame != currentFrame && blend > 0.001F) {
            this.setSprite(this.sprites.get(nextFrame, FRAME_COUNT - 1));
            renderFrame(buffer, bottomLeft, topLeft, topRight, bottomRight, light,
                    this.alpha * blend);
        }
    }

    private void renderFrame(VertexConsumer buffer, Vector3f bottomLeft, Vector3f topLeft,
                             Vector3f topRight, Vector3f bottomRight, int light, float alpha) {
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        vertex(buffer, bottomLeft, u1, v1, light, alpha);
        vertex(buffer, topLeft, u1, v0, light, alpha);
        vertex(buffer, topRight, u0, v0, light, alpha);
        vertex(buffer, bottomRight, u0, v1, light, alpha);
        vertex(buffer, bottomRight, u0, v1, light, alpha);
        vertex(buffer, topRight, u0, v0, light, alpha);
        vertex(buffer, topLeft, u1, v0, light, alpha);
        vertex(buffer, bottomLeft, u1, v1, light, alpha);
    }

    private void updateSprite(float partialTicks) {
        float animationAge = Math.max(0.0F, this.age + partialTicks);
        int frame = Math.min((int)(animationAge / FRAME_DURATION_TICKS), FRAME_COUNT - 1);
        this.setSprite(this.sprites.get(frame, FRAME_COUNT - 1));
    }

    private static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private void vertex(VertexConsumer buffer, Vector3f vertex, float u, float v, int light, float alpha) {
        buffer.vertex(vertex.x(), vertex.y(), vertex.z())
                .uv(u, v)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv2(light)
                .endVertex();
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240 | super.getLightColor(partialTick) & 0xFF0000;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
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
            return new BloodTempestKatanaLightningStrikeParticle(level, x, y, z, this.sprites);
        }
    }
}
