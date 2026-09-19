package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.common.particle.VolitansBreathMotion;
import com.leon.saintsdragons.common.particle.VolitansBreathParticleData;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class VolitansBreathEmitterParticle extends Particle {
    private record Droplet(VolitansBreathParticle particle, double birth) {}

    private final Volitans dragon;
    private final VolitansBreathEmission emission = new VolitansBreathEmission();
    private final List<Droplet> droplets = new ArrayList<>();
    private Vec3 serverDirection;
    private Vec3 lastMouth;
    private Vec3 lastDirection;
    private double lastFrame;
    private int waterIndex;
    private int quietTicks;
    private long lastEngineTick;

    private VolitansBreathEmitterParticle(ClientLevel level, Volitans dragon, Vec3 direction) {
        super(level, dragon.getX(), dragon.getY(), dragon.getZ());
        this.dragon = dragon;
        this.serverDirection = direction;
        lastEngineTick = level.getGameTime();
        hasPhysics = false;
        updateBounds();
    }

    private boolean emitting() {
        return dragon.isAlive() && !dragon.isRemoved() && dragon.isBreathing();
    }

    private boolean isManaged() {
        return isAlive() && level.getGameTime() - lastEngineTick <= 2;
    }

    @Override
    public void tick() {
        lastEngineTick = level.getGameTime();
        if (emitting()) {
            quietTicks = 0;
        } else {
            emission.reset();
            lastMouth = null;
            if (++quietTicks > VolitansBreathMotion.LIFETIME + 4) remove();
        }
        updateBounds();
    }

    private void updateBounds() {
        setPos(dragon.getX(), dragon.getY(), dragon.getZ());
        AABB bounds = dragon.getBoundingBox().inflate(VolitansBreathMotion.RANGE + 24.0D);
        for (Droplet droplet : droplets) bounds = bounds.minmax(droplet.particle.getBoundingBox());
        setBoundingBox(bounds);
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTick) {
        if (ShaderPassCompatibility.isIrisShadowPass()) return;
        double now = level.getGameTime() + (double) partialTick;
        if (emitting()) {
            Vec3 mouth = dragon.getBreathVisualOrigin(partialTick);
            var rider = dragon.getControllingPassenger();
            Vec3 direction = rider != null ? rider.getViewVector(partialTick) : serverDirection;
            if (direction.lengthSqr() < 1.0E-8) direction = dragon.getViewVector(partialTick);
            if (lastMouth == null || now < lastFrame || now - lastFrame > 2.0D
                    || mouth.distanceToSqr(lastMouth) > 256.0D) {
                emission.reset();
                lastFrame = now;
                lastMouth = mouth;
                lastDirection = direction;
            }
            Vec3 forward = direction;
            emission.emitUntil(now, dragon.isPoisonBreathMode(),
                    (kind, birth) -> emitDroplet(kind, birth, now, mouth, forward));
            lastMouth = mouth;
            lastDirection = direction;
        } else {
            emission.reset();
            lastMouth = null;
        }
        lastFrame = now;
        for (var iterator = droplets.iterator(); iterator.hasNext();) {
            Droplet droplet = iterator.next();
            droplet.particle.renderContinuous(buffer, camera, (float) Math.max(0, now - droplet.birth));
            if (!droplet.particle.isAlive()) iterator.remove();
        }
    }

    private void emitDroplet(VolitansBreathParticle.Kind kind, double birth, double now,
                             Vec3 mouth, Vec3 direction) {
        double fraction = now > lastFrame ? Mth.clamp((birth - lastFrame) / (now - lastFrame), 0, 1) : 1;
        Vec3 origin = lastMouth.lerp(mouth, fraction);
        Vec3 forward = lastDirection.lerp(direction, fraction);
        if (forward.lengthSqr() < 1.0E-8) forward = direction;
        Vec3 velocity = forward.normalize().scale(VolitansBreathMotion.SPEED);
        boolean core = kind == VolitansBreathParticle.Kind.POISON
                || kind == VolitansBreathParticle.Kind.WATER && waterIndex++ % 4 == 0;
        var particle = VolitansBreathParticle.createStreamParticle(level, origin, velocity, kind, core);
        if (particle != null) droplets.add(new Droplet(particle, birth));
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Factory implements ParticleProvider<VolitansBreathParticleData> {
        private final Map<Volitans, WeakReference<VolitansBreathEmitterParticle>> emitters = new WeakHashMap<>();

        @Override
        public Particle createParticle(@NotNull VolitansBreathParticleData data, @NotNull ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            if (!(level.getEntity(data.dragonId()) instanceof Volitans dragon)) return null;
            Vec3 direction = new Vec3(vx, vy, vz).normalize();
            var reference = emitters.get(dragon);
            var emitter = reference == null ? null : reference.get();
            if (emitter != null && emitter.isManaged()) {
                emitter.serverDirection = direction;
                return null;
            }
            emitter = new VolitansBreathEmitterParticle(level, dragon, direction);
            emitters.put(dragon, new WeakReference<>(emitter));
            return emitter;
        }
    }
}
