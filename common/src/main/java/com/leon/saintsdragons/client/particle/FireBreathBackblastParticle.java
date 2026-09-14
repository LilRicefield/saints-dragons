package com.leon.saintsdragons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.leon.saintsdragons.common.particle.ExpandingBreathSection;
import com.leon.saintsdragons.common.registry.ModParticles;
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

public final class FireBreathBackblastParticle extends TextureSheetParticle {
    private static final double RANGE = 8.0;
    private static final int FRAMES = 17;
    private static final float FRAME_TICKS = 0.5F;
    private final SpriteSet sprites;
    private final float peakSize;
    private double distance;
    private boolean stopped;
    private final boolean emitsEmbers;
    private final int sparkAge;
    private final int starAge;
    private int sparksEmitted;
    private boolean starEmitted;
    private boolean chargeBurst;

    private FireBreathBackblastParticle(ClientLevel level, double x, double y, double z,
                                    double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        emitsEmbers = random.nextFloat() < 0.15F;
        sparkAge = 2 + random.nextInt(3);
        starAge = random.nextFloat() < 0.35F ? 1 + random.nextInt(6) : -1;
        Vec3 forward = new Vec3(vx, vy, vz).normalize();
        double speed = Mth.clamp(new Vec3(vx, vy, vz).length(), 0.5, 12)
                * (0.85 + random.nextDouble() * 0.15);
        Vec3 reference = Math.abs(forward.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = forward.cross(reference).normalize();
        Vec3 up = right.cross(forward).normalize();
        Vec3 velocity = forward.add(right.scale((random.nextDouble() - 0.5) * 0.06))
                .add(up.scale((random.nextDouble() - 0.5) * 0.06)).normalize().scale(speed);
        this.xd = velocity.x;
        this.yd = velocity.y;
        this.zd = velocity.z;
        this.lifetime = Math.min(ExpandingBreathSection.MAX_TICKS,
                (int) Math.ceil(RANGE / speed)) + 1;
        this.peakSize = 0.65F + random.nextFloat() * 0.25F;
        this.roll = this.oRoll = 0;
        this.setColor(1.0F, 0.55F, 0.08F);
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
        if (stopped) {
            emitAccents();
            return;
        }
        Vec3 start = new Vec3(x, y, z);
        Vec3 velocity = new Vec3(xd, yd, zd);
        double travel = Math.min(velocity.length(), RANGE - distance);
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
        if (distance >= RANGE) stopped = true;
        emitAccents();
    }

    public void configureChargeBurst() {
        lifetime = Math.min(lifetime, 5);
        chargeBurst = true;
    }

    private void emitAccents() {
        if (chargeBurst) return;
        Vec3 position = new Vec3(xo, yo, zo).lerp(new Vec3(x, y, z), random.nextDouble());
        var engine = Minecraft.getInstance().particleEngine;
        if (!starEmitted && starAge >= 0 && age >= starAge) {
            starEmitted = true;
            engine.createParticle(ModParticles.FIRE_BREATH_STAR.get(),
                    position.x, position.y, position.z, 0, 0, 0);
        }
        if (!emitsEmbers || sparksEmitted >= 2 || age < sparkAge + sparksEmitted * 2) return;
        sparksEmitted++;
        Vec3 direction = new Vec3(xd, yd, zd).normalize();
        Vec3 reference = Math.abs(direction.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = direction.cross(reference).normalize();
        Vec3 up = right.cross(direction).normalize();
        double yaw = Math.toRadians((random.nextDouble() - 0.5) * -40);
        double pitch = Math.toRadians((random.nextDouble() - 0.5) * 40);
        Vec3 velocity = direction.scale(Math.cos(yaw) * Math.cos(pitch))
                .add(right.scale(Math.sin(yaw) * Math.cos(pitch))).add(up.scale(Math.sin(pitch)))
                .scale((1 + random.nextDouble() * 3) / 20.0);
        engine.createParticle(ModParticles.FIRE_BREATH_EMBER.get(), position.x, position.y, position.z,
                velocity.x, velocity.y, velocity.z);
        Particle glow = engine.createParticle(ModParticles.GLOWING_EMITTER.get(),
                position.x, position.y, position.z, random.nextGaussian() * 0.16,
                random.nextGaussian() * 0.16, random.nextGaussian() * 0.16);
        if (glow != null) glow.setColor(1.0F, 0.55F, 0.12F);
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
            return new FireBreathBackblastParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}

