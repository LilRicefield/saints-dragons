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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class IgnivorusExplosionFireParticle extends TextureSheetParticle {
    private static final int FRAME_COUNT = 16;
    private static final float SCROLL_SPEED = 0.5F;
    private static final float SIZE_SCALE = 5.0F;
    private static final float TAIL_START = 0.45F;
    private static final float EXPAND_AT_DEATH = 4.0F;
    private static final float TINT_R = 1.0F;
    private static final float TINT_G = 0.55F;
    private static final float TINT_B = 0.12F;

    private static final float[][] LAYERS = {
            { 0.00F,  0.00F, 1.00F,  0.00F,  0.00F },
    };

    private final SpriteSet sprites;
    private final float baseSize;
    private final float spin;

    private IgnivorusExplosionFireParticle(ClientLevel level, double x, double y, double z,
                                           double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.hasPhysics = false;
        this.lifetime = 12 + this.random.nextInt(5);
        this.baseSize = (0.9F + this.random.nextFloat() * 0.5F) * SIZE_SCALE;
        this.spin = (this.random.nextFloat() - 0.5F) * 0.16F;
        this.roll = this.oRoll = this.random.nextFloat() * (float) (Math.PI * 2.0);
        this.quadSize = 0.0F;
        this.setColor(TINT_R, TINT_G, TINT_B);
        this.pickSprite(sprites);
        this.updateCullBox();
    }

    private void updateCullBox() {
        double r = this.baseSize * (1.0F + EXPAND_AT_DEATH) * 1.6D;
        this.setBoundingBox(new AABB(this.x - r, this.y - r, this.z - r,
                this.x + r, this.y + r, this.z + r));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.roll += this.spin;
        // The centered render bounds are not a vanilla particle movement box (Y is its minimum).
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        this.xd *= 0.90;
        this.yd *= 0.90;
        this.zd *= 0.90;
        this.updateCullBox();
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        float progress = Mth.clamp((this.age - 1 + partialTicks) / this.lifetime, 0.0F, 1.0F);
        float growIn = smooth(Mth.clamp(progress / 0.18F, 0.0F, 1.0F));
        float tail = Mth.clamp((progress - TAIL_START) / (1.0F - TAIL_START), 0.0F, 1.0F);
        float expand = 1.0F - (1.0F - tail) * (1.0F - tail);
        this.alpha = 1.0F - smooth(tail);
        if (this.alpha <= 0.001F) {
            return;
        }

        float half = this.baseSize * growIn * (1.0F + EXPAND_AT_DEATH * expand);
        float frameBase = (this.age - 1 + partialTicks) * SCROLL_SPEED;
        float rollNow = Mth.lerp(partialTicks, this.oRoll, this.roll);

        Vec3 center = new Vec3(
                Mth.lerp(partialTicks, this.xo, this.x),
                Mth.lerp(partialTicks, this.yo, this.y),
                Mth.lerp(partialTicks, this.zo, this.z))
                .subtract(camera.getPosition());

        for (float[] layer : LAYERS) {
            int frame = Math.floorMod(Mth.floor(frameBase + layer[3]), FRAME_COUNT);
            this.setSprite(this.sprites.get(frame, FRAME_COUNT - 1));

            Quaternionf rotation = new Quaternionf(camera.rotation()).rotateZ(rollNow + layer[4]);
            float layerHalf = half * layer[2];
            float ox = layer[0];
            float oy = layer[1];

            corner(buffer, rotation, center, layerHalf, ox - 1.0F, oy - 1.0F, getU1(), getV1());
            corner(buffer, rotation, center, layerHalf, ox - 1.0F, oy + 1.0F, getU1(), getV0());
            corner(buffer, rotation, center, layerHalf, ox + 1.0F, oy + 1.0F, getU0(), getV0());
            corner(buffer, rotation, center, layerHalf, ox + 1.0F, oy - 1.0F, getU0(), getV1());
        }
    }

    private void corner(VertexConsumer buffer, Quaternionf rotation, Vec3 center, float half,
                        float unitX, float unitY, float u, float v) {
        Vector3f offset = new Vector3f(unitX, unitY, 0.0F).mul(half).rotate(rotation);
        buffer.vertex(center.x + offset.x, center.y + offset.y, center.z + offset.z)
                .uv(u, v)
                .color(this.rCol, this.gCol, this.bCol, this.alpha)
                .uv2(0xF000F0)
                .endVertex();
    }

    private static float smooth(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    @Override
    public int getLightColor(float partialTicks) {
        return 0xF000F0;
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
            return new IgnivorusExplosionFireParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
