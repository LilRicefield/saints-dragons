package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.common.particle.GroundDecalParticleData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

/**
 * World-fixed horizontal decal used by both ordinary cracks and Dragonlord's fissure visual.
 */
public final class GroundDecalParticle extends TextureSheetParticle {
    private static final float NORMAL_START_SCALE_RATIO = 4.0F / 7.0F;
    private static final float FISSURE_START_SCALE_RATIO = 0.72F;
    private static final float GROW_TICKS = 5.0F;
    private static final float FISSURE_FADE_START = 0.82F;

    private final boolean fissure;
    private final float finalScale;
    private final float rotation;

    private GroundDecalParticle(ClientLevel level, double x, double y, double z,
                                GroundDecalParticleData data, SpriteSet sprites) {
        super(level, x, y, z);
        this.fissure = data.fissure();
        this.finalScale = Math.max(0.5F, data.scale());
        this.rotation = -data.yaw() * Mth.DEG_TO_RAD;
        this.lifetime = Math.max(1, data.duration());
        this.quadSize = this.finalScale;
        this.alpha = 1.0F;
        this.hasPhysics = false;
        this.setSprite(sprites.get(this.random));
        double radius = this.finalScale * Math.sqrt(2.0D);
        this.setBoundingBox(new net.minecraft.world.phys.AABB(x - radius, y - 0.1D, z - radius,
                x + radius, y + 0.1D, z + radius));
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
        float age = this.age + partialTicks;
        float grow = Mth.clamp(age / GROW_TICKS, 0.0F, 1.0F);
        float startScale = this.fissure ? FISSURE_START_SCALE_RATIO : NORMAL_START_SCALE_RATIO;
        this.quadSize = this.finalScale * Mth.lerp(grow, startScale, 1.0F);

        float progress = Mth.clamp(age / this.lifetime, 0.0F, 1.0F);
        this.alpha = this.fissure
                ? (progress < FISSURE_FADE_START
                    ? 1.0F
                    : Mth.clamp((1.0F - progress) / (1.0F - FISSURE_FADE_START), 0.0F, 1.0F))
                : Math.max(1.0F - progress * progress, 0.0F);

        Vec3 cameraPos = camera.getPosition();
        float x = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float y = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float z = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());
        float rightX = Mth.cos(this.rotation);
        float rightZ = Mth.sin(this.rotation);
        float forwardX = -rightZ;
        float forwardZ = rightX;
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
    }

    private void drawQuad(VertexConsumer buffer, Vector3f[] vertices,
                          float u0, float u1, float v0, float v1, int light) {
        vertex(buffer, vertices[0], u1, v1, light);
        vertex(buffer, vertices[1], u1, v0, light);
        vertex(buffer, vertices[2], u0, v0, light);
        vertex(buffer, vertices[3], u0, v1, light);
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
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Factory implements ParticleProvider<GroundDecalParticleData> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull GroundDecalParticleData data, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new GroundDecalParticle(level, x, y, z, data, this.sprites);
        }
    }
}
