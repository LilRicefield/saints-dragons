package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.common.particle.GroundDecalParticleData;
import com.leon.saintsdragons.common.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class IgnivorusAftermathParticle extends NoRenderParticle {
    private final List<Vec3> groundPoints = new ArrayList<>();

    private IgnivorusAftermathParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        lifetime = 100;
    }

    @Override
    public void tick() {
        if (age == 0) {
            emitAirEffects();
            for (int i = 0; i < 49; i++) {
                Vec3 point = groundAt(x + random.nextDouble() * 32.0D - 16.0D,
                        z + random.nextDouble() * 32.0D - 16.0D);
                if (point != null) groundPoints.add(point);
            }
        }
        if (age == 12) {
            Vec3 ground = groundAt(x, z);
            if (ground != null) {
                Minecraft.getInstance().particleEngine.createParticle(
                        GroundDecalParticleData.fissure(0.0F, 16.0F, 88),
                        ground.x, ground.y, ground.z, 0.0D, 0.0D, 0.0D);
            }
        }
        if (age >= 12 && age <= 60 && age % 12 == 0) {
            for (Vec3 point : groundPoints) emitFire(point, new Vec3(0.0D, 0.04D, 0.0D));
        }
        if (++age >= lifetime) remove();
    }

    private Vec3 groundAt(double px, double pz) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.level() != level) return null;
        var hit = level.clip(new ClipContext(new Vec3(px, y + 8.0D, pz), new Vec3(px, y - 12.0D, pz),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == Direction.UP
                ? hit.getLocation().add(0.0D, 0.025D, 0.0D) : null;
    }

    private void emitAirEffects() {
        var engine = Minecraft.getInstance().particleEngine;
        Vec3 center = new Vec3(x, y + 20.0D, z);
        for (int i = 0; i < 64; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double height = random.nextDouble() * 1.5D - 0.5D;
            double horizontal = Math.sqrt(1.0D - height * height);
            Vec3 dir = new Vec3(Math.cos(angle) * horizontal, height, Math.sin(angle) * horizontal);
            Vec3 point = center.add(dir.scale(6.0D + random.nextDouble() * 15.0D));
            emitFire(point, dir.scale(0.3D + random.nextDouble() * 0.4D));
            if (i < 28) {
                Vec3 smokeOrigin = center.add(dir.scale(12.0D + random.nextDouble() * 18.0D));
                Vec3 velocity = dir.scale(0.7D + random.nextDouble() * 0.6D);
                engine.createParticle(ModParticles.IGNIVORUS_NOVA_SMOKE.get(), smokeOrigin.x, smokeOrigin.y, smokeOrigin.z,
                        velocity.x, velocity.y, velocity.z);
            }
        }
    }

    private void emitFire(Vec3 point, Vec3 velocity) {
        var engine = Minecraft.getInstance().particleEngine;
        engine.createParticle(ModParticles.IGNIVORUS_LINGERING_FIRE.get(), point.x, point.y + 0.4D, point.z,
                velocity.x, velocity.y, velocity.z);
        engine.createParticle(ModParticles.IGNIVORUS_LINGERING_SPEC.get(), point.x, point.y + 0.6D, point.z,
                velocity.x * 0.8D, velocity.y, velocity.z * 0.8D);
    }

    public static final class Factory implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new IgnivorusAftermathParticle(level, x, y, z);
        }
    }
}
