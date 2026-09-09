package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.common.particle.VolitansBreathMotion;
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
    private static final int FRAMES = 5;
    private static final float FRAME_TICKS = 3.0F;
    private final SpriteSet sprites;
    private final boolean poison;

    private VolitansBreathParticle(ClientLevel level, double x, double y, double z,
                                   Vec3 velocity, SpriteSet sprites, boolean poison) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.poison = poison;
        Vec3 forward = velocity.lengthSqr() > 1.0E-8 ? velocity.normalize() : new Vec3(0, 0, 1);
        Vec3 launch = forward.add(
                (random.nextDouble() - 0.5) * VolitansBreathMotion.SPREAD,
                (random.nextDouble() - 0.5) * VolitansBreathMotion.SPREAD,
                (random.nextDouble() - 0.5) * VolitansBreathMotion.SPREAD)
                .normalize().scale(VolitansBreathMotion.SPEED);
        xd = launch.x;
        yd = launch.y;
        zd = launch.z;
        lifetime = VolitansBreathMotion.LIFETIME;
        hasPhysics = false;
        setSize(1.44F, 1.44F);
        quadSize = 0.34F;
        setSprite(sprites.get(0, FRAMES - 1));
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
        Vec3 start = new Vec3(x, y, z);
        Vec3 end = start.add(xd, yd, zd);
        var cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity == null || !level.hasChunksAt(BlockPos.containing(start), BlockPos.containing(end))) {
            remove();
            return;
        }
        var hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, cameraEntity));
        if (hit.getType() != HitResult.Type.MISS || (!poison && touchesLava(start, end))) {
            remove();
            return;
        }
        setPos(end.x, end.y, end.z);
        xd *= VolitansBreathMotion.DRAG;
        yd *= VolitansBreathMotion.DRAG;
        zd *= VolitansBreathMotion.DRAG;
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
        setSprite(sprites.get(Mth.floor(time / FRAME_TICKS) % FRAMES, FRAMES - 1));
        quadSize = Mth.lerp(progress, 0.34F, 0.72F);
        alpha = Mth.lerp(progress, 1.0F, 0.82F);
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
        private final boolean poison;

        public Factory(SpriteSet sprites, boolean poison) {
            this.sprites = sprites;
            this.poison = poison;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            Vec3 velocity = new Vec3(vx, vy, vz);
            for (int i = 1; i < VolitansBreathMotion.PARTICLES_PER_SECTION; i++) {
                Minecraft.getInstance().particleEngine.add(
                        new VolitansBreathParticle(level, x, y, z, velocity, sprites, poison));
            }
            return new VolitansBreathParticle(level, x, y, z, velocity, sprites, poison);
        }
    }
}
