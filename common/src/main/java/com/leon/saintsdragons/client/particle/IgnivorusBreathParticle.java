package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.common.particle.ExpandingBreathSection;
import com.leon.saintsdragons.common.particle.FireBreathParticleData;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class IgnivorusBreathParticle extends Particle {
    private record Flame(FireBreathParticle particle, double birth) {}

    private final Ignivorus dragon;
    private final FireBreathParticleData data;
    private final SpriteSet sprites;
    private final FireBreathParticle.Factory factory;
    private final List<Flame> flames = new ArrayList<>();
    private double lastFrame = Double.NaN;
    private double nextFlame;
    private double nextSpec;
    private double nextSupport;
    private Vec3 lastMouth;
    private Vec3 lastDirection;
    private int flameIndex;
    private int quietTicks;
    private long lastEngineTick;

    IgnivorusBreathParticle(ClientLevel level, Ignivorus dragon, FireBreathParticleData data,
                            SpriteSet sprites, FireBreathParticle.Factory factory) {
        super(level, dragon.getX(), dragon.getY(), dragon.getZ());
        this.dragon = dragon;
        this.data = data;
        this.sprites = sprites;
        this.factory = factory;
        lastEngineTick = level.getGameTime();
        hasPhysics = false;
        updateBounds();
    }

    private boolean emitting() {
        return dragon.isAlive() && !dragon.isRemoved() && dragon.isBreathingFire()
                && dragon.getFireBreathStart() != null;
    }

    boolean isManaged() {
        return isAlive() && level.getGameTime() - lastEngineTick <= 2;
    }

    @Override
    public void tick() {
        lastEngineTick = level.getGameTime();
        if (emitting()) quietTicks = 0;
        else if (++quietTicks > ExpandingBreathSection.MAX_TICKS + 4) remove();
        updateBounds();
    }

    private void updateBounds() {
        setPos(dragon.getX(), dragon.getY(), dragon.getZ());
        AABB bounds = dragon.getBoundingBox().inflate(data.range() + 32.0D);
        for (Flame flame : flames) bounds = bounds.minmax(flame.particle.getBoundingBox());
        setBoundingBox(bounds);
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTick) {
        if (ShaderPassCompatibility.isIrisShadowPass()) return;
        double now = level.getGameTime() + (double) partialTick;
        if (emitting()) {
            Vec3 mouth = dragon.getFireBreathStartAnchor(partialTick);
            Vec3 direction = dragon.getFireBreathVisualDirection(partialTick);
            if (lastMouth == null || Double.isNaN(lastFrame) || now < lastFrame
                    || now - lastFrame > 2.0D || mouth.distanceToSqr(lastMouth) > 256.0D) {
                lastFrame = now;
                lastMouth = mouth;
                lastDirection = direction;
                nextFlame = nextSpec = nextSupport = now;
            }
            while (nextFlame <= now) {
                emitFlame(nextFlame, now, mouth, direction, false);
                nextFlame += 1.0D / 32;
            }
            while (nextSpec <= now) {
                emitFlame(nextSpec, now, mouth, direction, true);
                nextSpec += 1.0D / 16;
            }
            if (nextSupport <= now) {
                factory.emitSupportingParticles(level, mouth, direction.scale(ExpandingBreathSection.DEFAULT_SPEED));
                emitBackblast(mouth, partialTick);
                nextSupport += Math.floor(now - nextSupport) + 1;
            }
            lastMouth = mouth;
            lastDirection = direction;
        } else {
            lastMouth = null;
        }
        lastFrame = now;
        for (var iterator = flames.iterator(); iterator.hasNext();) {
            Flame flame = iterator.next();
            flame.particle.renderContinuous(buffer, camera, (float) Math.max(0, now - flame.birth));
            if (!flame.particle.isAlive()) iterator.remove();
        }
    }

    private void emitFlame(double birth, double now, Vec3 mouth, Vec3 direction, boolean spec) {
        double fraction = now > lastFrame ? Mth.clamp((birth - lastFrame) / (now - lastFrame), 0, 1) : 1;
        Vec3 origin = lastMouth.lerp(mouth, fraction);
        Vec3 forward = lastDirection.lerp(direction, fraction);
        if (forward.lengthSqr() < 1.0E-8) forward = direction;
        Vec3 velocity = forward.normalize().scale(ExpandingBreathSection.DEFAULT_SPEED);
        FireBreathParticle particle = new FireBreathParticle(level, origin.x, origin.y, origin.z,
                velocity, data, sprites, 0, !spec && flameIndex++ % 3 == 0, spec);
        flames.add(new Flame(particle, birth));
    }

    private void emitBackblast(Vec3 mouth, float partialTick) {
        Vec3 forward = Vec3.directionFromRotation(0, Mth.rotLerp(partialTick, dragon.yBodyRotO, dragon.yBodyRot));
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        for (int side : new int[] {-1, 1}) {
            Vec3 origin = mouth.subtract(forward.scale(0.35D)).add(right.scale(side * 0.75D));
            Vec3 velocity = forward.scale(-Math.cos(Math.PI / 6)).add(right.scale(side * Math.sin(Math.PI / 6)));
            Minecraft.getInstance().particleEngine.createParticle(ModParticles.FIRE_BREATH_BACKBLAST.get(),
                    origin.x, origin.y, origin.z, velocity.x, velocity.y, velocity.z);
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
