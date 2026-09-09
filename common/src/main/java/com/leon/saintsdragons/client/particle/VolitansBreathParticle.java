package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.common.particle.VolitansBreathMotion;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class VolitansBreathParticle extends TextureSheetParticle {
    private static final int WATER_PARTICLES_PER_SECTION = 32;
    private static final double WATER_EMISSION_INTERVAL_TICKS = 2.0D;
    public enum Kind { WATER, POISON, BUBBLES, EMITTER, STAR, POISON_SKULL, POISON_FLAME }

    private final SpriteSet sprites;
    private final Kind kind;
    private final int frames;
    private final float frameTicks;
    private final boolean highlight;
    private final boolean poison;
    private final float peakSize;
    private final int frameOffset;

    private VolitansBreathParticle(ClientLevel level, double x, double y, double z,
                                   Vec3 velocity, SpriteSet sprites, Kind kind) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.kind = kind;
        this.highlight = kind == Kind.EMITTER || kind == Kind.STAR;
        this.poison = kind == Kind.POISON || kind == Kind.POISON_SKULL || kind == Kind.POISON_FLAME;
        this.frames = switch (kind) {
            case EMITTER, STAR -> 1;
            case BUBBLES -> 12;
            case POISON_SKULL -> 16;
            case POISON_FLAME -> 20;
            default -> 5;
        };
        this.frameTicks = switch (kind) {
            case POISON -> 3.0F;
            case POISON_SKULL -> 0.75F;
            case POISON_FLAME -> 0.5F;
            default -> 1.0F;
        };
        this.frameOffset = kind == Kind.WATER ? random.nextInt(frames) : 0;
        this.peakSize = kind == Kind.STAR ? 0.35F + random.nextFloat() * 0.25F
                : 0.10F + random.nextFloat() * 0.10F;
        Vec3 forward = velocity.lengthSqr() > 1.0E-8 ? velocity.normalize() : new Vec3(0, 0, 1);
        double spread = VolitansBreathMotion.SPREAD * (highlight ? 2.0D : 1.0D);
        Vec3 launch = forward.add(
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread,
                (random.nextDouble() - 0.5) * spread)
                .normalize().scale(VolitansBreathMotion.SPEED);
        xd = launch.x;
        yd = launch.y;
        zd = launch.z;
        lifetime = VolitansBreathMotion.LIFETIME;
        if (highlight) {
            setColor(0.55F, 0.90F, 1.0F);
            roll = oRoll = random.nextFloat() * Mth.TWO_PI;
        } else if (kind == Kind.POISON_SKULL) {
            setColor(0.35F, 1.0F, 0.15F);
        }
        hasPhysics = false;
        setSize(1.44F, 1.44F);
        quadSize = 0.34F;
        if (highlight) pickSprite(sprites);
        else setSprite(sprites.get(frameOffset, frames - 1));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) {
            remove();
            return;
        }
        if (!advance(new Vec3(xd, yd, zd))) return;
        xd *= VolitansBreathMotion.DRAG;
        yd *= VolitansBreathMotion.DRAG;
        zd *= VolitansBreathMotion.DRAG;
    }

    private boolean advance(Vec3 movement) {
        Vec3 start = new Vec3(x, y, z);
        Vec3 end = start.add(movement);
        var cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity == null || !level.hasChunksAt(BlockPos.containing(start), BlockPos.containing(end))) {
            remove();
            return false;
        }
        var hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, cameraEntity));
        if (hit.getType() != HitResult.Type.MISS || (!poison && touchesLava(start, end))) {
            remove();
            return false;
        }
        setPos(end.x, end.y, end.z);
        return true;
    }

    private boolean touchesLava(Vec3 start, Vec3 end) {
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(Math.min(start.x, end.x), Math.min(start.y, end.y), Math.min(start.z, end.z)),
                BlockPos.containing(Math.max(start.x, end.x), Math.max(start.y, end.y), Math.max(start.z, end.z)))) {
            var fluid = level.getFluidState(pos);
            if (!fluid.is(FluidTags.LAVA)) continue;
            for (var box : fluid.getShape(level, pos).toAabbs()) {
                var worldBox = box.move(pos);
                if (worldBox.contains(start) || worldBox.clip(start, end).isPresent()) return true;
            }
        }
        return false;
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        float time = age + partialTicks;
        float progress = Mth.clamp(time / lifetime, 0.0F, 1.0F);
        int frame = Mth.floor(time / frameTicks) + frameOffset;
        if (!highlight) {
            setSprite(sprites.get(frame % frames, frames - 1));
        }
        if (highlight) {
            float fadeIn = Mth.clamp(progress / 0.15F, 0.0F, 1.0F);
            float fadeStart = kind == Kind.EMITTER ? 0.30F : 0.15F;
            float fadeOut = 1.0F - Mth.clamp((progress - fadeStart) / (1.0F - fadeStart), 0.0F, 1.0F);
            alpha = fadeIn * fadeOut;
            quadSize = peakSize * fadeIn * (0.35F + 0.65F * fadeOut);
        } else if (kind == Kind.BUBBLES) {
            quadSize = Mth.lerp(progress, 0.30F, 0.60F);
            alpha = 1.0F - Mth.clamp((progress - 0.75F) / 0.25F, 0.0F, 1.0F);
        } else {
            quadSize = Mth.lerp(progress, 0.34F, 0.72F);
            if (kind == Kind.WATER || kind == Kind.POISON_SKULL || kind == Kind.POISON_FLAME) {
                float fade = Mth.clamp((progress - 0.35F) / 0.65F, 0.0F, 1.0F);
                alpha = 1.0F - fade * fade * (3.0F - 2.0F * fade);
            } else {
                alpha = Mth.lerp(progress, 1.0F, 0.82F);
            }
        }
        super.render(buffer, camera, partialTicks);
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
        private final Kind kind;

        public Factory(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            Vec3 velocity = new Vec3(vx, vy, vz);
            var engine = Minecraft.getInstance().particleEngine;
            if (kind == Kind.WATER) {
                engine.createParticle(ModParticles.VOLITANS_BREATH_BUBBLES.get(), x, y, z, vx, vy, vz);
                engine.createParticle(ModParticles.VOLITANS_BREATH_EMITTER.get(), x, y, z, vx, vy, vz);
                engine.createParticle(ModParticles.VOLITANS_BREATH_STAR.get(), x, y, z, vx, vy, vz);
            } else if (kind == Kind.POISON) {
                engine.createParticle(ModParticles.VOLITANS_POISON_SKULL.get(), x, y, z, vx, vy, vz);
                engine.createParticle(ModParticles.VOLITANS_POISON_FLAME.get(), x, y, z, vx, vy, vz);
            }
            int count = switch (kind) {
                case WATER -> WATER_PARTICLES_PER_SECTION;
                case BUBBLES, POISON_SKULL -> 4;
                case EMITTER, STAR -> 16;
                default -> VolitansBreathMotion.PARTICLES_PER_SECTION;
            };
            for (int i = 1; i < count; i++) {
                var particle = new VolitansBreathParticle(level, x, y, z, velocity, sprites, kind);
                if (kind == Kind.WATER) {
                    // Fill the distance between two-tick emissions, with the same terrain checks
                    // as normal travel so extra particles cannot start on the far side of a wall.
                    double offsetTicks = WATER_EMISSION_INTERVAL_TICKS * i / count;
                    if (!particle.advance(new Vec3(particle.xd, particle.yd, particle.zd).scale(offsetTicks))) continue;
                    particle.xo = particle.x;
                    particle.yo = particle.y;
                    particle.zo = particle.z;
                }
                engine.add(particle);
            }
            return new VolitansBreathParticle(level, x, y, z, velocity, sprites, kind);
        }
    }
}
