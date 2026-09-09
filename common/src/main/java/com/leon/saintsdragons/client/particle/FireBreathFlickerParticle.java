package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.leon.saintsdragons.common.particle.ExpandingBreathSection;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class FireBreathFlickerParticle extends TextureSheetParticle {
    private static final int FRAMES = 5;
    private static final float FRAME_TICKS = 0.5F;
    private final SpriteSet sprites;
    private final float peakSize;
    private final float spin;
    private double distance;
    private boolean stopped;

    private FireBreathFlickerParticle(ClientLevel level, double x, double y, double z,
                                    double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        Vec3 forward = new Vec3(vx, vy, vz).normalize();
        double speed = Mth.clamp(new Vec3(vx, vy, vz).length(), 0.5, 12)
                * (0.85 + random.nextDouble() * 0.15);
        Vec3 reference = Math.abs(forward.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = forward.cross(reference).normalize();
        Vec3 up = right.cross(forward).normalize();
        Vec3 velocity = forward.add(right.scale((random.nextDouble() - 0.5) * 0.09))
                .add(up.scale((random.nextDouble() - 0.5) * 0.09)).normalize().scale(speed);
        this.xd = velocity.x;
        this.yd = velocity.y;
        this.zd = velocity.z;
        this.lifetime = Math.min(ExpandingBreathSection.MAX_TICKS,
                (int) Math.ceil(ExpandingBreathSection.DEFAULT_RANGE / speed)) + 1;
        this.peakSize = 0.85F + random.nextFloat() * 0.5F;
        this.spin = (random.nextFloat() - 0.5F) * 0.08F;
        this.roll = this.oRoll = random.nextFloat() * Mth.TWO_PI;
        this.hasPhysics = false;
        this.setSize(0.05F, 0.05F);
        this.quadSize = peakSize * 0.4F;
        this.alpha = 0;
        this.setSprite(sprites.get(0, FRAMES - 1));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        oRoll = roll;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        roll += spin;
        if (stopped) return;
        Vec3 start = new Vec3(x, y, z);
        Vec3 velocity = new Vec3(xd, yd, zd);
        double travel = Math.min(velocity.length(), ExpandingBreathSection.DEFAULT_RANGE - distance);
        Vec3 end = start.add(velocity.normalize().scale(travel));
        var contextEntity = Minecraft.getInstance().getCameraEntity();
        if (contextEntity == null || !level.hasChunksAt(BlockPos.containing(start), BlockPos.containing(end))) {
            remove();
            return;
        }
        HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, contextEntity));
        if (hit.getType() == HitResult.Type.BLOCK) {
            end = hit.getLocation().subtract(velocity.normalize().scale(0.03));
            stopped = true;
            lifetime = Math.min(lifetime, age + 3);
        }
        setPos(end.x, end.y, end.z);
        distance += travel;
        if (distance >= ExpandingBreathSection.DEFAULT_RANGE) stopped = true;
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        float renderAge = Math.max(0, age - 1 + partialTicks);
        setSprite(sprites.get((int) (renderAge / FRAME_TICKS) % FRAMES, FRAMES - 1));
        float progress = Mth.clamp(renderAge / lifetime, 0, 1);
        float grow = smooth(Mth.clamp(progress / 0.18F, 0, 1));
        float fade = 1 - smooth(Mth.clamp((progress - 0.45F) / 0.55F, 0, 1));
        quadSize = peakSize * Mth.lerp(grow, 0.4F, 1.0F) * Mth.lerp(fade, 0.3F, 1.0F);
        alpha = 0.9F * grow * fade;
        super.render(buffer, camera, partialTicks);
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
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new FireBreathFlickerParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
