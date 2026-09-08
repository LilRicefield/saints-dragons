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
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Cosmetic child of a flame; emitter settings use seconds and blocks as the stud scale. */
public final class FireBreathEmberParticle extends TextureSheetParticle {
    private static final double Z_OFFSET = 0.25;
    private static final double UP_ACCELERATION = 3.0 / (20.0 * 20.0);
    private final float spin;
    private final float peakSize;

    private FireBreathEmberParticle(ClientLevel level, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.lifetime = 30 + random.nextInt(31);
        this.spin = (float) Math.toRadians(random.nextDouble() * 180.0 / 20.0);
        this.peakSize = 0.045F + random.nextFloat() * 0.025F;
        this.hasPhysics = false;
        this.quadSize = 0;
        this.setColor(1.0F, 0.48F, 0.055F);
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.oRoll = roll;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        roll += spin;
        // Constant world-up acceleration; no drag or inherited flame velocity was requested.
        move(xd, yd + UP_ACCELERATION * 0.5, zd);
        yd += UP_ACCELERATION;
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        float progress = Mth.clamp((age - 1 + partialTicks) / lifetime, 0, 1);
        float grow = smooth(Mth.clamp(progress / 0.15F, 0, 1));
        float shrink = 1 - smooth(Mth.clamp((progress - 0.60F) / 0.40F, 0, 1));
        this.quadSize = peakSize * grow * shrink;
        this.alpha = 0.95F * shrink;

        Vec3 position = new Vec3(Mth.lerp(partialTicks, xo, x), Mth.lerp(partialTicks, yo, y),
                Mth.lerp(partialTicks, zo, z));
        Vec3 towardCamera = camera.getPosition().subtract(position);
        double distance = towardCamera.length();
        double offsetDistance = Math.min(Z_OFFSET, distance * 0.5);
        Vec3 offset = distance > 1.0E-6 ? towardCamera.scale(offsetDistance / distance) : Vec3.ZERO;
        // Offset only the drawing, preserving apparent size and leaving the motion untouched.
        if (distance > 1.0E-6) this.quadSize *= (float) ((distance - offsetDistance) / distance);
        Vec3 center = position.add(offset).subtract(camera.getPosition());
        Quaternionf rotation = new Quaternionf(camera.rotation())
                .rotateZ(Mth.lerp(partialTicks, oRoll, roll));
        // Vanilla particles stretch every texture onto a square. Preserve this ember's
        // 16x32 artwork with a half-width of 0.5 relative to its half-height.
        vertex(buffer, rotation, center, -0.5F, -1, getU1(), getV1());
        vertex(buffer, rotation, center, -0.5F, 1, getU1(), getV0());
        vertex(buffer, rotation, center, 0.5F, 1, getU0(), getV0());
        vertex(buffer, rotation, center, 0.5F, -1, getU0(), getV1());
    }

    private void vertex(VertexConsumer buffer, Quaternionf rotation, Vec3 center,
                        float localX, float localY, float u, float v) {
        Vector3f corner = new Vector3f(localX, localY, 0).mul(quadSize).rotate(rotation);
        buffer.vertex(center.x + corner.x, center.y + corner.y, center.z + corner.z)
                .uv(u, v).color(rCol, gCol, bCol, alpha).uv2(0xF000F0).endVertex();
    }

    private static float smooth(float value) {
        return value * value * (3 - 2 * value);
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
                                       double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new FireBreathEmberParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
