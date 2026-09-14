package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusFireballEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;

public final class IgnivorusFireballTrail {
    private IgnivorusFireballTrail() {}

    public static void emit(IgnivorusFireballEntity dragon) {
        if (dragon.getVisualScale() >= 8.0F) {
            emitCharged(dragon);
            return;
        }
        float sizeMultiplier = dragon.getVisualScale() >= 6.0F ? 1.5F : 1.0F;
        var random = dragon.level().random;
        Vec3 velocity = dragon.getDeltaMovement();
        Vec3 center = dragon.position().add(0.0D, dragon.getBbHeight() * 0.5D, 0.0D);
        for (int sample = 0; sample < 6; sample++) {
            double along = (sample + random.nextDouble()) / 6.0D;
            Vec3 origin = center.subtract(velocity.scale(along));
            for (int layer = 0; layer < 5; layer++) {
                var type = switch (layer) {
                    case 0 -> ModParticles.CINDERVANE_DARK_FIRE_TRAIL.get();
                    case 1 -> ModParticles.CINDERVANE_FIRE_TRAIL.get();
                    case 2 -> ModParticles.IGNIVORUS_FIREBALL_BRIGHT_FIRE.get();
                    case 3 -> ModParticles.IGNIVORUS_FIREBALL_ORANGE_SPEC.get();
                    default -> ModParticles.CINDERVANE_SPEC_TRAIL.get();
                };
                double spread = (layer == 0 ? 1.8D : layer == 2 ? 0.9D : 1.4D) * sizeMultiplier;
                Vec3 offset = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D,
                        random.nextDouble() - 0.5D).scale(spread);
                Vec3 point = origin.add(offset);
                Vec3 drift = velocity.scale(0.08D).add(offset.scale(0.12D)).add(0.0D, 0.025D, 0.0D);
                spawn(sizeMultiplier, type, point.x, point.y, point.z, drift.x, drift.y, drift.z);
            }
            Vec3 smoke = origin.add((random.nextDouble() - 0.5D) * 1.8D * sizeMultiplier,
                    (random.nextDouble() - 0.5D) * 1.8D * sizeMultiplier, (random.nextDouble() - 0.5D) * 1.8D * sizeMultiplier);
            spawn(sizeMultiplier, ModParticles.CINDERVANE_FIRE_BODY_SMOKE.get(),
                    smoke.x, smoke.y, smoke.z, velocity.x * 0.025D,
                    velocity.y * 0.025D + 0.035D, velocity.z * 0.025D);
        }
        for (int i = 0; i < 16; i++) {
            Vec3 offset = new Vec3(random.nextDouble() - 0.5D, random.nextDouble() - 0.5D,
                    random.nextDouble() - 0.5D);
            Vec3 point = center.add(offset.scale(1.2D * sizeMultiplier));
            Vec3 drift = velocity.scale(0.08D).add(offset.scale(0.45D));
            spawn(sizeMultiplier, ModParticles.CINDERVANE_MOUTH_EMITTER.get(),
                    point.x, point.y, point.z, drift.x, drift.y, drift.z);
        }
    }

    private static void emitCharged(IgnivorusFireballEntity fireball) {
        var random = fireball.level().random;
        Vec3 velocity = fireball.getDeltaMovement();
        Vec3 center = fireball.position().add(0, fireball.getBbHeight() * 0.5D, 0);
        Vec3 forward = velocity.normalize();
        if (forward.lengthSqr() < 1.0E-6D) forward = Vec3.directionFromRotation(fireball.getXRot(), fireball.getYRot());
        Vec3 right = forward.cross(Math.abs(forward.y) > 0.99D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(forward).normalize();
        for (int sample = 0; sample < 6; sample++) {
            Vec3 origin = center.subtract(velocity.scale((sample + random.nextDouble()) / 6.0D));
            for (int layer = 0; layer < 7; layer++) {
                if (layer == 5 && (sample & 1) != 0) continue;
                SimpleParticleType type = switch (layer) {
                    case 0, 6 -> ModParticles.IGNIVORUS_CHARGED_FIRE_TRAIL.get();
                    case 1 -> ModParticles.IGNIVORUS_CHARGED_SPEC_TRAIL.get();
                    case 2 -> ModParticles.IGNIVORUS_CHARGED_MORE_SPEC_TRAIL.get();
                    case 3 -> ModParticles.IGNIVORUS_CHARGED_EMBER_TRAIL.get();
                    case 4 -> ModParticles.IGNIVORUS_CHARGED_EMITTER_TRAIL.get();
                    default -> ModParticles.IGNIVORUS_CHARGED_STAR_TRAIL.get();
                };
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = (layer >= 3 && layer <= 5 ? 0.8D : 0.25D) + random.nextDouble() * 1.3D;
                Vec3 offset = right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
                Vec3 point = origin.add(offset);
                Vec3 drift = velocity.scale(0.04D).add(offset.scale(0.06D)).add(0, 0.025D, 0);
                spawn(1, type, point.x, point.y, point.z, drift.x, drift.y, drift.z);
            }
        }
    }

    private static void spawn(float size, SimpleParticleType type, double x, double y, double z,
                              double vx, double vy, double vz) {
        var particle = Minecraft.getInstance().particleEngine.createParticle(type, x, y, z, vx, vy, vz);
        if (particle instanceof CindervaneFireTrailParticle fire) fire.setBodySizeMultiplier(size);
        else if (particle instanceof CindervaneFireBodySmokeParticle smoke) smoke.setSizeMultiplier(size);
        else if (particle instanceof CindervaneImpactEmitterParticle emitter) emitter.setSizeMultiplier(size);
    }
}
