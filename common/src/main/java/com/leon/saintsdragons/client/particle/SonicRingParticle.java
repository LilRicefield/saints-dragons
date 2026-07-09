package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.common.particle.SonicRingData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SonicRingParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float yaw;
    private final float pitch;
    private final float baseSize;

    protected SonicRingParticle(ClientLevel level, double x, double y, double z,
                                double xSpeed, double ySpeed, double zSpeed,
                                SonicRingData data, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.yaw = data.yaw();
        this.pitch = data.pitch();
        this.baseSize = data.scale();
        this.lifetime = Math.max(1, data.duration());
        this.alpha = 0.95F;
        this.hasPhysics = false;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.setSpriteFromAge(this.sprites);
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.86D;
        this.yd *= 0.86D;
        this.zd *= 0.86D;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float progress = (this.age + partialTicks) / this.lifetime;
        this.quadSize = this.baseSize * (1.0F - progress - (float) Math.pow(2000.0D, -progress));
        this.alpha = Math.max(0.0F, 0.95F * (1.0F - progress));

        Vec3 cameraPos = camera.getPosition();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());
        Quaternionf rotation = new Quaternionf();
        rotation.mul(Axis.YP.rotation(this.yaw));
        rotation.mul(Axis.XP.rotation(this.pitch));

        Vector3f[] vertices = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };
        float size = this.getQuadSize(partialTicks);
        for (Vector3f vertex : vertices) {
            rotation.transform(vertex);
            vertex.mul(size);
            vertex.add(x, y, z);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);
        drawQuad(buffer, vertices, u0, u1, v0, v1, light);
        drawReverseQuad(buffer, vertices, u0, u1, v0, v1, light);
    }

    private void drawQuad(VertexConsumer buffer, Vector3f[] vertices, float u0, float u1, float v0, float v1, int light) {
        vertex(buffer, vertices[0], u1, v1, light);
        vertex(buffer, vertices[1], u1, v0, light);
        vertex(buffer, vertices[2], u0, v0, light);
        vertex(buffer, vertices[3], u0, v1, light);
    }

    private void drawReverseQuad(VertexConsumer buffer, Vector3f[] vertices, float u0, float u1, float v0, float v1, int light) {
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
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleProvider<SonicRingData> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SonicRingData type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new SonicRingParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, type, this.sprites);
        }
    }
}
